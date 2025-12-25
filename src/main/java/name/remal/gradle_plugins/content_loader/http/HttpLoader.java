package name.remal.gradle_plugins.content_loader.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.exists;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static name.remal.gradle_plugins.content_loader.http.ForceCache.FORCE_CACHE_CTX_ATTR;
import static name.remal.gradle_plugins.content_loader.http.HttpClientService.getHttpClientServiceProvider;
import static name.remal.gradle_plugins.content_loader.internal.ContentBuildCache.getContentBuildCacheFor;
import static name.remal.gradle_plugins.content_loader.internal.HostRateLimiter.getHostRateLimiterFor;
import static name.remal.gradle_plugins.content_loader.internal.LockUtils.getLockFilePath;
import static name.remal.gradle_plugins.content_loader.internal.LockUtils.withExclusiveLock;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PropertiesUtils.storeProperties;
import static name.remal.gradle_plugins.toolkit.ProxyUtils.toDynamicInterface;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsPredicate;
import static org.apache.hc.core5.http.HttpHeaders.CACHE_CONTROL;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.content_loader.content.Content;
import name.remal.gradle_plugins.content_loader.internal.ContentBuildCache;
import name.remal.gradle_plugins.content_loader.internal.FileContent;
import name.remal.gradle_plugins.content_loader.internal.HostRateLimiter;
import name.remal.gradle_plugins.content_loader.internal.StreamingContent;
import name.remal.gradle_plugins.toolkit.PathUtils;
import name.remal.gradle_plugins.toolkit.PropertiesUtils;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.gradle.api.Action;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Internal;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("try")
public abstract class HttpLoader implements Serializable {

    public static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofMinutes(1);


    private final transient HostRateLimiter hostRateLimiter = getHostRateLimiterFor(getGradle());
    private final transient ContentBuildCache contentBuildCache = getContentBuildCacheFor(getGradle());

    private final transient Provider<HttpClientService> httpClientServiceProvider =
        getHttpClientServiceProvider(getGradle());


    @Internal
    @org.gradle.api.tasks.Optional
    public abstract Property<Duration> getDefaultResponseTimeout();

    {
        getDefaultResponseTimeout().convention(DEFAULT_RESPONSE_TIMEOUT);
    }


    public Provider<Content> load(Action<? super HttpLoadParams> paramsConfigurer) {
        var params = getObjects().newInstance(HttpLoadParams.class);
        paramsConfigurer.execute(params);

        var provider = getObjects().property(Content.class);
        provider.value(getProviders().provider(() ->
            load(params)
        ));
        provider.finalizeValueOnRead();
        return provider;
    }

    private Content load(HttpLoadParams params) {
        params.finalizeValue();
        params.validate();

        final var buildCacheFilePath = params.getBuildCacheFile().getAsFile()
            .map(File::toPath)
            .map(PathUtils::normalizePath)
            .getOrNull();
        if (buildCacheFilePath == null) {
            return loadImpl(params);
        }

        final var buildCacheMetadataFilePath = params.getBuildCacheMetadataFile().getAsFile()
            .map(File::toPath)
            .map(PathUtils::normalizePath)
            .getOrNull();

        return requireNonNull(withExclusiveLock(getLockFilePath(buildCacheFilePath), () -> {
            if (exists(buildCacheFilePath)) {
                var metadata = Optional.ofNullable(buildCacheMetadataFilePath)
                    .filter(sneakyThrowsPredicate(Files::exists))
                    .map(PropertiesUtils::loadProperties)
                    .orElse(null);
                var source = Optional.ofNullable(metadata)
                    .map(props -> props.getProperty("source"))
                    .map(String::trim)
                    .filter(not(String::isEmpty))
                    .orElseGet(buildCacheFilePath::toString);
                var charset = Optional.ofNullable(metadata)
                    .map(props -> props.getProperty("charset"))
                    .map(String::trim)
                    .filter(not(String::isEmpty))
                    .map(Charset::forName)
                    .orElse(null);
                return new FileContent(source, buildCacheFilePath, charset);
            }


            var loadedContent = loadImpl(params);


            if (buildCacheMetadataFilePath != null) {
                var metadata = new LinkedHashMap<String, @Nullable String>();
                metadata.put("source", loadedContent.getSource());
                metadata.put("charset", Optional.ofNullable(loadedContent.getCharset())
                    .map(Charset::name)
                    .orElse(null)
                );
                storeProperties(metadata, buildCacheMetadataFilePath);
            }

            createParentDirectories(buildCacheFilePath);
            try (var in = loadedContent.openInputStream()) {
                copy(in, buildCacheFilePath);
            }

            return new FileContent(
                loadedContent.getSource(),
                buildCacheFilePath,
                loadedContent.getCharset()
            );
        }));
    }

    @SneakyThrows
    private Content loadImpl(HttpLoadParams params) {
        var untypedContent = contentBuildCache.getOrLoadContent(params.toMap(), () -> {
            var contentRef = new AtomicReference<@Nullable StreamingContent>();
            hostRateLimiter.withPermit(params.getUri().get(), () -> {
                var content = loadImplCached(params);
                contentRef.set(content);
            });
            return requireNonNull(contentRef.get());
        });
        var content = untypedContent instanceof Content typedContent
            ? typedContent
            : toDynamicInterface(untypedContent, Content.class);
        return content;
    }

    @SneakyThrows
    @SuppressWarnings("java:S3776")
    private StreamingContent loadImplCached(HttpLoadParams params) {
        final var ctx = HttpClientContext.create();

        var forcedCacheDuration = params.getForcedCacheDuration().getOrNull();
        if (forcedCacheDuration != null) {
            ctx.setAttribute(FORCE_CACHE_CTX_ATTR, forcedCacheDuration);
        }


        final var uri = params.getUriNormalized();
        final var host = HttpHost.create(uri);
        final var request = new HttpGet(uri);


        var username = params.getUsername().getOrNull();
        var token = params.getToken().getOrNull();
        if (username != null) {
            var auth = new BasicScheme();
            auth.initPreemptive(new UsernamePasswordCredentials(username, params.getPassword().get().toCharArray()));
            request.addHeader(
                params.getAuthorizationHeader().get(),
                auth.generateAuthResponse(host, request, ctx)
            );

        } else if (token != null) {
            request.addHeader(
                params.getAuthorizationHeader().get(),
                params.getTokenType().get() + ' ' + token
            );
        }


        params.getHeaders().get().forEach(header ->
            request.addHeader(header.getName().get(), header.getValue().getOrElse(""))
        );


        var responseTimeout = params.getResponseTimeout().orElse(getDefaultResponseTimeout()).get();
        request.setConfig(RequestConfig.custom()
            .setResponseTimeout(Timeout.of(responseTimeout))
            .build()
        );


        if (getGradle().getStartParameter().isOffline()) {
            request.addHeader(CACHE_CONTROL, "only-if-cached");
        }


        var client = httpClientServiceProvider.get().getClient();
        var response = client.executeOpen(host, request, ctx);
        var entity = response.getEntity();

        var contentType = ContentType.parseLenient(entity.getContentType());
        var charset = Optional.ofNullable(contentType)
            .map(ContentType::getCharset)
            .orElse(null);

        if (response.getCode() >= 400) {
            try {
                var isText = Optional.ofNullable(contentType)
                    .map(ContentType::getMimeType)
                    .map(String::toLowerCase)
                    .map(it -> it.startsWith("text/")
                        || Stream.of("xml, json, yaml, javascript, html, css", "markdown")
                        .anyMatch(suffix -> it.endsWith('/' + suffix) || it.endsWith('+' + suffix))
                    )
                    .orElse(false);

                var message = new StringBuilder();
                message
                    .append("Failed to load ").append(request.getMethod()).append(' ').append(uri)
                    .append(" : received status code ").append(response.getCode());
                if (isText) {
                    message.append(", response body:\n").append(EntityUtils.toString(entity, UTF_8));
                } else {
                    message.append(", response body is not textual");
                }
                throw new HttpLoadException(message.toString());

            } finally {
                EntityUtils.consume(entity);
                response.close();
            }
        }

        return new StreamingContent() {
            @Override
            @SneakyThrows
            public String getSource() {
                var builder = new URIBuilder(uri);
                builder.setUserInfo(null);
                return builder.build().toString();
            }

            @Override
            @SneakyThrows
            public InputStream getInputStream() {
                return entity.getContent();
            }

            @Override
            @Nullable
            public Charset getCharset() {
                return charset;
            }

            @Override
            public void close() throws Exception {
                EntityUtils.consume(entity);
                response.close();
            }
        };
    }


    @Inject
    protected abstract Gradle getGradle();

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

}

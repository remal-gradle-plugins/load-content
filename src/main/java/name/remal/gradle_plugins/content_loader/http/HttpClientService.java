package name.remal.gradle_plugins.content_loader.http;

import static java.lang.String.join;
import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.content_loader.http.Constants.PLUGIN_ID;

import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.content_loader.internal.ContentLoaderBuildService;
import org.apache.hc.client5.http.cache.HttpCacheStorage;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.cache.BasicHttpCacheStorage;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Internal;
import org.jspecify.annotations.Nullable;

@CustomLog
@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class HttpClientService implements ContentLoaderBuildService<HttpClientService.Parameters> {

    public static Provider<HttpClientService> getHttpClientServiceProvider(Gradle gradle) {
        var serviceName = join(
            "|",
            HttpClientService.class.getName(),
            String.valueOf(identityHashCode(HttpClientService.class)),
            Optional.ofNullable(HttpClientService.class.getClassLoader())
                .map(System::identityHashCode)
                .map(Object::toString)
                .orElse("")
        );
        var serviceProvider = gradle.getSharedServices().registerIfAbsent(
            serviceName,
            HttpClientService.class,
            spec -> {
                var gradleUserHomeDir = gradle.getGradleUserHomeDir();
                var cacheDir = new File(gradleUserHomeDir, PLUGIN_ID + "/cache/http");
                spec.getParameters().getCacheDir().fileValue(cacheDir);
                spec.getParameters().getOnlyIfCached().set(gradle.getStartParameter().isOffline());
            }
        );
        return serviceProvider;
    }


    public interface Parameters extends BuildServiceParameters {

        @Internal
        @org.gradle.api.tasks.Optional
        DirectoryProperty getCacheDir();

        @Internal
        @org.gradle.api.tasks.Optional
        Property<Boolean> getOnlyIfCached();

    }


    private static final long MAX_CACHE_OBJECT_SIZE_MB = 10;


    @Nullable
    private transient volatile CloseableHttpClient client;

    public HttpClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    var builder = CachingHttpClients.custom();
                    builder.useSystemProperties();
                    builder.disableCookieManagement();
                    builder.setUserAgent(PLUGIN_ID);

                    builder.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setConnectTimeout(Timeout.ofSeconds(10))
                            .build()
                        )
                        .setMaxConnTotal(0)
                        .setMaxConnPerRoute(0)
                        .build()
                    );

                    builder.setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                        .build()
                    );

                    builder.setRetryStrategy(CustomHttpRequestRetryStrategy.INSTANCE);

                    var cacheConfig = CacheConfig.custom()
                        .setSharedCache(false)
                        .setMaxObjectSize(MAX_CACHE_OBJECT_SIZE_MB * 1024 * 1024)
                        .setMaxCacheEntries(250)
                        .setMaxUpdateRetries(3)
                        .setHeuristicCachingEnabled(false)
                        .setAsynchronousWorkers(0)
                        .build();
                    builder.setCacheConfig(cacheConfig);

                    HttpCacheStorage cacheStorage;
                    var cacheDir = getParameters().getCacheDir().getAsFile().getOrNull();
                    if (cacheDir != null) {
                        cacheStorage = new CacheDirHttpCacheStorage(
                            cacheDir.toPath(),
                            cacheConfig.getMaxUpdateRetries()
                        );

                    } else {
                        builder.setResourceFactory(GzippedHeapResourceFactory.INSTANCE);
                        cacheStorage = new BasicHttpCacheStorage(cacheConfig);
                    }
                    var isOfflineOnly = getParameters().getOnlyIfCached().getOrElse(false);
                    if (isOfflineOnly) {
                        cacheStorage = new OfflineOnlyHttpCacheStorage(cacheStorage);
                    }
                    builder.setHttpCacheStorage(cacheStorage);

                    builder.addResponseInterceptorFirst(EtagHidesExpiration.INSTANCE);
                    builder.addResponseInterceptorFirst(ForceCache.INSTANCE);

                    client = builder.build();
                }
            }
        }

        return requireNonNull(client);
    }

}

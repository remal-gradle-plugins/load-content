package name.remal.gradle_plugins.content_loader.http;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static name.remal.gradle_plugins.toolkit.FileUtils.normalizeFile;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.unwrapProviders;
import static org.apache.hc.client5.http.auth.StandardAuthScheme.BEARER;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.content_loader.internal.HasConfigurableValues;
import org.apache.hc.client5.http.impl.cache.CacheKeyGenerator;
import org.gradle.api.Action;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.HasConfigurableValue;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.jspecify.annotations.Nullable;

public abstract class HttpLoadParams extends HasConfigurableValues {

    @Input
    public abstract Property<URI> getUri();

    /**
     * Supported value types:
     * <ul>
     *     <li>{@link URI}
     *     <li>{@link URL}
     *     <li>{@link CharSequence}
     *     <li>Providers of the above types: {@link Provider}, {@link Callable}, {@link Future}, etc.
     * </ul>
     */
    public void uri(Object untypedValue) {
        getUri().set(getProviders().provider(() -> {
            var value = unwrapProviders(untypedValue);
            return switch (value) {
                case null -> null;
                case URI it -> it;
                case URL it -> it.toURI();
                case CharSequence it -> URI.create(it.toString());
                default -> throw new IllegalArgumentException(format(
                    "Unsupported type for URI: %s",
                    value.getClass().getName()
                ));
            };
        }));
    }

    /**
     * @see #uri(Object)
     */
    public void url(Object untypedValue) {
        uri(untypedValue);
    }


    @Input
    public abstract Property<String> getAuthorizationHeader();

    {
        getAuthorizationHeader().convention(AUTHORIZATION);
    }

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getUsername();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getPassword();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getToken();

    @Input
    public abstract Property<String> getTokenType();

    {
        getTokenType().convention(BEARER);
    }


    @Nested
    public abstract ListProperty<HttpHeader> getHeaders();

    public void header(Action<? super HttpHeader> action) {
        var header = getObjects().newInstance(HttpHeader.class);
        action.execute(header);
        getHeaders().add(header);
    }

    public void header(CharSequence name, CharSequence value) {
        header(it -> {
            it.getName().set(name.toString());
            it.getValue().set(value.toString());
        });
    }

    public void header(Provider<? extends CharSequence> name, Provider<? extends CharSequence> value) {
        header(it -> {
            it.getName().set(name.map(CharSequence::toString));
            it.getValue().set(value.map(CharSequence::toString));
        });
    }


    @Internal
    @org.gradle.api.tasks.Optional
    public abstract Property<Duration> getResponseTimeout();


    @Internal
    @org.gradle.api.tasks.Optional
    public abstract Property<Duration> getForcedCacheDuration();


    @Internal
    @org.gradle.api.tasks.Optional
    public abstract RegularFileProperty getBuildCacheFile();

    @Internal
    @org.gradle.api.tasks.Optional
    public abstract RegularFileProperty getBuildCacheMetadataFile();

    {
        var regularFileProvider = getObjects().fileProperty().fileProvider(getProviders().provider(() -> {
            var cacheFile = getBuildCacheFile().getAsFile().getOrNull();
            if (cacheFile == null) {
                return null;
            }

            cacheFile = normalizeFile(cacheFile);
            return new File(cacheFile + ".metadata.properties");
        }));
        getBuildCacheMetadataFile().convention(regularFileProvider);
    }


    @Override
    @OverridingMethodsMustInvokeSuper
    protected Stream<HasConfigurableValue> streamConfigurableValues() {
        return Stream.of(
            getUri(),
            getAuthorizationHeader(),
            getUsername(),
            getPassword(),
            getToken(),
            getTokenType(),
            getHeaders(),
            getResponseTimeout(),
            getForcedCacheDuration(),
            getBuildCacheFile(),
            getBuildCacheMetadataFile()
        );
    }

    @Internal
    @SneakyThrows
    protected final URI getUriNormalized() {
        return CacheKeyGenerator.normalize(getUri().get());
    }

    protected void validate() {
        // required properties:
        getUri().get();

        // basic auth:
        var username = getUsername().getOrNull();
        var password = getPassword().getOrNull();
        if (username != null) {
            if (password == null) {
                throw new IllegalStateException("Username is set, but password is not");
            }
            // required properties:
            getAuthorizationHeader().get();
            getTokenType().get();
        } else if (password != null) {
            throw new IllegalStateException("Username is not set, but password is");
        }

        // token auth:
        var token = getToken().getOrNull();
        if (token != null) {
            if (username != null) {
                throw new IllegalStateException("Both username/password and token are set");
            }
            // required properties:
            getAuthorizationHeader().get();
            getTokenType().get();
        }

        // recursive validation:
        getHeaders().get().forEach(HttpHeader::validate);
    }

    protected Map<String, @Nullable Object> toMap() {
        var map = new LinkedHashMap<String, @Nullable Object>();
        map.put("uri", getUriNormalized());
        map.put("authorizationHeader", getAuthorizationHeader().getOrNull());
        map.put("username", getUsername().getOrNull());
        map.put("password", getPassword().getOrNull());
        map.put("tokenType", getTokenType().getOrNull());
        map.put("token", getToken().getOrNull());

        var buildCacheKeyHeaders = new ArrayList<>();
        map.put("headers", buildCacheKeyHeaders);
        getHeaders().get().forEach(header -> {
            buildCacheKeyHeaders.add(header.toMap());
        });

        return map;
    }


    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    protected abstract ObjectFactory getObjects();

}

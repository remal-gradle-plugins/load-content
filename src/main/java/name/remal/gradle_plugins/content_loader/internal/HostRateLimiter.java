package name.remal.gradle_plugins.content_loader.internal;

import static name.remal.gradle_plugins.content_loader.internal.SharedServices.getBuildService;

import java.net.URI;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.impl.cache.CacheKeyGenerator;
import org.apache.hc.core5.http.HttpHost;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildServiceParameters;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface HostRateLimiter extends ContentLoaderBuildService<HostRateLimiter.Parameters> {

    static HostRateLimiter getHostRateLimiterFor(Gradle gradle) {
        return getBuildService(gradle, HostRateLimiter.class, HostRateLimiterImpl.class);
    }


    void withPermit(String host, Runnable action);

    @SneakyThrows
    default void withPermit(URI uri, Runnable action) {
        uri = CacheKeyGenerator.normalize(uri);
        var hostName = HttpHost.create(uri).getHostName();
        withPermit(hostName, action);
    }


    abstract class Parameters implements BuildServiceParameters {

        public abstract Property<Integer> getMaxParallelRequestPerHost();

        {
            getMaxParallelRequestPerHost().convention(4);
        }

    }

}

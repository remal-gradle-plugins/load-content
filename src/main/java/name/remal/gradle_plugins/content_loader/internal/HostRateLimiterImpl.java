package name.remal.gradle_plugins.content_loader.internal;

import static lombok.AccessLevel.PUBLIC;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
public abstract class HostRateLimiterImpl implements HostRateLimiter {

    private final transient ConcurrentMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public void withPermit(String host, Runnable action) {
        var semaphore = semaphores.computeIfAbsent(host, __ ->
            new Semaphore(getParameters().getMaxParallelRequestPerHost().get())
        );
        semaphore.acquire();
        try {
            action.run();
        } finally {
            semaphore.release();
        }
    }

}

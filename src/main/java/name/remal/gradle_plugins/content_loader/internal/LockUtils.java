package name.remal.gradle_plugins.content_loader.internal;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static lombok.AccessLevel.PRIVATE;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
@NoArgsConstructor(access = PRIVATE)
public abstract class LockUtils {

    private static final int MAX_LOCK_ATTEMPTS = 100;

    @Nullable
    @SneakyThrows
    public static <T> T withExclusiveLock(Path lockPath, Callable<@Nullable T> action) {
        createDirectories(lockPath.getParent());

        try (var ch = FileChannel.open(lockPath, CREATE, WRITE)) {
            FileLock lock = null;
            try {
                for (var attempt = 1; attempt <= MAX_LOCK_ATTEMPTS; attempt++) {
                    try {
                        lock = ch.tryLock(); // non-blocking
                        if (lock != null) {
                            break;
                        }
                    } catch (OverlappingFileLockException e) {
                        // Same-JVM overlap => retry
                    }

                    if (attempt >= MAX_LOCK_ATTEMPTS) {
                        throw new IllegalStateException("Failed to acquire lock on " + lockPath);
                    }

                    var sleepMillis = 50L * attempt;
                    Thread.sleep(sleepMillis);
                }

                return action.call();

            } finally {
                if (lock != null && lock.isValid()) {
                    lock.release();
                }
            }
        }
    }

    public static Path getLockFilePath(Path basePath) {
        return basePath.resolveSibling(basePath.getFileName() + ".lock");
    }

}

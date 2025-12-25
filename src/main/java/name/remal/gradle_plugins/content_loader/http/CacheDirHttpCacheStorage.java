package name.remal.gradle_plugins.content_loader.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.move;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.content_loader.http.Constants.HTTP_CLIENT_CACHE_VERSION;
import static name.remal.gradle_plugins.content_loader.internal.LockUtils.withExclusiveLock;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;
import static org.apache.hc.client5.http.utils.Hex.encodeHexString;

import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.hc.client5.http.impl.cache.AbstractBinaryCacheStorage;
import org.apache.hc.client5.http.impl.cache.HttpByteArrayCacheEntrySerializer;
import org.jspecify.annotations.Nullable;

public class CacheDirHttpCacheStorage
    extends AbstractBinaryCacheStorage<CacheDirHttpCacheStorage.CasValue> {

    private final Path rootDir;

    @SneakyThrows
    public CacheDirHttpCacheStorage(Path cacheRootDir, int maxUpdateRetries) {
        super(maxUpdateRetries, HttpByteArrayCacheEntrySerializer.INSTANCE);
        this.rootDir = normalizePath(cacheRootDir.resolve(HTTP_CLIENT_CACHE_VERSION));
    }

    @Override
    @SneakyThrows
    protected String digestToStorageKey(String key) {
        var md = MessageDigest.getInstance("SHA-256");
        var digest = md.digest(key.getBytes(UTF_8));
        return encodeHexString(digest);
    }

    @Override
    @SneakyThrows
    protected void store(String storageKey, byte[] storageObject) {
        var paths = pathsFor(storageKey);
        withExclusiveLock(paths.getLockPath(), () -> {
            createDirectories(paths.getDir());

            var newVersion = 1L;
            atomicWrite(paths.getBinPath(), storageObject);
            atomicWrite(paths.getVersionPath(), encodeVersion(newVersion));

            return null;
        });
    }

    @Override
    @SneakyThrows
    protected byte @Nullable [] restore(String storageKey) {
        var paths = pathsFor(storageKey);
        return withExclusiveLock(paths.getLockPath(), () -> {
            if (!exists(paths.getBinPath()) || !exists(paths.getVersionPath())) {
                return null;
            }

            return readAllBytes(paths.getBinPath());
        });
    }

    @Override
    @SneakyThrows
    protected @Nullable CasValue getForUpdateCAS(String storageKey) {
        var paths = pathsFor(storageKey);
        return withExclusiveLock(paths.getLockPath(), () -> {
            if (!exists(paths.getBinPath()) || !exists(paths.getVersionPath())) {
                return null;
            }

            var version = decodeVersion(readAllBytes(paths.getVersionPath()));
            var bytes = readAllBytes(paths.getBinPath());
            return CasValue.builder()
                .version(version)
                .bytes(bytes)
                .build();
        });
    }

    @Override
    protected byte @Nullable [] getStorageObject(@Nullable CasValue cas) {
        return cas != null ? cas.getBytes() : null;
    }

    @Override
    @SneakyThrows
    protected boolean updateCAS(String storageKey, @Nullable CasValue cas, byte[] storageObject) {
        var paths = pathsFor(storageKey);
        return requireNonNull(withExclusiveLock(paths.getLockPath(), () -> {
            if (!exists(paths.getBinPath()) || !exists(paths.getVersionPath())) {
                return false;
            }

            var currentVersion = decodeVersion(readAllBytes(paths.getVersionPath()));
            if (cas == null || cas.getVersion() != currentVersion) {
                return false;
            }

            var newVersion = currentVersion + 1;
            atomicWrite(paths.getBinPath(), storageObject);
            atomicWrite(paths.getVersionPath(), encodeVersion(newVersion));
            return true;
        }));
    }

    @Override
    @SneakyThrows
    protected void delete(String storageKey) {
        var paths = pathsFor(storageKey);
        withExclusiveLock(paths.getLockPath(), () -> {
            deleteIfExists(paths.getBinPath());
            deleteIfExists(paths.getVersionPath());
            return null;
        });
    }

    @Override
    @SneakyThrows
    protected Map<String, byte[]> bulkRestore(Collection<String> storageKeys) {
        var result = new LinkedHashMap<String, byte[]>();
        for (var storageKey : storageKeys) {
            var bytes = restore(storageKey);
            if (bytes != null) {
                result.put(storageKey, bytes);
            }
        }
        return result;
    }

    @SneakyThrows
    private static void atomicWrite(Path target, byte[] data) {
        var dir = target.getParent();
        createDirectories(dir);

        var tmp = createTempFile(dir, target.getFileName().toString(), ".tmp");
        try {
            write(tmp, data, TRUNCATE_EXISTING);
            try {
                move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                move(tmp, target, REPLACE_EXISTING);
            }
        } finally {
            deleteIfExists(tmp);
        }
    }

    private static byte[] encodeVersion(long v) {
        return ByteBuffer.allocate(Long.BYTES).putLong(v).array();
    }

    private static long decodeVersion(byte[] b) {
        if (b.length != Long.BYTES) {
            return 0L;
        }

        return ByteBuffer.wrap(b).getLong();
    }

    private PathsForKey pathsFor(String storageKey) {
        var p1 = storageKey.substring(0, 2);
        var p2 = storageKey.substring(2, 4);
        var dir = rootDir.resolve(p1).resolve(p2);
        return PathsForKey.builder()
            .dir(dir)
            .binPath(dir.resolve(storageKey + ".bin"))
            .versionPath(dir.resolve(storageKey + ".version"))
            .lockPath(dir.resolve(storageKey + ".lock"))
            .build();
    }


    @Value
    @Builder
    protected static class CasValue {
        long version;
        byte @Nullable [] bytes;
    }

    @Value
    @Builder
    private static class PathsForKey {
        Path dir;
        Path binPath;
        Path versionPath;
        Path lockPath;
    }

}

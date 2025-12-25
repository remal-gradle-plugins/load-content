package name.remal.gradle_plugins.content_loader.http;

import static java.lang.System.nanoTime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class CacheDirHttpCacheStorageTest {

    CacheDirHttpCacheStorage storage;

    String key1;
    String key2;
    String key3;

    @BeforeEach
    void beforeEach(@TempDir(cleanup = CleanupMode.ALWAYS) Path root) {
        storage = new CacheDirHttpCacheStorage(root, 3);

        key1 = storage.digestToStorageKey("k1");
        key2 = storage.digestToStorageKey("k2");
        key3 = storage.digestToStorageKey("k3");
    }


    @Test
    void restoreMissing() {
        assertNull(storage.restore(storage.digestToStorageKey("missing")));
    }

    @Test
    void storeThenRestore() {
        storage.store(key1, "hello".getBytes(UTF_8));
        assertArrayEquals("hello".getBytes(UTF_8), storage.restore(key1));
    }

    @Test
    void deleteRemovesEntry() {
        storage.store(key1, "x".getBytes(UTF_8));
        assertNotNull(storage.restore(key1));

        storage.delete(key1);
        assertNull(storage.restore(key1));
    }

    @Test
    void casUpdateSuccessIncrementsVersion() {
        storage.store(key1, "v1".getBytes(UTF_8));

        var cas1 = storage.getForUpdateCAS(key1);
        assertNotNull(cas1);
        assertEquals(1L, cas1.getVersion());
        assertArrayEquals("v1".getBytes(UTF_8), cas1.getBytes());

        assertTrue(storage.updateCAS(key1, cas1, "v2".getBytes(UTF_8)));
        assertArrayEquals("v2".getBytes(UTF_8), storage.restore(key1));

        var cas2 = storage.getForUpdateCAS(key1);
        assertNotNull(cas2);
        assertEquals(2L, cas2.getVersion());
    }

    @Test
    void casUpdateFailsOnStaleCas() {
        storage.store(key1, "v1".getBytes(UTF_8));

        var cas1 = storage.getForUpdateCAS(key1);
        assertNotNull(cas1);

        assertTrue(storage.updateCAS(key1, cas1, "v2".getBytes(UTF_8)));
        assertFalse(storage.updateCAS(key1, cas1, "v3".getBytes(UTF_8)));

        assertArrayEquals("v2".getBytes(UTF_8), storage.restore(key1));
    }

    @Test
    void bulkRestoreReturnsOnlyExistingKeys() {
        storage.store(key1, "a".getBytes(UTF_8));
        storage.store(key2, "b".getBytes(UTF_8));

        var map = storage.bulkRestore(List.of(key1, key2, key3));
        assertEquals(2, map.size());
        assertArrayEquals("a".getBytes(UTF_8), map.get(key1));
        assertArrayEquals("b".getBytes(UTF_8), map.get(key2));
        assertNull(map.get(key3));
    }

    @Test
    void concurrentUpdatesSameKeyAreSerializedAndNoLostUpdates() throws Exception {
        storage.store(key1, longToBytes(0));

        var threads = 8;
        var itersPerThread = 100;
        var start = new CyclicBarrier(threads);

        try (var pool = newFixedThreadPool(threads)) {
            var futures = new ArrayList<Future<?>>(threads);
            for (var t = 0; t < threads; t++) {
                futures.add(pool.submit((Callable<Void>) () -> {
                    start.await();

                    for (var i = 0; i < itersPerThread; i++) {
                        while (true) {
                            var deadline = nanoTime() + SECONDS.toNanos(5);
                            var cas = storage.getForUpdateCAS(key1);
                            assertNotNull(cas);

                            var current = bytesToLong(cas.getBytes());
                            var next = current + 1;

                            if (storage.updateCAS(key1, cas, longToBytes(next))) {
                                break;
                            }

                            if (nanoTime() >= deadline) {
                                fail("CAS didn't succeed within 5s (broken CAS or deadlock)");
                            }
                        }
                    }

                    return null;
                }));
            }

            for (var future : futures) {
                future.get(30, SECONDS);
            }
        }

        var finalBytes = storage.restore(key1);
        assertNotNull(finalBytes);

        var expected = (long) threads * itersPerThread;
        assertEquals(expected, bytesToLong(finalBytes));
    }

    private static byte[] longToBytes(long v) {
        return ByteBuffer.allocate(Long.BYTES).putLong(v).array();
    }

    private static long bytesToLong(byte @Nullable [] b) {
        assertNotNull(b);
        assertEquals(Long.BYTES, b.length);
        return ByteBuffer.wrap(b).getLong();
    }

}

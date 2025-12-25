package name.remal.gradle_plugins.content_loader.http;

import static org.apache.hc.client5.http.utils.DateUtils.formatStandardDate;
import static org.apache.hc.core5.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.hc.core5.http.HttpHeaders.DATE;
import static org.apache.hc.core5.http.HttpHeaders.EXPIRES;
import static org.apache.hc.core5.http.HttpHeaders.PRAGMA;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.cache.HttpCacheCASOperation;
import org.apache.hc.client5.http.cache.HttpCacheEntry;
import org.apache.hc.client5.http.cache.HttpCacheStorage;
import org.apache.hc.client5.http.cache.HttpCacheUpdateException;
import org.apache.hc.client5.http.cache.ResourceIOException;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.HeaderGroup;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
class OfflineOnlyHttpCacheStorage implements HttpCacheStorage {

    private static final Duration REQUEST_RESPONSE_TIMESTAMP_ADJUSTER = Duration.ofDays(1);

    private final HttpCacheStorage delegate;

    @Nullable
    @Contract(value = "null->null; !null->!null")
    private HttpCacheEntry processEntry(@Nullable HttpCacheEntry entry) {
        if (entry == null) {
            return entry;
        }

        final var requestResponseTimestamp = Instant.now().minus(REQUEST_RESPONSE_TIMESTAMP_ADJUSTER);

        final var requestHeaders = new HeaderGroup();
        entry.requestHeaders().headerIterator().forEachRemaining(requestHeaders::addHeader);

        final var responseHeaders = new HeaderGroup();
        entry.responseHeaders().headerIterator().forEachRemaining(responseHeaders::addHeader);
        responseHeaders.removeHeaders(DATE);
        responseHeaders.removeHeaders(EXPIRES);
        responseHeaders.removeHeaders(PRAGMA);
        responseHeaders.removeHeaders(CACHE_CONTROL);
        responseHeaders.addHeader(new BasicHeader(DATE, formatStandardDate(requestResponseTimestamp)));
        responseHeaders.addHeader(new BasicHeader(CACHE_CONTROL, "max-age=" + Integer.MAX_VALUE));

        return new HttpCacheEntry(
            requestResponseTimestamp,
            requestResponseTimestamp,
            entry.getRequestMethod(),
            entry.getRequestURI(),
            requestHeaders,
            entry.getStatus(),
            responseHeaders,
            entry.getResource(),
            entry.getVariants()
        );
    }


    @Override
    public void putEntry(String key, HttpCacheEntry entry) throws ResourceIOException {
        delegate.putEntry(key, entry);
    }

    @Override
    @Nullable
    public HttpCacheEntry getEntry(String key) throws ResourceIOException {
        return processEntry(delegate.getEntry(key));
    }

    @Override
    public void removeEntry(String key) throws ResourceIOException {
        delegate.removeEntry(key);
    }

    @Override
    public void updateEntry(
        String key,
        HttpCacheCASOperation casOperation
    ) throws ResourceIOException, HttpCacheUpdateException {
        delegate.updateEntry(key, casOperation);
    }

    @Override
    public Map<String, HttpCacheEntry> getEntries(Collection<String> keys) throws ResourceIOException {
        var result = new LinkedHashMap<String, HttpCacheEntry>();
        delegate.getEntries(keys).forEach((key, entry) -> {
            if (key != null && entry != null) {
                result.put(key, processEntry(entry));
            }
        });
        return result;
    }

}

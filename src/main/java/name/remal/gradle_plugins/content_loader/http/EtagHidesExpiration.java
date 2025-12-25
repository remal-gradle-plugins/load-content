package name.remal.gradle_plugins.content_loader.http;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.apache.hc.core5.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.hc.core5.http.HttpHeaders.ETAG;
import static org.apache.hc.core5.http.HttpHeaders.EXPIRES;
import static org.apache.hc.core5.http.HttpHeaders.LAST_MODIFIED;

import com.google.common.base.Splitter;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jspecify.annotations.Nullable;

enum EtagHidesExpiration implements HttpResponseInterceptor {

    INSTANCE;

    @Override
    public void process(HttpResponse response, @Nullable EntityDetails entity, @Nullable HttpContext context) {
        var etag = response.getLastHeader(ETAG);
        if (etag == null || etag.getValue().isEmpty()) {
            return;
        }

        for (var header : response.getHeaders(LAST_MODIFIED)) {
            response.setHeader("X-" + header.getName(), header.getValue());
        }
        response.removeHeaders(LAST_MODIFIED);

        for (var header : response.getHeaders(EXPIRES)) {
            response.setHeader("X-" + header.getName(), header.getValue());
        }
        response.removeHeaders(EXPIRES);

        var cacheControlHeaders = response.getHeaders(CACHE_CONTROL);
        response.removeHeaders(CACHE_CONTROL);
        for (var header : cacheControlHeaders) {
            var newValue = Splitter.on(',').splitToStream(header.getValue())
                .map(String::trim)
                .filter(not(String::isEmpty))
                .filter(value -> !value.toLowerCase().startsWith("max-age"))
                .filter(value -> !value.toLowerCase().startsWith("s-maxage"))
                .collect(joining(", "));
            if (!newValue.isEmpty()) {
                response.addHeader(header.getName(), newValue);
            }
        }
    }

}

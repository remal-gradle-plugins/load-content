package name.remal.gradle_plugins.content_loader.http;

import static org.apache.hc.core5.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.hc.core5.http.HttpHeaders.EXPIRES;
import static org.apache.hc.core5.http.HttpHeaders.PRAGMA;

import java.time.Duration;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

enum ForceCache implements HttpResponseInterceptor {

    INSTANCE;


    public static final String FORCE_CACHE_CTX_ATTR = ForceCache.class.getName();

    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) {
        var forceCacheAttr = context.getAttribute(FORCE_CACHE_CTX_ATTR);
        if (forceCacheAttr instanceof Duration duration) {
            var durationSecs = duration.toSeconds();
            if (durationSecs > 0) {
                response.removeHeaders(EXPIRES);
                response.removeHeaders(PRAGMA);
                response.removeHeaders(CACHE_CONTROL);

                response.setHeader(CACHE_CONTROL, "max-age=" + durationSecs);
            }
        }
    }

}

package name.remal.gradle_plugins.content_loader.http;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.jspecify.annotations.Nullable;

class CustomHttpRequestRetryStrategy extends DefaultHttpRequestRetryStrategy {

    private static final TimeValue DEFAULT_RETRY_INTERVAL = TimeValue.ofSeconds(5);
    private static final TimeValue MAX_RETRY_INTERVAL = TimeValue.ofMinutes(1);

    public static final CustomHttpRequestRetryStrategy INSTANCE = new CustomHttpRequestRetryStrategy();

    private CustomHttpRequestRetryStrategy() {
        super(3, DEFAULT_RETRY_INTERVAL);
    }

    @Override
    public TimeValue getRetryInterval(HttpResponse response, int execCount, @Nullable HttpContext context) {
        var interval = super.getRetryInterval(response, execCount, context);
        if (interval.compareTo(MAX_RETRY_INTERVAL) > 0) {
            interval = MAX_RETRY_INTERVAL;
        }
        return interval;
    }

}

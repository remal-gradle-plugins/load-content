package name.remal.gradle_plugins.content_loader.http;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import lombok.Value;
import org.apache.hc.client5.http.cache.Resource;
import org.apache.hc.client5.http.cache.ResourceIOException;
import org.jspecify.annotations.Nullable;

class GzippedHeapResource extends Resource {

    @Serial
    private static final long serialVersionUID = 1L;


    private final AtomicReference<@Nullable GZippedContent> gzippedContent = new AtomicReference<>();

    public GzippedHeapResource(long contentLength, byte @Nullable [] gzippedContent) {
        super();
        if (gzippedContent != null) {
            this.gzippedContent.set(new GZippedContent(contentLength, gzippedContent));
        }
    }


    @Override
    @MustBeClosed
    public InputStream getInputStream() throws ResourceIOException {
        var gzippedContent = this.gzippedContent.get();
        if (gzippedContent == null) {
            throw new ResourceIOException("Resource already disposed");
        }

        var bytesIn = new ByteArrayInputStream(gzippedContent.getGzippedContent());
        try {
            return new GZIPInputStream(bytesIn);
        } catch (IOException e) {
            throw new ResourceIOException("Ungzipping exception", e);
        }
    }

    @Override
    public byte[] get() throws ResourceIOException {
        var byteOut = new ByteArrayOutputStream();
        try (var in = getInputStream()) {
            in.transferTo(byteOut);
            return byteOut.toByteArray();
        } catch (IOException e) {
            throw new ResourceIOException("Ungzipping exception", e);
        }
    }

    @Override
    public long length() {
        var content = this.gzippedContent.get();
        return content != null ? content.getContentLength() : -1;
    }

    @Override
    public void dispose() {
        gzippedContent.set(null);
    }


    @Value
    @SuppressWarnings("java:S1700")
    private static class GZippedContent {
        long contentLength;
        byte[] gzippedContent;
    }

}

package name.remal.gradle_plugins.content_loader.http;

import static lombok.AccessLevel.PRIVATE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import lombok.NoArgsConstructor;
import org.apache.hc.client5.http.cache.Resource;
import org.apache.hc.client5.http.cache.ResourceFactory;
import org.apache.hc.client5.http.cache.ResourceIOException;

@NoArgsConstructor(access = PRIVATE)
class GzippedHeapResourceFactory implements ResourceFactory {

    public static final GzippedHeapResourceFactory INSTANCE = new GzippedHeapResourceFactory();

    @Override
    public Resource generate(String requestId, byte[] content) throws ResourceIOException {
        var bytesOut = new ByteArrayOutputStream();
        try (var gzipOut = new GZIPOutputStream(bytesOut)) {
            gzipOut.write(content);
        } catch (IOException e) {
            throw new ResourceIOException("GZipping exception", e);
        }
        return new GzippedHeapResource(content.length, bytesOut.toByteArray());
    }

    @Override
    public Resource generate(String requestId, byte[] content, int off, int len) throws ResourceIOException {
        var bytesOut = new ByteArrayOutputStream();
        try (var gzipOut = new GZIPOutputStream(bytesOut)) {
            gzipOut.write(content, off, len);
        } catch (IOException e) {
            throw new ResourceIOException("GZipping exception", e);
        }
        return new GzippedHeapResource(content.length, bytesOut.toByteArray());
    }

    @Override
    @SuppressWarnings("deprecation")
    public Resource copy(String requestId, Resource resource) {
        return resource;
    }

}

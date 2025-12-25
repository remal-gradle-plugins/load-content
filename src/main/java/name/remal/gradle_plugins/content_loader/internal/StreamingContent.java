package name.remal.gradle_plugins.content_loader.internal;

import java.io.InputStream;
import java.nio.charset.Charset;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
@SuppressWarnings("try")
public interface StreamingContent extends AutoCloseable {

    String getSource();

    InputStream getInputStream();

    @Nullable
    Charset getCharset();

}

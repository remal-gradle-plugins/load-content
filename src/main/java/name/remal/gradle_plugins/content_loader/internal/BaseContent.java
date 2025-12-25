package name.remal.gradle_plugins.content_loader.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNullElse;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public interface BaseContent extends Serializable {

    String getSource();

    @MustBeClosed
    InputStream openInputStream();

    @Nullable
    Charset getCharset();

    @MustBeClosed
    @SuppressWarnings("MustBeClosedChecker")
    default Reader openReader() {
        return new InputStreamReader(openInputStream(), requireNonNullElse(getCharset(), UTF_8));
    }

    @SneakyThrows
    default String asString() {
        var stringWriter = new StringWriter();
        try (var reader = openReader()) {
            reader.transferTo(stringWriter);
        }
        return stringWriter.toString();
    }

}

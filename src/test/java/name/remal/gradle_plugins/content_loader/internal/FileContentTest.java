package name.remal.gradle_plugins.content_loader.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.deserializeFrom;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.serializeToBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FileContentTest {

    @Test
    void serialization() {
        var original = new FileContent(
            "source",
            Path.of("path/to/file.txt"),
            UTF_8
        );

        var bytes = serializeToBytes(original);
        var deserialized = deserializeFrom(bytes, FileContent.class);

        assertEquals(original.getSource(), deserialized.getSource());
        assertEquals(original.getContentFilePath().toUri(), deserialized.getContentFilePath().toUri());
        assertEquals(original.getCharset(), deserialized.getCharset());
    }

}

package name.remal.gradle_plugins.content_loader.internal;

import static java.nio.file.Files.newInputStream;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.content_loader.content.Content;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
@RequiredArgsConstructor
@Getter
@NoArgsConstructor(access = PRIVATE, force = true)
@SuppressWarnings("java:S1948")
public final class FileContent extends AbstractContent implements Content {

    private final String source;

    private final Path contentFilePath;

    @Nullable
    private final Charset charset;


    @Override
    @SneakyThrows
    public InputStream openInputStream() {
        return newInputStream(contentFilePath);
    }


    //#region Serialization

    @Serial
    private Object writeReplace() throws ObjectStreamException {
        return new SerializedForm(
            source,
            contentFilePath.toUri(),
            charset != null ? charset.name() : null
        );
    }

    @RequiredArgsConstructor
    @NoArgsConstructor(access = PRIVATE, force = true)
    private static final class SerializedForm implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String source;
        private final URI contentFilePath;
        private final @Nullable String charset;

        @Serial
        private Object readResolve() {
            return new FileContent(
                requireNonNull(source),
                Paths.get(requireNonNull(contentFilePath)),
                charset != null ? Charset.forName(charset) : null
            );
        }

    }

    //#endregion

}

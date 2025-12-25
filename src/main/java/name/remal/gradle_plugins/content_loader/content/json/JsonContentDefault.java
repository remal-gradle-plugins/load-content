package name.remal.gradle_plugins.content_loader.content.json;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;
import static name.remal.gradle_plugins.content_loader.internal.JacksonUtils.JSON_MAPPER;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;

import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.Charset;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.content_loader.content.Content;
import name.remal.gradle_plugins.content_loader.internal.AbstractContent;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.BinaryNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

@ApiStatus.Internal
@AllArgsConstructor(access = PROTECTED)
@SuppressWarnings("java:S1948")
public final class JsonContentDefault extends AbstractContent implements JsonContent {

    private final String source;

    @VisibleForTesting
    @Getter(PACKAGE)
    private final JsonNode node;

    public JsonContentDefault(Content content) {
        this(content.getSource(), parseContent(content));
    }

    private static JsonNode parseContent(Content content) {
        try (var reader = content.openReader()) {
            return JSON_MAPPER.readTree(reader);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(
                format(
                    "Can't parse JSON content from %s",
                    content.getSource()
                ),
                e
            );
        }
    }


    @Override
    public JsonContent selectByJsonPointer(String jsonPointer) {
        var newNode = node.at(jsonPointer);
        return new JsonContentDefault(
            source + " (jsonPointer: " + jsonPointer + ")",
            newNode
        );
    }

    @Override
    public JsonContent selectByJsonPath(@Language("JSONPath") String jsonPath) {
        // TODO: replace with native JacksonJsonProvider when it's released for Jackson 3.x
        var compiledJsonPath = JsonPath.compile(jsonPath);
        var configuration = Configuration.defaultConfiguration()
            .jsonProvider(Jackson3JsonNodeJsonProvider.INSTANCE);
        JsonNode newNode = compiledJsonPath.read(node, configuration);
        return new JsonContentDefault(
            source + " (jsonPath: " + jsonPath + ")",
            newNode
        );
    }


    @Override
    @Nullable
    public Object getValue() {
        return switch (node) {
            case MissingNode __ -> null;
            case NullNode __ -> null;
            case BooleanNode it -> it.booleanValue();
            case StringNode it -> requireNonNull(it.stringValue());
            case BinaryNode it -> requireNonNull(it.binaryValue());
            case ShortNode it -> it.shortValue();
            case IntNode it -> it.intValue();
            case LongNode it -> it.longValue();
            case BigIntegerNode it -> requireNonNull(it.bigIntegerValue());
            case FloatNode it -> it.floatValue();
            case DoubleNode it -> it.doubleValue();
            case DecimalNode it -> requireNonNull(it.decimalValue());
            default -> JSON_MAPPER.convertValue(node, Object.class);
        };
    }


    @Override
    public <T> T convertTo(Class<T> valueType) {
        T value = convertToNullable(valueType);
        if (value == null) {
            throw new IllegalStateException(format(
                "Cannot convert JSON content to %s: value is null",
                valueType
            ));
        }
        return value;
    }

    @Override
    public <T> T convertTo(TypeReference<T> valueType) {
        T value = convertToNullable(valueType);
        if (value == null) {
            throw new IllegalStateException(format(
                "Cannot convert JSON content to %s: value is null",
                valueType.getType()
            ));
        }
        return value;
    }

    @Override
    @Nullable
    public <T> T convertToNullable(Class<T> valueType) {
        return switch (node) {
            case MissingNode __ -> null;
            case NullNode __ -> null;
            default -> {
                try {
                    yield JSON_MAPPER.convertValue(node, valueType);
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(
                        format(
                            "Can't convert JSON content to %s: %s",
                            valueType,
                            source
                        ),
                        e
                    );
                }
            }
        };
    }

    @Override
    @Nullable
    public <T> T convertToNullable(TypeReference<T> valueType) {
        return switch (node) {
            case MissingNode __ -> null;
            case NullNode __ -> null;
            default -> {
                try {
                    yield JSON_MAPPER.convertValue(node, valueType);
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(
                        format(
                            "Can't convert JSON content to %s: %s",
                            valueType.getType(),
                            source
                        ),
                        e
                    );
                }
            }
        };
    }


    @Override
    public String getSource() {
        return source;
    }

    @Override
    public Reader openReader() {
        var string = asString();
        return new StringReader(string);
    }

    @Override
    public InputStream openInputStream() {
        var string = asString();
        var bytes = string.getBytes(getCharset());
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public Charset getCharset() {
        return UTF_8;
    }

    @Override
    @Language("JSON")
    public String asString() {
        return JSON_MAPPER.writeValueAsString(node);
    }


    //#region Serialization

    @Serial
    private Object writeReplace() throws ObjectStreamException {
        final String json;
        if (node.isMissingNode()) {
            json = null;
        } else {
            json = JSON_MAPPER.writer()
                .without(INDENT_OUTPUT)
                .writeValueAsString(node);
        }
        return new AbstractSerializedForm(source, json);
    }

    @RequiredArgsConstructor
    private static final class AbstractSerializedForm implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String source;
        private final @Nullable String json;

        @Serial
        private Object readResolve() {
            final JsonNode node;
            if (json == null) {
                node = MissingNode.getInstance();
            } else {
                try {
                    node = JSON_MAPPER.readTree(json);
                } catch (Throwable e) {
                    throw new IllegalStateException("Error parsing JSON content from " + source, e);
                }
            }
            return new JsonContentDefault(requireNonNull(source), node);
        }

    }

    //#endregion

}

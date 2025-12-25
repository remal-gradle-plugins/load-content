package name.remal.gradle_plugins.content_loader.content.json;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.content_loader.internal.JacksonUtils.JSON_MAPPER;
import static name.remal.gradle_plugins.content_loader.internal.JacksonUtils.JSON_NODES;

import com.google.common.collect.Iterables;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.BinaryNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.ContainerNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

class Jackson3JsonNodeJsonProvider extends AbstractJsonProvider {

    public static final Jackson3JsonNodeJsonProvider INSTANCE = new Jackson3JsonNodeJsonProvider();

    private Jackson3JsonNodeJsonProvider() {
    }


    @Override
    public Object parse(String json) throws InvalidJsonException {
        try {
            return JSON_MAPPER.readTree(json);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable e) {
            throw new InvalidJsonException(e, json);
        }
    }

    @Override
    public Object parse(byte[] json) throws InvalidJsonException {
        try {
            return JSON_MAPPER.readTree(json);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable e) {
            throw new InvalidJsonException(e, new String(json, UTF_8));
        }
    }

    @Override
    public Object parse(InputStream jsonStream, String charset) throws InvalidJsonException {
        try {
            return JSON_MAPPER.readTree(new InputStreamReader(jsonStream, charset));
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable e) {
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        if (!(obj instanceof JsonNode)) {
            throw new JsonPathException("Not a JSON Node");
        }
        return obj.toString();
    }

    @Override
    public Object createArray() {
        return JSON_MAPPER.createArrayNode();
    }

    @Override
    public Object createMap() {
        return JSON_MAPPER.createObjectNode();
    }

    @Nullable
    @Override
    public Object unwrap(@Nullable Object other) {
        return switch (other) {
            case MissingNode __ -> null;
            case NullNode __ -> null;
            case StringNode node -> node.stringValue();
            case BooleanNode node -> node.booleanValue();
            case BinaryNode node -> node.binaryValue();
            case ShortNode node -> node.shortValue();
            case IntNode node -> node.intValue();
            case LongNode node -> node.longValue();
            case BigIntegerNode node -> node.bigIntegerValue();
            case FloatNode node -> node.floatValue();
            case DoubleNode node -> node.doubleValue();
            case DecimalNode node -> node.decimalValue();
            case null, default -> other;
        };
    }

    @Nullable
    private JsonNode createJsonElement(@Nullable Object other) {
        return switch (other) {
            case null -> JSON_NODES.nullNode();
            case String it -> JSON_NODES.stringNode(it);
            case Boolean it -> JSON_NODES.booleanNode(it);
            case byte[] it -> JSON_NODES.binaryNode(it);
            case Byte it -> JSON_NODES.numberNode(it);
            case Short it -> JSON_NODES.numberNode(it);
            case Integer it -> JSON_NODES.numberNode(it);
            case Long it -> JSON_NODES.numberNode(it);
            case BigInteger it -> JSON_NODES.numberNode(it);
            case Float it -> JSON_NODES.numberNode(it);
            case Double it -> JSON_NODES.numberNode(it);
            case BigDecimal it -> JSON_NODES.numberNode(it);
            case JsonNode node -> node;
            default -> JSON_MAPPER.valueToTree(other);
        };
    }

    @Override
    @Contract("null->false")
    public boolean isArray(@Nullable Object obj) {
        return obj instanceof ArrayNode;
    }

    @Override
    public Object getArrayIndex(Object obj, int idx) {
        if (!(obj instanceof ArrayNode node)) {
            throw new UnsupportedOperationException();
        }

        return node.get(idx);
    }

    @Override
    public void setArrayIndex(Object array, int index, Object newValue) {
        if (!(array instanceof ArrayNode node)) {
            throw new UnsupportedOperationException();
        }

        if (index == node.size()) {
            node.add(createJsonElement(newValue));
        } else {
            node.set(index, createJsonElement(newValue));
        }
    }

    @Override
    public Object getMapValue(@Nullable Object obj, @Nullable String key) {
        if (key == null || !(obj instanceof ObjectNode node)) {
            return UNDEFINED;
        }

        if (node.has(key)) {
            return node.get(key);
        }

        return UNDEFINED;
    }

    @Override
    public void setProperty(@Nullable Object obj, @Nullable Object key, @Nullable Object value) {
        if (obj == null) {
            return;
        }

        switch (obj) {
            case ObjectNode node -> {
                var stringKey = requireNonNull(key).toString();
                node.set(stringKey, createJsonElement(value));
            }
            case ArrayNode node -> {
                final int index;
                if (key != null) {
                    index = key instanceof Integer intKey ? intKey : parseInt(key.toString());
                } else {
                    index = node.size();
                }
                if (index == node.size()) {
                    node.add(createJsonElement(value));
                } else {
                    node.set(index, createJsonElement(value));
                }
            }
            default -> {
                // do nothing
            }
        }
    }

    @Override
    public void removeProperty(Object obj, @Nullable Object key) {
        if (key == null) {
            return;
        }

        switch (obj) {
            case ObjectNode node -> {
                var stringKey = requireNonNull(key).toString();
                node.remove(stringKey);
            }
            case ArrayNode node -> {
                int index = key instanceof Integer intKey ? intKey : parseInt(key.toString());
                node.remove(index);
            }
            default -> {
                // do nothing
            }
        }
    }

    @Override
    @Contract("null->false")
    public boolean isMap(@Nullable Object obj) {
        return obj instanceof ObjectNode;
    }

    @Override
    @Nullable
    public Collection<String> getPropertyKeys(@Nullable Object obj) {
        if (!(obj instanceof ObjectNode node)) {
            return null;
        }
        return node.propertyNames();
    }

    @Override
    public int length(@Nullable Object obj) {
        return switch (obj) {
            case ContainerNode<?> it -> it.size();
            case StringNode it -> it.asString().length();
            case Collection<?> it -> it.size();
            case Map<?, ?> it -> it.size();
            case CharSequence it -> it.length();
            case null, default -> throw new JsonPathException(
                "length operation can not applied to " + (obj != null ? obj.getClass().getName() : null)
            );
        };
    }

    @Override
    @Nullable
    public Iterable<?> toIterable(Object obj) {
        if (!(obj instanceof ArrayNode node)) {
            return null;
        }
        return Iterables.transform(node, this::unwrap);
    }

}

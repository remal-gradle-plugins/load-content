package name.remal.gradle_plugins.content_loader.content.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import name.remal.gradle_plugins.content_loader.internal.BaseContent;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.node.ArrayNode;
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
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

public interface JsonContent extends BaseContent {

    JsonContent selectByJsonPointer(String jsonPointer);

    JsonContent selectByJsonPath(@Language("JSONPath") String jsonPath);


    /**
     * Get the value of the JSON node represented by this content.
     * <ul>
     *     <li>{@link MissingNode} - {@code null}
     *     <li>{@link NullNode} - {@code null}
     *     <li>{@link BooleanNode} - {@link Boolean}
     *     <li>{@link StringNode} - {@link String}
     *     <li>{@link BinaryNode} - {@code byte[]}
     *     <li>{@link ShortNode} - {@link Short}
     *     <li>{@link IntNode} - {@link Integer}
     *     <li>{@link LongNode} - {@link Long}
     *     <li>{@link BigIntegerNode} - {@link BigInteger}
     *     <li>{@link FloatNode} - {@link Float}
     *     <li>{@link DoubleNode} - {@link Double}
     *     <li>{@link DecimalNode} - {@link BigDecimal}
     *     <li>{@link ArrayNode} - {@link List}, elements will be converted to the listed types recursively
     *     <li>{@link ObjectNode} - {@link Map}, keys and values will be converted to the listed types recursively
     * </ul>
     */
    @Nullable
    Object getValue();


    <T> T convertTo(Class<T> valueType);

    <T> T convertTo(TypeReference<T> valueType);

    @Nullable
    <T> T convertToNullable(Class<T> valueType);

    @Nullable
    <T> T convertToNullable(TypeReference<T> valueType);


    @Override
    @Language("JSON")
    String asString();

}

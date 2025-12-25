package name.remal.gradle_plugins.content_loader.content.json;

import static name.remal.gradle_plugins.content_loader.internal.JacksonUtils.JSON_NODES;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.deserializeFrom;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.serializeToBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonContentDefaultTest {

    @Nested
    class SelectByJsonPointer {

        @Test
        void simple() {
            var node = JSON_NODES.objectNode();
            node
                .putObject("level1")
                .put("name", "value");
            var content = new JsonContentDefault(
                "source",
                node
            );
            var selected = content.selectByJsonPointer("/level1/name");
            assertEquals("value", selected.getValue());
        }

    }


    @Nested
    class SelectByJsonPath {

        @Test
        void simple() {
            var node = JSON_NODES.objectNode();
            node
                .putObject("level1")
                .put("name", "value");
            var content = new JsonContentDefault(
                "source",
                node
            );
            var selected = content.selectByJsonPath("$.level1.name");
            assertEquals("value", selected.getValue());
        }

    }


    @Nested
    class GetValue {

        @Test
        void missing() {
            var content = new JsonContentDefault(
                "source",
                JSON_NODES.missingNode()
            );
            assertNull(content.getValue());
        }

        @Test
        void nullNode() {
            var content = new JsonContentDefault(
                "source",
                JSON_NODES.nullNode()
            );
            assertNull(content.getValue());
        }

        @Test
        void string() {
            var content = new JsonContentDefault(
                "source",
                JSON_NODES.stringNode("value")
            );
            assertEquals("value", content.getValue());
        }

        @Test
        void bigInteger() {
            var content = new JsonContentDefault(
                "source",
                JSON_NODES.numberNode(BigInteger.valueOf(123))
            );
            assertEquals(BigInteger.valueOf(123), content.getValue());
        }

        @Test
        void map() {
            var content = new JsonContentDefault(
                "source",
                JSON_NODES.objectNode()
                    .put("name", "value")
            );
            assertEquals(Map.of("name", "value"), content.getValue());
        }

        @Test
        void array() {
            var content = new JsonContentDefault(
                "source",
                JSON_NODES.arrayNode()
                    .add("value")
            );
            assertEquals(List.of("value"), content.getValue());
        }

    }


    @Nested
    class Serialization {

        @Test
        void objectNode() {
            var original = new JsonContentDefault(
                "source",
                JSON_NODES.objectNode()
                    .put("name", "value")
            );

            var bytes = serializeToBytes(original);
            var deserialized = deserializeFrom(bytes, JsonContentDefault.class);

            assertEquals(original.getSource(), deserialized.getSource());
            assertEquals(original.getNode(), deserialized.getNode());
        }

        @Test
        void missingNode() {
            var original = new JsonContentDefault(
                "source",
                JSON_NODES.missingNode()
            );

            var bytes = serializeToBytes(original);
            var deserialized = deserializeFrom(bytes, JsonContentDefault.class);

            assertEquals(original.getSource(), deserialized.getSource());
            assertEquals(original.getNode(), deserialized.getNode());
        }

        @Test
        void nullNode() {
            var original = new JsonContentDefault(
                "source",
                JSON_NODES.nullNode()
            );

            var bytes = serializeToBytes(original);
            var deserialized = deserializeFrom(bytes, JsonContentDefault.class);

            assertEquals(original.getSource(), deserialized.getSource());
            assertEquals(original.getNode(), deserialized.getNode());
        }

    }

}

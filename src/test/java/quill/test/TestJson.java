package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.RenderError;
import quill.json.Json;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestJson {

    @Test
    @SuppressWarnings("unchecked")
    void parsesObjectWithNestedArray() {
        Object o = Json.parse("{\"a\":1,\"b\":[true,false,null,\"x\"],\"c\":{\"d\":2.5}}");
        assertTrue(o instanceof Map);
        Map<String, Object> m = (Map<String, Object>) o;
        assertEquals(1L, m.get("a"));
        List<Object> b = (List<Object>) m.get("b");
        assertEquals(4, b.size());
        assertEquals(true, b.get(0));
        assertNull(b.get(2));
        Map<String, Object> c = (Map<String, Object>) m.get("c");
        assertEquals(2.5, (Double) c.get("d"), 1e-9);
    }

    @Test
    void parsesEscapes() {
        Map<String, Object> m = (Map<String, Object>) Json.parse("{\"s\":\"a\\tb\\n\\\"c\\\"\"}");
        assertEquals("a\tb\n\"c\"", m.get("s"));
    }

    @Test
    void parsesUnicode() {
        Map<String, Object> m = (Map<String, Object>) Json.parse("{\"s\":\"\\u00e9\"}");
        assertEquals("é", m.get("s"));
    }

    @Test
    void writesAndReparses() {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("name", "Ada");
        m.put("age", 36L);
        m.put("vip", true);
        List<Object> list = java.util.Arrays.asList("a", 1L, false);
        m.put("tags", list);
        String json = Json.write(m);
        Object back = Json.parse(json);
        assertEquals(m, back);
    }

    @Test
    void trailingContentThrows() {
        assertThrows(RenderError.class, () -> Json.parse("{} junk"));
    }

    @Test
    void unterminatedStringThrows() {
        assertThrows(RenderError.class, () -> Json.parse("\"abc"));
    }
}

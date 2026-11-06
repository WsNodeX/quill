package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.RenderError;
import quill.core.RenderContext;
import quill.core.TemplateEngine;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TestTemplateEngine {

    private final TemplateEngine engine = TemplateEngine.DEFAULT;

    @Test
    void simpleVariable() {
        assertEquals("Hello Ada", engine.render("Hello {{ name }}", Map.of("name", "Ada")));
    }

    @Test
    void literalTextAroundTags() {
        assertEquals("aXb", engine.render("a{{ v }}b", Map.of("v", "X")));
    }

    @Test
    void dottedAndIndexed() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "Bo");
        root.put("user", user);
        root.put("items", Arrays.asList("p", "q"));
        assertEquals("Bo q", engine.render("{{ user.name }} {{ items[1] }}", root));
    }

    @Test
    void filtersChain() {
        assertEquals("HELLO", engine.render("{{ v | upper }}", Map.of("v", "hello")));
        assertEquals("Hi There", engine.render("{{ v | title }}", Map.of("v", "hi there")));
        assertEquals("ab", engine.render("{{ v | maxlen: 2 }}", Map.of("v", "abcdef")));
        assertEquals("fallback", engine.render("{{ v | default: \"fallback\" }}", new HashMap<>()));
    }

    @Test
    void ifElseBranch() {
        String tpl = "{% if vip %}VIP{% else %}guest{% endif %}";
        assertEquals("VIP", engine.render(tpl, Map.of("vip", true)));
        assertEquals("guest", engine.render(tpl, Map.of("vip", false)));
    }

    @Test
    void ifWithComparison() {
        String tpl = "{% if n > 3 %}big{% else %}small{% endif %}";
        assertEquals("big", engine.render(tpl, Map.of("n", 5)));
        assertEquals("small", engine.render(tpl, Map.of("n", 2)));
    }

    @Test
    void ifWithStringEquality() {
        String tpl = "{% if lang == \"fr\" %}bonjour{% else %}hello{% endif %}";
        assertEquals("bonjour", engine.render(tpl, Map.of("lang", "fr")));
        assertEquals("hello", engine.render(tpl, Map.of("lang", "en")));
    }

    @Test
    void forLoopAndLoopVars() {
        String tpl = "{% for w in words %}{{ loop.index }}:{{ w }} {% endfor %}";
        assertEquals("1:a 2:b 3:c ", engine.render(tpl, Map.of("words", Arrays.asList("a", "b", "c"))));
    }

    @Test
    void nestedForLoop() {
        String tpl = "{% for row in matrix %}{% for c in row %}{{ c }}{% endfor %}|{% endfor %}";
        List<List<String>> m = Arrays.asList(
                Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        assertEquals("ab|cd|", engine.render(tpl, Map.of("matrix", m)));
    }

    @Test
    void commentsStripped() {
        assertEquals("ab", engine.render("a{# hidden #}b", new HashMap<>()));
    }

    @Test
    void undefinedVariableThrows() {
        assertThrows(RenderError.class, () -> engine.render("{{ missing }}", new HashMap<>()));
    }

    @Test
    void unterminatedTagThrows() {
        assertThrows(RenderError.class, () -> engine.render("{{ x", new HashMap<>()));
    }

    @Test
    void unterminatedIfThrows() {
        assertThrows(RenderError.class, () -> engine.render("{% if x %}y", new HashMap<>()));
    }

    @Test
    void emptyForBodyAllowed() {
        String tpl = "{% for w in words %}{% endfor %}done";
        assertEquals("done", engine.render(tpl, Map.of("words", Arrays.asList("a", "b"))));
    }

    @Test
    void truthyConditionFromVariable() {
        String tpl = "{% if items %}has{% else %}none{% endif %}";
        assertEquals("has", engine.render(tpl, Map.of("items", Arrays.asList(1))));
        assertEquals("none", engine.render(tpl, Map.of("items", Collections.emptyList())));
    }

    @Test
    void renderIsDeterministicAcrossCalls() {
        String tpl = "{{ a }}-{{ b }}";
        Map<String, Object> v = Map.of("a", 1, "b", 2);
        assertEquals(engine.render(tpl, v), engine.render(tpl, v));
    }
}

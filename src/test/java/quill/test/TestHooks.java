package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.Hook;
import quill.core.Hooks;
import quill.core.RenderContext;
import quill.core.RenderError;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestHooks {

    private final RenderContext ctx = new RenderContext(Collections.emptyMap());

    @Test
    void trimAndCollapse() {
        assertEquals("hi", Hooks.trim().apply("  hi  ", ctx));
        assertEquals("a b c", Hooks.collapseWhitespace().apply("a   b\n\tc", ctx));
    }

    @Test
    void maxLength() {
        assertEquals("abc", Hooks.maxLength(3).apply("abcdef", ctx));
        assertEquals("abcdef", Hooks.maxLength(10).apply("abcdef", ctx));
    }

    @Test
    void ensurePrefixSuffix() {
        assertEquals(">> x", Hooks.ensurePrefix(">> ").apply("x", ctx));
        assertEquals("x <<", Hooks.ensureSuffix(" <<").apply("x", ctx));
    }

    @Test
    void conditionalAppliesOnlyWhenPredicateTrue() {
        Hook h = Hooks.conditional(s -> s.startsWith("#"), Hooks.trim());
        assertEquals("# x", h.apply("# x  ", ctx));
        assertEquals("  y  ", h.apply("  y  ", ctx));
    }

    @Test
    void byNameResolvesBuiltins() {
        assertEquals("x", Hooks.byName("trim").apply(" x ", ctx));
        assertEquals("x", Hooks.byName("collapse").apply("  x  ", ctx));
    }

    @Test
    void byNameUnknownThrows() {
        assertThrows(RenderError.class, () -> Hooks.byName("nope"));
    }

    @Test
    void hookIsPure() {
        Hook h = Hooks.trim();
        assertEquals(h.apply(" a ", ctx), h.apply(" a ", ctx));
        assertTrue(Map.of("k", h.apply(" a ", ctx)).containsKey("k"));
    }
}

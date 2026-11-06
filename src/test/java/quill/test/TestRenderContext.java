package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.RenderContext;
import quill.core.RenderError;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TestRenderContext {

    @Test
    void dottedAccessIntoMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "Ada");
        root.put("user", user);
        RenderContext ctx = new RenderContext(root);
        assertEquals("Ada", ctx.resolve("user.name"));
    }

    @Test
    void indexAccessIntoList() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("items", Arrays.asList("a", "b", "c"));
        RenderContext ctx = new RenderContext(root);
        assertEquals("b", ctx.resolve("items[1]"));
        assertEquals("c", ctx.resolve("items[-1]"));
    }

    @Test
    void beanPropertyAccess() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("p", new Point(3, 4));
        RenderContext ctx = new RenderContext(root);
        assertEquals(3, ctx.resolve("p.x"));
    }

    @Test
    void loopFrameScoping() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("x", "outer");
        RenderContext ctx = new RenderContext(root);
        assertEquals("outer", ctx.resolve("x"));
        ctx.enterLoop("x", "inner", 1, 1);
        assertEquals("inner", ctx.resolve("x"));
        assertEquals(1, ctx.resolve("loop.index"));
        assertTrue((Boolean) ctx.resolve("loop.first"));
        ctx.exitLoop();
        assertEquals("outer", ctx.resolve("x"));
    }

    @Test
    void truthinessRules() {
        assertFalse(RenderContext.isTruthy(null));
        assertFalse(RenderContext.isTruthy(""));
        assertTrue(RenderContext.isTruthy("x"));
        assertFalse(RenderContext.isTruthy(0));
        assertTrue(RenderContext.isTruthy(1));
        assertFalse(RenderContext.isTruthy(false));
        assertTrue(RenderContext.isTruthy(true));
        assertFalse(RenderContext.isTruthy(Collections.emptyList()));
        assertTrue(RenderContext.isTruthy(Arrays.asList(1)));
    }

    @Test
    void containsReflectsScope() {
        Map<String, Object> root = new LinkedHashMap<>();
        RenderContext ctx = new RenderContext(root);
        assertFalse(ctx.contains("missing"));
        ctx.enterLoop("item", "v", 1, 1);
        assertTrue(ctx.contains("item"));
        ctx.exitLoop();
        assertFalse(ctx.contains("item"));
    }

    @Test
    void unknownBeanPropertyThrows() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("p", new Point(1, 2));
        RenderContext ctx = new RenderContext(root);
        assertThrows(RenderError.class, () -> ctx.resolve("p.nope"));
    }

    public static class Point {
        private final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        public int getX() { return x; }
        public int getY() { return y; }
    }
}

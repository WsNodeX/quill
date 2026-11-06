package quill.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Mutable state carried through a single render pass.
 *
 * <p>Resolves variable references such as {@code user.name}, {@code items[0]}
 * and loop-scoped names like {@code item} or {@code loop.index}. Loop bodies
 * push a frame onto a stack so that the same name can be reused across nested
 * iterations without leaking into the outer scope.</p>
 */
public final class RenderContext {

    private final Map<String, Object> vars;
    private final Deque<Map<String, Object>> frames = new ArrayDeque<>();

    public RenderContext(Map<String, Object> vars) {
        this.vars = vars;
    }

    /** Whether a bare name is defined in any scope or the root map. */
    public boolean contains(String name) {
        for (Map<String, Object> frame : frames) {
            if (frame.containsKey(name)) return true;
        }
        return vars.containsKey(name);
    }

    /** Look up a bare name, searching loop frames first, then the root map. */
    public Object lookup(String name) {
        for (Map<String, Object> frame : frames) {
            if (frame.containsKey(name)) {
                return frame.get(name);
            }
        }
        return vars.get(name);
    }

    /**
     * Resolve a possibly compound reference.
     *
     * <p>Supports {@code a.b.c} (map / bean property chains) and
     * {@code list[0]} (list / array indexing). The two can be combined, e.g.
     * {@code users[1].name}.</p>
     */
    public Object resolve(String expr) {
        String e = expr.trim();
        // Split off a trailing index expression list, e.g. "items[0][1]".
        List<String> indices = new ArrayList<>();
        while (e.endsWith("]")) {
            int open = e.lastIndexOf('[');
            if (open < 0) break;
            indices.add(0, e.substring(open + 1, e.length() - 1).trim());
            e = e.substring(0, open).trim();
        }
        String[] parts = e.split("\\.", -1);
        Object value = lookup(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            value = property(value, parts[i].trim());
        }
        for (String idx : indices) {
            value = indexInto(value, idx);
        }
        return value;
    }

    private Object indexInto(Object value, String idx) {
        if (value == null) return null;
        if (value instanceof List) {
            return ((List<?>) value).get(parseIndex(idx, ((List<?>) value).size()));
        }
        if (value.getClass().isArray()) {
            List<Object> asList = new ArrayList<>();
            Object[] arr = (Object[]) value;
            for (Object o : arr) asList.add(o);
            return asList.get(parseIndex(idx, asList.size()));
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).get(unquote(idx));
        }
        throw new RenderError("cannot index into " + value.getClass().getSimpleName());
    }

    private int parseIndex(String idx, int size) {
        try {
            int i = Integer.parseInt(idx.trim());
            if (i < 0) i += size;
            return i;
        } catch (NumberFormatException ex) {
            throw new RenderError("index must be an integer, got: " + idx);
        }
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /** Walk a dotted path, supporting maps and plain old Java objects. */
    public Object property(Object base, String key) {
        if (base == null) return null;
        if (base instanceof Map) {
            return ((Map<?, ?>) base).get(key);
        }
        if (base instanceof List && key.matches("\\d+")) {
            return ((List<?>) base).get(Integer.parseInt(key));
        }
        return beanProperty(base, key);
    }

    private Object beanProperty(Object base, String key) {
        Class<?> cls = base.getClass();
        String cap = Character.toUpperCase(key.charAt(0)) + key.substring(1);
        try {
            Method m = cls.getMethod("get" + cap);
            return m.invoke(base);
        } catch (NoSuchMethodException ignore) {
            // fall through to field / isXxx
        } catch (Exception ex) {
            throw new RenderError("cannot read property " + key + ": " + ex.getMessage());
        }
        try {
            Method m = cls.getMethod("is" + cap);
            return m.invoke(base);
        } catch (NoSuchMethodException ignore) {
            // fall through
        } catch (Exception ex) {
            throw new RenderError("cannot read property " + key + ": " + ex.getMessage());
        }
        try {
            Field f = cls.getDeclaredField(key);
            f.setAccessible(true);
            return f.get(base);
        } catch (Exception ex) {
            throw new RenderError("unknown property '" + key + "' on " + cls.getSimpleName());
        }
    }

    /** Push a loop iteration scope. {@code loop} holds iteration metadata. */
    public void enterLoop(String itemName, Object item, int index, int total) {
        Map<String, Object> frame = new java.util.LinkedHashMap<>();
        frame.put(itemName, item);
        Map<String, Object> loop = new java.util.LinkedHashMap<>();
        loop.put("index", index);          // 1-based, friendly for prompts
        loop.put("idx", index - 1);        // 0-based
        loop.put("first", index == 1);
        loop.put("last", index == total);
        loop.put("length", total);
        loop.put("total", total);
        frame.put("loop", loop);
        frames.push(frame);
    }

    public void exitLoop() {
        if (!frames.isEmpty()) frames.pop();
    }

    /** Truthiness used by {@code {% if %}} conditions. */
    public static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return !((String) value).isEmpty();
        if (value instanceof Number) return ((Number) value).doubleValue() != 0.0;
        if (value instanceof java.util.Collection) return !((java.util.Collection<?>) value).isEmpty();
        if (value instanceof Map) return !((Map<?, ?>) value).isEmpty();
        if (value.getClass().isArray()) return java.lang.reflect.Array.getLength(value) > 0;
        return true;
    }

    public Map<String, Object> rootVars() {
        return vars;
    }
}

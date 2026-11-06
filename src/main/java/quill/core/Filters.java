package quill.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A small, dependency-free library of value filters usable inside
 * {@code {{ var | filter }}} expressions.
 *
 * <p>Filters are pure functions from {@link Object} to {@link String}. A few
 * accept a single argument, written {@code {{ x | default: "n/a" }}}.</p>
 */
public final class Filters {

    private static final Map<String, FilterFn> REGISTRY = new LinkedHashMap<>();

    @FunctionalInterface
    private interface FilterFn {
        String apply(Object value, String arg);
    }

    static {
        register("upper", (v, a) -> asString(v).toUpperCase());
        register("lower", (v, a) -> asString(v).toLowerCase());
        register("trim", (v, a) -> asString(v).trim());
        register("collapse", (v, a) -> asString(v).replaceAll("\\s+", " ").trim());
        register("capitalize", (v, a) -> cap(asString(v)));
        register("title", (v, a) -> title(asString(v)));
        register("length", (v, a) -> Integer.toString(len(v)));
        register("default", (v, a) -> (v == null || asString(v).isEmpty()) ? (a == null ? "" : a) : asString(v));
        register("upperfirst", (v, a) -> cap(asString(v)));
        register("maxlen", (v, a) -> {
            int n = a == null ? Integer.MAX_VALUE : Integer.parseInt(a.trim());
            String s = asString(v);
            return s.length() <= n ? s : s.substring(0, n);
        });
        register("prefix", (v, a) -> (a == null ? "" : a) + asString(v));
        register("suffix", (v, a) -> asString(v) + (a == null ? "" : a));
    }

    private Filters() {}

    private static void register(String name, FilterFn fn) {
        REGISTRY.put(name, fn);
    }

    public static boolean exists(String name) {
        return REGISTRY.containsKey(name);
    }

    public static String apply(String name, Object value, String arg) {
        FilterFn fn = REGISTRY.get(name);
        if (fn == null) {
            throw new RenderError("unknown filter: " + name);
        }
        return fn.apply(value, arg);
    }

    private static String asString(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static int len(Object v) {
        if (v == null) return 0;
        if (v instanceof String) return ((String) v).length();
        if (v instanceof java.util.Collection) return ((java.util.Collection<?>) v).size();
        if (v instanceof Map) return ((Map<?, ?>) v).size();
        if (v.getClass().isArray()) return java.lang.reflect.Array.getLength(v);
        return asString(v).length();
    }

    private static String cap(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String title(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                upper = true;
                sb.append(c);
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }
}

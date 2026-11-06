package quill.core;

import java.util.function.Predicate;

/**
 * Built-in {@link Hook} implementations registered by name and reused by
 * {@link quill.registry.PromptRegistry} when a prompt declares hooks.
 */
public final class Hooks {

    private Hooks() {}

    /** Trim leading and trailing whitespace. */
    public static Hook trim() {
        return (text, ctx) -> text.trim();
    }

    /** Collapse runs of whitespace into a single space and trim. */
    public static Hook collapseWhitespace() {
        return (text, ctx) -> text.replaceAll("\\s+", " ").trim();
    }

    /** Cap the output at {@code max} characters. */
    public static Hook maxLength(int max) {
        return (text, ctx) -> text.length() <= max ? text : text.substring(0, max);
    }

    /** Ensure the output starts with {@code prefix}. */
    public static Hook ensurePrefix(String prefix) {
        return (text, ctx) -> text.startsWith(prefix) ? text : prefix + text;
    }

    /** Ensure the output ends with {@code suffix}. */
    public static Hook ensureSuffix(String suffix) {
        return (text, ctx) -> text.endsWith(suffix) ? text : text + suffix;
    }

    /** Apply {@code transform} only when {@code when} returns true for the text. */
    public static Hook conditional(Predicate<String> when, Hook transform) {
        return (text, ctx) -> when.test(text) ? transform.apply(text, ctx) : text;
    }

    /** Resolve a built-in hook by its registered name. */
    public static Hook byName(String name) {
        switch (name.trim().toLowerCase()) {
            case "trim":
                return trim();
            case "collapse":
                return collapseWhitespace();
            case "maxlen":
                return maxLength(2000);
            default:
                throw new RenderError("unknown hook: " + name);
        }
    }
}

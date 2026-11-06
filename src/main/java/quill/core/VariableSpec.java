package quill.core;

/**
 * Declares a variable a {@link Prompt} expects.
 *
 * <p>Required variables are checked before rendering so a missing value fails
 * fast with a clear message rather than silently producing a broken prompt.
 * Optional variables may carry a default that is substituted when absent.</p>
 */
public final class VariableSpec {

    private final String name;
    private final boolean required;
    private final Object defaultValue;
    private final String description;

    public VariableSpec(String name, boolean required, Object defaultValue, String description) {
        this.name = name;
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description == null ? "" : description;
    }

    public static VariableSpec required(String name) {
        return new VariableSpec(name, true, null, "");
    }

    public static VariableSpec required(String name, String description) {
        return new VariableSpec(name, true, null, description);
    }

    public static VariableSpec optional(String name, Object defaultValue) {
        return new VariableSpec(name, false, defaultValue, "");
    }

    public static VariableSpec optional(String name, Object defaultValue, String description) {
        return new VariableSpec(name, false, defaultValue, description);
    }

    public String name() { return name; }
    public boolean required() { return required; }
    public Object defaultValue() { return defaultValue; }
    public String description() { return description; }
}

package quill.core;

/**
 * The speaker of a chat message.
 *
 * <p>Maps cleanly onto the role field expected by every major chat
 * completion API (OpenAI, Anthropic, Mistral, ...). Only the three roles that
 * are universally supported are modelled here.</p>
 */
public enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private final String wire;

    Role(String wire) {
        this.wire = wire;
    }

    /** The exact string expected by chat completion APIs. */
    public String wire() {
        return wire;
    }

    /** Parse a wire string (case-insensitive) into a {@link Role}. */
    public static Role from(String value) {
        if (value == null) {
            throw new RenderError("role must not be null");
        }
        for (Role r : values()) {
            if (r.wire.equalsIgnoreCase(value) || r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new RenderError("unknown role: " + value);
    }
}

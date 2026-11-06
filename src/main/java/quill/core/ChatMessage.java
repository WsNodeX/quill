package quill.core;

import java.util.Objects;

/**
 * A single chat message: a {@link Role} plus its textual content.
 *
 * <p>Immutable and trivially serialisable. A list of these is the canonical
 * request shape for chat-completion endpoints.</p>
 */
public final class ChatMessage {

    private final Role role;
    private final String content;

    public ChatMessage(Role role, String content) {
        this.role = Objects.requireNonNull(role, "role");
        this.content = Objects.requireNonNull(content, "content");
    }

    public static ChatMessage of(Role role, String content) {
        return new ChatMessage(role, content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }

    public Role role() {
        return role;
    }

    public String content() {
        return content;
    }

    /** A small map view suitable for JSON serialisation. */
    public java.util.Map<String, String> toMap() {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("role", role.wire());
        m.put("content", content);
        return m;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage)) return false;
        ChatMessage that = (ChatMessage) o;
        return role == that.role && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }

    @Override
    public String toString() {
        return "ChatMessage{" + role.wire() + ": \"" + content + "\"}";
    }
}

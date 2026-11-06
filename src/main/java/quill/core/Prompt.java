package quill.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A named, versioned prompt definition.
 *
 * <p>A prompt is either a single text template or a sequence of chat messages
 * (with optional few-shot examples). In both forms it declares the variables it
 * expects, enforces required ones, applies defaults, and runs output hooks.</p>
 *
 * <p>Build instances with the fluent {@link Builder}:</p>
 * <pre>
 * Prompt p = Prompt.builder("translate")
 *     .version("1.0.0")
 *     .template("Translate {{ text }} into {{ lang | default: \"English\" }}.")
 *     .variable(VariableSpec.required("text", "source text"))
 *     .hook(Hooks.trim())
 *     .build();
 * </pre>
 */
public final class Prompt {

    private final String name;
    private final String version;
    private final String description;
    private final String template;                 // non-null for text prompts
    private final List<MessageTemplate> messages;   // non-null for chat prompts
    private final List<VariableSpec> variables;
    private final List<Hook> hooks;
    private final List<FewShotExample> fewShot;

    private final TemplateEngine engine;

    Prompt(Builder b) {
        this.name = b.name;
        this.version = b.version;
        this.description = b.description;
        this.template = b.template;
        this.messages = b.messages;
        this.variables = b.variables;
        this.hooks = b.hooks;
        this.fewShot = b.fewShot;
        this.engine = b.engine == null ? TemplateEngine.DEFAULT : b.engine;
        if (template == null && (messages == null || messages.isEmpty())) {
            throw new RenderError("prompt '" + name + "' must have a template or messages");
        }
    }

    public String name() { return name; }
    public String version() { return version; }
    public String description() { return description; }
    public List<VariableSpec> variables() { return variables; }
    public boolean isChat() { return messages != null && !messages.isEmpty(); }

    /** Render as a single text string. Only valid for text prompts. */
    public String render(Map<String, Object> vars) {
        if (template == null) {
            throw new RenderError("prompt '" + name + "' is a chat prompt; use renderChat");
        }
        Map<String, Object> resolved = prepare(vars);
        String out = engine.compile(template).render(resolved);
        return applyHooks(out, resolved);
    }

    /** Render as a list of chat messages, inserting few-shot examples. */
    public List<ChatMessage> renderChat(Map<String, Object> vars) {
        if (!isChat()) {
            throw new RenderError("prompt '" + name + "' is a text prompt; use render");
        }
        Map<String, Object> resolved = prepare(vars);
        List<ChatMessage> out = new ArrayList<>();
        List<MessageTemplate> nonSystem = new ArrayList<>();
        for (MessageTemplate mt : messages) {
            if (mt.role == Role.SYSTEM) {
                out.add(new ChatMessage(Role.SYSTEM, applyHooks(engine.compile(mt.template).render(resolved), resolved)));
            } else {
                nonSystem.add(mt);
            }
        }
        for (FewShotExample ex : fewShot) {
            out.add(ChatMessage.user(applyHooks(engine.compile(ex.user).render(resolved), resolved)));
            out.add(ChatMessage.assistant(applyHooks(engine.compile(ex.assistant).render(resolved), resolved)));
        }
        for (MessageTemplate mt : nonSystem) {
            out.add(new ChatMessage(mt.role, applyHooks(engine.compile(mt.template).render(resolved), resolved)));
        }
        return out;
    }

    /** Validate that all required variables are present (throws on failure). */
    public void validate(Map<String, Object> vars) {
        for (VariableSpec spec : variables) {
            if (spec.required() && !vars.containsKey(spec.name())) {
                throw new RenderError("prompt '" + name + "' requires variable: " + spec.name());
            }
        }
    }

    private Map<String, Object> prepare(Map<String, Object> vars) {
        Map<String, Object> resolved = new LinkedHashMap<>(vars);
        for (VariableSpec spec : variables) {
            if (!resolved.containsKey(spec.name()) && spec.defaultValue() != null) {
                resolved.put(spec.name(), spec.defaultValue());
            }
        }
        validate(resolved);
        return resolved;
    }

    private String applyHooks(String text, Map<String, Object> vars) {
        RenderContext ctx = new RenderContext(vars);
        String out = text;
        for (Hook h : hooks) {
            out = h.apply(out, ctx);
        }
        return out;
    }

    // ------------------------------------------------------------------

    /** A role plus a template for one chat message. */
    public static final class MessageTemplate {
        public final Role role;
        public final String template;
        public MessageTemplate(Role role, String template) {
            this.role = Objects.requireNonNull(role);
            this.template = Objects.requireNonNull(template);
        }
    }

    /** A user/assistant pair used as a few-shot demonstration. */
    public static final class FewShotExample {
        public final String user;
        public final String assistant;
        public FewShotExample(String user, String assistant) {
            this.user = Objects.requireNonNull(user);
            this.assistant = Objects.requireNonNull(assistant);
        }
    }

    // ------------------------------------------------------------------

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Fluent builder for {@link Prompt}. */
    public static final class Builder {
        private final String name;
        private String version = "1.0.0";
        private String description = "";
        private String template;
        private List<MessageTemplate> messages = new ArrayList<>();
        private List<VariableSpec> variables = new ArrayList<>();
        private List<Hook> hooks = new ArrayList<>();
        private List<FewShotExample> fewShot = new ArrayList<>();
        private TemplateEngine engine;

        Builder(String name) { this.name = Objects.requireNonNull(name); }

        public Builder version(String v) { this.version = v; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder template(String t) { this.template = t; return this; }
        public Builder engine(TemplateEngine e) { this.engine = e; return this; }

        public Builder message(Role role, String tpl) { messages.add(new MessageTemplate(role, tpl)); return this; }
        public Builder system(String tpl) { return message(Role.SYSTEM, tpl); }
        public Builder user(String tpl) { return message(Role.USER, tpl); }
        public Builder assistant(String tpl) { return message(Role.ASSISTANT, tpl); }

        public Builder variable(VariableSpec spec) { variables.add(spec); return this; }
        public Builder hook(Hook hook) { hooks.add(hook); return this; }
        public Builder fewShot(String user, String assistant) {
            fewShot.add(new FewShotExample(user, assistant)); return this;
        }

        public Prompt build() { return new Prompt(this); }
    }
}

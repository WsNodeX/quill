package quill.registry;

import quill.core.ChatMessage;
import quill.core.Hook;
import quill.core.Hooks;
import quill.core.Prompt;
import quill.core.RenderError;
import quill.core.Role;
import quill.core.TemplateEngine;
import quill.core.VariableSpec;
import quill.json.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of named, versioned {@link Prompt}s.
 *
 * <p>Prompts can be registered programmatically or loaded in bulk from a JSON
 * document. The registry is the recommended entry point for applications that
 * keep their prompts as versioned, reviewable assets rather than inline
 * strings scattered through the codebase.</p>
 */
public final class PromptRegistry {

    private final Map<String, Prompt> prompts = new LinkedHashMap<>();
    private final TemplateEngine engine;

    public PromptRegistry() {
        this(TemplateEngine.DEFAULT);
    }

    public PromptRegistry(TemplateEngine engine) {
        this.engine = engine;
    }

    public PromptRegistry register(Prompt prompt) {
        prompts.put(prompt.name(), prompt);
        return this;
    }

    public boolean contains(String name) {
        return prompts.containsKey(name);
    }

    public Prompt get(String name) {
        Prompt p = prompts.get(name);
        if (p == null) throw new RenderError("no such prompt: " + name);
        return p;
    }

    public List<String> names() {
        return new ArrayList<>(prompts.keySet());
    }

    public String render(String name, Map<String, Object> vars) {
        return get(name).render(vars);
    }

    public List<ChatMessage> renderChat(String name, Map<String, Object> vars) {
        return get(name).renderChat(vars);
    }

    // ------------------------------------------------------------------
    //  JSON loading
    // ------------------------------------------------------------------

    /** Parse a JSON document (as produced by {@link quill.core.Json#write}) into prompts. */
    @SuppressWarnings("unchecked")
    public PromptRegistry loadFromJson(String json) {
        Object root = Json.parse(json);
        if (!(root instanceof Map)) {
            throw new RenderError("registry JSON must be an object");
        }
        Object promptsObj = ((Map<String, Object>) root).get("prompts");
        if (!(promptsObj instanceof List)) {
            throw new RenderError("registry JSON must contain a 'prompts' array");
        }
        for (Object item : (List<Object>) promptsObj) {
            if (!(item instanceof Map)) {
                throw new RenderError("each prompt must be an object");
            }
            register(fromMap((Map<String, Object>) item));
        }
        return this;
    }

    /** Read a JSON file and load its prompts. */
    public PromptRegistry loadFromFile(String path) {
        try {
            String content = new String(Files.readAllBytes(Path.of(path)), StandardCharsets.UTF_8);
            return loadFromJson(content);
        } catch (IOException e) {
            throw new RenderError("cannot read registry file: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Prompt fromMap(Map<String, Object> m) {
        String name = str(m, "name");
        if (name == null) throw new RenderError("prompt missing 'name'");

        Prompt.Builder b = Prompt.builder(name)
                .version(str(m, "version", "1.0.0"))
                .description(str(m, "description", ""))
                .engine(engine);

        Object template = m.get("template");
        if (template instanceof String) {
            b.template((String) template);
        }

        Object messages = m.get("messages");
        if (messages instanceof List) {
            for (Object mo : (List<Object>) messages) {
                if (!(mo instanceof Map)) continue;
                Map<String, Object> mm = (Map<String, Object>) mo;
                b.message(Role.from(str(mm, "role")), str(mm, "template"));
            }
        }

        Object variables = m.get("variables");
        if (variables instanceof List) {
            for (Object vo : (List<Object>) variables) {
                if (!(vo instanceof Map)) continue;
                Map<String, Object> vm = (Map<String, Object>) vo;
                String vn = str(vm, "name");
                boolean req = Boolean.TRUE.equals(vm.get("required"));
                Object def = vm.get("default");
                b.variable(new VariableSpec(vn, req, def, str(vm, "description", "")));
            }
        }

        Object hooks = m.get("hooks");
        if (hooks instanceof List) {
            for (Object ho : (List<Object>) hooks) {
                if (ho instanceof String) b.hook(Hooks.byName((String) ho));
            }
        }

        Object fewShot = m.get("fewShot");
        if (fewShot instanceof List) {
            for (Object fo : (List<Object>) fewShot) {
                if (!(fo instanceof Map)) continue;
                Map<String, Object> fm = (Map<String, Object>) fo;
                b.fewShot(str(fm, "user"), str(fm, "assistant"));
            }
        }

        return b.build();
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String str(Map<String, Object> m, String key, String dflt) {
        String v = str(m, key);
        return v == null ? dflt : v;
    }
}

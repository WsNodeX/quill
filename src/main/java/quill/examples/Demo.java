package quill.examples;

import quill.core.ChatMessage;
import quill.core.Hooks;
import quill.core.Prompt;
import quill.core.Role;
import quill.core.TemplateEngine;
import quill.core.VariableSpec;
import quill.registry.PromptRegistry;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained tour of Quill's capabilities. Run with
 * {@code java quill.examples.Demo} &mdash; no network access required.
 */
public final class Demo {

    public static void main(String[] args) {
        TemplateEngine engine = TemplateEngine.DEFAULT;

        separator("1. Variables, filters and control flow");
        String src = """
                Dear {{ name | upper }},

                {% if vip %}
                Thanks for being a VIP member!
                {% else %}
                Consider upgrading to VIP.
                {% endif %}

                Your orders:
                {% for o in orders %}
                {{ loop.index }}. {{ o | capitalize }}
                {% endfor %}
                """;
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("name", "ada lovelace");
        vars.put("vip", true);
        vars.put("orders", Arrays.asList("calculus book", "abacus", "notebook"));
        System.out.println(engine.render(src, vars));

        separator("2. Text prompt with required variables + a trim hook");
        Prompt summarize = Prompt.builder("summarize")
                .version("1.0.0")
                .description("Condense text into a target language.")
                .template("Summarize the following in {{ lang | default: \"English\" }}:\n\n{{ text }}")
                .variable(VariableSpec.required("text", "text to summarize"))
                .variable(VariableSpec.optional("lang", "English"))
                .hook(Hooks.trim())
                .build();
        Map<String, Object> sVars = new LinkedHashMap<>();
        sVars.put("text", "Quill is a small Java library for composing LLM prompts.");
        System.out.println(summarize.render(sVars));

        separator("3. Chat prompt with few-shot examples");
        Prompt qa = Prompt.builder("qa")
                .system("You are a concise maths tutor.")
                .user("Question: {{ q }}")
                .fewShot("What is 2+2?", "4")
                .fewShot("What is 5*3?", "15")
                .build();
        List<ChatMessage> chat = qa.renderChat(Map.of("q", "What is 12/4?"));
        for (ChatMessage m : chat) {
            System.out.println("[" + m.role().wire() + "] " + m.content());
        }

        separator("4. Prompt registry loaded from JSON");
        PromptRegistry registry = new PromptRegistry()
                .register(Prompt.builder("greet")
                        .template("Hello {{ name | capitalize }}!")
                        .variable(VariableSpec.required("name"))
                        .build());
        System.out.println(registry.render("greet", Map.of("name", "world")));
        System.out.println("Registered prompts: " + registry.names());
    }

    private static void separator(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}

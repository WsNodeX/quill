package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.ChatMessage;
import quill.core.RenderError;
import quill.core.Role;
import quill.registry.PromptRegistry;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestRegistry {

    private static final String JSON = """
            {
              "prompts": [
                {
                  "name": "greet",
                  "version": "2.1.0",
                  "description": "A friendly greeting.",
                  "template": "Hello {{ name | capitalize }}!",
                  "variables": [ { "name": "name", "required": true } ],
                  "hooks": [ "trim" ]
                },
                {
                  "name": "qa",
                  "version": "1.0.0",
                  "description": "Few-shot Q&A.",
                  "messages": [
                    { "role": "system", "template": "You are a tutor." },
                    { "role": "user", "template": "Q: {{ q }}" }
                  ],
                  "fewShot": [
                    { "user": "1+1?", "assistant": "2" }
                  ]
                }
              ]
            }
            """;

    @Test
    void loadsTextPromptAndRenders() {
        PromptRegistry reg = new PromptRegistry().loadFromJson(JSON);
        assertTrue(reg.contains("greet"));
        assertEquals("Hello Ada!", reg.render("greet", java.util.Map.of("name", "ada")));
    }

    @Test
    void loadsChatPromptWithFewShot() {
        PromptRegistry reg = new PromptRegistry().loadFromJson(JSON);
        java.util.List<ChatMessage> msgs = reg.renderChat("qa", java.util.Map.of("q", "2+2?"));
        assertEquals(Role.SYSTEM, msgs.get(0).role());
        // system + 1 few-shot pair + user turn
        assertEquals(4, msgs.size());
        assertEquals("Q: 2+2?", msgs.get(3).content());
    }

    @Test
    void versionAndDescriptionCarried() {
        PromptRegistry reg = new PromptRegistry().loadFromJson(JSON);
        assertEquals("2.1.0", reg.get("greet").version());
        assertEquals("A friendly greeting.", reg.get("greet").description());
    }

    @Test
    void missingPromptThrows() {
        PromptRegistry reg = new PromptRegistry().loadFromJson(JSON);
        assertThrows(RenderError.class, () -> reg.get("nope"));
    }

    @Test
    void loadingFromTempFileWorks() throws Exception {
        Path f = Files.createTempFile("quill-reg", ".json");
        Files.writeString(f, JSON, StandardCharsets.UTF_8);
        PromptRegistry reg = new PromptRegistry().loadFromFile(f.toString());
        assertEquals("Hello Bob!", reg.render("greet", java.util.Map.of("name", "bob")));
        Files.deleteIfExists(f);
    }

    @Test
    void malformedJsonThrows() {
        assertThrows(RenderError.class, () ->
                new PromptRegistry().loadFromJson("{ not json"));
    }
}

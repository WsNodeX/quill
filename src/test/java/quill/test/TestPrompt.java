package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.ChatMessage;
import quill.core.Hooks;
import quill.core.Prompt;
import quill.core.RenderError;
import quill.core.Role;
import quill.core.VariableSpec;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TestPrompt {

    @Test
    void textRenderWithDefault() {
        Prompt p = Prompt.builder("t")
                .template("Hi {{ name | default: \"guest\" }}")
                .build();
        assertEquals("Hi guest", p.render(new HashMap<>()));
        assertEquals("Hi Ada", p.render(Map.of("name", "Ada")));
    }

    @Test
    void requiredVariableMissingThrows() {
        Prompt p = Prompt.builder("t")
                .template("Hi {{ name }}")
                .variable(VariableSpec.required("name"))
                .build();
        assertThrows(RenderError.class, () -> p.render(new HashMap<>()));
    }

    @Test
    void requiredVariablePresentPasses() {
        Prompt p = Prompt.builder("t")
                .template("Hi {{ name }}")
                .variable(VariableSpec.required("name"))
                .build();
        assertEquals("Hi Ada", p.render(Map.of("name", "Ada")));
    }

    @Test
    void hookApplied() {
        Prompt p = Prompt.builder("t")
                .template("  spaced  ")
                .hook(Hooks.trim())
                .build();
        assertEquals("spaced", p.render(new HashMap<>()));
    }

    @Test
    void chatRenderAssemblesRoles() {
        Prompt p = Prompt.builder("c")
                .system("You are helpful.")
                .user("Answer: {{ q }}")
                .build();
        List<ChatMessage> msgs = p.renderChat(Map.of("q", "2+2?"));
        assertEquals(2, msgs.size());
        assertEquals(Role.SYSTEM, msgs.get(0).role());
        assertEquals("You are helpful.", msgs.get(0).content());
        assertEquals(Role.USER, msgs.get(1).role());
        assertEquals("Answer: 2+2?", msgs.get(1).content());
    }

    @Test
    void fewShotInsertedAfterSystem() {
        Prompt p = Prompt.builder("c")
                .system("Tutor.")
                .user("Q: {{ q }}")
                .fewShot("2+2?", "4")
                .fewShot("3*3?", "9")
                .build();
        List<ChatMessage> msgs = p.renderChat(Map.of("q", "5+5?"));
        // system, two few-shot pairs (4 msgs), then the real user turn => 6
        assertEquals(6, msgs.size());
        assertEquals(Role.SYSTEM, msgs.get(0).role());
        assertEquals(Role.USER, msgs.get(1).role());
        assertEquals("2+2?", msgs.get(1).content());
        assertEquals(Role.ASSISTANT, msgs.get(2).role());
        assertEquals("4", msgs.get(2).content());
        assertEquals(Role.USER, msgs.get(3).role());
        assertEquals("3*3?", msgs.get(3).content());
        assertEquals(Role.ASSISTANT, msgs.get(4).role());
        assertEquals("9", msgs.get(4).content());
        assertEquals(Role.USER, msgs.get(5).role());
        assertEquals("Q: 5+5?", msgs.get(5).content());
    }

    @Test
    void textPromptCannotRenderChat() {
        Prompt p = Prompt.builder("t").template("x").build();
        assertThrows(RenderError.class, () -> p.renderChat(new HashMap<>()));
    }

    @Test
    void chatPromptCannotRenderText() {
        Prompt p = Prompt.builder("c").user("x").build();
        assertThrows(RenderError.class, () -> p.render(new HashMap<>()));
    }

    @Test
    void promptWithoutContentThrows() {
        assertThrows(RenderError.class, () -> Prompt.builder("empty").build());
    }

    @Test
    void variableDescriptionAccessible() {
        Prompt p = Prompt.builder("t")
                .template("{{ x }}")
                .variable(VariableSpec.optional("x", "d", "the x"))
                .build();
        assertEquals(1, p.variables().size());
        assertEquals("the x", p.variables().get(0).description());
    }
}

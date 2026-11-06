package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.ChatMessage;
import quill.core.Role;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestChatMessage {

    @Test
    void factoriesSetRole() {
        assertEquals(Role.SYSTEM, ChatMessage.system("x").role());
        assertEquals(Role.USER, ChatMessage.user("x").role());
        assertEquals(Role.ASSISTANT, ChatMessage.assistant("x").role());
    }

    @Test
    void toMapProducesWireShape() {
        Map<String, String> m = ChatMessage.user("hi").toMap();
        assertEquals("user", m.get("role"));
        assertEquals("hi", m.get("content"));
    }

    @Test
    void equalityAndToString() {
        assertEquals(ChatMessage.user("a"), ChatMessage.user("a"));
        assertNotEquals(ChatMessage.user("a"), ChatMessage.user("b"));
        assertTrue(ChatMessage.user("a").toString().contains("user"));
    }

    @Test
    void nullContentRejected() {
        assertThrows(NullPointerException.class, () -> ChatMessage.user(null));
    }
}

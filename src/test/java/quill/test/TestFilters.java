package quill.test;

import org.junit.jupiter.api.Test;
import quill.core.Filters;
import quill.core.RenderError;

import static org.junit.jupiter.api.Assertions.*;

class TestFilters {

    @Test
    void upperLowerTrim() {
        assertEquals("HELLO", Filters.apply("upper", "hello", null));
        assertEquals("hello", Filters.apply("lower", "HELLO", null));
        assertEquals("hi", Filters.apply("trim", "  hi  ", null));
    }

    @Test
    void capitalizeAndTitle() {
        assertEquals("Hello", Filters.apply("capitalize", "hello", null));
        assertEquals("Hello World", Filters.apply("title", "hello world", null));
    }

    @Test
    void defaultFilter() {
        assertEquals("n/a", Filters.apply("default", null, "n/a"));
        assertEquals("n/a", Filters.apply("default", "", "n/a"));
        assertEquals("kept", Filters.apply("default", "kept", "n/a"));
    }

    @Test
    void maxlenCuts() {
        assertEquals("abc", Filters.apply("maxlen", "abcdef", "3"));
        assertEquals("abcdef", Filters.apply("maxlen", "abcdef", "10"));
    }

    @Test
    void lengthOfCollection() {
        java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
        assertEquals("3", Filters.apply("length", list, null));
    }

    @Test
    void unknownFilterThrows() {
        assertThrows(RenderError.class, () -> Filters.apply("nope", "x", null));
    }
}

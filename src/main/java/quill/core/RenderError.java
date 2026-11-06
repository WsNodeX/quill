package quill.core;

/**
 * Thrown when a template or prompt cannot be rendered.
 *
 * <p>All rendering failures (unknown variables, type mismatches, malformed
 * templates, failed assertions) surface through this single exception type so
 * that callers can catch template problems uniformly.</p>
 */
public class RenderError extends RuntimeException {

    public RenderError(String message) {
        super(message);
    }

    public RenderError(String message, Throwable cause) {
        super(message, cause);
    }
}

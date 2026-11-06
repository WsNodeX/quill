package quill.core;

/**
 * A post-render transform applied to a prompt's textual output.
 *
 * <p>Hooks let you enforce invariants on generated text — trim stray
 * whitespace, cap length, normalise spacing — without editing every template.
 * Implementations must be pure: the same input always yields the same output.</p>
 */
@FunctionalInterface
public interface Hook {

    /** Transform {@code text} given the rendering context. */
    String apply(String text, RenderContext ctx);
}

package quill.cli;

import quill.core.RenderError;
import quill.registry.PromptRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Command-line front-end for Quill.
 *
 * <p>Usage:</p>
 * <pre>
 *   java quill.cli.Main version
 *   java quill.cli.Main render --registry prompts.json --prompt greet --set name=World
 * </pre>
 *
 * <p>The {@code render} command loads a registry file, renders the named prompt
 * with the supplied {@code --set key=value} pairs, and prints the result.</p>
 */
public final class Main {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Quill — prompt engineering toolkit. Try 'version' or 'render'.");
                return;
            }
            String cmd = args[0];
            switch (cmd) {
                case "version":
                    System.out.println("quill 1.0.0");
                    break;
                case "render":
                    runRender(args);
                    break;
                default:
                    System.out.println("unknown command: " + cmd);
                    System.exit(2);
            }
        } catch (RenderError e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runRender(String[] args) {
        String registryPath = null;
        String promptName = null;
        Map<String, Object> vars = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--registry") && i + 1 < args.length) {
                registryPath = args[++i];
            } else if (a.equals("--prompt") && i + 1 < args.length) {
                promptName = args[++i];
            } else if (a.equals("--set") && i + 1 < args.length) {
                String kv = args[++i];
                int eq = kv.indexOf('=');
                if (eq < 0) throw new RenderError("bad --set value (expected key=value): " + kv);
                vars.put(kv.substring(0, eq), kv.substring(eq + 1));
            }
        }
        if (registryPath == null) throw new RenderError("--registry is required");
        if (promptName == null) throw new RenderError("--prompt is required");
        PromptRegistry registry = new PromptRegistry().loadFromFile(registryPath);
        System.out.println(registry.render(promptName, vars));
    }
}

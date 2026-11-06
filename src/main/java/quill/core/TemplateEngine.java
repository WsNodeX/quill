package quill.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A tiny, dependency-free template engine for building LLM prompts.
 *
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code {{ var }}} &mdash; substitute a variable. Supports dotted
 *       ({@code user.name}) and indexed ({@code items[0]}) access.</li>
 *   <li>{@code {{ var | upper }}} &mdash; apply a filter. Chains are allowed
 *       and some filters take an argument, e.g. {@code {{ x | default: "n/a" }}}.</li>
 *   <li>{@code {% if cond %}...{% else %}...{% endif %}} &mdash; conditional
 *       blocks. Conditions may be a bare name or
 *       {@code name == "v"}, {@code n > 3}, etc.</li>
 *   <li>{@code {% for item in list %}...{% endfor %}} &mdash; iteration. Inside
 *       the body, {@code item} and {@code loop.index}/{@code loop.first}/
 *       {@code loop.last} are available.</li>
 *   <li>{@code {# a comment #}} &mdash; stripped from output.</li>
 * </ul>
 *
 * <p>The engine compiles a template once into an AST and reuses it across
 * renders; rendering is allocation-light and fully deterministic.</p>
 */
public final class TemplateEngine {

    public static final TemplateEngine DEFAULT = new TemplateEngine();

    private TemplateEngine() {}

    /** Compile {@code source} into a reusable {@link Template}. */
    public Template compile(String source) {
        Objects.requireNonNull(source, "source");
        List<Token> tokens = new Lexer(source).tokenize();
        List<Node> nodes = new Parser(tokens).parseBlock(null);
        return new Template(source, nodes);
    }

    /** Convenience: compile and immediately render. */
    public String render(String source, Map<String, Object> vars) {
        return compile(source).render(vars);
    }

    // ------------------------------------------------------------------
    //  Token + AST model
    // ------------------------------------------------------------------

    enum Kind { TEXT, VAR, TAG, COMMENT }

    static final class Token {
        final Kind kind;
        final String text;
        Token(Kind kind, String text) { this.kind = kind; this.text = text; }
    }

    interface Node {
        void render(RenderContext ctx, StringBuilder out);
    }

    static final class TextNode implements Node {
        private final String text;
        TextNode(String text) { this.text = text; }
        public void render(RenderContext ctx, StringBuilder out) { out.append(text); }
    }

    static final class VarNode implements Node {
        private final String base;
        private final List<Filter> filters;
        VarNode(String base, List<Filter> filters) { this.base = base; this.filters = filters; }
        public void render(RenderContext ctx, StringBuilder out) {
            String top = topName(base);
            boolean hasDefault = false;
            for (Filter f : filters) {
                if ("default".equals(f.name)) hasDefault = true;
            }
            if (!ctx.contains(top) && !hasDefault) {
                throw new RenderError("undefined variable: " + base);
            }
            Object value = ctx.contains(top) ? ctx.resolve(base) : null;
            String s = (value == null) ? "" : String.valueOf(value);
            for (Filter f : filters) {
                s = Filters.apply(f.name, s, f.arg);
            }
            out.append(s);
        }
    }

    static final class Filter {
        final String name;
        final String arg;
        Filter(String name, String arg) { this.name = name; this.arg = arg; }
    }

    static final class IfNode implements Node {
        private final String condition;
        private final List<Node> thenBranch;
        private final List<Node> elseBranch;
        IfNode(String condition, List<Node> thenBranch, List<Node> elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
        public void render(RenderContext ctx, StringBuilder out) {
            if (evalCondition(condition, ctx)) {
                for (Node n : thenBranch) n.render(ctx, out);
            } else if (elseBranch != null) {
                for (Node n : elseBranch) n.render(ctx, out);
            }
        }
    }

    static final class ForNode implements Node {
        private final String itemName;
        private final String listExpr;
        private final List<Node> body;
        ForNode(String itemName, String listExpr, List<Node> body) {
            this.itemName = itemName; this.listExpr = listExpr; this.body = body;
        }
        public void render(RenderContext ctx, StringBuilder out) {
            Object listObj = ctx.resolve(listExpr);
            if (listObj == null) {
                throw new RenderError("cannot iterate over undefined: " + listExpr);
            }
            List<Object> items = toList(listObj);
            int total = items.size();
            for (int i = 0; i < total; i++) {
                ctx.enterLoop(itemName, items.get(i), i + 1, total);
                for (Node n : body) n.render(ctx, out);
                ctx.exitLoop();
            }
        }
    }

    // ------------------------------------------------------------------
    //  Rendering entry point
    // ------------------------------------------------------------------

    /** A compiled template. */
    public static final class Template {
        private final String source;
        private final List<Node> nodes;

        Template(String source, List<Node> nodes) {
            this.source = source;
            this.nodes = nodes;
        }

        public String render(Map<String, Object> vars) {
            RenderContext ctx = new RenderContext(vars);
            StringBuilder out = new StringBuilder();
            for (Node n : nodes) n.render(ctx, out);
            return out.toString();
        }

        public String source() { return source; }
    }

    // ------------------------------------------------------------------
    //  Lexer
    // ------------------------------------------------------------------

    static final class Lexer {
        private final String src;
        private int pos;

        Lexer(String src) { this.src = src; }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            int n = src.length();
            int start = 0;
            while (pos < n) {
                if (src.startsWith("{{", pos)) {
                    flushText(tokens, start, pos);
                    int end = src.indexOf("}}", pos + 2);
                    if (end < 0) throw new RenderError("unterminated {{ at " + pos);
                    tokens.add(new Token(Kind.VAR, src.substring(pos + 2, end).trim()));
                    pos = end + 2; start = pos;
                } else if (src.startsWith("{#", pos)) {
                    flushText(tokens, start, pos);
                    int end = src.indexOf("#}", pos + 2);
                    if (end < 0) throw new RenderError("unterminated {# at " + pos);
                    tokens.add(new Token(Kind.COMMENT, src.substring(pos + 2, end)));
                    pos = end + 2; start = pos;
                } else if (src.startsWith("{%", pos)) {
                    flushText(tokens, start, pos);
                    int end = src.indexOf("%}", pos + 2);
                    if (end < 0) throw new RenderError("unterminated {% at " + pos);
                    tokens.add(new Token(Kind.TAG, src.substring(pos + 2, end).trim()));
                    pos = end + 2; start = pos;
                } else {
                    pos++;
                }
            }
            flushText(tokens, start, pos);
            return tokens;
        }

        private void flushText(List<Token> tokens, int from, int to) {
            if (to > from) {
                tokens.add(new Token(Kind.TEXT, src.substring(from, to)));
            }
        }
    }

    // ------------------------------------------------------------------
    //  Parser
    // ------------------------------------------------------------------

    static final class Parser {
        private final List<Token> tokens;
        private int pos;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        /** Parse a block of nodes until a TAG with one of {@code stopWords} is hit. */
        List<Node> parseBlock(String[] stopWords) {
            List<Node> nodes = new ArrayList<>();
            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                switch (t.kind) {
                    case TEXT:
                        nodes.add(new TextNode(t.text)); pos++; break;
                    case COMMENT:
                        pos++; break;
                    case VAR:
                        nodes.add(parseVar(t.text)); pos++; break;
                    case TAG:
                        String cmd = firstWord(t.text);
                        if (stopWords != null && contains(stopWords, cmd)) {
                            return nodes; // leave the stop token for the caller
                        }
                        if (cmd.equals("if")) {
                            pos++;
                            nodes.add(parseIf(t.text));
                        } else if (cmd.equals("for")) {
                            pos++;
                            nodes.add(parseFor(t.text));
                        } else if (cmd.equals("else") || cmd.equals("endif") || cmd.equals("endfor")) {
                            if (stopWords != null && contains(stopWords, cmd)) {
                                return nodes;
                            }
                            throw new RenderError("unexpected tag: " + cmd);
                        } else {
                            throw new RenderError("unknown tag: " + cmd);
                        }
                        break;
                }
            }
            if (stopWords != null) {
                throw new RenderError("unterminated block, expected one of: "
                        + String.join(", ", stopWords));
            }
            return nodes;
        }

        private VarNode parseVar(String text) {
            String[] parts = text.split("\\|", -1);
            String base = parts[0].trim();
            if (base.isEmpty()) throw new RenderError("empty variable expression");
            List<Filter> filters = new ArrayList<>();
            for (int k = 1; k < parts.length; k++) {
                String f = parts[k].trim();
                int colon = f.indexOf(':');
                if (colon < 0) {
                    filters.add(new Filter(f, null));
                } else {
                    String arg = f.substring(colon + 1).trim();
                    if (arg.length() >= 2 && arg.startsWith("\"") && arg.endsWith("\"")) {
                        arg = arg.substring(1, arg.length() - 1);
                    }
                    filters.add(new Filter(f.substring(0, colon).trim(), arg));
                }
            }
            return new VarNode(base, filters);
        }

        private IfNode parseIf(String text) {
            String cond = text.substring(2).trim(); // drop leading "if"
            List<Node> thenBranch = parseBlock(new String[]{"else", "endif"});
            List<Node> elseBranch = null;
            if (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (firstWord(t.text).equals("else")) {
                    pos++; // consume else
                    elseBranch = parseBlock(new String[]{"endif"});
                }
            }
            if (pos >= tokens.size() || !firstWord(tokens.get(pos).text).equals("endif")) {
                throw new RenderError("unterminated {% if %}");
            }
            pos++; // consume endif
            return new IfNode(cond, thenBranch, elseBranch);
        }

        private ForNode parseFor(String text) {
            String rest = text.substring(3).trim(); // drop leading "for"
            int in = rest.toLowerCase().indexOf(" in ");
            if (in < 0) throw new RenderError("malformed for loop: " + text);
            String itemName = rest.substring(0, in).trim();
            String listExpr = rest.substring(in + 4).trim();
            List<Node> body = parseBlock(new String[]{"endfor"});
            if (pos >= tokens.size() || !firstWord(tokens.get(pos).text).equals("endfor")) {
                throw new RenderError("unterminated {% for %}");
            }
            pos++; // consume endfor
            return new ForNode(itemName, listExpr, body);
        }

        private static String firstWord(String s) {
            int sp = s.indexOf(' ');
            return (sp < 0 ? s : s.substring(0, sp)).trim().toLowerCase();
        }

        private static boolean contains(String[] arr, String v) {
            for (String a : arr) if (a.equals(v)) return true;
            return false;
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    static String topName(String expr) {
        String e = expr.trim();
        int dot = e.indexOf('.');
        int bracket = e.indexOf('[');
        int end = Math.min(dot < 0 ? Integer.MAX_VALUE : dot, bracket < 0 ? Integer.MAX_VALUE : bracket);
        return end == Integer.MAX_VALUE ? e : e.substring(0, end);
    }

    static List<Object> toList(Object obj) {
        List<Object> out = new ArrayList<>();
        if (obj instanceof List) {
            out.addAll((List<?>) obj);
        } else if (obj instanceof Collection) {
            out.addAll((Collection<?>) obj);
        } else if (obj.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) out.add(java.lang.reflect.Array.get(obj, i));
        } else {
            throw new RenderError("cannot iterate over " + obj.getClass().getSimpleName());
        }
        return out;
    }

    static boolean evalCondition(String cond, RenderContext ctx) {
        String c = cond.trim();
        if (c.isEmpty()) return false;
        String[] ops = {"==", "!=", ">=", "<=", ">", "<"};
        for (String op : ops) {
            int idx = c.indexOf(op);
            if (idx > 0) {
                String left = c.substring(0, idx).trim();
                String right = c.substring(idx + op.length()).trim();
                return compare(ctx.resolve(left), parseLiteralOrVar(right, ctx), op);
            }
        }
        Object v = ctx.resolve(c);
        if (v == null && !ctx.contains(topName(c))) {
            throw new RenderError("undefined variable in condition: " + c);
        }
        return RenderContext.isTruthy(v);
    }

    private static Object parseLiteralOrVar(String raw, RenderContext ctx) {
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.equals("true")) return Boolean.TRUE;
        if (raw.equals("false")) return Boolean.FALSE;
        if (raw.matches("-?\\d+(\\.\\d+)?")) {
            return raw.contains(".") ? Double.parseDouble(raw) : Long.parseLong(raw);
        }
        return ctx.resolve(raw);
    }

    @SuppressWarnings("unchecked")
    static boolean compare(Object left, Object right, String op) {
        boolean bothNum = left instanceof Number && right instanceof Number;
        if (bothNum) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            switch (op) {
                case "==": return l == r;
                case "!=": return l != r;
                case ">":  return l > r;
                case ">=": return l >= r;
                case "<":  return l < r;
                case "<=": return l <= r;
            }
        }
        if (op.equals("==")) {
            if (left instanceof Boolean || right instanceof Boolean) {
                return Boolean.valueOf(String.valueOf(left)).equals(Boolean.valueOf(String.valueOf(right)));
            }
            return Objects.equals(String.valueOf(left), String.valueOf(right));
        }
        if (op.equals("!=")) {
            return !Objects.equals(String.valueOf(left), String.valueOf(right));
        }
        // relational on non-numbers: compare as strings
        int cmp = String.valueOf(left).compareTo(String.valueOf(right));
        switch (op) {
            case ">":  return cmp > 0;
            case ">=": return cmp >= 0;
            case "<":  return cmp < 0;
            case "<=": return cmp <= 0;
        }
        throw new RenderError("unsupported operator: " + op);
    }
}

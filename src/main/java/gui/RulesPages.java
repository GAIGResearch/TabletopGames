package gui;

import games.GameType;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A game's rules, written in Markdown in {@code data/rules/<GameType>}: one file for each page (each tab of the GUI),
 * in the order of their file names, e.g. {@code data/rules/Whist/1-rules.md} and {@code 2-how-to-play.md}. A
 * page's title is given by a first line {@code <!-- title: How to Play -->}, or else made from the file name. Use
 * plain Markdown (headings, paragraphs, lists, emphasis, links and tables), whose HTML a {@link gui.views.RulesView} can
 * show as well as a browser. A heading can be linked to as {@code #its-text-in-lower-case}.
 * <p>
 * The text is a template, filled in from the game's parameters, so that the rules are those of the game being played:
 * <ul>
 *     <li>{@code {name}} is the value of the parameter (a field of the parameters object, or a method, as
 *     {@code name()}, {@code getName()} or {@code isName()}); a dotted path follows the values, e.g.
 *     {@code {map.victoryCentres}}. Braces may also hold arithmetic: {@code {handSize + 1}},
 *     {@code {78 - 3 * handSize}}, {@code {tradeValue(3)}} (see {@link Expression}). {@code \{} is a brace.</li>
 *     <li>{@code <!-- if cond -->}, {@code <!-- elif cond -->}, {@code <!-- else -->} and {@code <!-- end -->} keep
 *     the text of the first branch whose condition holds. A condition is a parameter, true if it is true, non-zero or
 *     not empty; {@code not} a parameter; or a parameter compared with a number, a word (an enum constant, say) or a
 *     quoted string by {@code == != < <= > >=}; joined by {@code and} and {@code or} (which binds less tightly).</li>
 * </ul>
 * A name that is not a parameter is an error, so that a mistake is found when the rules are first shown.
 */
public final class RulesPages {

    /**
     * A page of rules: its title, and its body as HTML (without the html and body tags).
     */
    public record Page(String title, String html) {
    }

    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create(), HeadingAnchorExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private static final Pattern TITLE = Pattern.compile("\\A\\s*<!--\\s*title:\\s*(.*?)\\s*-->[ \\t]*\\R?");
    // a directive, with the line it is on when it is alone on the line
    private static final Pattern DIRECTIVE = Pattern.compile(
            "(?m)(^[ \\t]*)?<!--\\s*(if|elif|else|end)\\b\\s*(.*?)\\s*-->([ \\t]*\\R)?");
    private static final Pattern VALUE = Pattern.compile("(\\\\?)\\{([\\w(-][^{}\\n]*)}");
    private static final Pattern COMPARISON = Pattern.compile(
            "([A-Za-z_][\\w.]*)\\s*(==|!=|<=|>=|<|>)\\s*(\"[^\"]*\"|'[^']*'|-?[\\w.]+)");

    private RulesPages() {
    }

    /**
     * The game's pages, filled in from the parameters (none if it has no rules written).
     */
    public static List<Page> load(GameType game, Object params) {
        return load(Path.of("data", "rules", game.name()), params);
    }

    /**
     * The pages in the directory, filled in from the parameters (none if there is no such directory).
     */
    static List<Page> load(Path dir, Object params) {
        List<Page> pages = new ArrayList<>();
        if (!Files.isDirectory(dir)) return pages;
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(".md")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list " + dir, e);
        }
        for (Path file : files) {
            try {
                pages.add(page(file.getFileName().toString(), Files.readString(file), params));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + file, e);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(file + ": " + e.getMessage(), e);
            }
        }
        return pages;
    }

    /**
     * A page from the Markdown in a file of this name.
     */
    static Page page(String fileName, String markdown, Object params) {
        String title;
        Matcher m = TITLE.matcher(markdown);
        if (m.find()) {
            title = m.group(1);
            markdown = markdown.substring(m.end());
        } else {
            // "2-how-to-play.md" is "How to play"
            String name = fileName.replaceFirst("\\.md$", "").replaceFirst("^\\d+[-_ ]*", "").replace('-', ' ');
            title = name.isEmpty() ? fileName : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return new Page(title, markdownToHtml(fill(markdown, params)));
    }

    public static String markdownToHtml(String markdown) {
        return RENDERER.render(PARSER.parse(markdown));
    }

    /**
     * The template filled in from the parameters.
     */
    static String fill(String template, Object params) {
        StringBuilder out = new StringBuilder();
        // for each if being read: whether its text is kept, and whether a branch has been kept
        Deque<boolean[]> ifs = new ArrayDeque<>();
        Matcher m = DIRECTIVE.matcher(template);
        int from = 0;
        while (m.find()) {
            // text not kept is filled in all the same, so that a mistake in any branch is found
            String text = values(template.substring(from, m.start()), params);
            if (keeping(ifs)) out.append(text);
            String kind = m.group(2), cond = m.group(3);
            switch (kind) {
                case "if" -> {
                    boolean holds = holds(cond, params) && keeping(ifs);
                    ifs.push(new boolean[]{holds, holds});
                }
                case "elif" -> {
                    boolean[] top = top(ifs, kind);
                    top[0] = holds(cond, params) && !top[1] && keepingOutside(ifs);
                    top[1] |= top[0];
                }
                case "else" -> {
                    boolean[] top = top(ifs, kind);
                    top[0] = !top[1] && keepingOutside(ifs);
                    top[1] = true;
                }
                default -> {
                    top(ifs, kind);
                    ifs.pop();
                }
            }
            // A directive alone on its line takes the line with it. Otherwise the space before it and the line end
            // after it belong to the text that follows it, and are kept with that text.
            boolean ownLine = m.group(1) != null && m.group(4) != null;
            if (!ownLine && keeping(ifs)) {
                if (m.group(1) != null) out.append(m.group(1));
                if (m.group(4) != null) out.append(m.group(4));
            }
            from = m.end();
        }
        if (!ifs.isEmpty()) throw new IllegalArgumentException("An if has no end");
        out.append(values(template.substring(from), params));
        return out.toString();
    }

    private static boolean keeping(Deque<boolean[]> ifs) {
        for (boolean[] i : ifs)
            if (!i[0]) return false;
        return true;
    }

    // whether the text around the innermost if is kept
    private static boolean keepingOutside(Deque<boolean[]> ifs) {
        boolean[] top = ifs.pop();
        boolean keeping = keeping(ifs);
        ifs.push(top);
        return keeping;
    }

    private static boolean[] top(Deque<boolean[]> ifs, String kind) {
        if (ifs.isEmpty()) throw new IllegalArgumentException("An " + kind + " with no if");
        return ifs.peek();
    }

    private static String values(String text, Object params) {
        Matcher m = VALUE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String replacement = m.group(1).isEmpty()
                    ? format(new Expression(m.group(2), params).evaluate()) : "{" + m.group(2) + "}";
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    static boolean holds(String cond, Object params) {
        for (String any : cond.split("\\s+or\\s+")) {
            boolean all = true;
            for (String term : any.split("\\s+and\\s+"))
                all &= term(term.trim(), params);
            if (all) return true;
        }
        return false;
    }

    private static boolean term(String term, Object params) {
        Matcher m = COMPARISON.matcher(term);
        if (m.matches()) {
            Object value = value(m.group(1), params);
            String op = m.group(2), literal = m.group(3);
            if (literal.startsWith("\"") || literal.startsWith("'")) literal = literal.substring(1, literal.length() - 1);
            int c = compare(value, literal);
            return switch (op) {
                case "==" -> c == 0;
                case "!=" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                default -> c >= 0;
            };
        }
        if (term.startsWith("not ")) return !truthy(value(term.substring(4).trim(), params));
        if (term.matches("[A-Za-z_][\\w.]*")) return truthy(value(term, params));
        throw new IllegalArgumentException("Not a condition: " + term);
    }

    private static int compare(Object value, String literal) {
        if (value instanceof Number n) {
            try {
                return Double.compare(n.doubleValue(), Double.parseDouble(literal));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a number: " + literal);
            }
        }
        return format(value).compareTo(literal);
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value instanceof java.util.Collection<?> c) return !c.isEmpty();
        return value != null && !format(value).isEmpty();
    }

    private static String format(Object value) {
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
        }
        if (value instanceof Enum<?> e) return e.name();
        return String.valueOf(value);
    }

    /**
     * The value at the dotted path from the parameters.
     */
    static Object value(String path, Object params) {
        Object value = params;
        for (String name : path.split("\\.")) {
            if (value == null) throw new IllegalArgumentException("No value for " + path + ": " + name + " is on null");
            value = member(value, name, List.of(), path);
        }
        return value;
    }

    /**
     * A value in braces: a parameter, or arithmetic ({@code + - * /} and brackets) on parameters and numbers. A
     * parameter that is a method with arguments is called with numbers, as {@code tradeValue(3)}. Division of whole
     * numbers is whole (rounded down).
     */
    static final class Expression {
        private final String text;
        private final Object params;
        private int at;

        Expression(String text, Object params) {
            this.text = text;
            this.params = params;
        }

        Object evaluate() {
            Object value = sum();
            space();
            if (at < text.length()) throw error("Unexpected '" + text.charAt(at) + "'");
            return value;
        }

        private Object sum() {
            Object value = product();
            for (char op; (op = peek()) == '+' || op == '-'; ) {
                at++;
                value = arithmetic(op, value, product());
            }
            return value;
        }

        private Object product() {
            Object value = factor();
            for (char op; (op = peek()) == '*' || op == '/'; ) {
                at++;
                value = arithmetic(op, value, factor());
            }
            return value;
        }

        private Object factor() {
            char c = peek();
            if (c == '(') {
                at++;
                Object value = sum();
                expect(')');
                return value;
            }
            if (c == '-') {
                at++;
                return arithmetic('-', 0L, factor());
            }
            if (Character.isDigit(c)) {
                int start = at;
                while (at < text.length() && (Character.isDigit(text.charAt(at)) || text.charAt(at) == '.')) at++;
                String number = text.substring(start, at);
                return number.contains(".") ? (Object) Double.parseDouble(number) : (Object) Long.parseLong(number);
            }
            if (Character.isLetter(c) || c == '_') return path();
            throw error(at < text.length() ? "Unexpected '" + c + "'" : "Expected a value");
        }

        // a dotted path, whose names may be methods called with arguments
        private Object path() {
            Object value = params;
            while (true) {
                int start = at;
                while (at < text.length() && (Character.isLetterOrDigit(text.charAt(at)) || text.charAt(at) == '_')) at++;
                String name = text.substring(start, at);
                List<Object> args = new ArrayList<>();
                if (peek() == '(') {
                    at++;
                    if (peek() != ')') {
                        args.add(sum());
                        while (peek() == ',') {
                            at++;
                            args.add(sum());
                        }
                    }
                    expect(')');
                }
                if (value == null) throw error(name + " is on null");
                value = member(value, name, args, text);
                if (at < text.length() && text.charAt(at) == '.') at++;
                else return value;
            }
        }

        private Object arithmetic(char op, Object a, Object b) {
            if (!(a instanceof Number x) || !(b instanceof Number y))
                throw error("Arithmetic on something not a number: " + format(a) + " " + op + " " + format(b));
            boolean whole = !(x instanceof Double || x instanceof Float || y instanceof Double || y instanceof Float);
            if (whole) {
                long p = x.longValue(), q = y.longValue();
                return switch (op) {
                    case '+' -> p + q;
                    case '-' -> p - q;
                    case '*' -> p * q;
                    default -> Math.floorDiv(p, q);
                };
            }
            double p = x.doubleValue(), q = y.doubleValue();
            return switch (op) {
                case '+' -> p + q;
                case '-' -> p - q;
                case '*' -> p * q;
                default -> p / q;
            };
        }

        private char peek() {
            space();
            return at < text.length() ? text.charAt(at) : '\0';
        }

        private void space() {
            while (at < text.length() && text.charAt(at) == ' ') at++;
        }

        private void expect(char c) {
            if (peek() != c) throw error("Expected '" + c + "'");
            at++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " in {" + text + "}");
        }
    }

    private static Object member(Object owner, String name, List<Object> args, String path) {
        String capital = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            for (String methodName : new String[]{name, "get" + capital, "is" + capital}) {
                for (Method method : c.getDeclaredMethods()) {
                    if (!method.getName().equals(methodName) || method.getParameterCount() != args.size()) continue;
                    try {
                        method.setAccessible(true);
                        return method.invoke(owner, arguments(method, args, path));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalArgumentException("Could not read " + path, e);
                    }
                }
            }
            if (!args.isEmpty()) continue;
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException e) {
                // try the superclass
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Could not read " + path, e);
            }
        }
        throw new IllegalArgumentException("No parameter " + name + " (in {" + path + "}) on " + owner.getClass().getSimpleName());
    }

    // the numbers as the method's parameter types
    private static Object[] arguments(Method method, List<Object> args, String path) {
        Class<?>[] types = method.getParameterTypes();
        Object[] result = new Object[args.size()];
        for (int i = 0; i < result.length; i++) {
            if (!(args.get(i) instanceof Number n))
                throw new IllegalArgumentException("Not a number: " + format(args.get(i)) + " in {" + path + "}");
            Class<?> t = types[i];
            if (t == int.class || t == Integer.class) result[i] = n.intValue();
            else if (t == long.class || t == Long.class) result[i] = n.longValue();
            else if (t == double.class || t == Double.class) result[i] = n.doubleValue();
            else if (t == float.class || t == Float.class) result[i] = n.floatValue();
            else throw new IllegalArgumentException(method.getName() + " takes a " + t.getSimpleName() + " in {" + path + "}");
        }
        return result;
    }
}

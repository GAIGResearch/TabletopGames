package gui;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A game's rules written in Markdown, in its data directory: {@code rules.md} for the Rules page and
 * {@code how-to-play.md} for How to Play, each turned into HTML. Use plain Markdown (headings, paragraphs, lists,
 * emphasis, links and tables), whose HTML a {@link gui.views.RulesView} can show as well as a browser. A heading can be
 * linked to as {@code #its-text-in-lower-case}.
 */
public final class RulesPages {

    /**
     * A page of rules: its title, and its body as HTML (without the html and body tags).
     */
    public record Page(String title, String html) {
    }

    // the files read, and the title of each page
    private static final String[][] FILES = {{"rules.md", "Rules"}, {"how-to-play.md", "How to Play"}};

    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create(), HeadingAnchorExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private RulesPages() {
    }

    /**
     * The pages in the data directory (none if it is null or has no rules files).
     */
    public static List<Page> fromDataDirectory(String dataPath) {
        List<Page> pages = new ArrayList<>();
        if (dataPath == null) return pages;
        for (String[] file : FILES) {
            Path path = Path.of(dataPath, file[0]);
            if (!Files.isRegularFile(path)) continue;
            try {
                pages.add(new Page(file[1], markdownToHtml(Files.readString(path))));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + path, e);
            }
        }
        return pages;
    }

    public static String markdownToHtml(String markdown) {
        return RENDERER.render(PARSER.parse(markdown));
    }
}

package gui;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class RulesPagesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void markdownBecomesHtmlWithLinkableHeadingsAndTables() {
        String html = RulesPages.markdownToHtml("## The year\n\nEach year has *two* turns.\n\n| Unit | Moves |\n|---|---|\n| Army | Land |\n");
        assertTrue(html, html.contains("<h2 id=\"the-year\">The year</h2>"));
        assertTrue(html, html.contains("<em>two</em>"));
        assertTrue(html, html.contains("<td>Army</td>"));
    }

    @Test
    public void pagesAreReadFromTheDataDirectoryInOrder() throws IOException {
        Files.writeString(folder.getRoot().toPath().resolve("how-to-play.md"), "Click a card.");
        Files.writeString(folder.getRoot().toPath().resolve("rules.md"), "# Rules");
        List<RulesPages.Page> pages = RulesPages.fromDataDirectory(folder.getRoot().getPath());
        assertEquals(2, pages.size());
        assertEquals("Rules", pages.get(0).title());
        assertEquals("How to Play", pages.get(1).title());
        assertTrue(pages.get(1).html().contains("<p>Click a card.</p>"));
    }

    @Test
    public void noDirectoryOrNoFilesIsNoPages() {
        assertTrue(RulesPages.fromDataDirectory(null).isEmpty());
        assertTrue(RulesPages.fromDataDirectory(folder.getRoot().getPath()).isEmpty());
    }
}

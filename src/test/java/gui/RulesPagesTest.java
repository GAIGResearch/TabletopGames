package gui;

import games.GameType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class RulesPagesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    enum Mode {TURN_UP, ROTATION}

    static class Map {
        int victoryCentres() {
            return 18;
        }
    }

    static class Params {
        public int nDeals = 1;
        double target = 2.5;
        boolean noTrumps = true;
        Mode trumpMode = Mode.TURN_UP;
        String name = "";

        Map getMap() {
            return new Map();
        }
    }

    @Test
    public void markdownBecomesHtmlWithLinkableHeadingsAndTables() {
        String html = RulesPages.markdownToHtml("## The year\n\nEach year has *two* turns.\n\n| Unit | Moves |\n|---|---|\n| Army | Land |\n");
        assertTrue(html, html.contains("<h2 id=\"the-year\">The year</h2>"));
        assertTrue(html, html.contains("<em>two</em>"));
        assertTrue(html, html.contains("<td>Army</td>"));
    }

    @Test
    public void valuesAreFilledInFromFieldsAndMethods() {
        Params p = new Params();
        assertEquals("1 deal, to 2.5, of 18 centres, TURN_UP, {kept}",
                RulesPages.fill("{nDeals} deal, to {target}, of {map.victoryCentres} centres, {trumpMode}, \\{kept}", p));
    }

    @Test
    public void theFirstBranchWhoseConditionHoldsIsKept() {
        String template = """
                Start.
                <!-- if nDeals == 1 -->
                One deal.
                <!-- elif nDeals < 5 -->
                A few deals.
                <!-- else -->
                {nDeals} deals.
                <!-- end -->
                End.
                """;
        Params p = new Params();
        assertEquals("Start.\nOne deal.\nEnd.\n", RulesPages.fill(template, p));
        p.nDeals = 3;
        assertEquals("Start.\nA few deals.\nEnd.\n", RulesPages.fill(template, p));
        p.nDeals = 7;
        assertEquals("Start.\n7 deals.\nEnd.\n", RulesPages.fill(template, p));
    }

    @Test
    public void conditionsCombineAndNestAndMayBeInline() {
        Params p = new Params();
        assertEquals("Trumps turned up, with no-trump deals.",
                RulesPages.fill("Trumps <!-- if trumpMode == TURN_UP and nDeals >= 1 -->turned up<!-- else -->rotate<!-- end -->"
                        + "<!-- if noTrumps --><!-- if not name -->, with no-trump deals<!-- end --><!-- end -->.", p));
        assertTrue(RulesPages.holds("trumpMode == ROTATION or noTrumps", p));
        assertFalse(RulesPages.holds("trumpMode != TURN_UP or not noTrumps", p));
        assertTrue(RulesPages.holds("name == \"\"", p));
    }

    @Test
    public void anUnknownNameIsAnErrorEvenInABranchNotKept() {
        Params p = new Params();
        assertThrows(IllegalArgumentException.class, () -> RulesPages.fill("{nDeal}", p));
        assertThrows(IllegalArgumentException.class, () -> RulesPages.fill("<!-- if nDeals == 2 -->{oops}<!-- end -->", p));
        assertThrows(IllegalArgumentException.class, () -> RulesPages.fill("<!-- if nDeals == 2 -->x", p));
    }

    @Test
    public void pagesAreReadFromTheRulesDirectoryInOrder() throws IOException {
        Path rules = Files.createDirectory(folder.getRoot().toPath().resolve("rules"));
        Files.writeString(rules.resolve("2-how-to-play.md"), "<!-- title: How to Play -->\nClick a card.");
        Files.writeString(rules.resolve("1-rules.md"), "# Rules for {nDeals} deal");
        List<RulesPages.Page> pages = RulesPages.load(folder.getRoot().toPath().resolve("rules"), new Params());
        assertEquals(2, pages.size());
        assertEquals("Rules", pages.get(0).title());
        assertTrue(pages.get(0).html().contains("Rules for 1 deal"));
        assertEquals("How to Play", pages.get(1).title());
        assertEquals("<p>Click a card.</p>\n", pages.get(1).html());
    }

    @Test
    public void everyGamesRulesFillInFromItsDefaultParameters() {
        for (GameType game : GameType.values()) {
            List<RulesPages.Page> pages = RulesPages.load(game, game.createParameters(1));
            for (RulesPages.Page page : pages)
                assertFalse(game + " " + page.title(), page.html().isBlank());
        }
    }

    @Test
    public void noDirectoryOrNoFilesIsNoPages() {
        assertTrue(RulesPages.load(folder.getRoot().toPath().resolve("none"), new Params()).isEmpty());
        assertTrue(RulesPages.load(folder.getRoot().toPath().resolve("rules"), new Params()).isEmpty());
    }
}

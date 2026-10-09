package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import gui.views.RulesView;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;

import static org.junit.Assert.*;

public class ChromeReaderTest {

    @Test
    public void standardInfoLinesAreReadIntoFields() {
        JsonObject info = ChromeReader.info(List.of("Game status: GAME_ONGOING", "Player Scores: 3, 0, 12",
                "Game phase: Main", "Turn: 4; Round: 2", "Current player: 1"));
        assertEquals(3.0, info.getAsJsonArray("scores").get(0).getAsDouble(), 0);
        assertEquals(12.0, info.getAsJsonArray("scores").get(2).getAsDouble(), 0);
        assertEquals("Main", info.get("phase").getAsString());
        assertEquals(4, info.get("turn").getAsInt());
        assertEquals(2, info.get("round").getAsInt());
        assertEquals(1, info.get("current").getAsInt());
        assertEquals(0, info.getAsJsonArray("lines").size());
    }

    @Test
    public void infoLinesNotInTheStandardFormAreKeptAsTheyAre() {
        // Rummy hides the scores until the end of a single deal, so its line is shown, not read as scores
        JsonObject info = ChromeReader.info(List.of("Player Scores: shown at the end", "Pot: 20 / ", "<html><b>x</b></html>"));
        assertFalse(info.has("scores"));
        JsonArray lines = info.getAsJsonArray("lines");
        assertEquals(3, lines.size());
        assertEquals("Player Scores: shown at the end", lines.get(0).getAsString());
        assertEquals("Pot: 20 / ", lines.get(1).getAsString());
    }

    @Test
    public void historyLinesNameTheirPlayer() {
        JsonObject line = ChromeReader.historyLine("Player 2 : Play 7 of Hearts");
        assertEquals(2, line.get("player").getAsInt());
        assertEquals("Play 7 of Hearts", line.get("text").getAsString());

        JsonObject other = ChromeReader.historyLine("Player 0 finishes at position 1 with score: 30");
        assertEquals(-1, other.get("player").getAsInt());
        assertEquals("Player 0 finishes at position 1 with score: 30", other.get("text").getAsString());
    }

    @Test
    public void svgPathIsMovedByTheOffset() {
        assertEquals("M12 25L42 25L42 45L12 45L12 25Z", ChromeReader.svgPath(new Rectangle(2, 5, 30, 20), 10, 20));
        Path2D.Double half = new Path2D.Double();
        half.moveTo(0.25, 0);
        half.lineTo(1.5, 2);
        half.closePath();
        assertEquals("M0.3 0L1.5 2Z", ChromeReader.svgPath(half, 0, 0));
    }

    @Test
    public void svgPathFlattensCurves() {
        String path = ChromeReader.svgPath(new Ellipse2D.Double(0, 0, 20, 20), 0, 0);
        assertTrue(path.startsWith("M"));
        assertFalse(path.contains("C") || path.contains("Q"));
        assertTrue(path.endsWith("Z"));
    }

    @Test
    public void rulesAreFoundInTheComponentsOfARulesTab() {
        assertEquals("<h2>Rules</h2>", ChromeReader.rulesHtml(new RulesView("<h2>Rules</h2>", 300)));

        String long_ = "<html><p>" + "Play a card. ".repeat(10) + "</p></html>";
        JPanel panel = new JPanel();
        panel.add(new JScrollPane(new JLabel(long_)));
        assertEquals(long_, ChromeReader.rulesHtml(panel));

        JTextArea area = new JTextArea("Lead <any> card & follow suit.\n".repeat(5));
        assertTrue(ChromeReader.rulesHtml(area).contains("Lead &lt;any&gt; card &amp; follow suit."));

        // a short label is a caption, not rules
        assertNull(ChromeReader.rulesHtml(new JLabel("Score: 4")));
    }

    @Test
    public void kindIsTheActionsClassAndNullForNone() {
        assertEquals("DoNothing", ChromeReader.kind(new DoNothing()));
        AbstractAction anonymous = new DoNothing() {
        };
        assertEquals("DoNothing", ChromeReader.kind(anonymous));
        assertNull(ChromeReader.kind(null));
    }
}

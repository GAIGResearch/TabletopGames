package web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;

import static org.junit.Assert.*;

public class ChromeReaderTest {

    @Test
    public void standardInfoLinesAreReadIntoFields() {
        ObjectNode info = ChromeReader.info(List.of("Game status: GAME_ONGOING", "Player Scores: 3, 0, 12",
                "Game phase: Main", "Turn: 4; Round: 2", "Current player: 1"));
        assertEquals(3.0, info.get("scores").get(0).asDouble(), 0);
        assertEquals(12.0, info.get("scores").get(2).asDouble(), 0);
        assertEquals("Main", info.get("phase").asText());
        assertEquals(4, info.get("turn").asInt());
        assertEquals(2, info.get("round").asInt());
        assertEquals(1, info.get("current").asInt());
        assertEquals(0, info.get("lines").size());
    }

    @Test
    public void infoLinesNotInTheStandardFormAreKeptAsTheyAre() {
        // Rummy hides the scores until the end of a single deal, so its line is shown, not read as scores
        ObjectNode info = ChromeReader.info(List.of("Player Scores: shown at the end", "Pot: 20 / ", "<html><b>x</b></html>"));
        assertFalse(info.has("scores"));
        JsonNode lines = info.get("lines");
        assertEquals(3, lines.size());
        assertEquals("Player Scores: shown at the end", lines.get(0).asText());
        assertEquals("Pot: 20 / ", lines.get(1).asText());
    }

    @Test
    public void historyLinesNameTheirPlayer() {
        ObjectNode line = ChromeReader.historyLine("Player 2 : Play 7 of Hearts");
        assertEquals(2, line.get("player").asInt());
        assertEquals("Play 7 of Hearts", line.get("text").asText());

        ObjectNode other = ChromeReader.historyLine("Player 0 finishes at position 1 with score: 30");
        assertEquals(-1, other.get("player").asInt());
        assertEquals("Player 0 finishes at position 1 with score: 30", other.get("text").asText());
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
    public void kindIsTheActionsClassAndNullForNone() {
        assertEquals("DoNothing", ChromeReader.kind(new DoNothing()));
        AbstractAction anonymous = new DoNothing() {
        };
        assertEquals("DoNothing", ChromeReader.kind(anonymous));
        assertNull(ChromeReader.kind(null));
    }
}

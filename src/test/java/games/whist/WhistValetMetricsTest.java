package games.whist;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.metrics.Event;
import evaluation.metrics.GameMetrics;
import evaluation.metrics.MetricsTestRunner;
import games.GameType;
import games.whist.metrics.WhistValetMetrics;
import org.junit.BeforeClass;
import org.junit.Test;
import players.simple.RandomPlayer;
import tech.tablesaw.api.Table;

import java.util.*;

import static org.junit.Assert.*;

public class WhistValetMetricsTest {

    static Map<Event.GameEvent, Table> tables;

    @BeforeClass
    public static void runGames() throws Exception {
        AbstractPlayer focus = new RandomPlayer();
        focus.setName("Focus");
        tables = MetricsTestRunner.runFocus(GameType.Whist, 4, null, List.of(focus, new RandomPlayer()), 10, true,
                new GameMetrics.ActionsReduced(), new WhistValetMetrics.GameSummary());
    }

    private static double value(Table table, int row, String column) {
        return Double.parseDouble(table.column("GameSummary(" + column + ")").getString(row));
    }

    private static String text(Table table, int row, String column) {
        return table.column("GameSummary(" + column + ")").getString(row);
    }

    @Test
    public void oneRowPerGameWithEveryTrickCounted() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        assertEquals(10, games.rowCount());
        int nDeals = new WhistParameters().nDeals;
        for (int g = 0; g < games.rowCount(); g++) {
            assertEquals(52 * nDeals, value(games, g, "Actions"), 0.0);
            assertTrue(value(games, g, "Decisions") <= value(games, g, "Actions"));
            assertEquals(13 * nDeals, value(games, g, "Team-0 Tricks") + value(games, g, "Team-1 Tricks"), 0.0);
            for (int t = 0; t < 2; t++)
                assertTrue(value(games, g, "Team-" + t + " TrumpTricks") <= value(games, g, "Team-" + t + " Tricks"));
            assertEquals(nDeals, text(games, g, "TrumpSuits").split("\\|").length);
            assertEquals("3|0|1|2", text(games, g, "Dealers"));
        }
    }

    @Test
    public void scoresPositionsAndResultsAgree() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        for (int g = 0; g < games.rowCount(); g++) {
            for (int p = 0; p < 4; p++) {
                assertEquals(value(games, g, "Team-" + (p % 2) + " Score"), value(games, g, "Player-" + p + " Score"), 0.0);
                String result = text(games, g, "Player-" + p + " Result");
                double position = value(games, g, "Player-" + p + " Position");
                assertEquals(result.equals("Loss") ? 3.0 : 1.0, position, 0.0);
            }
        }
    }

    @Test
    public void focusPlayerIsInBothSeatsOfOneTeam() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        for (int g = 0; g < games.rowCount(); g++) {
            int team = text(games, g, "Player-0 Type").equals("Focus") ? 0 : 1;
            for (int p = 0; p < 4; p++)
                assertEquals(p % 2 == team, text(games, g, "Player-" + p + " Type").equals("Focus"));
        }
    }

    @Test
    public void valetParametersPlayOneHand() throws Exception {
        AbstractPlayer focus = new RandomPlayer();
        focus.setName("Focus");
        AbstractParameters params = AbstractParameters.createFromFile(GameType.Whist, "data/whist/Whist_Valet.json");
        Table games = MetricsTestRunner.runFocus(GameType.Whist, 4, params, List.of(focus, new RandomPlayer()), 10, true,
                new WhistValetMetrics.GameSummary()).get(Event.GameEvent.GAME_OVER);
        assertEquals(10, games.rowCount());
        for (int g = 0; g < games.rowCount(); g++) {
            assertEquals(52, value(games, g, "Actions"), 0.0);
            assertEquals(13, value(games, g, "Team-0 Tricks") + value(games, g, "Team-1 Tricks"), 0.0);
            assertFalse(text(games, g, "TrumpSuits").contains("|"));
            assertEquals("3", text(games, g, "Dealers"));
            for (int t = 0; t < 2; t++)
                assertEquals(Math.max(0, value(games, g, "Team-" + t + " Tricks") - 6), value(games, g, "Team-" + t + " Score"), 0.0);
        }
    }

    @Test
    public void perDecisionRecordHasNoGameSummaryColumns() {
        Table actions = tables.get(Event.GameEvent.ACTION_CHOSEN);
        assertEquals(10 * 52 * new WhistParameters().nDeals, actions.rowCount());
        assertTrue(actions.containsColumn("ActionsReduced(Size)"));
        assertTrue(actions.containsColumn("ActionsReduced(PlayerType)"));
        assertTrue(actions.columnNames().stream().noneMatch(c -> c.startsWith("GameSummary")));
    }
}

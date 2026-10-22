package games.gofish;

import core.AbstractPlayer;
import evaluation.metrics.Event;
import evaluation.metrics.MetricsTestRunner;
import games.GameType;
import games.gofish.metrics.GoFishValetMetrics;
import org.junit.BeforeClass;
import org.junit.Test;
import players.simple.RandomPlayer;
import tech.tablesaw.api.Table;

import java.util.*;

import static org.junit.Assert.*;

public class GoFishValetMetricsTest {

    static Map<Event.GameEvent, Table> tables;

    @BeforeClass
    public static void runGames() throws Exception {
        AbstractPlayer focus = new RandomPlayer();
        focus.setName("Focus");
        tables = MetricsTestRunner.runFocus(GameType.GoFish, 3, null, List.of(focus, new RandomPlayer()), 15, false,
                new GoFishValetMetrics.GameSummary());
    }

    private static double value(Table table, int row, String column) {
        return Double.parseDouble(table.column("GameSummary(" + column + ")").getString(row));
    }

    @Test
    public void askCountsAreConsistent() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        assertEquals(15, games.rowCount());
        for (int g = 0; g < games.rowCount(); g++) {
            double totalAsks = 0, totalBooks = 0;
            for (int p = 0; p < 3; p++) {
                double asks = value(games, g, "Player-" + p + " Asks");
                double successful = value(games, g, "Player-" + p + " SuccessfulAsks");
                double draws = value(games, g, "Player-" + p + " GoFishDraws");
                assertTrue(successful + draws <= asks);
                assertTrue(value(games, g, "Player-" + p + " CardsReceived") >= successful);
                assertTrue(value(games, g, "Player-" + p + " CardsReceived") <= 3 * successful);
                assertTrue(value(games, g, "Player-" + p + " DrawsOfAskedRank") <= draws);
                assertEquals(value(games, g, "Player-" + p + " Books"), value(games, g, "Player-" + p + " Score"), 0.0);
                totalAsks += asks;
                totalBooks += value(games, g, "Player-" + p + " Books");
            }
            // every action in Go Fish is an ask
            assertEquals(value(games, g, "Actions"), totalAsks, 0.0);
            assertTrue(totalBooks <= 13);
            assertTrue(Set.of("DeckEmpty", "HandEmpty", "Other")
                    .contains(games.column("GameSummary(EndReason)").getString(g)));
        }
    }

    @Test
    public void someAsksSucceedAndSomeGoFish() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        double successful = 0, draws = 0, drawsOfRank = 0;
        for (int g = 0; g < games.rowCount(); g++)
            for (int p = 0; p < 3; p++) {
                successful += value(games, g, "Player-" + p + " SuccessfulAsks");
                draws += value(games, g, "Player-" + p + " GoFishDraws");
                drawsOfRank += value(games, g, "Player-" + p + " DrawsOfAskedRank");
            }
        assertTrue(successful > 0);
        assertTrue(draws > 0);
        assertTrue(drawsOfRank > 0);
    }
}

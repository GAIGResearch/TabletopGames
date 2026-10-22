package games.scopa;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.metrics.Event;
import evaluation.metrics.MetricsTestRunner;
import games.GameType;
import games.scopa.metrics.ScopaValetMetrics;
import org.junit.BeforeClass;
import org.junit.Test;
import players.simple.RandomPlayer;
import tech.tablesaw.api.Table;

import java.util.*;

import static org.junit.Assert.*;

public class ScopaValetMetricsTest {

    static Map<Event.GameEvent, Table> tables;

    @BeforeClass
    public static void runGames() throws Exception {
        AbstractPlayer focus = new RandomPlayer();
        focus.setName("Focus");
        tables = MetricsTestRunner.runFocus(GameType.Scopa, 2, null, List.of(focus, new RandomPlayer()), 10, false,
                new ScopaValetMetrics.GameSummary());
    }

    private static double value(Table table, int row, String column) {
        return Double.parseDouble(table.column("GameSummary(" + column + ")").getString(row));
    }

    @Test
    public void everyCardIsCapturedOncePerDeal() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        assertEquals(10, games.rowCount());
        for (int g = 0; g < games.rowCount(); g++) {
            double deals = value(games, g, "Deals");
            assertTrue(deals >= 1);
            assertEquals(40 * deals, value(games, g, "Player-0 Cards") + value(games, g, "Player-1 Cards"), 0.0);
            assertEquals(10 * deals, value(games, g, "Player-0 Coins") + value(games, g, "Player-1 Coins"), 0.0);
            assertEquals(deals, value(games, g, "Player-0 Settebello") + value(games, g, "Player-1 Settebello"), 0.0);
        }
    }

    @Test
    public void valetParametersPlayOneDeal() throws Exception {
        AbstractPlayer focus = new RandomPlayer();
        focus.setName("Focus");
        AbstractParameters params = AbstractParameters.createFromFile(GameType.Scopa, "data/scopa/Scopa_Valet.json");
        Table games = MetricsTestRunner.runFocus(GameType.Scopa, 2, params, List.of(focus, new RandomPlayer()), 10, false,
                new ScopaValetMetrics.GameSummary()).get(Event.GameEvent.GAME_OVER);
        assertEquals(10, games.rowCount());
        for (int g = 0; g < games.rowCount(); g++) {
            assertEquals(1, value(games, g, "Deals"), 0.0);
            assertEquals(40, value(games, g, "Player-0 Cards") + value(games, g, "Player-1 Cards"), 0.0);
            assertEquals(1, value(games, g, "Player-0 Settebello") + value(games, g, "Player-1 Settebello"), 0.0);
            for (int p = 0; p < 2; p++) {
                double points = 0;
                for (String category : List.of("CardsPoints", "CoinsPoints", "SettebelloPoints", "PrimieraPoints", "ScopaPoints"))
                    points += value(games, g, "Player-" + p + " " + category);
                assertEquals(value(games, g, "Player-" + p + " Score"), points, 0.0);
            }
        }
    }

    @Test
    public void pointsAddUpToTheFinalScore() {
        Table games = tables.get(Event.GameEvent.GAME_OVER);
        for (int g = 0; g < games.rowCount(); g++)
            for (int p = 0; p < 2; p++) {
                double points = 0;
                for (String category : List.of("CardsPoints", "CoinsPoints", "SettebelloPoints", "PrimieraPoints", "ScopaPoints"))
                    points += value(games, g, "Player-" + p + " " + category);
                assertEquals(value(games, g, "Player-" + p + " Score"), points, 0.0);
                assertEquals(value(games, g, "Player-" + p + " Scopas"), value(games, g, "Player-" + p + " ScopaPoints"), 0.0);
            }
    }
}

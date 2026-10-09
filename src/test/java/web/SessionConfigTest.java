package web;

import core.AbstractParameters;
import games.GameType;
import games.lawnandorder.LawnAndOrderParameters;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Reading a game setup from the play page's query string, and setting game parameters from the strings a form sends.
 */
public class SessionConfigTest {

    private final GameCatalog games = new GameCatalog("LawnAndOrder,TicTacToe");
    private final OpponentCatalog opponents = new OpponentCatalog(OpponentCatalog.DEFAULT_DIR);

    private static Map<String, List<String>> query(String... keyValues) {
        Map<String, List<String>> q = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2)
            q.put(keyValues[i], List.of(keyValues[i + 1]));
        return q;
    }

    private SessionConfig read(String... keyValues) {
        return SessionConfig.fromQuery(query(keyValues), games, opponents);
    }

    @Test
    public void aFullSetupIsRead() {
        SessionConfig c = read("game", "LawnAndOrder", "players", "4", "seat", "2",
                "opponents", "Granny,random,you,osla", "seed", "42", "pause", "0", "p.handSize", "6", "insight", "1");
        assertEquals(GameType.LawnAndOrder, c.game());
        assertEquals(4, c.nPlayers());
        assertEquals(2, c.seat());
        assertEquals(List.of("Granny", "random", "you", "osla"), c.opponents());
        assertEquals(42, c.seed());
        assertEquals(0, c.turnPause());
        assertEquals(Map.of("handSize", "6"), c.params());
        assertTrue(c.insight());
    }

    @Test
    public void defaultsFillTheGaps() {
        SessionConfig c = read();
        assertEquals(GameType.LawnAndOrder, c.game());   // the first game offered
        assertEquals(3, c.nPlayers());
        assertEquals(0, c.seat());
        assertEquals(List.of("you", "Enid", "Enid"), c.opponents());   // the default in json/players/webserver
        assertEquals(-1, c.seed());
        assertEquals(SessionConfig.DEFAULT_TURN_PAUSE, c.turnPause());
        assertTrue(c.params().isEmpty());
        assertFalse(c.insight());
    }

    @Test
    public void theDefaultPlayerCountFitsTheGame() {
        assertEquals(2, read("game", "TicTacToe").nPlayers());
    }

    @Test
    public void theBrowserSeatIsAlwaysYou() {
        SessionConfig c = read("players", "2", "seat", "1", "opponents", "random,random");
        assertEquals(List.of("random", "you"), c.opponents());
    }

    @Test
    public void badSetupsAreRefused() {
        assertRefused("Unknown game", "game", "Chess");   // not offered
        assertRefused("takes 2 to 6 players", "players", "9");
        assertRefused("seat must be between", "players", "3", "seat", "3");
        assertRefused("Unknown opponent", "opponents", "you,nobody");
        assertRefused("Unknown parameter", "p.noSuchThing", "1");
        assertRefused("Bad value for handSize", "p.handSize", "lots");
        assertRefused("Not a whole number for seed", "seed", "abc");
        assertRefused("pause must be between", "pause", "-5");
    }

    private void assertRefused(String messagePart, String... keyValues) {
        try {
            read(keyValues);
            fail("Expected the setup to be refused: " + messagePart);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage() + " should mention: " + messagePart, e.getMessage().contains(messagePart));
        }
    }

    @Test
    public void parametersAreSetFromStrings() {
        AbstractParameters params = GameType.LawnAndOrder.createParameters(1);
        GameCatalog.apply(params, Map.of("handSize", "7", "maxRounds", "12"));
        LawnAndOrderParameters p = (LawnAndOrderParameters) params;
        assertEquals(7, p.handSize);
        assertEquals(7, p.getParameterValue("handSize"));
        assertEquals(12, p.getParameterValue("maxRounds"));
    }

    @Test
    public void parametersWithoutAListOfSettingsAreFreeEntry() {
        var described = GameCatalog.describeParameters(GameType.LawnAndOrder);
        for (var e : described) {
            var p = e.getAsJsonObject();
            if (p.get("name").getAsString().equals("maxRounds"))
                assertEquals(0, p.getAsJsonArray("values").size());
            if (p.get("name").getAsString().equals("handSize"))
                assertTrue(p.getAsJsonArray("values").size() > 1);
        }
    }
}

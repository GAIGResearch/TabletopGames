package web;

import core.AbstractPlayer;
import games.GameType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The opponents offered come from a directory of agent JSON files.
 */
public class OpponentCatalogTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private void agent(String name, String json) throws IOException {
        Files.writeString(new File(folder.getRoot(), name + ".json").toPath(), json);
    }

    @Test
    public void theAgentsAreTheFilesInTheDirectory() throws IOException {
        agent("zed", "{\"class\": \"players.simple.RandomPlayer\"}");
        agent("alpha", "{\"label\": \"Quick MCTS\", \"class\": \"players.mcts.MCTSParams\", \"budgetType\": \"BUDGET_TIME\", \"budget\": 10}");
        OpponentCatalog catalog = new OpponentCatalog(folder.getRoot().getPath());

        List<OpponentCatalog.Opponent> all = catalog.forGame(GameType.LawnAndOrder);
        assertEquals(List.of("alpha", "zed"), all.stream().map(OpponentCatalog.Opponent::id).toList());
        assertEquals("Quick MCTS", all.get(0).description());
        assertEquals("", all.get(1).description());

        AbstractPlayer mcts = catalog.find(GameType.LawnAndOrder, "alpha").create();
        assertTrue(mcts instanceof MCTSPlayer);
        assertEquals("alpha", mcts.toString());
        assertTrue(catalog.find(GameType.LawnAndOrder, "zed").create() instanceof RandomPlayer);
    }

    @Test
    public void aBadAgentFileIsReportedByName() throws IOException {
        agent("broken", "{\"class\": \"players.mcts.MCTSParams\", \"treePolicy\": \"NoSuchPolicy\"}");
        OpponentCatalog catalog = new OpponentCatalog(folder.getRoot().getPath());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> catalog.find(GameType.LawnAndOrder, "broken").create());
        assertTrue(e.getMessage(), e.getMessage().contains("broken") && e.getMessage().contains("NoSuchPolicy"));
    }

    @Test
    public void theDefaultIsTheMarkedAgentElseTheFirst() throws IOException {
        agent("a", "{\"class\": \"players.simple.RandomPlayer\"}");
        agent("b", "{\"class\": \"players.simple.RandomPlayer\"}");
        OpponentCatalog catalog = new OpponentCatalog(folder.getRoot().getPath());
        assertEquals("a", catalog.defaultFor(GameType.LawnAndOrder));

        agent("b", "{\"default\": true, \"class\": \"players.simple.RandomPlayer\"}");
        assertEquals("b", catalog.defaultFor(GameType.LawnAndOrder));
        assertTrue(catalog.describe(GameType.LawnAndOrder).get(1).getAsJsonObject().get("default").getAsBoolean());
    }

    @Test
    public void theDefaultDirectoryOffersTheStandardAgents() {
        OpponentCatalog catalog = new OpponentCatalog(OpponentCatalog.DEFAULT_DIR);
        List<String> ids = catalog.forGame(GameType.LawnAndOrder).stream().map(OpponentCatalog.Opponent::id).toList();
        assertTrue(ids.containsAll(List.of("random", "osla", "Granny", "Enid", "Fabian")));
        for (String id : ids)
            assertNotNull(id, catalog.find(GameType.LawnAndOrder, id).create());
    }

    @Test
    public void aGameAddsItsOwnAgents() {
        OpponentCatalog catalog = new OpponentCatalog(OpponentCatalog.DEFAULT_DIR);
        // from data/tictactoe/agents
        assertNotNull(catalog.find(GameType.TicTacToe, "MCTS_for_TicTacToe"));
        assertThrows(IllegalArgumentException.class, () -> catalog.find(GameType.LawnAndOrder, "MCTS_for_TicTacToe"));
    }
}

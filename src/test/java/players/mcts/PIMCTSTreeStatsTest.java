package players.mcts;

import core.AbstractPlayer;
import core.interfaces.IStateKey;
import evaluation.metrics.Event;
import evaluation.metrics.MetricsTestRunner;
import games.GameType;
import games.gofish.metrics.GoFishTightInfoKey;
import games.scopa.metrics.ScopaTightInfoKey;
import games.whist.metrics.WhistTightInfoKey;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import tech.tablesaw.api.Table;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Plays a few games of each game with each kind of MCTS agent against Random, and checks the TreeStats recorded for
 * the agent's decisions.
 */
public class PIMCTSTreeStatsTest {

    static final List<String> NON_ZERO_COLUMNS = List.of("Iterations", "Trees", "ActionsAtRoot", "MaxDepth",
            "MeanLeafDepth", "MeanNodeDepth", "Nodes", "NodesTotal", "LeafNodes", "MeanActionsAtNode",
            "MaxActionsAtNode", "MeanActionsExpanded", "RolloutLength", "maxVisitProportion", "fmCalls", "time");

    static MCTSPlayer agent(String name, Map<String, Object> settings) {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 40);
        settings.forEach(params::setParameterValue);
        return new MCTSPlayer(params, name);
    }

    static List<MCTSPlayer> agents(IStateKey tightInfoKey) {
        return List.of(
                agent("ISMCTS", Map.of()),
                agent("FlatMC", Map.of("numDeterminizations", 3, "maxTreeDepth", 1)),
                agent("PIMCTS", Map.of("numDeterminizations", 3)),
                agent("MCGS", Map.of("opponentTreePolicy", MCTSEnums.OpponentTreePolicy.MCGS, "MCGSStateKey", tightInfoKey)));
    }

    /**
     * The TreeStats rows of the ACTION_CHOSEN table, as column name -> values.
     */
    static List<Map<String, String>> treeStats(GameType gameType, int nPlayers, boolean byTeam, MCTSPlayer agent) throws Exception {
        Table table = MetricsTestRunner.runFocus(gameType, nPlayers, null, List.of(agent, new RandomPlayer()), 6, byTeam,
                new PIMCTSMetrics.TreeStats()).get(Event.GameEvent.ACTION_CHOSEN);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int r = 0; r < table.rowCount(); r++) {
            Map<String, String> row = new HashMap<>();
            for (String column : table.columnNames())
                if (column.startsWith("TreeStats("))
                    row.put(column.substring(10, column.length() - 1), table.column(column).getString(r));
            rows.add(row);
        }
        return rows;
    }

    static void checkTreeStats(GameType gameType, int nPlayers, boolean byTeam, IStateKey tightInfoKey) throws Exception {
        for (MCTSPlayer agent : agents(tightInfoKey)) {
            String context = gameType + " " + agent;
            List<Map<String, String>> rows = treeStats(gameType, nPlayers, byTeam, agent);
            assertFalse(context, rows.isEmpty());
            boolean forest = agent.getParameters().numDeterminizations > 1;
            for (Map<String, String> row : rows) {
                assertEquals(context, agent.toString(), row.get("PlayerType"));
                assertTrue(context, Double.parseDouble(row.get("ActionsAtRoot")) > 1);
                assertEquals(context, forest ? 3 : 1, Double.parseDouble(row.get("Trees")), 0.0);
                // no child is created for an action that ends the game, so the last decision may have depth 0
                if (agent.toString().equals("FlatMC"))
                    assertTrue(context, Double.parseDouble(row.get("MaxDepth")) <= 1);
            }
            for (String column : NON_ZERO_COLUMNS) {
                boolean hasValue = rows.stream().map(r -> Double.parseDouble(r.get(column)))
                        .anyMatch(v -> v != 0.0 && !Double.isNaN(v));
                assertTrue(context + " " + column + " is all zero or NaN", hasValue);
            }
        }
    }

    @Test
    public void whist() throws Exception {
        checkTreeStats(GameType.Whist, 4, true, new WhistTightInfoKey());
    }

    @Test
    public void goFish() throws Exception {
        checkTreeStats(GameType.GoFish, 3, false, new GoFishTightInfoKey());
    }

    @Test
    public void scopa() throws Exception {
        checkTreeStats(GameType.Scopa, 2, false, new ScopaTightInfoKey());
    }
}

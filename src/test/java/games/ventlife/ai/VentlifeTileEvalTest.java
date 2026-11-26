package games.ventlife.ai;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.actions.PlaceTile;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class VentlifeTileEvalTest {

    private static VentlifeHeuristic heuristic(double oppMax, double oppMean, double exposure, double oppExposure,
                                               double risk, double oppRisk, boolean endScaling) {
        VentlifeHeuristic h = new VentlifeHeuristic();
        h.setParameterValue("oppMaxWeight", oppMax);
        h.setParameterValue("oppMeanWeight", oppMean);
        h.setParameterValue("exposure", exposure);
        h.setParameterValue("oppExposure", oppExposure);
        h.setParameterValue("risk", risk);
        h.setParameterValue("oppRisk", oppRisk);
        h.setParameterValue("endScaling", endScaling);
        h.setParameterValue("exposureMoveFactor", 0.6);
        h.setParameterValue("exposureCrabValue", 3.5);
        return h;
    }

    @Test
    public void tileValuesMatchHeuristicAfterTileSim() {
        Random rnd = new Random(21);
        List<VentlifeHeuristic> heuristics = List.of(heuristic(0, 1.25, 0, -1, 1.5, 0.5, false),
                heuristic(0.3, 1, 0.7, 0.2, 0, -1, true), heuristic(0, 1, 0.5, -1, 1.0, -1, true),
                heuristic(1, 0, 0, -1, 0, -1, false));
        int checked = 0;
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            VentlifeParameters params = new VentlifeParameters();
            params.setParameterValue("nSpecies", 5);
            Game game = GameType.Ventlife.createGameInstance(nPlayers, 19, params);
            for (int g = 0; g < 15; g++) {
                game.reset(List.<AbstractPlayer>of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(),
                        new RandomPlayer()).subList(0, nPlayers), 400 + g);
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                while (state.isNotTerminal()) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    if (actions.get(0) instanceof PlaceTile) {
                        int player = state.getCurrentPlayer();
                        VentlifeHeuristic h = heuristics.get(rnd.nextInt(heuristics.size()));
                        VentlifeTileEval eval = new VentlifeTileEval(state, player, h);
                        for (AbstractAction a : actions) {
                            PlaceTile t = (PlaceTile) a;
                            double expected = h.evaluateState(VentlifeTileSim.afterTile(state, t), player);
                            assertEquals(a + " in game " + g + " tick " + state.getGameTick(), expected,
                                    eval.tileValue(player, t.tile, t.smoker, t.orientation), 1e-9);
                            checked++;
                        }
                    }
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                }
            }
        }
        System.out.println("Checked " + checked + " tile placements");
    }
}

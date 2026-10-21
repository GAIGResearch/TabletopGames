package games.ventlife.ai;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.actions.PlaceCreature;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class VentlifeFastEvalTest {

    private static VentlifeHeuristic heuristic(double oppMax, double oppMean, double exposure, double oppExposure) {
        VentlifeHeuristic h = new VentlifeHeuristic();
        h.setParameterValue("oppMaxWeight", oppMax);
        h.setParameterValue("oppMeanWeight", oppMean);
        h.setParameterValue("exposure", exposure);
        h.setParameterValue("oppExposure", oppExposure);
        h.setParameterValue("exposureMoveFactor", 0.4);
        h.setParameterValue("exposureCrabValue", 2.5);
        return h;
    }

    @Test
    public void placementValuesMatchHeuristicAfterPlacement() {
        Random rnd = new Random(3);
        List<VentlifeHeuristic> heuristics = List.of(heuristic(0, 1, 0.7, -1), heuristic(0.5, 0.5, 1.0, 0.3),
                heuristic(1, 0, 0, -1));
        int checked = 0;
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            VentlifeParameters params = new VentlifeParameters();
            // every species gets a turn in play
            params.setParameterValue("nSpecies", 5);
            Game game = GameType.Ventlife.createGameInstance(nPlayers, 11, params);
            for (int g = 0; g < 30; g++) {
                game.reset(List.<AbstractPlayer>of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(),
                        new RandomPlayer()).subList(0, nPlayers), 100 + g);
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                while (state.isNotTerminal()) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    int player = state.getCurrentPlayer();
                    VentlifeHeuristic h = heuristics.get(rnd.nextInt(heuristics.size()));
                    VentlifeFastEval fast = new VentlifeFastEval(state, player, h);
                    assertEquals(h.evaluateState(state, player), fast.value(player), 1e-9);
                    for (AbstractAction a : actions) {
                        if (!(a instanceof PlaceCreature pc)) continue;
                        VentlifeGameState next = (VentlifeGameState) state.copy();
                        fm.next(next, a);
                        if (!next.isNotTerminal()) continue;
                        assertEquals(a + " in game " + g, h.evaluateState(next, player),
                                fast.placementValue(player, pc.species, pc.hex, pc.count), 1e-9);
                        checked++;
                    }
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                }
            }
        }
        System.out.println("Checked " + checked + " placements");
    }
}

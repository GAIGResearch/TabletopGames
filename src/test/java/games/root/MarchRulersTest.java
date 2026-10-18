package games.root;

import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.root.actions.Move;
import games.root.actions.extended.March;
import games.root.components.RootBoardNodeWithRootEdges;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class MarchRulersTest {

    /**
     * The forward model is not told about the decisions within a March, so the March must itself keep the rulers up to
     * date: the second move's options depend on who rules each clearing after the first.
     */
    @Test
    public void rulersAreUpToDateBeforeEachDecisionOfAMarch() {
        int firstMovesChangingRulers = 0;
        for (int seed = 0; seed < 20; seed++) {
            Game game = GameType.Root.createGameInstance(4, seed);
            game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
            RootGameState state = (RootGameState) game.getGameState();
            RootForwardModel fm = (RootForwardModel) game.getForwardModel();
            Random rnd = new Random(seed);

            while (state.isNotTerminal() && state.getTurnCounter() < 150) {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                AbstractAction chosen = actions.stream().filter(a -> a instanceof March).findFirst()
                        .orElse(actions.get(rnd.nextInt(actions.size())));
                if (!(chosen instanceof March)) {
                    fm.next(state, chosen);
                    continue;
                }
                fm.next(state, chosen);
                boolean firstMoveDone = false;
                while (state.isActionInProgress() && state.currentActionInProgress() instanceof March) {
                    assertEquals(freshRulers(state), rulers(state));
                    Map<Integer, Integer> before = rulers(state);
                    List<AbstractAction> marchActions = fm.computeAvailableActions(state);
                    AbstractAction decision = marchActions.get(rnd.nextInt(marchActions.size()));
                    fm.next(state, decision);
                    if (decision instanceof Move && !firstMoveDone) {
                        firstMoveDone = true;
                        if (!before.equals(rulers(state)) && state.isActionInProgress())
                            firstMovesChangingRulers++;
                    }
                }
            }
        }
        assertTrue("no first move of a March changed a ruler, so nothing was tested", firstMovesChangingRulers > 0);
    }

    private static Map<Integer, Integer> rulers(AbstractGameState state) {
        return ((RootGameState) state).getGameMap().getBoardNodes().stream()
                .collect(Collectors.toMap(RootBoardNodeWithRootEdges::getComponentID, n -> n.rulerID));
    }

    private static Map<Integer, Integer> freshRulers(RootGameState state) {
        RootGameState copy = (RootGameState) state.copy();
        copy.getGameMap().updateRulers();
        return rulers(copy);
    }
}

package games.seasaltpaper;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.seasaltpaper.SeaSaltPaperGameState.TurnPhase;
import games.seasaltpaper.actions.CrabDuo;
import games.seasaltpaper.actions.SwimmerSharkDuo;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class DuoPhaseTest {

    /**
     * Crab and Swimmer/Shark duos need further decisions. Like any other duo, they leave the player in the Duo phase,
     * free to play another duo or end their turn.
     */
    @Test
    public void duosWithFurtherDecisionsDoNotEndTheTurn() {
        int duosChecked = 0;
        for (int seed = 0; seed < 20; seed++) {
            Game game = GameType.SeaSaltPaper.createGameInstance(3, seed);
            game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
            SeaSaltPaperGameState state = (SeaSaltPaperGameState) game.getGameState();
            SeaSaltPaperForwardModel fm = (SeaSaltPaperForwardModel) game.getForwardModel();
            Random rnd = new Random(seed);

            while (state.isNotTerminal()) {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                AbstractAction chosen = actions.stream().filter(a -> a instanceof CrabDuo || a instanceof SwimmerSharkDuo)
                        .findFirst().orElse(actions.get(rnd.nextInt(actions.size())));
                boolean isDuo = chosen instanceof CrabDuo || chosen instanceof SwimmerSharkDuo;
                int player = state.getCurrentPlayer();
                int turn = state.getTurnCounter();
                fm.next(state, chosen);
                if (!isDuo) continue;
                while (state.isActionInProgress()) {
                    List<AbstractAction> decisions = fm.computeAvailableActions(state);
                    fm.next(state, decisions.get(rnd.nextInt(decisions.size())));
                }
                if (!state.isNotTerminal()) break;
                assertEquals(TurnPhase.DUO, state.currentPhase);
                assertEquals(player, state.getCurrentPlayer());
                assertEquals(turn, state.getTurnCounter());
                duosChecked++;
            }
        }
        assertTrue("no Crab or Swimmer/Shark duo was played, so nothing was tested", duosChecked > 0);
    }
}

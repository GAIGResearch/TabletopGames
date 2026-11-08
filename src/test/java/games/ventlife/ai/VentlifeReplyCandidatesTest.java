package games.ventlife.ai;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import games.ventlife.actions.PlaceTile;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VentlifeReplyCandidatesTest {

    @Test
    public void replyCandidatesAreLegalAndIncludeEveryPlateauPlacement() {
        Random rnd = new Random(8);
        VentlifeGreedyPlayer player = new VentlifeGreedyPlayer();
        Game game = GameType.Ventlife.createGameInstance(4, 23);
        for (int g = 0; g < 20; g++) {
            game.reset(List.<AbstractPlayer>of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(),
                    new RandomPlayer()), 500 + g);
            VentlifeGameState state = (VentlifeGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            while (state.isNotTerminal()) {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                if (actions.get(0) instanceof PlaceTile && !state.getField().isEmpty()) {
                    Set<AbstractAction> legal = new HashSet<>(actions);
                    List<PlaceTile> candidates = player.replyCandidates(state, state.getCurrentPlayer());
                    for (PlaceTile t : candidates)
                        assertTrue(t + " is not legal", legal.contains(t));
                    long plateaus = actions.stream().filter(a -> state.getCell(((PlaceTile) a).smoker) != null).count();
                    assertEquals(plateaus, candidates.stream().filter(t -> state.getCell(t.smoker) != null).count());
                    assertEquals(candidates.size(), new HashSet<>(candidates).size());
                }
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
            }
        }
    }
}

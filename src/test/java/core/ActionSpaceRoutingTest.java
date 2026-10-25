package core;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import games.GameType;
import games.catan.actions.setup.DeepPlaceSettlementThenRoad;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.loveletter.actions.PlayCard;
import games.stratego.actions.DeepMove;
import games.tictactoe.TicTacToeForwardModel;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Checks that a non-default ActionSpace reaches the forward model's _computeAvailableActions(state, actionSpace).
 */
public class ActionSpaceRoutingTest {

    private static final ActionSpace DEEP = new ActionSpace(ActionSpace.Structure.Deep);

    private Game newGame(GameType type, int nPlayers) {
        Game game = type.createGameInstance(nPlayers, 42);
        RandomPlayer[] players = new RandomPlayer[nPlayers];
        for (int i = 0; i < nPlayers; i++) players[i] = new RandomPlayer();
        game.reset(List.of(players));
        return game;
    }

    @Test
    public void nonDefaultActionSpaceIsPassedToTheForwardModel() {
        Game game = newGame(GameType.TicTacToe, 2);
        ActionSpaceForwardModel fm = new ActionSpaceForwardModel();

        assertEquals(List.of(new DoNothing()), fm.computeAvailableActions(game.getGameState(), DEEP));
        assertEquals(List.of(new DoNothing()), fm.computeAvailableActions(game.getGameState(), DEEP, 0));
        assertEquals(DEEP, fm.received);
        assertEquals(9, fm.computeAvailableActions(game.getGameState(), ActionSpace.Default).size());
    }

    @Test
    public void defaultCoreActionSpaceGivesTheGamesStandardActions() {
        Game game = newGame(GameType.TicTacToe, 2);
        ActionSpaceForwardModel fm = new ActionSpaceForwardModel();

        assertTrue(game.getCoreParameters().actionSpace.isDefault());
        assertEquals(9, fm.computeAvailableActions(game.getGameState()).size());
        assertNull(fm.received);
    }

    @Test
    public void strategoDeepActionSpaceChoosesThePieceFirst() {
        Game game = newGame(GameType.Stratego, 2);
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(game.getGameState(), DEEP);
        assertFalse(actions.isEmpty());
        assertTrue(actions.stream().allMatch(a -> a instanceof DeepMove));
    }

    @Test
    public void catanDeepActionSpaceSplitsSettlementAndRoad() {
        Game game = newGame(GameType.Catan, 3);
        AbstractForwardModel fm = game.getForwardModel();
        List<AbstractAction> standard = fm.computeAvailableActions(game.getGameState());
        assertFalse(standard.isEmpty());
        assertTrue(standard.stream().allMatch(a -> a.getClass() == PlaceSettlementWithRoad.class));
        List<AbstractAction> deep = fm.computeAvailableActions(game.getGameState(), DEEP);
        assertFalse(deep.isEmpty());
        assertTrue(deep.stream().allMatch(a -> a instanceof DeepPlaceSettlementThenRoad));
    }

    @Test
    public void loveLetterContextDependentActionsRecordTheCardIndex() {
        Game game = newGame(GameType.LoveLetter, 2);
        AbstractForwardModel fm = game.getForwardModel();
        // PlayCard.equals() ignores the card index, so we check it directly
        List<AbstractAction> standard = fm.computeAvailableActions(game.getGameState());
        List<AbstractAction> dependent = fm.computeAvailableActions(game.getGameState(), new ActionSpace(ActionSpace.Context.Dependent));
        assertFalse(dependent.isEmpty());
        assertTrue(standard.stream().allMatch(a -> ((PlayCard) a).getCardIdx() == -1));
        assertTrue(dependent.stream().allMatch(a -> ((PlayCard) a).getCardIdx() >= 0));
    }

    /**
     * Gives a sentinel action list when asked for a specific action space, and the normal TicTacToe actions otherwise.
     */
    static class ActionSpaceForwardModel extends TicTacToeForwardModel {
        ActionSpace received;

        @Override
        protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
            if (actionSpace.isDefault())
                return super._computeAvailableActions(gameState, actionSpace);
            received = actionSpace;
            return List.of(new DoNothing());
        }
    }
}

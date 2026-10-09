package games.catan;

import core.Game;
import core.actions.AbstractAction;
import core.components.Counter;
import games.GameType;
import games.catan.actions.dev.PlayDevCard;
import games.catan.components.CatanCard;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.DefaultGamePhase.Main;
import static org.junit.Assert.*;

public class PlayDevCardTest {

    @Test
    public void aDevCardWithNoLegalOptionDoesNotEndTheTurn() {
        Game game = GameType.Catan.createGameInstance(4, 42);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        CatanGameState state = (CatanGameState) game.getGameState();
        CatanForwardModel fm = (CatanForwardModel) game.getForwardModel();
        Random rnd = new Random(1);
        while (state.getGamePhase() != Main || state.isActionInProgress() || state.tradeOffer != null) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
        }
        int player = state.getCurrentPlayer();
        int turn = state.getTurnCounter();

        // With nothing left in the bank, Year of Plenty has no legal option
        for (Counter c : state.resourcePool.values())
            c.setValue(0);
        CatanCard.CardType yearOfPlenty = CatanCard.CardType.YEAR_OF_PLENTY;
        fm.next(state, new PlayDevCard(player, yearOfPlenty, yearOfPlenty.nDeepSteps((CatanParameters) state.getGameParameters())));

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(List.of(new PlayDevCard.NoOption(player)), actions);
        fm.next(state, actions.get(0));

        assertFalse(state.isActionInProgress());
        assertEquals(Main, state.getGamePhase());
        assertEquals(player, state.getCurrentPlayer());
        assertEquals(turn, state.getTurnCounter());
    }
}

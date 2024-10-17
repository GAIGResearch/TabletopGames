package games.dominion;

import core.Game;
import core.components.PartialObservableDeck;
import games.GameType;
import games.dominion.DominionConstants.DeckType;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.DominionParameters;
import games.dominion.actions.AttackReaction;
import games.dominion.actions.DominionAction;
import games.dominion.actions.EndPhase;
import games.dominion.actions.Militia;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class TestFullObservabilityCopy {

    DominionForwardModel fm = new DominionForwardModel();

    private void moveForwardToNextPlayer(DominionGameState state) {
        int startingPlayer = state.getCurrentPlayer();
        while (state.getCurrentPlayer() == startingPlayer)
            fm.next(state, new EndPhase((DominionGameState.DominionGamePhase) state.getGamePhase()));
    }

    @Test
    public void gameStateCopyVanilla() {
        DominionParameters params = new DominionParameters();
        params.setRandomSeed(36);
        Game game = new Game(GameType.Dominion, new DominionForwardModel(), new DominionGameState(params, 4));
        DominionGameState startState = (DominionGameState) game.getGameState();
        startState.setDefended(2);

        DominionGameState fullCopy = (DominionGameState) startState.copy();
        Assert.assertTrue(startState.isDefended(2));
        Assert.assertTrue(fullCopy.isDefended(2));

        PartialObservableDeck<DominionCard> startHand = (PartialObservableDeck<DominionCard>) startState.getDeck(DeckType.HAND, 1);
        Assert.assertTrue(startHand.getDeckVisibility()[1]);
        IntStream.of(0, 2, 3).forEach(i -> Assert.assertFalse(startHand.getDeckVisibility()[i]));

        PartialObservableDeck<DominionCard> fullCopyHand = (PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.HAND, 1);
        Assert.assertTrue(fullCopyHand.getDeckVisibility()[1]);
        IntStream.of(0, 2, 3).forEach(i -> Assert.assertFalse(fullCopyHand.getDeckVisibility()[i]));

        moveForwardToNextPlayer(fullCopy);
        assertEquals(0, startState.getCurrentPlayer());
        assertEquals(1, fullCopy.getCurrentPlayer());

        startState.setDefended(0);
        fullCopy.setDefended(1);
        Assert.assertTrue(startState.isDefended(0));
        Assert.assertFalse(startState.isDefended(1));
        Assert.assertFalse(fullCopy.isDefended(0));
        Assert.assertTrue(fullCopy.isDefended(1));
        Assert.assertTrue(startState.isDefended(2));
        Assert.assertFalse(fullCopy.isDefended(2));

    }

    @Test
    public void gameStateCopyWithActionInProgress() {
        DominionParameters params = new DominionParameters();
        params.setRandomSeed(36);
        Game game = new Game(GameType.Dominion, new DominionForwardModel(), new DominionGameState(params, 4));
        DominionGameState startState = (DominionGameState) game.getGameState();
        fm.endPlayerTurn(startState);
        fm.endPlayerTurn(startState);
        assertEquals(2, startState.getCurrentPlayer());
        DominionAction militia = new Militia(2);
        startState.addCard(CardType.MILITIA, 2, DeckType.HAND);
        startState.addCard(CardType.MOAT, 3, DeckType.HAND);

        fm.next(startState, militia);
        // this should put two actionsInProgress
        Assert.assertTrue(startState.currentActionInProgress() instanceof AttackReaction);

        DominionGameState fullCopy = (DominionGameState) startState.copy();
        Assert.assertTrue(fullCopy.currentActionInProgress() instanceof AttackReaction);
        Assert.assertNotSame(startState.currentActionInProgress(), fullCopy.currentActionInProgress());
        assertEquals(startState.currentActionInProgress(), fullCopy.currentActionInProgress());

        fullCopy.setActionInProgress(null);
        Assert.assertTrue(fullCopy.currentActionInProgress() instanceof Militia);
        Assert.assertTrue(startState.currentActionInProgress() instanceof AttackReaction);
        startState.setActionInProgress(null);
        Assert.assertNotSame(startState.currentActionInProgress(), fullCopy.currentActionInProgress());
        assertEquals(startState.currentActionInProgress(), fullCopy.currentActionInProgress());
    }


}

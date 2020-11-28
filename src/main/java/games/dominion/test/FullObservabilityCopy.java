package games.dominion.test;

import core.AbstractPlayer;
import core.actions.*;
import core.components.PartialObservableDeck;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.actions.*;
import games.dominion.cards.*;
import games.dominion.DominionGameState.*;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

public class FullObservabilityCopy {

    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void gameStateCopyVanilla() {
        DominionGame game = new DominionGame(new DominionParameters(36), 4);
        DominionGameState startState = (DominionGameState) game.getGameState();
        DominionGameState fullCopy = (DominionGameState) startState.copy();

        // TODO: Add checks

    }

    @Test
    public void gameStateCopyWithActionInProgress() {
        DominionGame game = new DominionGame(new DominionParameters(36), 4);
        DominionGameState startState = (DominionGameState) game.getGameState();
        startState.endOfTurn(0);
        startState.endOfTurn(1);
        assertEquals(2, startState.getCurrentPlayer());
        DominionAction militia = new Militia(2);
        startState.addCard(CardType.MILITIA, 2, DeckType.HAND);
        startState.addCard(CardType.MOAT, 3, DeckType.HAND);

        fm.next(startState, militia);
        // this should put two actionsInProgress
        assertTrue(startState.currentActionInProgress() instanceof AttackReaction);

        DominionGameState fullCopy = (DominionGameState) startState.copy();
        assertTrue(fullCopy.currentActionInProgress() instanceof AttackReaction);
        assertNotSame(startState.currentActionInProgress(), fullCopy.currentActionInProgress());
        assertEquals(startState.currentActionInProgress(), fullCopy.currentActionInProgress());

        fullCopy.setActionInProgress(null);
        assertTrue(fullCopy.currentActionInProgress() instanceof Militia);
        assertTrue(startState.currentActionInProgress() instanceof AttackReaction);
        startState.setActionInProgress(null);
        assertNotSame(startState.currentActionInProgress(), fullCopy.currentActionInProgress());
        assertEquals(startState.currentActionInProgress(), fullCopy.currentActionInProgress());
    }

}

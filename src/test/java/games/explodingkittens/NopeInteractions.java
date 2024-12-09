package games.explodingkittens;

import core.actions.AbstractAction;
import games.explodingkittens.actions.ChoiceOfCardToGive;
import games.explodingkittens.actions.Nope;
import games.explodingkittens.actions.Pass;
import games.explodingkittens.actions.PlayEKCard;
import games.explodingkittens.cards.ExplodingKittensCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.*;
import static org.junit.Assert.*;

public class NopeInteractions {


    ExplodingKittensGameState state;
    ExplodingKittensForwardModel fm;
    ExplodingKittensParameters params;

    @Before
    public void init() {
        params = new ExplodingKittensParameters();
        // this removes any cards which have extra action decisions to make
        params.cardCounts.put(ATTACK, 0);
        params.cardCounts.put(SKIP, 0);
        params.cardCounts.put(FAVOR, 0);
        params.cardCounts.put(NOPE, 0);  // we want to add these manually
        state = new ExplodingKittensGameState(params, 4);
        fm = new ExplodingKittensForwardModel();
        fm.setup(state);
        state.drawPile.add(new ExplodingKittensCard(TACOCAT));
        state.drawPile.add(new ExplodingKittensCard(RAINBOWCAT));
        state.drawPile.add(new ExplodingKittensCard(BEARDCAT));
    }

    @Test
    public void nopeNotPlayableByItself() {
        state.getPlayerHand(0).clear();
        state.getPlayerHand(0).add(new ExplodingKittensCard(NOPE));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new Pass(), actions.get(0));
    }


    @Test
    public void interruptOptionForNopePlayers() {
        state.getPlayerHand(2).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        assertEquals(8, state.getPlayerHand(1).getSize());

        fm.next(state, new PlayEKCard(FAVOR, 1));
        assertTrue(state.isActionInProgress());
        assertEquals(2, state.getCurrentPlayer());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new Nope(), actions.get(1));

        fm.next(state, new Pass());
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof ChoiceOfCardToGive);
    }

    @Test
    public void deadPlayerDoesNotGetANopeOption()  {
        state.getPlayerHand(2).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));

        fm.killPlayer(state, 2);

        fm.next(state, new PlayEKCard(FAVOR, 1));
        assertTrue(state.isActionInProgress());
        assertTrue(state.currentActionInProgress() instanceof ChoiceOfCardToGive);
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void nopingACardMeansItHasNoEffect() {
        state.getPlayerHand(2).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        assertEquals(8, state.getPlayerHand(1).getSize());

        fm.next(state, new PlayEKCard(FAVOR, 1));
        assertTrue(state.isActionInProgress());
        assertEquals(2, state.getCurrentPlayer());

        fm.next(state, new Nope());
        // and also that Nope card is discarded
        assertFalse(state.isActionInProgress()); // because no other NOPE cards
        assertEquals(2, state.discardPile.getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(8, state.getPlayerHand(1).getSize());
    }

    @Test
    public void nopingANopeCardMeansCardHasEffect() {
        state.getPlayerHand(2).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        state.getPlayerHand(0).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(1).add(new ExplodingKittensCard(NOPE));
        assertEquals(9, state.getPlayerHand(1).getSize());

        fm.next(state, new PlayEKCard(FAVOR, 1));
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Pass()); // player 1 passes
        assertEquals(2, state.getCurrentPlayer());

        fm.next(state, new Nope());
        assertTrue(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Nope());
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Pass());  // player 1 passes again

        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof ChoiceOfCardToGive);
    }

    @Test
    public void nopingThreeFoldRecursionHasNoEffect() {
        state.getPlayerHand(2).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        state.getPlayerHand(0).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(1).add(new ExplodingKittensCard(NOPE));
        fm.next(state, new PlayEKCard(FAVOR, 1));
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Pass()); // player 1 passes
        assertEquals(2, state.getCurrentPlayer());

        fm.next(state, new Nope());
        // and also that Nope card is discarded
        assertTrue(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Nope());
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Nope());  // player 1 now Nopes

        assertFalse(state.isActionInProgress()); // because no other NOPE cards

        assertEquals(4, state.discardPile.getSize());  // FAVOR and 3 NOPE
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(8, state.getPlayerHand(1).getSize());
    }

}

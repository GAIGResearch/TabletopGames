package games.explodingkittens;

import core.actions.AbstractAction;
import games.explodingkittens.actions.Favor;
import games.explodingkittens.actions.Pass;
import games.explodingkittens.cards.ExplodingKittensCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.*;
import static org.junit.Assert.assertEquals;

public class SpecialCards {

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
    public void favourActions() {
        state.getPlayerHand(0).clear();
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new Favor(0, 1), actions.get(1));
        assertEquals(new Favor(0, 2), actions.get(2));
        assertEquals(new Favor(0, 3), actions.get(3));
    }

    @Test
    public void favourFunctionality() {
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        assertEquals(9, state.getPlayerHand(0).getSize());
        assertEquals(8, state.getPlayerHand(1).getSize());
        fm.next(state, new Favor(0, 1));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(10, state.getPlayerHand(0).getSize());
        assertEquals(7, state.getPlayerHand(1).getSize());
    }
}

package games.explodingkittens;

import core.actions.AbstractAction;
import core.components.Deck;
import games.explodingkittens.actions.*;
import games.explodingkittens.cards.ExplodingKittensCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static games.explodingkittens.cards.ExplodingKittensCard.CardType.*;
import static org.junit.Assert.*;

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

    @Test
    public void shuffle() {
        state.getPlayerHand(0).clear();
        ExplodingKittensCard shuffleCard = new ExplodingKittensCard(SHUFFLE);
        state.getPlayerHand(0).add(shuffleCard);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new Shuffle(0), actions.get(1));

        Deck<ExplodingKittensCard> oldDrawPile = state.drawPile.copy();
        fm.next(state, new Shuffle(0));
        assertEquals(oldDrawPile.getSize()-1, state.drawPile.getSize());
        boolean allSame = true;
        for (int i = 1; i < oldDrawPile.getSize(); i++) {
            if (oldDrawPile.get(i) != state.drawPile.get(i)) {
                allSame = false;
                break;
            }
        }
        assertFalse(allSame);
        boolean containsAll = true;
        for (ExplodingKittensCard c : state.drawPile) {
            if (!oldDrawPile.contains(c)) {
                containsAll = false;
                break;
            }
            oldDrawPile.remove(c);
        }
        assertTrue(containsAll);
    }

    @Test
    public void skip() {
        state.getPlayerHand(0).clear();
        ExplodingKittensCard shuffleCard = new ExplodingKittensCard(SKIP);
        state.getPlayerHand(0).add(shuffleCard);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new Skip(0), actions.get(1));
        int drawDeck = state.drawPile.getSize();

        assertFalse(state.skip);
        fm.next(state, new Skip(0));
        assertFalse(state.skip);

        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(drawDeck, state.drawPile.getSize());
    }

    @Test
    public void attack() {
        state.getPlayerHand(0).clear();
        ExplodingKittensCard shuffleCard = new ExplodingKittensCard(ATTACK);
        state.getPlayerHand(0).add(shuffleCard);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new Attack(0), actions.get(1));
        int drawDeck = state.drawPile.getSize();

        fm.next(state, new Attack(0));
        assertEquals(drawDeck, state.drawPile.getSize());
        assertEquals(2, state.currentPlayerTurnsLeft);
        assertEquals(1, state.getCurrentPlayer());

        fm.next(state, new Pass());
        assertEquals(1, state.currentPlayerTurnsLeft);
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Pass());

        assertEquals(1, state.currentPlayerTurnsLeft);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(drawDeck - 2, state.drawPile.getSize());
    }

    @Test
    public void deathInMiddleOfDoubleMove() {
        ExplodingKittensCard attackCard = new ExplodingKittensCard(ATTACK);
        state.getPlayerHand(0).add(attackCard);
        state.getPlayerHand(1).clear();  // remove DEFUSE card

        fm.next(state, new Attack(0));
        assertEquals(2, state.currentPlayerTurnsLeft);
        assertEquals(1, state.getCurrentPlayer());

        state.drawPile.add(new ExplodingKittensCard(EXPLODING_KITTEN));
        fm.next(state, new Pass());

        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(1, state.currentPlayerTurnsLeft);
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void doubleAttack() {
        ExplodingKittensCard attackCard = new ExplodingKittensCard(ATTACK);
        state.getPlayerHand(0).add(attackCard);

        fm.next(state, new Attack(0));
        assertEquals(2, state.currentPlayerTurnsLeft);
        assertEquals(1, state.getCurrentPlayer());

        state.getPlayerHand(1).add(attackCard);
        fm.next(state, new Attack(1));

        assertEquals(4, state.currentPlayerTurnsLeft);
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void attackAndNope() {
        ExplodingKittensCard attackCard = new ExplodingKittensCard(ATTACK);
        state.getPlayerHand(0).add(attackCard);
        ExplodingKittensCard nopeCard = new ExplodingKittensCard(NOPE);
        state.getPlayerHand(1).add(nopeCard);
        int drawDeck = state.drawPile.getSize();

        fm.next(state, new Attack(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());

        fm.next(state, new PlayInterruptibleCard(NOPE, 1));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getTurnOwner());
        assertEquals(drawDeck - 1, state.drawPile.getSize());

        assertEquals(1, state.currentPlayerTurnsLeft);
    }

}

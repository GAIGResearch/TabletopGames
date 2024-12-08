package games.explodingkittens;

import core.actions.AbstractAction;
import core.components.Deck;
import games.explodingkittens.actions.*;
import games.explodingkittens.cards.ExplodingKittensCard;
import games.hearts.actions.Play;
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
        assertEquals(new PlayEKCard(FAVOR, 1), actions.get(1));
        assertEquals(new PlayEKCard(FAVOR, 2), actions.get(2));
        assertEquals(new PlayEKCard(FAVOR, 3), actions.get(3));
    }

    @Test
    public void favourFunctionality() {
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        assertEquals(9, state.getPlayerHand(0).getSize());
        assertEquals(8, state.getPlayerHand(1).getSize());
        fm.next(state, new PlayEKCard(FAVOR, 1));
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isActionInProgress());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(state.getPlayerHand(1).stream().map(c -> c.cardType).distinct().count(), actions.size());
        fm.next(state, new GiveCard(1, 0, state.getPlayerHand(1).get(0).cardType));
        assertFalse(state.isActionInProgress());

        assertEquals(10, state.getPlayerHand(0).getSize());
        assertEquals(7, state.getPlayerHand(1).getSize());
    }

    @Test
    public void cannotAskFavourOfTheDead() {
        state.getPlayerHand(0).add(new ExplodingKittensCard(FAVOR));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new PlayEKCard(FAVOR, 1)));
        fm.killPlayer(state, 1);
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new PlayEKCard(FAVOR, 1)));
    }

    @Test
    public void shuffle() {
        state.getPlayerHand(0).clear();
        ExplodingKittensCard shuffleCard = new ExplodingKittensCard(SHUFFLE);
        state.getPlayerHand(0).add(shuffleCard);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new PlayEKCard(SHUFFLE), actions.get(1));

        Deck<ExplodingKittensCard> oldDrawPile = state.drawPile.copy();
        fm.next(state, new PlayEKCard(SHUFFLE));
        assertEquals(oldDrawPile.getSize() - 1, state.drawPile.getSize());
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
        assertEquals(new PlayEKCard(SKIP), actions.get(1));
        int drawDeck = state.drawPile.getSize();

        assertFalse(state.skip);
        fm.next(state, new PlayEKCard(SKIP));
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
        assertEquals(new PlayEKCard(ATTACK), actions.get(1));
        int drawDeck = state.drawPile.getSize();

        fm.next(state, new PlayEKCard(ATTACK));
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

        fm.next(state, new PlayEKCard(ATTACK));
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

        fm.next(state, new PlayEKCard(ATTACK));
        assertEquals(2, state.currentPlayerTurnsLeft);
        assertEquals(1, state.getCurrentPlayer());

        state.getPlayerHand(1).add(attackCard);
        fm.next(state, new PlayEKCard(ATTACK));

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

        fm.next(state, new PlayEKCard(ATTACK));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());

        fm.next(state, new Nope());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getTurnOwner());
        assertEquals(drawDeck - 1, state.drawPile.getSize());

        assertEquals(1, state.currentPlayerTurnsLeft);
    }

    @Test
    public void attackAndPotentialNopeLoop() {
        ExplodingKittensCard attackCard = new ExplodingKittensCard(ATTACK);
        state.getPlayerHand(0).add(attackCard);
        state.getPlayerHand(1).add(new ExplodingKittensCard(NOPE));
        state.getPlayerHand(2).add(new ExplodingKittensCard(NOPE));

        fm.next(state, new PlayEKCard(ATTACK));
        fm.next(state, new Pass());
        fm.next(state, new Pass());
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
        assertEquals(2, state.currentPlayerTurnsLeft);
    }

    @Test
    public void seeTheFuture() {
        state.getPlayerHand(0).clear();
        ExplodingKittensCard seeTheFutureCard = new ExplodingKittensCard(SEETHEFUTURE);
        state.getPlayerHand(0).add(seeTheFutureCard);
        int drawDeck = state.drawPile.getSize();

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new PlayEKCard(SEETHEFUTURE), actions.get(1));

        fm.next(state, new PlayEKCard(SEETHEFUTURE));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getTurnOwner());
        assertEquals(drawDeck - 1, state.drawPile.getSize());

        // and check drawpile visibility
        for (int i = 0; i < state.drawPile.getSize(); i++) {
            if (i < 2)  // only the top 2 cards should be visible, as we drew the top one
                assertTrue(state.drawPile.getVisibilityForPlayer(i, 0));
            else
                assertFalse(state.drawPile.getVisibilityForPlayer(i, 0));

            assertFalse(state.drawPile.getVisibilityForPlayer(i, 1));
            assertFalse(state.drawPile.getVisibilityForPlayer(i, 2));
            assertFalse(state.drawPile.getVisibilityForPlayer(i, 3));
        }
    }

    @Test
    public void needTwoCatCardsToPlay() {
        state.getPlayerHand(0).clear();
        state.getPlayerHand(0).add(new ExplodingKittensCard(TACOCAT));
        state.getPlayerHand(0).add(new ExplodingKittensCard(RAINBOWCAT));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new Pass(), actions.get(0));

        state.getPlayerHand(0).add(new ExplodingKittensCard(TACOCAT));
        actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertEquals(new Pass(), actions.get(0));
        assertEquals(new PlayEKCard(TACOCAT, 1), actions.get(1));
        assertEquals(new PlayEKCard(TACOCAT, 2), actions.get(2));

        assertEquals(3, state.getPlayerHand(0).getSize());
        fm.next(state, new PlayEKCard(TACOCAT, 1));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
        assertEquals(2, state.getDiscardPile().getSize());
        assertTrue(state.getDiscardPile().stream().anyMatch(c -> c.cardType == TACOCAT));
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(7, state.getPlayerHand(1).getSize());
    }

    @Test
    public void nopeTwoCatCardsDiscardsBoth() {
        state.getPlayerHand(0).add(new ExplodingKittensCard(TACOCAT));
        state.getPlayerHand(0).add(new ExplodingKittensCard(TACOCAT));
        state.getPlayerHand(3).add(new ExplodingKittensCard(NOPE));

        fm.next(state, new PlayEKCard(TACOCAT, 3));
        assertTrue(state.isActionInProgress());
        fm.next(state, new Nope());
        assertFalse(state.isActionInProgress());

        assertEquals(3, state.getDiscardPile().getSize()); // 2 x TACOCAT + NOPE
        assertEquals(9, state.getPlayerHand(0).getSize());
    }
}

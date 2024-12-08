package games.explodingkittens;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.explodingkittens.actions.DefuseKitten;
import games.explodingkittens.actions.Pass;
import games.explodingkittens.actions.PlaceKitten;
import games.explodingkittens.actions.PlayEKCard;
import games.explodingkittens.cards.ExplodingKittensCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.*;
import static org.junit.Assert.*;

public class BasicGameTurn {

    ExplodingKittensGameState state;
    ExplodingKittensForwardModel fm;
    ExplodingKittensParameters params;
    Random rnd = new Random(4033);

    @Before
    public void init() {
        params = new ExplodingKittensParameters();
        // this removes any cards which have extra action decisions to make
        params.cardCounts.put(ATTACK, 0);
        params.cardCounts.put(SKIP, 0);
        params.cardCounts.put(FAVOR, 0);
        params.cardCounts.put(NOPE, 0);
        state = new ExplodingKittensGameState(params, 4);
        fm = new ExplodingKittensForwardModel();
        fm.setup(state);
    }


    @Test
    public void setupTest() {
        // check that every player has at least on DEFUSE card (and 8 cards in total)
        // and that the draw pile contains 3 Exploding kittens
        // and that the discard pile is empty

        for (int player = 0; player < state.getNPlayers(); player++) {
            assertEquals(8, state.playerHandCards.get(player).getSize());
            assertTrue(state.playerHandCards.get(player).stream().filter(c -> c.cardType == ExplodingKittensCard.CardType.DEFUSE).count() == 1);
        }
        assertEquals(0, state.discardPile.getSize());
        assertEquals(3, state.drawPile.stream().filter(c -> c.cardType == ExplodingKittensCard.CardType.EXPLODING_KITTEN).count());
    }

    @Test
    public void basicTurnSequenceTest() {
        // Each player should play or pass until the game ends
        int expectedPlayer = 0;
        do {
                System.out.println("Player " + expectedPlayer + " playing");
            assertEquals(expectedPlayer, state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
            if (!state.isActionInProgress())
                do {
                    expectedPlayer = (expectedPlayer + 1) % state.getNPlayers();
                } while (state.getPlayerResults()[expectedPlayer] == CoreConstants.GameResult.LOSE_GAME);
        } while (state.isNotTerminal());
    }

    @Test
    public void passActionDrawsCard() {
        state.drawPile.add(new ExplodingKittensCard(TACOCAT));
        fm.next(state, new Pass());
        assertEquals(9, state.playerHandCards.get(0).getSize());
        assertEquals(0, state.discardPile.getSize());
    }

    @Test
    public void playActionDrawsCard() {
        state.drawPile.add(new ExplodingKittensCard(TACOCAT));
        state.drawPile.add(new ExplodingKittensCard(TACOCAT));
        state.playerHandCards.get(0).add(new ExplodingKittensCard(SHUFFLE));
        assertEquals(9, state.playerHandCards.get(0).getSize());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(new Pass(), actions.get(0));
        fm.next(state, new PlayEKCard(SHUFFLE));
        if (state.getDiscardPile().getSize() == 2) {
            assertTrue(state.discardPile.stream().anyMatch(c -> c.cardType == DEFUSE));
            assertTrue(state.discardPile.stream().anyMatch(c -> c.cardType == SHUFFLE));
            assertEquals(8, state.playerHandCards.get(0).getSize());
        } else {
            assertEquals(1, state.discardPile.getSize());
            assertEquals(9, state.playerHandCards.get(0).getSize());
        }
;
    }

    @Test
    public void defuseStopsExplodingKitten() {
        state.drawPile.add(new ExplodingKittensCard(EXPLODING_KITTEN));
        assertTrue(state.getPlayerHand(0).stream().anyMatch(c -> c.cardType == DEFUSE));
        fm.next(state, new Pass());
        assertTrue(state.isNotTerminalForPlayer(0));
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.discardPile.getSize());
        assertEquals(3, state.drawPile.stream().filter(c -> c.cardType == EXPLODING_KITTEN).count());
    }

    @Test
    public void defuseGivesChoiceOfPlacementToPlayer() {
        state.drawPile.add(new ExplodingKittensCard(EXPLODING_KITTEN));
        assertTrue(state.getPlayerHand(0).stream().anyMatch(c -> c.cardType == DEFUSE));
        fm.next(state, new Pass());
        assertTrue(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof DefuseKitten);

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(state.drawPile.getSize()+1, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof PlaceKitten));

        assertEquals(1, state.discardPile.getSize());
        int index = ((PlaceKitten) actions.get(3)).index;
        fm.next(state, actions.get(3));

        assertEquals(1, state.discardPile.getSize());
        assertEquals(4, state.drawPile.stream().filter(c -> c.cardType == EXPLODING_KITTEN).count());
        assertEquals(EXPLODING_KITTEN, state.drawPile.get(index).cardType);
        assertTrue(state.drawPile.getVisibilityForPlayer(index, 0));
        assertFalse(state.drawPile.getVisibilityForPlayer(index, 1));
        assertFalse(state.drawPile.getVisibilityForPlayer(index, 2));
        assertFalse(state.drawPile.getVisibilityForPlayer(index, 3));

        assertEquals(0, state.getActionsInProgress().size());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void defusingDoesNotDrawExtraCard() {
        state.drawPile.add(new ExplodingKittensCard(EXPLODING_KITTEN));
        fm.next(state, new Pass());
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.discardPile.getSize());
        assertEquals(8, state.playerHandCards.get(0).getSize()); // +KITTEN -DEFUSE
        fm.next(state, new PlaceKitten(3));
        assertEquals(1, state.discardPile.getSize());
        assertEquals(4, state.drawPile.stream().filter(c -> c.cardType == EXPLODING_KITTEN).count());
    }

    @Test
    public void explodingKittenKillsPlayer() {
        state.drawPile.add(new ExplodingKittensCard(EXPLODING_KITTEN));
        List<ExplodingKittensCard> defuseCards = state.playerHandCards.get(0).stream().filter(c -> c.cardType == DEFUSE).toList();
        state.playerHandCards.get(0).removeAll(defuseCards);
        assertTrue(state.getPlayerHand(0).stream().noneMatch(c -> c.cardType == DEFUSE));
        fm.next(state, new Pass());
        assertFalse(state.isNotTerminalForPlayer(0));
        assertEquals(1, state.discardPile.getSize());
        assertEquals(3, state.drawPile.stream().filter(c -> c.cardType == EXPLODING_KITTEN).count());
    }

    @Test
    public void deadPlayerMissesAllFutureTurns() {
        fm.killPlayer(state, 2);
        do {
            assertNotEquals(2, state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
        } while (state.isNotTerminal());
    }

}

package games.explodingkittens;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.explodingkittens.actions.Pass;
import games.explodingkittens.actions.PlayInterruptibleCard;
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
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(new Pass(), actions.get(0));
        assertTrue(actions.get(1) instanceof PlayInterruptibleCard);
        fm.next(state, actions.get(1));
        assertEquals(8, state.playerHandCards.get(1).getSize());
        assertEquals(1, state.discardPile.getSize());
    }

    @Test
    public void defuseStopsExplodingKitten() {
        state.drawPile.add(new ExplodingKittensCard(EXPLODING_KITTEN));
        assertTrue(state.getPlayerHand(0).stream().anyMatch(c -> c.cardType == DEFUSE));
        fm.next(state, new Pass());
        assertTrue(state.isNotTerminalForPlayer(0));
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
}
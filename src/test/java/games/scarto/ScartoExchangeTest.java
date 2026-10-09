package games.scarto;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static core.components.TarotCard.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * The dealer's exchange (ScartoParameters.dealerExchange): the pick-up of the scarto, which cards may be discarded,
 * the discards themselves and their scoring, and whole random games with the exchange.
 */
public class ScartoExchangeTest {

    ScartoForwardModel fm = new ScartoForwardModel();

    /** Asserts the dealer (2) is to act and exchanging, then discards the card with fm.next. */
    void discard(ScartoGameState state, TarotCard card) {
        assertEquals("player to act before discarding " + card, 2, state.getCurrentPlayer());
        fm.next(state, new Discard(card));
    }

    private static List<Discard> discards(TarotCard... cards) {
        return java.util.Arrays.stream(cards).map(Discard::new).toList();
    }

    // ---- the pick-up at the deal ----

    @Test
    public void withTheExchangeTheDealerPicksUpTheScartoAndIsToDiscard() {
        ScartoGameState state = newState(3, fm, exchange());
        assertEquals(2, state.getDealer());
        // 25 dealt + the 3 scarto cards = 28; the scarto is empty until the dealer discards
        assertEquals(28, state.getPlayerHand(2).getSize());
        assertEquals(25, state.getPlayerHand(0).getSize());
        assertEquals(25, state.getPlayerHand(1).getSize());
        assertEquals(0, state.getScarto().getSize());
        assertAllCardsPresent(state);
        assertTrue(state.isExchanging());
        assertEquals(2, state.getCurrentPlayer());
        // the first trick is still to be led by player 0, after the dealer
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.isEmpty());
        for (AbstractAction a : actions) {
            assertTrue("not a Discard: " + a, a instanceof Discard);
            assertTrue(state.getPlayerHand(2).contains(((Discard) a).card));
        }
    }

    @Test
    public void theDealersTwentyEightAreTheirDealtHandAndTheScarto() {
        // the same seed deals the same cards with and without the exchange: with it, the dealer holds their 25
        // and the 3 cards that would have been the scarto
        ScartoGameState without = newState(7, fm);
        ScartoGameState with = newState(7, fm, exchange());
        Set<TarotCard> expected = new HashSet<>(without.getPlayerHand(2).getComponents());
        expected.addAll(without.getScarto().getComponents());
        assertEquals(expected, setOf(with.getPlayerHand(2)));
        assertEquals(setOf(without.getPlayerHand(0)), setOf(with.getPlayerHand(0)));
    }

    @Test
    public void withTheExchangeTheScartoIsTheDealersAndVisibleToThem() {
        ScartoGameState state = newState(3, fm, exchange());
        assertEquals(2, state.getScarto().getOwnerId());
        assertEquals(VISIBLE_TO_OWNER, state.getScarto().getVisibilityMode());
    }

    @Test
    public void withoutTheExchangeTheScartoIsHiddenAndUnowned() {
        ScartoGameState state = newState(3, fm);
        assertEquals(-1, state.getScarto().getOwnerId());
        assertEquals(HIDDEN_TO_ALL, state.getScarto().getVisibilityMode());
        assertFalse(state.isExchanging());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void withoutTheExchangeADealerHoldingMoreThanAHandIsNotExchanging() {
        // the scarto moved into the dealer's hand by hand (28 cards): with dealerExchange off this is no exchange,
        // and player 0 still leads with any of their 25 cards
        ScartoGameState state = newState(3, fm);
        for (TarotCard c : List.copyOf(state.getScarto().getComponents())) {
            state.getScarto().remove(c);
            state.getPlayerHand(2).add(c);
        }
        assertEquals(28, state.getPlayerHand(2).getSize());
        assertFalse(state.isExchanging());
        assertEquals(playCards(state.getPlayerHand(0).getComponents().toArray(new TarotCard[0])),
                fm.computeAvailableActions(state));
    }

    // ---- which cards may be discarded ----

    @Test
    public void theDealerMayDiscardAnyCardButAKingTheAngelThePagatOrTheFool() {
        ScartoGameState state = newState(9, fm, exchange());
        // a 28-card dealer's hand (displaced cards to player 0): the 4 Kings, Angel, Pagat and Fool, plus 21 others
        // including two other trumps (so the Pagat is not the only trump anyway)
        TarotCard[] hand = {
                sword(KING), sword(1), sword(2), trump(ANGEL), sword(3), sword(4), baton(KING), sword(5),
                trump(PAGAT), sword(6), sword(7), fool(), sword(8), cup(KING), sword(9), sword(10), trump(5),
                baton(1), coin(KING), baton(2), baton(3), trump(WORLD), baton(4), baton(5), cup(QUEEN),
                coin(KNAVE), cup(CAVALIER), baton(QUEEN)};
        assertEquals(28, hand.length);
        setDeck(state, state.playerHands.get(2), state.playerHands.get(0), hand);
        assertEquals(2, state.getCurrentPlayer());
        // the 21 other cards, in hand order
        assertEquals(discards(sword(1), sword(2), sword(3), sword(4), sword(5), sword(6), sword(7), sword(8),
                        sword(9), sword(10), trump(5), baton(1), baton(2), baton(3), trump(WORLD), baton(4),
                        baton(5), cup(QUEEN), coin(KNAVE), cup(CAVALIER), baton(QUEEN)),
                fm.computeAvailableActions(state));
    }

    @Test
    public void thePagatMayBeDiscardedWhenItIsTheOnlyTrumpAndThereIsNoFool() {
        // no other trump, no Fool: the Pagat is allowed (in hand order); the King still is not
        assertEquals(List.of(cup(3), trump(PAGAT), baton(5)),
                ScartoUtils.discardable(List.of(cup(3), trump(PAGAT), sword(KING), baton(5))));
    }

    @Test
    public void thePagatMayNotBeDiscardedWithAnotherTrump() {
        // trump 7 is another trump: the Pagat stays; trump 7 itself may go
        assertEquals(List.of(cup(3), trump(7), baton(5)),
                ScartoUtils.discardable(List.of(cup(3), trump(PAGAT), trump(7), baton(5))));
    }

    @Test
    public void thePagatMayNotBeDiscardedWithTheFool() {
        // the Fool counts as a trump, so the Pagat is not the only one; the Fool may never go
        assertEquals(List.of(cup(3), baton(5)),
                ScartoUtils.discardable(List.of(cup(3), fool(), trump(PAGAT), baton(5))));
    }

    @Test
    public void theAngelCountsAsAnotherTrumpForThePagat() {
        // the Angel is a trump: neither honour may go
        assertEquals(List.of(cup(3)),
                ScartoUtils.discardable(List.of(trump(ANGEL), trump(PAGAT), cup(3))));
    }

    // ---- discarding ----

    @Test
    public void aDiscardMovesTheCardToTheScartoAndTheDealerDiscardsAgain() {
        ScartoGameState state = newState(5, fm, exchange());
        putInHand(state, 2, sword(3), baton(4));

        discard(state, sword(3));
        // 28 - 1 = 27 in hand; the scarto holds the discard; the dealer is still exchanging
        assertEquals(27, state.getPlayerHand(2).getSize());
        assertFalse(state.getPlayerHand(2).contains(sword(3)));
        assertEquals(List.of(sword(3)), state.getScarto().getComponents());
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isExchanging());

        discard(state, baton(4));
        assertEquals(26, state.getPlayerHand(2).getSize());
        assertEquals(Set.of(sword(3), baton(4)), setOf(state.getScarto()));
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isExchanging());
        for (AbstractAction a : fm.computeAvailableActions(state))
            assertTrue("not a Discard: " + a, a instanceof Discard);
        assertAllCardsPresent(state);
    }

    @Test
    public void afterTheThirdDiscardThePlayerAfterTheDealerLeads() {
        ScartoGameState state = newState(5, fm, exchange());
        putInHand(state, 2, sword(3), baton(4), coin(QUEEN));
        discard(state, sword(3));
        discard(state, baton(4));
        discard(state, coin(QUEEN));

        // 28 - 3 = 25 in hand, the scarto is the three discards
        assertEquals(25, state.getPlayerHand(2).getSize());
        assertEquals(Set.of(sword(3), baton(4), coin(QUEEN)), setOf(state.getScarto()));
        assertFalse(state.isExchanging());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        // player 0 leads: any of their 25 cards, in hand order
        assertEquals(playCards(state.getPlayerHand(0).getComponents().toArray(new TarotCard[0])),
                fm.computeAvailableActions(state));
        // discarding reveals no voids
        for (int p = 0; p < 3; p++)
            assertTrue(state.getKnownVoids().get(p).isEmpty());
        // the scarto still belongs to the dealer
        assertEquals(2, state.getScarto().getOwnerId());
        assertEquals(VISIBLE_TO_OWNER, state.getScarto().getVisibilityMode());
        assertAllCardsPresent(state);
    }

    // ---- scoring ----

    @Test
    public void theDiscardsScoreForTheDealerAtTheEndOfTheDeal() {
        ScartoGameState state = newState(11, fm, exchange());
        putInHand(state, 2, sword(QUEEN), coin(CAVALIER), cup(KNAVE));
        discard(state, sword(QUEEN));
        discard(state, coin(CAVALIER));
        discard(state, cup(KNAVE));
        assertEquals(0, state.getCurrentPlayer());

        // arrange the last trick, keeping the scarto (the discards): displaced cards all go to player 1's pile.
        // Player 0 holds cup King, player 1 cup Queen, player 2 trump 2; player 0 has won the Angel and coin King
        Deck<TarotCard> pile1 = state.cardsWon.get(1);
        setDeck(state, state.playerHands.get(0), pile1, cup(KING));
        setDeck(state, state.playerHands.get(1), pile1, cup(QUEEN));
        setDeck(state, state.playerHands.get(2), pile1, trump(2));
        setDeck(state, state.cardsWon.get(0), pile1, trump(ANGEL), coin(KING));
        setDeck(state, state.cardsWon.get(2), pile1);
        assertEquals(Set.of(sword(QUEEN), coin(CAVALIER), cup(KNAVE)), setOf(state.getScarto()));
        // 78 - 3 (hands) - 2 (p0's pile) - 3 (scarto) = 70
        assertEquals(70, pile1.getSize());
        assertAllCardsPresent(state);

        play(state, fm, 0, cup(KING));
        play(state, fm, 1, cup(QUEEN));
        play(state, fm, 2, trump(2));  // the trump takes the trick
        assertFalse(state.isNotTerminal());

        // pack card points 51; outside p1's pile: cup King 4, cup Queen 3, Angel 4, coin King 4, scarto
        // Queen 3 + Cavalier 2 + Knave 1 = 6 -> 21, so p1's 70 cards hold 30
        // p0: 8 + (2 + 1) / 3 = 1 -> 9
        // p1: 30 + (70 + 1) / 3 = 23 -> 53
        // p2 (dealer): trick 4 + 3 + 0 = 7 plus discards 6 = 13, 6 cards -> (6 + 1) / 3 = 2 -> 15
        //   (without the discards: 7 + 1 = 8). 9 + 53 + 15 = 77
        assertEquals(9, state.getGameScore(0), 1e-9);
        assertEquals(53, state.getGameScore(1), 1e-9);
        assertEquals(15, state.getGameScore(2), 1e-9);
    }

    // ---- random games ----

    @Test
    public void aRandomDealWithTheExchangeStartsWithThreeDiscardsByTheDealer() {
        for (long seed = 1; seed <= 3; seed++) {
            Game game = newGame(seed, exchange());
            ScartoGameState state = (ScartoGameState) game.getGameState();
            AbstractForwardModel gfm = game.getForwardModel();
            Random rnd = new Random(seed);
            int actions = 0;
            while (state.isNotTerminal()) {
                // 3 discards + 25 tricks of 3 cards = 78
                assertTrue("seed " + seed + ": more than 78 actions", actions < 78);
                List<AbstractAction> available = gfm.computeAvailableActions(state);
                assertFalse("seed " + seed + ": no actions at action " + actions, available.isEmpty());
                boolean exchanging = actions < 3;
                if (exchanging)
                    assertEquals(2, state.getCurrentPlayer());
                for (AbstractAction a : available)
                    assertEquals("seed " + seed + " action " + actions + ": " + a, exchanging, a instanceof Discard);
                gfm.next(state, available.get(rnd.nextInt(available.size())));
                actions++;
                assertAllCardsPresent(state);
            }
            assertEquals(78, actions);
            double total = 0;
            for (int p = 0; p < 3; p++)
                total += state.getGameScore(p);
            assertEquals("seed " + seed, 77, total, 1e-9);
        }
    }

    @Test
    public void inARandomThreeDealGameEachDealerExchangesAtTheirDeal() {
        for (long seed = 1; seed <= 3; seed++) {
            Game game = newGame(seed, exchangeThreeDeals());
            ScartoGameState state = (ScartoGameState) game.getGameState();
            AbstractForwardModel gfm = game.getForwardModel();
            Random rnd = new Random(seed);
            List<Integer> dealers = new ArrayList<>();
            int actions = 0, nDiscards = 0, nPlays = 0;
            while (state.isNotTerminal()) {
                // 3 deals of 3 discards + 75 cards played = 234
                assertTrue("seed " + seed + ": more than 234 actions", actions < 234);
                int dealer = state.getDealer();
                if (actions % 78 == 0) {
                    // a fresh deal: the dealer holds 28 and is to discard; the scarto is theirs, empty
                    assertEquals("seed " + seed, actions / 78, state.getRoundCounter());
                    dealers.add(dealer);
                    assertEquals(28, state.getPlayerHand(dealer).getSize());
                    assertEquals(25, state.getPlayerHand((dealer + 1) % 3).getSize());
                    assertEquals(25, state.getPlayerHand((dealer + 2) % 3).getSize());
                    assertEquals(0, state.getScarto().getSize());
                    assertEquals(dealer, state.getScarto().getOwnerId());
                    assertEquals(VISIBLE_TO_OWNER, state.getScarto().getVisibilityMode());
                    assertEquals((dealer + 1) % 3, state.getCurrentTrick().getLeader());
                }
                List<AbstractAction> available = gfm.computeAvailableActions(state);
                assertFalse("seed " + seed + ": no actions at action " + actions, available.isEmpty());
                boolean exchanging = actions % 78 < 3;
                if (exchanging)
                    assertEquals("seed " + seed + " action " + actions, dealer, state.getCurrentPlayer());
                for (AbstractAction a : available)
                    assertEquals("seed " + seed + " action " + actions + ": " + a, exchanging, a instanceof Discard);
                AbstractAction chosen = available.get(rnd.nextInt(available.size()));
                if (chosen instanceof Discard) nDiscards++;
                if (chosen instanceof PlayCard) nPlays++;
                gfm.next(state, chosen);
                actions++;
                assertAllCardsPresent(state);
            }
            assertEquals(234, actions);
            assertEquals(9, nDiscards);
            assertEquals(225, nPlays);
            assertEquals(List.of(2, 0, 1), dealers);
            double total = 0;
            for (int p = 0; p < 3; p++)
                total += state.getGameScore(p);
            assertEquals("seed " + seed, 3 * 77, total, 1e-9);
        }
    }

    @Test
    public void withoutTheExchangeNoDiscardIsEverOfferedAndTheScartoStaysHidden() {
        for (long seed = 1; seed <= 2; seed++) {
            Game game = newGame(seed, threeDeals());
            ScartoGameState state = (ScartoGameState) game.getGameState();
            AbstractForwardModel gfm = game.getForwardModel();
            Random rnd = new Random(seed);
            int actions = 0;
            while (state.isNotTerminal()) {
                assertTrue("seed " + seed + ": more than 225 actions", actions < 225);
                assertEquals(-1, state.getScarto().getOwnerId());
                assertEquals(HIDDEN_TO_ALL, state.getScarto().getVisibilityMode());
                List<AbstractAction> available = gfm.computeAvailableActions(state);
                assertFalse(available.isEmpty());
                for (AbstractAction a : available)
                    assertTrue("seed " + seed + " action " + actions + ": " + a, a instanceof PlayCard);
                gfm.next(state, available.get(rnd.nextInt(available.size())));
                actions++;
            }
            assertEquals(225, actions);
        }
    }
}

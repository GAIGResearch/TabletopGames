package games.sueca;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * What a redeterminised copy, copy(p), keeps of the hidden information: the trump card was turned face up, so it
 * stays in the dealer's hand until played; and a player known to be void in a suit (from failing to follow it) is
 * never given a card of that suit, when SuecaParameters.rememberVoids.
 * <p>
 * The constraint tests take many copies: an unconstrained shuffle breaks each constraint in a single copy with the
 * probability given in each test, so the chance of a false pass is that probability's complement to the power of the
 * number of copies.
 */
public class SuecaRedeterminisationTest {

    private static final int COPIES = 100;

    /**
     * Hearts trumps (2H with player 3, the dealer); player 1 holds no diamonds, player 3 no clubs.
     */
    private static final String[][] VOID_DEAL = {
            {"AD", "KD", "QD", "5D", "AS", "2S", "7H", "3H", "JC", "5C"},
            {"7S", "3S", "JS", "QS", "AH", "4H", "6H", "KC", "6C", "2C"},
            {"KH", "5H", "JD", "4D", "2D", "AC", "7C", "QC", "4C", "3C"},
            {"2H", "JH", "QH", "7D", "6D", "3D", "KS", "6S", "5S", "4S"}
    };

    /**
     * VOID_DEAL, then two tricks through fm.next, each led and won by player 0:
     * AD QS 2D 3D - player 1 cannot follow diamonds and discards QS (so is void in diamonds);
     * JC 2C 3C 4S - player 3 (the dealer) cannot follow clubs and discards 4S, keeping the trump card 2H.
     * Player 0 is to lead the third trick; each player holds 8 cards.
     */
    private static SuecaGameState voidPosition(boolean rememberVoids) {
        SuecaParameters params = new SuecaParameters();
        params.setParameterValue("rememberVoids", rememberVoids);
        SuecaGameState state = newState(7, params);
        SuecaForwardModel fm = new SuecaForwardModel();
        arrangeHands(state, cards(VOID_DEAL[0]), cards(VOID_DEAL[1]), cards(VOID_DEAL[2]), cards(VOID_DEAL[3]));
        setTrumpCard(state, "2H");
        arrangeTrick(state, 0);
        playCards(state, fm, "AD", "QS", "2D", "3D");
        playCards(state, fm, "JC", "2C", "3C", "4S");
        // arrangement guard
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(8, state.getTeamPile(0).getSize());
        for (int p = 0; p < 4; p++)
            assertEquals(8, state.getPlayerHand(p).getSize());
        assertEquals(3, state.getDealer());
        assertTrue(state.getPlayerHand(3).contains(card("2H")));
        assertAllCardsPresent(state);
        return state;
    }

    private static Set<FrenchCard.Suite> suitsOf(SuecaGameState s, int player) {
        Set<FrenchCard.Suite> suits = EnumSet.noneOf(FrenchCard.Suite.class);
        for (FrenchCard c : s.getPlayerHand(player).getComponents())
            suits.add(c.suite);
        return suits;
    }

    private static void assertHandSizesKept(SuecaGameState state, SuecaGameState copy) {
        for (int p = 0; p < 4; p++)
            assertEquals("hand " + p, state.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
    }

    // ---- the trump card ----

    @Test
    public void theDealerKeepsTheTrumpCardInEveryRedeterminisationWhileHoldingIt() {
        // an unconstrained shuffle of the 30 hidden cards leaves the trump card in the dealer's 10 with chance 1/3
        // per copy: a false pass needs it to stay there in all 5 x 3 x 40 = 600 copies, (1/3)^600
        for (long seed = 1; seed <= 5; seed++) {
            SuecaGameState state = newState(seed);
            int dealer = state.getDealer();
            FrenchCard trumpCard = state.getTrumpCard();
            assertEquals(3, dealer);
            assertTrue(state.getPlayerHand(dealer).contains(trumpCard));
            for (int observer = 0; observer < 3; observer++) {
                int dealerHandChanged = 0;
                for (int i = 0; i < 40; i++) {
                    SuecaGameState copy = (SuecaGameState) state.copy(observer);
                    assertTrue("seed " + seed + ", observer " + observer + ": the dealer lost the trump card " + trumpCard,
                            copy.getPlayerHand(dealer).contains(trumpCard));
                    assertEquals(trumpCard, copy.getTrumpCard());
                    assertHandSizesKept(state, copy);
                    assertAllCardsPresent(copy);
                    if (!new HashSet<>(cardsOf(copy.getPlayerHand(dealer))).equals(new HashSet<>(cardsOf(state.getPlayerHand(dealer)))))
                        dealerHandChanged++;
                }
                // only the trump card is known: the dealer's other nine cards are still redeterminised
                assertTrue("seed " + seed + ", observer " + observer + ": the dealer's hand was never redeterminised",
                        dealerHandChanged > 0);
            }
        }
    }

    @Test
    public void theDealersOwnCopyKeepsTheirHandExactly() {
        // the observer's own hand is never reshuffled
        for (long seed = 1; seed <= 5; seed++) {
            SuecaGameState state = newState(seed);
            for (int i = 0; i < 20; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(3);
                assertEquals(cardsOf(state.getPlayerHand(3)), cardsOf(copy.getPlayerHand(3)));
                assertHandSizesKept(state, copy);
            }
        }
    }

    @Test
    public void onceTheTrumpCardIsInTheTrickNothingIsKeptForTheDealer() {
        SuecaGameState state = newState(42);
        arrangeDeal(state);
        // players 1, 2 and 3 have played to a trick led by player 1, the dealer (3) trumping with 2H; player 0 to play
        arrangeTrick(state, 1, "7S", "KS", "2H");
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isTrumpCardHeld());
        for (int observer = 0; observer < 3; observer++) {
            for (int i = 0; i < COPIES; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(observer);
                assertEquals(cards("7S", "KS", "2H"), cardsOf(copy.getCurrentTrick()));
                assertFalse("observer " + observer + ": the played trump card came back to the dealer",
                        copy.getPlayerHand(3).contains(card("2H")));
                assertHandSizesKept(state, copy);   // 10, 9, 9, 9
                assertAllCardsPresent(copy);        // no duplicate trump card
            }
        }
    }

    @Test
    public void onceTheTrumpCardIsOnATeamPileNothingIsKeptForTheDealer() {
        SuecaGameState state = newState(42);
        arrangeDeal(state);
        arrangeTrick(state, 1, "7S", "KS", "2H", "AS");   // a complete first trick including the trump card...
        arrangeTrick(state, 0);                            // ...moved to team 0's pile; player 0 to lead
        assertTrue(state.getTeamPile(0).contains(card("2H")));
        assertFalse(state.isTrumpCardHeld());
        for (int observer = 0; observer < 3; observer++) {
            for (int i = 0; i < COPIES; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(observer);
                assertEquals(cardsOf(state.getTeamPile(0)), cardsOf(copy.getTeamPile(0)));
                assertFalse("observer " + observer + ": the played trump card came back to the dealer",
                        copy.getPlayerHand(3).contains(card("2H")));
                assertHandSizesKept(state, copy);   // 9 each
                assertAllCardsPresent(copy);
            }
        }
    }

    // ---- known voids ----

    @Test
    public void failingToFollowSuitRecordsAVoidWhenRememberVoids() {
        // a guard for the redeterminisation tests below; the voids are recorded by the shared PlayCard
        SuecaGameState state = voidPosition(true);
        assertEquals(EnumSet.noneOf(FrenchCard.Suite.class), state.getKnownVoids().get(0));
        assertEquals(EnumSet.of(Diamonds), state.getKnownVoids().get(1));
        assertEquals(EnumSet.noneOf(FrenchCard.Suite.class), state.getKnownVoids().get(2));
        assertEquals(EnumSet.of(Clubs), state.getKnownVoids().get(3));

        SuecaGameState forgetful = voidPosition(false);
        for (int p = 0; p < 4; p++)
            assertEquals(EnumSet.noneOf(FrenchCard.Suite.class), forgetful.getKnownVoids().get(p));
    }

    @Test
    public void aPlayerKnownToBeVoidInASuitIsNeverGivenThatSuit() {
        // Observer 0 sees 24 hidden cards (the other three hands of 8), 4 of them diamonds (JD 4D 7D 6D).
        // Unconstrained, player 1's 8 get no diamond with chance C(20,8)/C(24,8) = (16*15*14*13)/(24*23*22*21)
        // = 0.171 per copy (observer 2, with 5 hidden diamonds, less): a false pass is below 0.171^200.
        SuecaGameState state = voidPosition(true);
        for (int observer : new int[]{0, 2}) {
            for (int i = 0; i < COPIES; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(observer);
                assertFalse("observer " + observer + ": player 1 was given a diamond: " + cardsOf(copy.getPlayerHand(1)),
                        suitsOf(copy, 1).contains(Diamonds));
                assertEquals(cardsOf(state.getPlayerHand(observer)), cardsOf(copy.getPlayerHand(observer)));
                assertEquals(state.getKnownVoids(), copy.getKnownVoids());
                assertHandSizesKept(state, copy);
                assertAllCardsPresent(copy);
            }
        }
    }

    @Test
    public void withoutRememberVoidsTheSuitCanComeBack() {
        // the same play with rememberVoids = false records nothing, so player 1 may be given a diamond again:
        // with 0.829 per copy, it fails to happen in 100 copies with chance 0.171^100
        SuecaGameState state = voidPosition(false);
        int diamondsToPlayer1 = 0;
        int clubsToPlayer3 = 0;
        for (int i = 0; i < COPIES; i++) {
            SuecaGameState copy = (SuecaGameState) state.copy(0);
            if (suitsOf(copy, 1).contains(Diamonds)) diamondsToPlayer1++;
            if (suitsOf(copy, 3).contains(Clubs)) clubsToPlayer3++;
            assertHandSizesKept(state, copy);
            assertAllCardsPresent(copy);
        }
        assertTrue("player 1 was never given a diamond", diamondsToPlayer1 > 0);
        assertTrue("player 3 was never given a club", clubsToPlayer3 > 0);
    }

    @Test
    public void theDealerKnownToBeVoidKeepsTheTrumpCardAndGetsNoCardOfTheVoidSuit() {
        // Player 3 (the dealer) is void in clubs and holds the trump card 2H. Unconstrained, the dealer's 8 of the
        // 24 hidden cards keep 2H with chance 1/3 per copy, and get no club with chance C(24-k,8)/C(24,8) for the
        // k hidden clubs: observer 0 k = 6 (0.06), observer 1 k = 5 (0.10), observer 2 k = 3 (0.28). Both hold by
        // chance with at most 0.28/3 per copy: a false pass over 300 copies is below 0.1^300.
        SuecaGameState state = voidPosition(true);
        for (int observer = 0; observer < 3; observer++) {
            for (int i = 0; i < COPIES; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(observer);
                assertTrue("observer " + observer + ": the dealer lost the trump card",
                        copy.getPlayerHand(3).contains(card("2H")));
                assertFalse("observer " + observer + ": the dealer was given a club: " + cardsOf(copy.getPlayerHand(3)),
                        suitsOf(copy, 3).contains(Clubs));
                if (observer != 1)
                    assertFalse("observer " + observer + ": player 1 was given a diamond", suitsOf(copy, 1).contains(Diamonds));
                assertHandSizesKept(state, copy);
                assertAllCardsPresent(copy);
            }
        }
    }

    // ---- integration ----

    @Test
    public void redeterminisationsDuringRandomDealsRespectTheTrumpCardAndKnownVoids() {
        // at every decision of 5 seeded random deals, 3 copies for the player to act check both constraints
        int trumpChecks = 0;
        int voidChecks = 0;
        for (long seed = 1; seed <= 5; seed++) {
            Game game = newGame(seed);
            SuecaGameState state = (SuecaGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            int dealer = state.getDealer();
            FrenchCard trumpCard = state.getTrumpCard();
            Random rnd = new Random(seed);
            int actions = 0;
            while (state.isNotTerminal() && actions < 100) {
                int observer = state.getCurrentPlayer();
                boolean held = state.getPlayerHand(dealer).contains(trumpCard);
                for (int i = 0; i < 3; i++) {
                    SuecaGameState copy = (SuecaGameState) state.copy(observer);
                    assertHandSizesKept(state, copy);
                    assertAllCardsPresent(copy);
                    assertEquals("seed " + seed + ", action " + actions + ", observer " + observer
                            + ": the dealer holds the trump card " + trumpCard, held, copy.getPlayerHand(dealer).contains(trumpCard));
                    if (held && observer != dealer) trumpChecks++;
                    for (int p = 0; p < 4; p++) {
                        Set<FrenchCard.Suite> voids = state.getKnownVoids().get(p);
                        if (p == observer || voids.isEmpty()) continue;
                        Set<FrenchCard.Suite> given = suitsOf(copy, p);
                        given.retainAll(voids);
                        assertTrue("seed " + seed + ", action " + actions + ": player " + p + " void in " + voids
                                + " was given " + cardsOf(copy.getPlayerHand(p)), given.isEmpty());
                        voidChecks++;
                    }
                }
                List<AbstractAction> available = fm.computeAvailableActions(state);
                fm.next(state, available.get(rnd.nextInt(available.size())));
                actions++;
            }
            assertFalse("seed " + seed + ": the deal did not end", state.isNotTerminal());
        }
        // guards: both constraints were exercised
        assertTrue("trump card checks " + trumpChecks, trumpChecks > 30);
        assertTrue("known void checks " + voidChecks, voidChecks > 30);
    }
}

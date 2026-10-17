package games.president;

import games.president.actions.GiveCard;
import games.president.actions.PlayCards;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.president.PresidentGameState.Phase.EXCHANGE;
import static games.president.PresidentGameState.Phase.PLAY;
import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * The exchange before each later deal: PresidentForwardModel.startExchange on arranged hands, then the President's
 * GiveCard decisions.
 */
public class PresidentExchangeTest {

    PresidentForwardModel fm;

    @Before
    public void setup() {
        fm = new PresidentForwardModel();
    }

    @Test
    public void theScumsTwoGoesToThePresidentWithOneCardExchanged() {
        PresidentGameState state = newState(multiDealParams(11, 1), 4, 81, fm);
        giveHand(state, 1, "KD", "2S", "AH", "5C");
        giveHand(state, 3, "3H", "6D");

        fm.startExchange(state, 3, 1);

        // the Two (rank 15) is above the Ace (14)
        assertEquals(new HashSet<>(cards("KD", "AH", "5C")), setOf(state.getPlayerHand(1)));
        assertEquals(new HashSet<>(cards("3H", "6D", "2S")), setOf(state.getPlayerHand(3)));
        assertEquals(EXCHANGE, state.getGamePhase());
        assertEquals(1, state.getScum());
        assertEquals(1, state.getCardsToGive());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new GiveCard(card("3H")), new GiveCard(card("6D")), new GiveCard(card("2S"))),
                actionSet(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void equalRanksAreTakenInSuitOrderWithTwoCardsExchanged() {
        PresidentGameState state = newState(multiDealParams(11, 2), 4, 82, fm);
        giveHand(state, 2, "AS", "AC", "AH", "KD", "9D");
        giveHand(state, 0, "4C");

        fm.startExchange(state, 0, 2);

        // three Aces are the highest; in suit order Diamonds (none), Hearts, Clubs, Spades: AH and AC go
        assertEquals(new HashSet<>(cards("AS", "KD", "9D")), setOf(state.getPlayerHand(2)));
        assertEquals(new HashSet<>(cards("4C", "AH", "AC")), setOf(state.getPlayerHand(0)));
        assertEquals(EXCHANGE, state.getGamePhase());
        assertEquals(2, state.getScum());
        assertEquals(2, state.getCardsToGive());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aTwoAndTheFirstAceInSuitOrderGoWithTwoCardsExchanged() {
        PresidentGameState state = newState(multiDealParams(11, 2), 5, 83, fm);
        giveHand(state, 4, "2C", "AS", "AD", "QH");
        giveHand(state, 1, "5S", "7D");

        fm.startExchange(state, 1, 4);

        // the Two first, then of the two Aces the Diamond (Diamonds before Spades)
        assertEquals(new HashSet<>(cards("AS", "QH")), setOf(state.getPlayerHand(4)));
        assertEquals(new HashSet<>(cards("5S", "7D", "2C", "AD")), setOf(state.getPlayerHand(1)));
        assertAllCardsPresent(state);
    }

    @Test
    public void withNoCardsExchangedThePresidentLeadsStraightAway() {
        PresidentGameState state = newState(multiDealParams(11, 0), 4, 84, fm);
        giveHand(state, 1, "KD", "2S", "AH", "5C");
        giveHand(state, 3, "3H", "3D", "6D");

        fm.startExchange(state, 3, 1);

        assertEquals(new HashSet<>(cards("KD", "2S", "AH", "5C")), setOf(state.getPlayerHand(1)));
        assertEquals(new HashSet<>(cards("3H", "3D", "6D")), setOf(state.getPlayerHand(3)));
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(-1, state.getScum());
        assertEquals(0, state.getCardsToGive());
        assertEquals(3, state.getCurrentPlayer());
        // leading: any set held, no Pass
        assertEquals(Set.of(new PlayCards(3, 1), new PlayCards(3, 2), new PlayCards(6, 1)), actionSet(state, fm));
    }

    @Test
    public void givingTheOneCardReturnsToPlayWithThePresidentLeading() {
        // 4 players, exchangeCards 1: the President (3) has received the 2S and gives one card back to the Scum (1)
        PresidentGameState state = newState(multiDealParams(11, 1), 4, 85, fm);
        giveHand(state, 1, "KD", "AH", "5C");
        giveHand(state, 3, "3H", "6D", "2S");
        arrangeExchange(state, 3, 1, 1);

        // one GiveCard per card in the President's hand - no Pass, no PlayCards
        assertEquals(Set.of(new GiveCard(card("3H")), new GiveCard(card("6D")), new GiveCard(card("2S"))),
                actionSet(state, fm));

        fm.next(state, new GiveCard(card("3H")));

        assertEquals(new HashSet<>(cards("KD", "AH", "5C", "3H")), setOf(state.getPlayerHand(1)));
        assertEquals(new HashSet<>(cards("6D", "2S")), setOf(state.getPlayerHand(3)));
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(-1, state.getScum());
        assertEquals(0, state.getCardsToGive());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCards(6, 1), new PlayCards(2, 1)), actionSet(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void withTwoCardsExchangedThePresidentMakesTwoDecisionsThenLeads() {
        PresidentGameState state = newState(multiDealParams(11, 2), 5, 86, fm);
        giveHand(state, 4, "4D", "8H");
        giveHand(state, 0, "3H", "3D", "6D", "2S");
        arrangeExchange(state, 0, 4, 2);

        fm.next(state, new GiveCard(card("3H")));

        // one still to give: the same President chooses again from what is left
        assertEquals(new HashSet<>(cards("4D", "8H", "3H")), setOf(state.getPlayerHand(4)));
        assertEquals(EXCHANGE, state.getGamePhase());
        assertEquals(4, state.getScum());
        assertEquals(1, state.getCardsToGive());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new GiveCard(card("3D")), new GiveCard(card("6D")), new GiveCard(card("2S"))),
                actionSet(state, fm));

        fm.next(state, new GiveCard(card("6D")));

        assertEquals(new HashSet<>(cards("4D", "8H", "3H", "6D")), setOf(state.getPlayerHand(4)));
        assertEquals(new HashSet<>(cards("3D", "2S")), setOf(state.getPlayerHand(0)));
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(-1, state.getScum());
        assertEquals(0, state.getCardsToGive());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCards(3, 1), new PlayCards(2, 1)), actionSet(state, fm));
        assertAllCardsPresent(state);
    }
}

package games.pitch;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.pitch.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;
import games.tricktaking.CardOrder;
import games.tricktaking.Trick;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * Card play: the legal plays, setting trumps, and who wins a trick.
 */
public class PitchPlayRulesTest {

    PitchForwardModel fm;
    PitchGameState state;

    @Before
    public void setup() {
        fm = new PitchForwardModel();
        state = newState(21, fm);
    }

    private Set<AbstractAction> available() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    private static Set<AbstractAction> plays(String... codes) {
        Set<AbstractAction> set = new HashSet<>();
        for (FrenchCard c : cards(codes))
            set.add(new PlayCard(c));
        return set;
    }

    // ---------------------------------------------------------------- legal plays (arranged, unit)

    @Test
    public void thePitcherMayLeadAnyCard() {
        giveHand(state, 2, "5H", "KH", "3S", "9D", "AC", "2S");
        startPlay(state, 2, 3, null, 2);
        assertEquals(plays("5H", "KH", "3S", "9D", "AC", "2S"), available());
    }

    @Test
    public void aLaterLeaderMayLeadAnyCard() {
        // trumps are Spades from an earlier trick; leading need not be a trump or avoid one
        giveHand(state, 1, "5H", "3S", "9D");
        startPlay(state, 0, 2, Spades, 1);
        assertEquals(plays("5H", "3S", "9D"), available());
    }

    @Test
    public void aFollowerHoldingTheSuitLedMayPlayThatSuitOrATrump() {
        // trumps Spades, Hearts led: player 1 holds hearts, so hearts or spades only (not 9D, AC)
        giveHand(state, 1, "5H", "KH", "3S", "9D", "AC", "2S");
        startPlay(state, 0, 2, Spades, 0);
        arrangeTrick(state, 0, "QH");
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(plays("5H", "KH", "3S", "2S"), available());
    }

    @Test
    public void aFollowerHoldingTheSuitLedWithoutTrumpsMustFollow() {
        giveHand(state, 1, "5H", "KH", "9D", "AC");
        startPlay(state, 0, 2, Spades, 0);
        arrangeTrick(state, 0, "QH");
        assertEquals(plays("5H", "KH"), available());
    }

    @Test
    public void aFollowerWithoutTheSuitLedMayPlayAnyCard() {
        giveHand(state, 1, "3S", "9D", "AC");
        startPlay(state, 0, 2, Spades, 0);
        arrangeTrick(state, 0, "QH");
        assertEquals(plays("3S", "9D", "AC"), available());
    }

    @Test
    public void whenTrumpsAreLedAFollowerHoldingTrumpsMustPlayOne() {
        giveHand(state, 2, "5S", "KS", "AH", "2D");
        startPlay(state, 0, 2, Spades, 0);
        arrangeTrick(state, 0, "QS", "3H");
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(plays("5S", "KS"), available());
    }

    // ---------------------------------------------------------------- playing a card (fm.next)

    @Test
    public void theFirstCardOfTheDealSetsTrumps() {
        giveHand(state, 1, "7D", "KH", "3S", "9D", "AC", "2S");
        giveHand(state, 2, "5H", "6H", "4C", "8D", "10C", "JC");
        startPlay(state, 1, 2, null, 1);
        assertNull(state.getTrumpSuit());

        fm.next(state, new PlayCard(card("7D")));
        assertEquals(Diamonds, state.getTrumpSuit());
        assertEquals(cards("7D"), state.getCurrentTrick().getComponents());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertFalse(state.getPlayerHand(1).contains(card("7D")));
        assertEquals(5, state.getPlayerHand(1).getSize());
        assertEquals("play passes clockwise", 2, state.getCurrentPlayer());
        assertEquals(PitchGameState.Phase.PLAYING, state.getGamePhase());

        // the second card, of another suit, does not change trumps
        fm.next(state, new PlayCard(card("5H")));
        assertEquals(Diamonds, state.getTrumpSuit());
        assertEquals(cards("7D", "5H"), state.getCurrentTrick().getComponents());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(3, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void leadingALaterTrickDoesNotChangeTrumps() {
        // one trick already taken by team 0; trumps Spades; player 2 leads a heart
        setTeamTricks(state, 0, "2C", "3C", "4C", "5C");
        giveHand(state, 2, "5H", "6H", "4D", "8D", "10C");
        startPlay(state, 0, 2, Spades, 2);
        fm.next(state, new PlayCard(card("5H")));
        assertEquals(Spades, state.getTrumpSuit());
        assertEquals(cards("5H"), state.getCurrentTrick().getComponents());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(3, state.getCurrentPlayer());
    }

    @Test
    public void aLowTrumpWinsTheTrickForItsTeamAndLeadsNext() {
        // trumps Spades, player 1 led KH, player 2 AH, player 3 2S; player 0 plays 10D (no hearts, no spades).
        // The 2S is the only trump: player 3 wins, team 1 takes all four cards, player 3 leads.
        giveHand(state, 0, "10D", "4D");
        giveHand(state, 1, "7C");
        giveHand(state, 2, "8C");
        giveHand(state, 3, "9C");
        startPlay(state, 1, 2, Spades, 1);
        arrangeTrick(state, 1, "KH", "AH", "2S");
        assertAllCardsPresent(state);

        play(state, fm, 0, "10D");
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals("the winner leads the next trick", 3, state.getCurrentTrick().getLeader());
        assertEquals(Set.of(card("KH"), card("AH"), card("2S"), card("10D")), setOf(state.getTeamTricks(1)));
        assertEquals(0, state.getTeamTricks(0).getSize());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Spades, state.getTrumpSuit());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void withoutTrumpsTheHighestCardOfTheSuitLedWins() {
        // trumps Spades, player 3 led 5H, player 0 AC (off-suit, never wins), player 1 9H; player 2 plays KH.
        // KH is the highest heart: player 2 wins for team 0 and leads.
        giveHand(state, 0, "4D");
        giveHand(state, 1, "7C");
        giveHand(state, 2, "KH", "8C");
        giveHand(state, 3, "9C");
        startPlay(state, 1, 2, Spades, 3);
        arrangeTrick(state, 3, "5H", "AC", "9H");

        play(state, fm, 2, "KH");
        assertEquals(Set.of(card("5H"), card("AC"), card("9H"), card("KH")), setOf(state.getTeamTricks(0)));
        assertEquals(0, state.getTeamTricks(1).getSize());
        assertEquals(2, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    // ---------------------------------------------------------------- Trick.winner with Pitch's trumps

    private static Trick<FrenchCard, FrenchCard.Suite> trick(int leader, String... codes) {
        Trick<FrenchCard, FrenchCard.Suite> trick = new Trick<>("Trick", 4, leader, CardOrder.STANDARD);
        for (FrenchCard c : cards(codes))
            trick.play(c);
        return trick;
    }

    @Test
    public void trickWinner() {
        // the only trump beats higher cards of the suit led
        assertEquals(3, trick(1, "KH", "AH", "2S", "10D").winner(Spades));
        // the higher of two trumps wins
        assertEquals(3, trick(1, "5H", "3S", "JS", "AH").winner(Spades));
        // no trump: the highest heart; the off-suit Ace does not win
        assertEquals(0, trick(1, "5H", "AC", "9H", "KH").winner(Spades));
        // Aces are high
        assertEquals(2, trick(1, "KH", "AH", "2H", "3D").winner(Spades));
        // trumps led: the highest trump, the lead here
        assertEquals(1, trick(1, "4S", "AH", "2S", "3S").winner(Spades));
        // the leader wins when nobody follows or trumps
        assertEquals(2, trick(2, "4D", "AH", "KC", "QH").winner(Spades));
    }
}

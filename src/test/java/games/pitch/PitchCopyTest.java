package games.pitch;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.FrenchCard.Suite.Hearts;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * Full and per-player copies of a mid-deal position. Player 0's copy may reshuffle the other hands and the undealt
 * deck.
 */
public class PitchCopyTest {

    PitchForwardModel fm;
    PitchGameState state;

    @Before
    public void setup() {
        // mid-deal: player 0 bid 2 and player 1 pitched 3; trumps are Hearts. Team 0 has won one trick, two cards
        // are in the current trick, and neither team has scored yet
        fm = new PitchForwardModel();
        state = newState(41, fm);
        setTeamTricks(state, 0, "3H", "2C", "KC", "9D");
        giveHand(state, 0, "4H", "5C", "6C", "7C", "8C");
        giveHand(state, 1, "AH", "2D", "3D", "4D", "5D");
        giveHand(state, 2, "5H", "2S", "3S", "4S");
        giveHand(state, 3, "6H", "5S", "6S", "7S");
        startPlay(state, 1, 3, Hearts, 0);
        state.playerBids[0] = 2;
        arrangeTrick(state, 2, "QH", "JS");
        assertAllCardsPresent(state);
    }

    @Test
    public void aFullCopyIsEqualAndIndependent() {
        PitchGameState copy = (PitchGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // change the copy: play a card from player 0's hand to its trick, record a score and a bid
        FrenchCard c = card("4H");
        copy.getPlayerHand(0).remove(c);
        copy.currentTrick.play(c);
        copy.teamScores[1] = 5;
        copy.playerBids[2] = 4;
        copy.teamTricks.get(1).add(card("2H"));
        assertNotEquals(state, copy);

        assertTrue(state.getPlayerHand(0).contains(c));
        assertEquals(2, state.getCurrentTrick().getSize());
        assertEquals(0, state.getTeamScore(1));
        assertEquals(0, state.getPlayerBid(2));
        assertEquals(0, state.getTeamTricks(1).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayersCopyKeepsWhatThePlayerCanSee() {
        Set<FrenchCard> hiddenFromZero = new HashSet<>();
        for (int p = 1; p < 4; p++)
            hiddenFromZero.addAll(state.getPlayerHand(p).getComponents());
        hiddenFromZero.addAll(state.getUndealtDeck().getComponents());

        int changed = 0;
        for (int i = 0; i < 10; i++) {
            PitchGameState copy = (PitchGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            for (int p = 1; p < 4; p++)
                assertEquals(state.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
            assertEquals(state.getUndealtDeck().getSize(), copy.getUndealtDeck().getSize());
            assertEquals(state.getCurrentTrick(), copy.getCurrentTrick());
            assertEquals(state.getTeamTricks(0).getComponents(), copy.getTeamTricks(0).getComponents());
            assertEquals(state.getTeamTricks(1).getComponents(), copy.getTeamTricks(1).getComponents());
            assertEquals(Hearts, copy.getTrumpSuit());
            assertEquals(1, copy.getPitcher());
            assertArrayEquals(state.playerBids, copy.playerBids);
            assertAllCardsPresent(copy);

            // the hidden cards are the same cards, shared out afresh
            Set<FrenchCard> hidden = new HashSet<>();
            for (int p = 1; p < 4; p++)
                hidden.addAll(copy.getPlayerHand(p).getComponents());
            hidden.addAll(copy.getUndealtDeck().getComponents());
            assertEquals(hiddenFromZero, hidden);
            // without the reshuffle every copy would give player 1 the same cards
            if (!setOf(copy.getPlayerHand(1)).equals(setOf(state.getPlayerHand(1))))
                changed++;
        }
        assertTrue("player 1's hand never changed in 10 copies", changed > 0);
    }

    @Test
    public void aPlayersCopyDoesNotChangeTheOriginal() {
        List<FrenchCard> hand1 = List.copyOf(state.getPlayerHand(1).getComponents());
        List<FrenchCard> undealt = List.copyOf(state.getUndealtDeck().getComponents());
        state.copy(0);
        assertEquals(hand1, state.getPlayerHand(1).getComponents());
        assertEquals(undealt, state.getUndealtDeck().getComponents());
    }
}

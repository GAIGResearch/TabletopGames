package games.leducpoker;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.leducpoker.LeducPokerTestUtils.*;
import static org.junit.Assert.*;

public class LeducPokerCopyTest {

    LeducPokerParameters params;
    LeducPokerGameState state;
    LeducPokerForwardModel fm;

    @Before
    public void setup() {
        params = new LeducPokerParameters();
        params.setRandomSeed(42);
        state = new LeducPokerGameState(params, 2);
        fm = new LeducPokerForwardModel();
        fm.setup(state);
        // A second-round position: P0 JS, P1 KS, board QH; chips and counters set away from their defaults
        arrangeWithBoard(state, card("JS"), card("KS"), card("QH"));
        state.contributions[0] = 9;
        state.contributions[1] = 5;
        // as after earlier hands of a match, so a copy that loses netChips is seen
        state.netChips[0] = 4;
        state.netChips[1] = -4;
        state.raisesThisRound = 1;
        state.actionsThisRound = 1;
    }

    @Test
    public void faithfulCopyEqualsAndHashesAsTheOriginal() {
        LeducPokerGameState copy = (LeducPokerGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(state.getHand(1).getComponents(), copy.getHand(1).getComponents());
        assertEquals(state.getDrawDeck().getComponents(), copy.getDrawDeck().getComponents());
    }

    @Test
    public void changingTheCopyLeavesTheOriginalAlone() {
        LeducPokerGameState copy = (LeducPokerGameState) state.copy();
        copy.contributions[1] = 13;
        copy.raisesThisRound = 2;
        copy.drawDeck.add(copy.board.draw());
        assertNotEquals(state, copy);
        assertEquals(5, state.getContribution(1));
        assertEquals(1, state.getRaisesThisRound());
        assertEquals(card("QH"), state.getBoard().get(0));
        assertEquals(3, state.getDrawDeck().getSize());
    }

    @Test
    public void eachFieldTakesPartInEquality() {
        LeducPokerGameState copy = (LeducPokerGameState) state.copy();
        copy.netChips[0] = 1;
        assertNotEquals(state, copy);
        copy = (LeducPokerGameState) state.copy();
        copy.actionsThisRound = 0;
        assertNotEquals(state, copy);
        copy = (LeducPokerGameState) state.copy();
        copy.contributions[0] = 8;
        assertNotEquals(state, copy);
    }

    @Test
    public void copyForAPlayerRedeterminisesOnlyTheOpponentsCardAndTheDrawDeck() {
        // Player 0 sees JS and the board QH; unseen: QS, KS, JH, KH
        checkRedeterminisation(0, card("JS"), Set.of(card("QS"), card("KS"), card("JH"), card("KH")));
        // Player 1 sees KS and the board QH; unseen: JS, QS, JH, KH
        checkRedeterminisation(1, card("KS"), Set.of(card("JS"), card("QS"), card("JH"), card("KH")));
    }

    @Test
    public void inTheFirstRoundTheOpponentsCardComesFromTheFiveUnseenCards() {
        arrange(state, card("JS"), card("KS"));
        state.contributions[0] = 3;
        state.contributions[1] = 1;
        state.raisesThisRound = 1;
        state.actionsThisRound = 1;
        checkRedeterminisation(0, card("JS"),
                Set.of(card("QS"), card("KS"), card("JH"), card("QH"), card("KH")));
    }

    private void checkRedeterminisation(int p, FrenchCard own, Set<FrenchCard> unseen) {
        int opp = 1 - p;
        List<FrenchCard> board = state.getBoard().getComponents();
        Set<FrenchCard> opponentCards = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            LeducPokerGameState c = (LeducPokerGameState) state.copy(p);
            assertEquals(List.of(own), c.getHand(p).getComponents());
            assertEquals(board, c.getBoard().getComponents());
            assertEquals(1, c.getHand(opp).getSize());
            assertEquals(state.getDrawDeck().getSize(), c.getDrawDeck().getSize());
            for (int q = 0; q < 2; q++) {
                assertEquals(state.getContribution(q), c.getContribution(q));
                assertEquals(state.getNetChips(q), c.getNetChips(q));
            }
            assertEquals(state.getRaisesThisRound(), c.getRaisesThisRound());
            assertEquals(state.getActionsThisRound(), c.getActionsThisRound());
            assertAllCardsPresent(c);
            opponentCards.add(c.getHand(opp).get(0));
        }
        // never the observer's own card or the board card, and every unseen card turns up
        assertEquals(unseen, opponentCards);
    }
}

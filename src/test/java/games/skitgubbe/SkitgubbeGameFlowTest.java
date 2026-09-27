package games.skitgubbe;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.skitgubbe.actions.PlayCard;
import games.skitgubbe.actions.TurnUpCard;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/** Phase one in real games from the factory, driven only by fm.next. */
public class SkitgubbeGameFlowTest {

    @Test
    public void scriptedOpeningTricksWithAWinABounceAndAWinCollectingTheHeldCards() {
        Game game = newGame(3, 11);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        giveHand(state, 0, "5H", "2S", "3S");
        giveHand(state, 1, "9C", "7D", "3H");
        giveHand(state, 2, "7S", "2C", "4S");
        assertEquals(43, state.drawDeck.getSize());

        fm.next(state, new PlayCard(card("5H")));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("9C")));     // 9 > 5: player 1 wins and leads to player 2
        assertEquals(setOf("5H", "9C"), asSet(state.collectedCards.get(1)));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("7D")));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("7S")));     // 7 = 7: bounce, player 1 leads again
        assertEquals(List.of(card("7D")), state.heldCards.get(1).getComponents());
        assertEquals(List.of(card("7S")), state.heldCards.get(2).getComponents());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("3H")));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("2C")));     // 3 > 2: player 1 wins the trick and the held cards

        // 2 from the first trick + 2 held + 2 from this trick = 6
        assertEquals(setOf("5H", "9C", "7D", "7S", "3H", "2C"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.heldCards.get(1).getSize());
        assertEquals(0, state.heldCards.get(2).getSize());
        assertEquals(1, state.getCurrentPlayer());
        for (int p = 0; p < 3; p++)
            assertEquals("hand of " + p, 3, state.playerHands.get(p).getSize());
        assertEquals(37, state.drawDeck.getSize());   // 43 - 6 plays, each followed by a draw
        assertEquals(SkitgubbeGameState.Phase.PHASE_ONE, state.getGamePhase());
        assertAllCardsPresent(state);
    }

    @Test
    public void withFourPlayersTheLeadPassesRoundTheTableAndWrapsToPlayerZero() {
        Game game = newGame(4, 12);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        giveHand(state, 0, "2H", "2S", "3S");
        giveHand(state, 1, "5H", "4C", "3C");
        giveHand(state, 2, "9H", "6D", "3D");
        giveHand(state, 3, "JH", "5S", "4D");

        fm.next(state, new PlayCard(card("2H")));
        fm.next(state, new PlayCard(card("5H")));     // 5 > 2: player 1 wins
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("4C")));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("9H")));     // 9 > 4: player 2 wins
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("6D")));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("JH")));     // Jack 11 > 6: player 3 wins
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("5S")));
        assertEquals(0, state.getCurrentPlayer());    // player 3's follower is (3 + 1) % 4 = 0

        assertEquals(setOf("2H", "5H"), asSet(state.collectedCards.get(1)));
        assertEquals(setOf("4C", "9H"), asSet(state.collectedCards.get(2)));
        assertEquals(setOf("6D", "JH"), asSet(state.collectedCards.get(3)));
        assertEquals(List.of(card("5S")), state.trick.getComponents());
        assertAllCardsPresent(state);
    }

    @Test
    public void scriptedTurnUpsByLeaderAndFollowerResolveAsPlaysFromHand() {
        Game game = newGame(3, 13);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        giveHand(state, 0, "2H", "2S", "3S");
        giveHand(state, 1, "5H", "4C", "3C");
        giveHand(state, 2, "6D", "3D", "4D");
        stackDrawDeck(state, "9D", "10C", "JS", "8H");
        assertEquals(43, state.drawDeck.getSize());   // 52 - 3 * 3

        assertTrue(fm.computeAvailableActions(state).contains(new TurnUpCard()));
        fm.next(state, new TurnUpCard());             // player 0 turns up 9D
        assertEquals(List.of(card("9D")), state.trick.getComponents());
        assertEquals(setOf("2H", "2S", "3S"), asSet(state.playerHands.get(0)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(4, fm.computeAvailableActions(state).size());   // 3 hand cards + turn-up

        fm.next(state, new PlayCard(card("5H")));     // player 1 draws 10C; 9 > 5: player 0 wins, leads again
        assertEquals(setOf("9D", "5H"), asSet(state.collectedCards.get(0)));
        assertEquals(setOf("4C", "3C", "10C"), asSet(state.playerHands.get(1)));
        assertEquals(0, state.getCurrentPlayer());

        fm.next(state, new PlayCard(card("2H")));     // player 0 draws JS
        assertEquals(setOf("2S", "3S", "JS"), asSet(state.playerHands.get(0)));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new TurnUpCard());             // player 1 turns up 8H; 8 > 2: player 1 wins, leads to 2

        assertEquals(setOf("2H", "8H"), asSet(state.collectedCards.get(1)));
        assertEquals(setOf("4C", "3C", "10C"), asSet(state.playerHands.get(1)));   // unchanged by the turn-up
        assertEquals(setOf("9D", "5H"), asSet(state.collectedCards.get(0)));
        assertEquals(1, state.getCurrentPlayer());
        for (int p = 0; p < 3; p++)
            assertEquals("hand of " + p, 3, state.playerHands.get(p).getSize());
        assertEquals(39, state.drawDeck.getSize());   // 43 - 2 turned up - 2 drawn after plays from hand
        assertEquals(SkitgubbeGameState.Phase.PHASE_ONE, state.getGamePhase());
        assertAllCardsPresent(state);
    }

    private void randomPhaseOne(int nPlayers, int handSize, long seed) {
        SkitgubbeParameters params = SkitgubbeTestUtils.valetParams();
        params.setParameterValue("handSize", handSize);
        Game game = newGame(nPlayers, seed, params);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        Random rnd = new Random(seed);
        FrenchCard trump = null;
        int trumpDrawer = -1;
        int steps = 0, turnUps = 0;
        while (state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE && steps++ < 200) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no actions in phase one at step " + steps, actions.isEmpty());
            assertEquals("TurnUpCard offered iff the draw deck holds 2 or more", state.drawDeck.getSize() >= 2,
                    actions.contains(new TurnUpCard()));
            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
            if (chosen instanceof TurnUpCard) turnUps++;
            fm.next(state, chosen);
            assertAllCardsPresent(state);
            for (int p = 0; p < nPlayers; p++) {
                assertTrue("hand of " + p + " above handSize", state.playerHands.get(p).getSize() <= handSize);
                // until the trump card is drawn every hand is full, after a turn-up as after a play from hand
                if (state.drawDeck.getSize() > 0)
                    assertEquals("hand of " + p + " at step " + steps, handSize, state.playerHands.get(p).getSize());
            }
            if (trump == null && state.trumpCard.getSize() == 1) {
                trump = state.trumpCard.peek();
                trumpDrawer = state.trumpPlayer;
            }
        }
        assertEquals("phase one did not end within 200 actions", SkitgubbeGameState.Phase.PHASE_TWO, state.getGamePhase());
        assertTrue("no turn-up was played (seed " + seed + ")", turnUps > 0);
        assertNotNull("the trump card was never drawn", trump);

        int collected = 0, holders = 0;
        for (Deck<FrenchCard> pile : state.collectedCards) {
            collected += pile.getSize();
            if (pile.getSize() > 0) holders++;
        }
        assertEquals(52, collected);
        assertEquals(trump.suite, state.getTrumpSuit());
        assertEquals(trumpDrawer, state.getTrumpPlayer());
        assertTrue(state.collectedCards.get(trumpDrawer).contains(trump));
        assertEquals(trumpDrawer, state.getCurrentPlayer());
        assertEquals(holders, state.getTrickSize());
        for (int p = 0; p < nPlayers; p++)
            assertEquals("exit score of " + p, state.collectedCards.get(p).getSize() == 0 ? holders : 0, state.getExitScore(p));
    }

    @Test
    public void randomPhaseOnesWithThreePlayersReachPhaseTwoConservingTheCards() {
        for (long seed = 1; seed <= 5; seed++) randomPhaseOne(3, 3, seed);
    }

    @Test
    public void randomPhaseOnesWithFourPlayersReachPhaseTwoConservingTheCards() {
        for (long seed = 1; seed <= 5; seed++) randomPhaseOne(4, 3, seed);
    }

    @Test
    public void randomPhaseOnesWithHandSizeTwoReachPhaseTwoConservingTheCards() {
        for (long seed = 1; seed <= 5; seed++) randomPhaseOne(3, 2, seed);
    }
}

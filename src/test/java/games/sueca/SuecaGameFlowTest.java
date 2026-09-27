package games.sueca;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult.*;
import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * A real game driven by fm.next: a scripted opening, and seeded random deals played to the end against oracles
 * written from the rules.
 */
public class SuecaGameFlowTest {

    private static Set<AbstractAction> plays(String... codes) {
        return Arrays.stream(codes).map(c -> (AbstractAction) new PlayCard<>(card(c))).collect(Collectors.toSet());
    }

    private static Set<AbstractAction> plays(List<FrenchCard> cards) {
        return cards.stream().map(c -> (AbstractAction) new PlayCard<>(c)).collect(Collectors.toSet());
    }

    @Test
    public void theFirstPlayerMayLeadAnyCardOfTheDealtHand() {
        Game game = newGame(21);
        SuecaGameState state = (SuecaGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays(cardsOf(state.getPlayerHand(0))), new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals(10, fm.computeAvailableActions(state).size());
    }

    @Test
    public void scriptedOpeningThreeTricks() {
        Game game = newGame(4);
        SuecaGameState state = (SuecaGameState) game.getGameState();
        SuecaForwardModel fm = (SuecaForwardModel) game.getForwardModel();
        arrangeDeal(state);                                   // Hearts trumps (2H with player 3, the dealer)
        assertEquals(0, state.getCurrentPlayer());

        // trick 1, led by player 0; play passes 0 -> 1 -> 2 -> 3
        playCards(state, fm, "AS");
        assertEquals(1, state.getCurrentPlayer());
        // player 1 holds 7S 3S JS: must follow spades although holding the trumps AH 4H
        assertEquals(plays("7S", "3S", "JS"), new HashSet<>(fm.computeAvailableActions(state)));
        playCards(state, fm, "3S");
        assertEquals(2, state.getCurrentPlayer());
        playCards(state, fm, "KS");
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(cards("AS", "3S", "KS"), cardsOf(state.getCurrentTrick()));
        playCards(state, fm, "5S");
        // the Ace of Spades wins: team 0 takes A 11 + 3 0 + K 4 + 5 0 = 15
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(new HashSet<>(cards("AS", "3S", "KS", "5S")), new HashSet<>(cardsOf(state.getTeamPile(0))));
        assertEquals(15, state.getCardPoints(0));
        assertEquals(0, state.getTeamPile(1).getSize());
        assertEquals(1, state.getTricksWon(0));
        assertAllCardsPresent(state);

        // trick 2: KD, QD, JD, then player 3's Ace of Diamonds wins for team 1: K 4 + Q 2 + J 3 + A 11 = 20
        playCards(state, fm, "KD", "QD", "JD", "AD");
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getCurrentTrick().getLeader());
        assertEquals(20, state.getCardPoints(1));
        assertEquals(15, state.getCardPoints(0));
        assertEquals(1, state.getTricksWon(1));

        // trick 3 led by player 3, then round the table 0 -> 1 -> 2
        playCards(state, fm, "7C");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("JC", "5C"), new HashSet<>(fm.computeAvailableActions(state)));
        playCards(state, fm, "JC");
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, "2C");
        assertEquals(2, state.getCurrentPlayer());
        playCards(state, fm, "AC");
        // the Ace beats the 7 led: player 2 wins for team 0: 7 10 + J 3 + 2 0 + A 11 = 24; team 0 15 + 24 = 39
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(39, state.getCardPoints(0));
        assertEquals(20, state.getCardPoints(1));
        assertEquals(2, state.getTricksWon(0));
        for (int p = 0; p < 4; p++)
            assertEquals(7, state.getPlayerHand(p).getSize());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomDealsPlayToTheEndInFortyActionsFollowingTheRules() {
        for (long seed = 1; seed <= 10; seed++) {
            Game game = newGame(seed);
            SuecaGameState state = (SuecaGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            FrenchCard.Suite trumps = state.getTrumpSuit();
            Random rnd = new Random(seed);
            int[] expectedPoints = new int[2];
            int actions = 0;
            while (state.isNotTerminal() && actions < 100) {
                int player = state.getCurrentPlayer();
                List<FrenchCard> trick = cardsOf(state.getCurrentTrick());
                int leader = state.getCurrentTrick().getLeader();
                assertEquals("seed " + seed + ": turn passes round from the leader", (leader + trick.size()) % 4, player);
                List<AbstractAction> available = fm.computeAvailableActions(state);
                assertEquals("seed " + seed + ": legal plays",
                        plays(expectedLegalPlays(cardsOf(state.getPlayerHand(player)), trick)), new HashSet<>(available));
                double h = state.getHeuristicScore(player);
                assertTrue("heuristic " + h, h >= 0 && h <= 1);

                @SuppressWarnings("unchecked")
                PlayCard<FrenchCard> action = (PlayCard<FrenchCard>) available.get(rnd.nextInt(available.size()));
                fm.next(state, action);
                actions++;
                assertAllCardsPresent(state);

                if (trick.size() == 3) {
                    trick.add(action.card);
                    int winner = (leader + expectedWinningIndex(trick, trumps)) % 4;
                    expectedPoints[winner % 2] += expectedCardPoints(trick);
                    assertEquals(0, state.getCurrentTrick().getSize());
                    assertEquals("seed " + seed + ": trick winner", winner, state.getCurrentTrick().getLeader());
                    // after the last trick the game is over, and the turn does not pass to the winner
                    if (state.isNotTerminal())
                        assertEquals("seed " + seed + ": trick winner to play", winner, state.getCurrentPlayer());
                    assertEquals(expectedPoints[0], state.getCardPoints(0));
                    assertEquals(expectedPoints[1], state.getCardPoints(1));
                }
            }
            assertFalse("seed " + seed + ": the deal did not end", state.isNotTerminal());
            assertEquals("seed " + seed + ": actions", 40, actions);
            assertEquals(120, expectedPoints[0] + expectedPoints[1]);
            assertEquals(40, state.getTeamPile(0).getSize() + state.getTeamPile(1).getSize());
            for (int p = 0; p < 4; p++)
                assertEquals(expectedPoints[p % 2], state.getGameScore(p), 0.0);
            GameResult team0 = expectedPoints[0] > 60 ? WIN_GAME : expectedPoints[0] == 60 ? DRAW_GAME : LOSE_GAME;
            GameResult team1 = expectedPoints[1] > 60 ? WIN_GAME : expectedPoints[1] == 60 ? DRAW_GAME : LOSE_GAME;
            assertArrayEquals("seed " + seed, new GameResult[]{team0, team1, team0, team1}, state.getPlayerResults());
        }
    }
}

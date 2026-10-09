package games.scopa;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.TarotCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.components.TarotCard.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * A real game driven by fm.next: turns alternate, new hands are dealt, the deal ends with the table going to the
 * last capturer, and a whole deal lasts 36 plays.
 */
public class ScopaGameFlowTest {

    @Test
    public void playersAlternateAndNewHandsAreDealtWhenBothHandsAreEmpty() {
        Game game = newGame(21);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrange(state, 0, of(sword(2), baton(KING), sword(7)), of(sword(5), baton(3), cup(1)),
                coin(7), cup(5), cup(CAVALIER), baton(KNAVE));
        assertEquals(30, state.getDrawDeck().getSize());

        // 1. P0 2 of Swords: no 2 on the table, no two cards sum to 2 -> table
        play(state, fm, 0, toTable(sword(2)));
        assertEquals(setOf(coin(7), cup(5), cup(CAVALIER), baton(KNAVE), sword(2)), setOf(state.getTable()));

        // 2. P1: the 5 of Swords must take the 5 of Cups; the 3 and Ace capture nothing (no 3 or Ace; no sum of
        //    two cards of {7, 5, 9, 8, 2} is 3 or 1)
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(capture(sword(5), cup(5)), toTable(baton(3)), toTable(cup(1))),
                actionSet(fm, state));
        play(state, fm, 1, capture(sword(5), cup(5)));
        assertEquals(1, state.getLastCapturer());

        // 3. P0 King (10): from {7, 9, 8, 2} only Knave 8 + 2 = 10 (7+2 = 9, triples at least 2+7+8 = 17)
        play(state, fm, 0, capture(baton(KING), baton(KNAVE), sword(2)));
        assertEquals(0, state.getLastCapturer());
        assertEquals(setOf(coin(7), cup(CAVALIER)), setOf(state.getTable()));

        // 4. P1 3 of Batons: nothing in {7, 9} -> table
        play(state, fm, 1, toTable(baton(3)));

        // 5. P0 7 of Swords takes the 7 of Coins
        play(state, fm, 0, capture(sword(7), coin(7)));
        assertEquals(setOf(cup(CAVALIER), baton(3)), setOf(state.getTable()));

        // the next six cards of the draw deck: player 0 (after the dealer, player 1) gets the first three
        List<TarotCard> next6 = new ArrayList<>(state.getDrawDeck().getComponents().subList(0, 6));

        // 6. P1 Ace of Cups: nothing in {9, 3} sums to 1 -> table; both hands are now empty -> 3 new cards each
        play(state, fm, 1, toTable(cup(1)));

        assertEquals(setOf(cup(CAVALIER), baton(3), cup(1)), setOf(state.getTable()));   // no cards added
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(3, state.getPlayerHand(1).getSize());
        assertEquals(new HashSet<>(next6.subList(0, 3)), setOf(state.getPlayerHand(0)));
        assertEquals(new HashSet<>(next6.subList(3, 6)), setOf(state.getPlayerHand(1)));
        assertEquals(24, state.getDrawDeck().getSize());   // 30 - 6
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getLastCapturer());
        // P0 captured K + Knave + 2 and 7 + 7; P1 5 + 5; no scopas
        assertEquals(setOf(baton(KING), baton(KNAVE), sword(2), sword(7), coin(7)), setOf(state.getCapturedCards(0)));
        assertEquals(setOf(sword(5), cup(5)), setOf(state.getCapturedCards(1)));
        assertEquals(0, state.getScopas(0));
        assertEquals(0, state.getScopas(1));
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void atTheEndOfTheDealTheTableGoesToTheLastCapturerAndTheGameEnds() {
        Game game = newGame(22);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrange(state, 0, of(sword(7)), of(coin(1)), coin(7), cup(3), baton(KING));
        emptyDrawDeck(state, 1);   // 40 - 1 - 1 - 3 = 35 cards in player 1's pile
        state.lastCapturer = 1;
        assertEquals(35, state.getCapturedCards(1).getSize());

        play(state, fm, 0, capture(sword(7), coin(7)));
        assertTrue(state.isNotTerminal());
        // player 1's Ace captures nothing and is the last card of the deal: the table goes to player 0, who captured
        // last, not to player 1, who played last
        play(state, fm, 1, toTable(coin(1)));

        assertEquals(0, state.getTable().getSize());
        assertEquals(setOf(sword(7), coin(7), cup(3), baton(KING), coin(1)), setOf(state.getCapturedCards(0)));
        assertEquals(35, state.getCapturedCards(1).getSize());
        // taking the table at the end is not a scopa
        assertEquals(0, state.getScopas(0));
        assertEquals(0, state.getScopas(1));
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertFalse(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void withNoCaptureAllDealTheTableStaysAsItIs() {
        Game game = newGame(23);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrange(state, 0, of(coin(1)), of(cup(2)), baton(KING), cup(CAVALIER));
        emptyDrawDeck(state, 0);
        assertEquals(-1, state.getLastCapturer());

        play(state, fm, 0, toTable(coin(1)));   // nothing in {10, 9} sums to 1
        play(state, fm, 1, toTable(cup(2)));    // no 2; {10, 9, 1}: no pair sums to 2

        assertEquals(setOf(baton(KING), cup(CAVALIER), coin(1), cup(2)), setOf(state.getTable()));
        assertEquals(0, state.getCapturedCards(1).getSize());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomDealsLastThirtySixPlaysAndEndWithEveryCardCaptured() {
        int totalCaptures = 0;
        for (long seed = 1; seed <= 6; seed++) {
            Game game = newGame(seed);
            ScopaGameState state = (ScopaGameState) game.getGameState();
            ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
            Random rnd = new Random(seed);
            int[] expectedScopas = new int[2];
            int expectedLastCapturer = -1;
            int plays = 0;
            while (state.isNotTerminal() && plays < 100) {
                int p = state.getCurrentPlayer();
                assertEquals("seed " + seed + " play " + plays + ": players alternate", plays % 2, p);
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse("seed " + seed + " play " + plays + ": no actions", actions.isEmpty());
                for (AbstractAction a : actions)
                    checkLegal(state, (PlayCard) a);
                PlayCard action = (PlayCard) actions.get(rnd.nextInt(actions.size()));
                fm.next(state, action);
                plays++;
                if (action.isCapture()) {
                    totalCaptures++;
                    expectedLastCapturer = p;
                    // a sweep scores unless it is the 36th (last) play of the deal
                    if (state.getTable().getSize() == 0 && plays < 36)
                        expectedScopas[p]++;
                }
                assertAllCardsPresent(state);
                assertEquals(expectedLastCapturer, state.getLastCapturer());
                assertEquals(expectedScopas[0], state.getScopas(0));
                assertEquals(expectedScopas[1], state.getScopas(1));
                if (plays % 6 == 0 && plays < 36) {
                    // new hands: 30 - plays cards left in the draw deck
                    assertEquals(3, state.getPlayerHand(0).getSize());
                    assertEquals(3, state.getPlayerHand(1).getSize());
                    assertEquals(30 - plays, state.getDrawDeck().getSize());
                }
            }
            // 4 table cards + 36 dealt; one card played per action
            assertEquals("seed " + seed, 36, plays);
            assertFalse(state.isNotTerminal());
            assertNotEquals(-1, state.getLastCapturer());
            assertEquals(0, state.getTable().getSize());
            assertEquals(40, state.getCapturedCards(0).getSize() + state.getCapturedCards(1).getSize());
        }
        assertTrue("no captures made", totalCaptures > 0);
    }

    private static void checkLegal(ScopaGameState state, PlayCard a) {
        assertTrue(a + " not in hand", state.getPlayerHand(state.getCurrentPlayer()).contains(a.card));
        List<TarotCard> table = state.getTable().getComponents();
        boolean rankOnTable = table.stream().anyMatch(c -> c.number == a.card.number);
        if (!a.isCapture()) {
            // compulsory capture: at the least, no same-rank card may be on the table
            assertFalse(a + " offered although a same-rank card is on the table", rankOnTable);
            return;
        }
        assertTrue(a + " captures cards not on the table", table.containsAll(a.captured));
        if (a.captured.size() == 1) {
            assertEquals(a + ": a single capture must be of the same rank", a.card.number, a.captured.get(0).number);
        } else {
            assertFalse(a + ": a sum capture although a same-rank card is on the table", rankOnTable);
            int sum = a.captured.stream().mapToInt(c -> switch (c.number) {
                case KNAVE -> 8;
                case CAVALIER -> 9;
                case KING -> 10;
                default -> c.number;
            }).sum();
            int value = switch (a.card.number) {
                case KNAVE -> 8;
                case CAVALIER -> 9;
                case KING -> 10;
                default -> a.card.number;
            };
            assertEquals(a + ": sum of capture values", value, sum);
        }
    }
}

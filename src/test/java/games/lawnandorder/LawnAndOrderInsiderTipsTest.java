package games.lawnandorder;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.lawnandorder.components.RuleCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static org.junit.Assert.*;

/**
 * Insider Tips: who sees them, that they stay out of play during a round, the new tips each round, and what a copy
 * for a player keeps. Tip i lies between players i and (i + 1) mod n.
 */
public class LawnAndOrderInsiderTipsTest {

    /** Each tip is seen by exactly the two players beside it. */
    private static void assertTipsSeenByNeighbours(LawnAndOrderGameState state) {
        int n = state.getNPlayers();
        assertEquals(n, state.insiderTips.getSize());
        for (int i = 0; i < n; i++)
            for (int p = 0; p < n; p++)
                assertEquals("tip " + i + " seen by player " + p + " (" + n + " players)",
                        p == i || p == (i + 1) % n, state.insiderTips.getVisibilityForPlayer(i, p));
    }

    /** The 16 Rule cards less the named ones, in a fixed order (for an arranged agenda). */
    private static List<RuleCard> allRuleCardsExcept(RuleCard... excluded) {
        List<RuleCard> rest = new ArrayList<>(allRuleCards());
        for (RuleCard r : excluded)
            assertTrue(rest.remove(r));
        return rest;
    }

    // ---------------------------------------------------------------- visibility

    @Test
    public void withTwoPlayersBothPlayersSeeBothTips() {
        // tip 0 lies between 0 and 1, tip 1 between 1 and 0
        LawnAndOrderGameState state = newState(2, 3);
        assertTipsSeenByNeighbours(state);
        for (int i = 0; i < 2; i++)
            for (int p = 0; p < 2; p++)
                assertTrue(state.insiderTips.getVisibilityForPlayer(i, p));
    }

    @Test
    public void withThreePlayersEachTipIsSeenByItsTwoNeighboursOnly() {
        LawnAndOrderGameState state = newState(3, 3);
        assertTipsSeenByNeighbours(state);
        // player 0 sees tips 0 (between 0 and 1) and 2 (between 2 and 0), not tip 1 (between 1 and 2)
        assertTrue(state.insiderTips.getVisibilityForPlayer(0, 0));
        assertFalse(state.insiderTips.getVisibilityForPlayer(1, 0));
        assertTrue(state.insiderTips.getVisibilityForPlayer(2, 0));
    }

    @Test
    public void withSixPlayersEachTipIsSeenByItsTwoNeighboursOnly() {
        LawnAndOrderGameState state = newState(6, 3);
        assertTipsSeenByNeighbours(state);
        // tip 5 lies between players 5 and 0
        assertTrue(state.insiderTips.getVisibilityForPlayer(5, 0));
        assertTrue(state.insiderTips.getVisibilityForPlayer(5, 5));
        assertFalse(state.insiderTips.getVisibilityForPlayer(5, 3));
    }

    @Test
    public void aNewRoundDealsNewTipsSeenByTheNeighbours() {
        Game game = newGame(3, 11);
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        setAgendaTop(state, rule(REPURPOSED));
        // one card each (at most 1 citation on 1 card: safe), then everyone passes -> the round ends
        playTurn(fm, state, state.getHand(0).get(0), state.getHand(1).get(0), state.getHand(2).get(0));
        everyonePasses(fm, state);

        assertEquals(1, state.getRoundCounter());
        assertTipsSeenByNeighbours(state);
        // all 16 Rule cards are back: 3 tips, 16 - 3 = 13 in the agenda, none revealed
        assertEquals(13, state.agenda.getSize());
        assertEquals(0, state.revealedRules.getSize());
        assertAllCardsPresent(state);
    }

    // ---------------------------------------------------------------- never revealed

    @Test
    public void tipsStayOutOfTheAgendaAndAreNeverRevealedDuringARound() {
        Game game = newGame(3, 7);
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        RuleCard t0 = rule(PINK), t1 = rule(OVERSIZED), t2 = rule(FURNITURE);
        setRules(state, allRuleCardsExcept(t0, t1, t2), List.of(t0, t1, t2), List.of());
        List<RuleCard> tips = List.of(t0, t1, t2);

        Random rnd = new Random(3);
        int steps = 0;
        while (state.getRoundCounter() == 0) {
            assertTrue("the round did not end", ++steps < 1000);
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
            if (state.getRoundCounter() != 0) break;
            assertEquals(tips, state.insiderTips.getComponents());
            for (RuleCard t : tips) {
                assertFalse(t + " revealed", state.revealedRules.contains(t));
                assertFalse(t + " in the agenda", state.agenda.contains(t));
            }
            // the 13 other cards are all in the agenda or revealed
            assertEquals(13, state.agenda.getSize() + state.revealedRules.getSize());
        }
        assertTrue("the round took at least one turn", steps > 1);
    }

    // ---------------------------------------------------------------- redeterminisation

    @Test
    public void copyForAPlayerKeepsTheTipsTheySeeAndReshufflesTheOthersWithTheAgenda() {
        LawnAndOrderGameState state = newState(3, 8);
        RuleCard t0 = rule(PINK), t1 = rule(RED), t2 = rule(OVERSIZED);
        setRules(state, allRuleCardsExcept(t0, t1, t2), List.of(t0, t1, t2), List.of());
        // player 0 sees tips 0 and 2, not tip 1: tip 1 and the 13 agenda cards are shuffled among themselves
        List<RuleCard> hidden = new ArrayList<>(state.agenda.getComponents());
        hidden.add(t1);

        boolean hiddenTipChanged = false;
        for (int k = 0; k < 10; k++) {
            LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy(0);
            assertEquals(t0, copy.insiderTips.get(0));
            assertEquals(t2, copy.insiderTips.get(2));
            for (int i = 0; i < 3; i++)
                assertArrayEquals(state.insiderTips.getVisibilityOfComponent(i), copy.insiderTips.getVisibilityOfComponent(i));
            assertFalse(copy.agenda.contains(t0));
            assertFalse(copy.agenda.contains(t2));
            List<RuleCard> copyHidden = new ArrayList<>(copy.agenda.getComponents());
            copyHidden.add(copy.insiderTips.get(1));
            assertEquals(multiset(hidden), multiset(copyHidden));
            assertEquals(13, copy.agenda.getSize());
            assertAllCardsPresent(copy);
            if (!copy.insiderTips.get(1).equals(t1)) hiddenTipChanged = true;
        }
        // tip 1 holds "No Red" in a copy with chance 1/14; over 10 copies at least one differs
        assertTrue("the tip player 0 cannot see is never reshuffled", hiddenTipChanged);
        // the original is untouched
        assertEquals(List.of(t0, t1, t2), state.insiderTips.getComponents());
    }

    @Test
    public void copyForTheNextPlayerKeepsTheTipsOnEitherSideOfThem() {
        LawnAndOrderGameState state = newState(3, 8);
        RuleCard t0 = rule(PINK), t1 = rule(RED), t2 = rule(OVERSIZED);
        setRules(state, allRuleCardsExcept(t0, t1, t2), List.of(t0, t1, t2), List.of());
        // player 1 sees tips 0 (between 0 and 1) and 1 (between 1 and 2), not tip 2
        boolean hiddenTipChanged = false;
        for (int k = 0; k < 10; k++) {
            LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy(1);
            assertEquals(t0, copy.insiderTips.get(0));
            assertEquals(t1, copy.insiderTips.get(1));
            assertFalse(copy.agenda.contains(t0));
            assertFalse(copy.agenda.contains(t1));
            assertAllCardsPresent(copy);
            if (!copy.insiderTips.get(2).equals(t2)) hiddenTipChanged = true;
        }
        assertTrue("the tip player 1 cannot see is never reshuffled", hiddenTipChanged);
    }

    @Test
    public void withTwoPlayersACopyKeepsBothTipsAndReshufflesTheAgenda() {
        LawnAndOrderGameState state = newState(2, 8);
        RuleCard t0 = rule(PINK), t1 = rule(RED);
        setRules(state, allRuleCardsExcept(t0, t1), List.of(t0, t1), List.of());
        List<RuleCard> agenda = new ArrayList<>(state.agenda.getComponents());

        LawnAndOrderGameState a = (LawnAndOrderGameState) state.copy(1);
        LawnAndOrderGameState b = (LawnAndOrderGameState) state.copy(1);
        for (LawnAndOrderGameState copy : List.of(a, b)) {
            assertEquals(List.of(t0, t1), copy.insiderTips.getComponents());
            assertEquals(multiset(agenda), multiset(copy.agenda.getComponents()));
            assertAllCardsPresent(copy);
        }
        // two copies that differ only in the random numbers drawn for them
        assertNotEquals(a.agenda.getComponents(), b.agenda.getComponents());
    }

    @Test
    public void withSixPlayersACopyKeepsOnlyTheTwoTipsBesideThePlayer() {
        LawnAndOrderGameState state = newState(6, 8);
        List<RuleCard> tips = List.of(rule(PINK), rule(RED), rule(BLUE), rule(YELLOW), rule(OVERSIZED), rule(PLASTIC));
        setRules(state, allRuleCardsExcept(tips.toArray(new RuleCard[0])), tips, List.of());
        // player 3 sees tips 2 (between 2 and 3) and 3 (between 3 and 4); tips 0, 1, 4, 5 and the 10 agenda cards are hidden
        boolean hiddenTipChanged = false;
        for (int k = 0; k < 10; k++) {
            LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy(3);
            assertEquals(tips.get(2), copy.insiderTips.get(2));
            assertEquals(tips.get(3), copy.insiderTips.get(3));
            assertFalse(copy.agenda.contains(tips.get(2)));
            assertFalse(copy.agenda.contains(tips.get(3)));
            assertEquals(10, copy.agenda.getSize());
            assertAllCardsPresent(copy);
            for (int i : new int[]{0, 1, 4, 5})
                if (!copy.insiderTips.get(i).equals(tips.get(i))) hiddenTipChanged = true;
        }
        assertTrue("the tips player 3 cannot see are never reshuffled", hiddenTipChanged);
    }
}

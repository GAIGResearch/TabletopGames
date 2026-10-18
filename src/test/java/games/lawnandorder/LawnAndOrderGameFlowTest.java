package games.lawnandorder;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.Pass;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static games.lawnandorder.LawnAndOrderGameState.Decision.*;
import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderGameState.Phase.PLAY_OBJECT;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static org.junit.Assert.*;

/**
 * Games created through the GameType factory and driven only by fm.next: scripted rounds, and seeded games played to
 * the end.
 */
public class LawnAndOrderGameFlowTest {

    @Test
    public void scriptedRoundWithAnEmergencySessionRevealingZeroTolerance() {
        Game game = newGame(3, 11);
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        setAgendaTop(state, special(RuleCard.Special.EMERGENCY_SESSION), rule(PLASTIC),
                special(RuleCard.Special.ZERO_TOLERANCE), rule(YELLOW));
        LawnCard a0 = card(ORNAMENT, PINK, PLASTIC), a1 = card(FURNITURE, YELLOW, ILLUMINATED), a2 = card(STRUCTURE, RED, OVERSIZED);
        LawnCard b1 = card(FURNITURE, RED, ILLUMINATED), b2 = card(WATER_FEATURE, YELLOW, PLASTIC);
        putInHand(state, 0, a0);
        putInHand(state, 1, a1, b1);
        putInHand(state, 2, a2, b2);

        // Turn 1: the Emergency Session reveals "No Plastic" and Zero Tolerance. Player 0: Plastic +1 -> 1 on 1 card,
        // limit 1 - 1 = 0 -> Cease & Desist at once. Players 1 and 2: 0 on 1 card, limit 0 -> safe.
        playTurn(fm, state, a0, a1, a2);
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(1 + 4, state.getDiscardDeck().getSize());
        assertTrue(state.isZeroTolerance());
        assertEquals(rule(YELLOW), state.getAgenda().peek());
        assertEquals(List.of(1, 2), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
        decide(fm, state, null, CONTINUE, CONTINUE);

        // Turn 2: "No Yellow". Player 1: no immediate citation; Yellow x1 (turn 1 card) -> 1 on 2 cards, limit 1,
        // safe. Player 2: Plastic +1 immediately, Yellow +1 -> 2 on 2 cards, limit 1 -> Cease & Desist (without
        // Zero Tolerance 2 on 2 would be safe).
        playTurn(fm, state, null, b1, b2);
        assertEquals(1, state.getCitations(1));
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(CEASE_AND_DESIST, state.getStatus(2));
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);

        // Player 1 passes: the round ends. Two Furniture (1), Yellow and Red (0), two Illuminated (1)
        decide(fm, state, null, PASS, null);
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(new int[]{1, 0, 1}, tracks(state, 1));
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 2));
        assertEquals(1, state.getRoundCounter());
        assertFalse(state.isZeroTolerance());
        assertAllCardsPresent(state);
    }

    @Test
    public void scriptedGoodwillOverThreeRounds() {
        Game game = newGame(3, 11);
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        assertFalse(state.hasGoodwill(0) || state.hasGoodwill(1) || state.hasGoodwill(2));

        // Player 0 plays the same two cards in rounds 1 and 2, with "No Pink" then "No Plastic" revealed:
        // Turn 1 Pink: 1 on 1 card (safe). Turn 2 Pink Plastic: +1 immediately (Pink) and +1 (Plastic) -> 3 on 2 cards.
        LawnCard a0 = card(ORNAMENT, PINK, OVERSIZED), b0 = card(FURNITURE, PINK, PLASTIC);
        LawnCard a2 = card(ORNAMENT, YELLOW, ILLUMINATED), b2 = card(FURNITURE, RED, OVERSIZED);

        // ---- Round 1: nobody holds Goodwill, so player 0's 3 on 2 cards (limit 2) is a Cease & Desist
        LawnCard a1 = card(STRUCTURE, RED, ILLUMINATED), b1 = card(WATER_FEATURE, BLUE, OVERSIZED);
        playScriptedRound(fm, state, a0, b0, a1, b1, a2, b2);
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
        decide(fm, state, null, PASS, PASS);
        assertEquals(1, state.getRoundCounter());
        // Round 2: Goodwill exactly for player 0 (busted in round 1)
        assertTrue(state.hasGoodwill(0));
        assertFalse(state.hasGoodwill(1));
        assertFalse(state.hasGoodwill(2));
        assertAllCardsPresent(state);

        // ---- Round 2: player 0's limit on 2 cards is 2 + 1 = 3, so 3 citations survive; player 1 now plays the
        // Pink / Pink Plastic pattern without Goodwill: 3 on 2 cards, limit 2 -> Cease & Desist
        LawnCard c1 = card(STRUCTURE, PINK, ILLUMINATED), d1 = card(WATER_FEATURE, PINK, PLASTIC);
        playScriptedRound(fm, state, a0, b0, c1, d1, a2, b2);
        assertEquals(3, state.getCitations(0));
        assertEquals(2 + 1, state.getCitationLimit(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(CEASE_AND_DESIST, state.getStatus(1));
        assertTrue(state.hasGoodwill(0));   // unchanged during the round
        assertFalse(state.hasGoodwill(1));
        decide(fm, state, PASS, null, PASS);
        assertEquals(2, state.getRoundCounter());
        // player 0 scored round 2: Ornament + Furniture 0, Pink x2 = 1, Oversized + Plastic 0 (round 1: busted, 0)
        assertArrayEquals(new int[]{0, 1, 0}, tracks(state, 0));
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 1));
        // Round 3: Goodwill returned by player 0 (did not bust in round 2), given to player 1 (busted)
        assertFalse(state.hasGoodwill(0));
        assertTrue(state.hasGoodwill(1));
        assertFalse(state.hasGoodwill(2));
        assertEquals(0, state.getCitationLimit(0));
        assertEquals(0 + 1, state.getCitationLimit(1));
        assertAllCardsPresent(state);
    }

    /** Two turns of a 3-player round with "No Pink" then "No Plastic" revealed: everyone plays their first card,
     * everyone continues, everyone still active plays their second card (a busted player is skipped). */
    private static void playScriptedRound(AbstractForwardModel fm, LawnAndOrderGameState state, LawnCard a0, LawnCard b0,
                                          LawnCard a1, LawnCard b1, LawnCard a2, LawnCard b2) {
        setAgendaTop(state, rule(PINK), rule(PLASTIC));
        putInHand(state, 0, a0, b0);
        putInHand(state, 1, a1, b1);
        putInHand(state, 2, a2, b2);
        playTurn(fm, state, a0, a1, a2);
        assertEquals(List.of(0, 1, 2), state.getPlayersStillToChoose());
        decide(fm, state, CONTINUE, CONTINUE, CONTINUE);
        playTurn(fm, state, b0, b1, b2);
    }

    @Test
    public void scriptedRoundWithAPassABustAndScoring() {
        Game game = newGame(3, 11);
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        setAgendaTop(state, rule(PINK), rule(PLASTIC));

        // Turn 1: "No Pink" is revealed; only player 0's card is Pink
        LawnCard a0 = card(ORNAMENT, PINK, PLASTIC), a1 = card(FURNITURE, BLUE, PLASTIC), a2 = card(STRUCTURE, RED, ILLUMINATED);
        putInHand(state, 0, a0);
        putInHand(state, 1, a1);
        putInHand(state, 2, a2);
        playTurn(fm, state, a0, a1, a2);
        assertArrayEquals(new int[]{1, 0, 0}, state.citations);
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        assertAllCardsPresent(state);

        decide(fm, state, PASS, CONTINUE, CONTINUE);
        assertEquals(PASSED, state.getStatus(0));
        assertEquals(5, state.getHand(1).getSize());
        assertEquals(5, state.getHand(2).getSize());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(1, 2), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);

        // Turn 2: player 1 plays a Pink Plastic card (+1 immediately), then "No Plastic": two Plastic cards (+2),
        // 3 citations on 2 cards -> Cease & Desist. Player 0 has passed: immune to "No Plastic".
        LawnCard b1 = card(FURNITURE, PINK, PLASTIC), b2 = card(STRUCTURE, YELLOW, ILLUMINATED);
        putInHand(state, 1, b1);
        putInHand(state, 2, b2);
        playTurn(fm, state, null, b1, b2);
        assertEquals(1, state.getCitations(0));
        assertEquals(0, state.getCitations(2));
        assertEquals(CEASE_AND_DESIST, state.getStatus(1));
        assertEquals(2 + 4, state.getDiscardDeck().getSize());
        assertEquals(List.of(2), state.getPlayersStillToChoose());
        assertEquals(2, state.getCurrentPlayer());
        assertAllCardsPresent(state);

        // Player 2 passes: the round ends. Player 0: one card, 0. Player 1: busted, 0.
        // Player 2: two Structures (1), Red and Yellow (0), two Illuminated (1).
        decide(fm, state, null, null, PASS);
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 1));
        assertArrayEquals(new int[]{1, 0, 1}, tracks(state, 2));
        assertEquals(1, state.getRoundCounter());
        assertEquals(List.of(0, 1, 2), state.getPlayersStillToChoose());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertAllCardsPresent(state);
    }

    @Test
    public void seededGameIsPlayedToTheEnd() {
        LawnAndOrderGameState state = playRandomGame(newGame(3, 7), 7, 0.8);
        assertResultsByTheRules(state);
    }

    @Test
    public void sixPlayerGameStartingWithAnEmptyDrawDeckIsPlayedToTheEnd() {
        // The first round starts with an empty draw deck, and its first Agenda card cites nobody, so no player busts
        // on one card and every player continues from the empty draw deck: 6 x 1 = 6 such decisions at least.
        Game game = newGame(6, 8);
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        setAsideDrawDeck(state, 0);
        setAgendaTop(state, special(RuleCard.Special.ADMINISTRATIVE_ERROR));
        state = playRandomGame(game, 8, 1.0);
        assertResultsByTheRules(state);
    }

    /**
     * Play a game to the end with random cards and a Continue policy (continue with the given probability while
     * citations are below the lawn size), checking conservation (so no null card anywhere) after every action.
     */
    private static LawnAndOrderGameState playRandomGame(Game game, long seed, double continueProbability) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        Random rnd = new Random(seed);
        int steps = 0, emptyDeckContinues = 0;
        boolean emptyAtStart = state.getDrawDeck().getSize() == 0;
        while (state.isNotTerminal() && steps++ < 100_000) {
            int p = state.getCurrentPlayer();
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no action for player " + p, actions.isEmpty());
            AbstractAction action;
            if (state.getGamePhase() == PLAY_OBJECT) {
                action = actions.get(rnd.nextInt(actions.size()));
            } else {
                boolean safe = state.getCitations(p) < state.getLawn(p).getSize();
                action = safe && rnd.nextDouble() < continueProbability ? new Continue(p) : new Pass(p);
                assertTrue(actions.contains(action));
                if (action instanceof Continue && state.getDrawDeck().getSize() == 0)
                    emptyDeckContinues++;
            }
            fm.next(state, action);
            assertAllCardsPresent(state);
        }
        assertFalse("game did not end within 100000 actions", state.isNotTerminal());
        if (emptyAtStart)
            assertTrue("arrangement: someone continued with an empty draw deck", emptyDeckContinues >= state.getNPlayers());
        return state;
    }

    /** Checks the final results against an oracle written from the rules. */
    private static void assertResultsByTheRules(LawnAndOrderGameState state) {
        int n = state.getNPlayers();
        // the qualifiers have at least 10 on all three tracks, and the best combined total among them wins or draws
        List<Integer> candidates = new ArrayList<>();
        for (int p = 0; p < n; p++)
            if (tracks(state, p)[0] >= 10 && tracks(state, p)[1] >= 10 && tracks(state, p)[2] >= 10)
                candidates.add(p);
        if (candidates.isEmpty()) {
            // the game ended at the default maxRounds, and the best combined total among all players wins or draws
            assertEquals("nobody qualified, so the game should have ended by the round limit", 30, state.getRoundCounter());
            for (int p = 0; p < n; p++)
                candidates.add(p);
        }
        double best = candidates.stream().mapToDouble(state::getGameScore).max().orElseThrow();
        long nBest = candidates.stream().filter(p -> state.getGameScore(p) == best).count();
        for (int p = 0; p < n; p++) {
            GameResult expected = !candidates.contains(p) || state.getGameScore(p) < best ? GameResult.LOSE_GAME
                    : nBest == 1 ? GameResult.WIN_GAME : GameResult.DRAW_GAME;
            assertEquals("result of player " + p, expected, state.getPlayerResults()[p]);
        }
    }
}

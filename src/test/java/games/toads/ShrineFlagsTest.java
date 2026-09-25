package games.toads;

import core.CoreConstants;
import games.toads.abilities.GeneralOne;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import static games.toads.ToadConstants.ToadCardType.*;
import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * Shrine Flags and the lowest-Casualty tiebreak.
 * Player 0 attacks the first Battle; playCards plays the current player's Field and Flank, then the other player's.
 */
public class ShrineFlagsTest {

    ToadParameters params;
    ToadForwardModel fm;
    ToadGameState state;

    @Before
    public void setUp() {
        params = tacticsParams(933);
        fm = new ToadForwardModel();
        state = newState(params, fm);
    }

    private void assertFlags(int war, int expected) {
        assertEquals("player 0 Flags in War " + war, expected, state.getShrineFlags(war, 0));
        assertEquals("player 1 Flags in War " + war, expected, state.getShrineFlags(war, 1));
    }

    // ---------------------------------------------------------------- Battles

    @Test
    public void calmDoubleWinKeepsOneHostageAndSendsTheOtherStackToTheShrine() {
        // 0 v 0 Hostages before the Battle, so player 0 is Calm. Lanes 5 v 2 and 5 v 2: player 0 wins both.
        // Calm: keeps 1 Hostage (2 - 1), the other stack goes to the Shrine: +1 Flag each.
        playCards(state, fm, plain(5), plain(5), plain(2), plain(2));
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
        assertFlags(0, 1);
    }

    @Test
    public void angryDoubleWinKeepsBothHostagesAndGivesNoFlags() {
        // player 0 has 0 Hostages v player 1's 1 before the Battle, so player 0 is Angry.
        // Lanes 5 v 2 and 5 v 2: player 0 wins both and keeps both stacks (0 + 2 = 2); nothing goes to the Shrine.
        state.battlesWon[0][1] = 1;
        playCards(state, fm, plain(5), plain(5), plain(2), plain(2));
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
        assertFlags(0, 0);
    }

    @Test
    public void frogOverrideDoubleWinKeepsBothHostagesAndGivesNoFlags() {
        // 0 v 0 Hostages, so player 0 is Calm, but its hidden card (legacy GeneralOne ability) sets frogOverride.
        // Lanes 5 v 2 and 5 v 2: player 0 keeps both stacks (2 Hostages) and, as when Angry, gets no Flags.
        ToadCard frog = new ToadCard("FrogGeneral", 5, GENERAL_ONE, new GeneralOne());
        playCards(state, fm, plain(5), frog, plain(2), plain(2));
        assertEquals(2, state.battlesWon[0][0]);
        assertFlags(0, 0);
    }

    @Test
    public void eachTiedLaneGivesBothPlayersAFlagAccumulatedOverBattles() {
        // Battle 1 (player 0 attacks): 3 v 3 and 4 v 4 - two tied lanes: +2 Flags each, no Hostages.
        playCards(state, fm, plain(3), plain(4), plain(3), plain(4));
        assertEquals(0, state.battlesWon[0][0] + state.battlesWon[0][1]);
        assertFlags(0, 2);
        // Battle 2 (player 1, the Defender, now attacks): Field 6 v 2 (player 1 wins), Flank 3 v 3 (tied).
        // One tied lane: 2 + 1 = 3 Flags each; one lane won is not a double win, so no Shrine stack.
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, plain(6), plain(3), plain(2), plain(3));
        assertEquals(1, state.battlesWon[0][1]);
        assertFlags(0, 3);
    }

    @Test
    public void battleSplitOneLaneEachGivesNoFlags() {
        // Field 5 v 2 (player 0 wins), Flank 2 v 5 (player 1 wins): 1 Hostage each, nothing to the Shrine.
        playCards(state, fm, plain(5), plain(2), plain(2), plain(5));
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
        assertFlags(0, 0);
    }

    // ---------------------------------------------------------------- Casualties

    @Test
    public void eachPlayerStartsWarTwoWithOneFlagAndKeepsTheirWarOneFlags() {
        params.setParameterValue("useTactics", false);
        state = newState(params, fm);
        // Flags already in the Shrine in War 1; they must stay in War 1 and not carry into War 2.
        state.shrineFlags[0][0] = 3;
        state.shrineFlags[0][1] = 3;
        int steps = 0;
        while (state.getRoundCounter() == 0 && state.isNotTerminal() && steps < 200) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
            steps++;
        }
        assertEquals("War 1 did not end within the step cap", 1, state.getRoundCounter());
        // War 2: only the Casualty beside the Shrine - 1 Flag each.
        assertFlags(1, 1);
        // War 1: the 3 arranged Flags, plus at least one per tied lane (Calm double wins may add more);
        // the two players' counts are always equal.
        int tiedLanes = state.getBattlesTied(0);
        assertTrue("War 1 Flags " + state.getShrineFlags(0, 0) + " < 3 + " + tiedLanes + " tied lanes",
                state.getShrineFlags(0, 0) >= 3 + tiedLanes);
        assertEquals(state.getShrineFlags(0, 0), state.getShrineFlags(0, 1));
    }

    // ---------------------------------------------------------------- Tiebreak

    /**
     * War 1 is set to 3 v 3 (Stalemate); War 2 is one Battle split 1-1 (Field 5 v 3, Flank 6 v 7), also a Stalemate,
     * which ends the game. The Casualties are the given cards.
     */
    private void stalemateBothWars(ToadCard casualty0, ToadCard casualty1) {
        params.setParameterValue("useTactics", false);
        state = newState(params, fm);
        state.battlesWon[0][0] = 3;
        state.battlesWon[0][1] = 3;
        fm.endRound(state, 1);
        for (int p = 0; p < 2; p++) {
            state.playerDecks.get(p).clear();
            state.playerHands.get(p).clear();
        }
        state.tieBreakers[0] = casualty0;
        state.tieBreakers[1] = casualty1;
        state.fieldCards[0] = plain(5);
        state.fieldCards[1] = plain(3);
        ToadCard flank0 = plain(6);
        ToadCard flank1 = plain(7);
        state.hiddenFlankCards[0] = flank0;
        state.hiddenFlankCards[1] = flank1;
        state.playerHands.get(0).add(flank0);
        state.playerHands.get(1).add(flank1);
        fm._afterAction(state, null);
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        // both Wars Stalemated: the game scores are unchanged (5 each)
        assertEquals(5.0, state.getGameScore(0), 0.001);
        assertEquals(5.0, state.getGameScore(1), 0.001);
    }

    @Test
    public void lowerCasualtyWinsWhenBothWarsAreStalemated() {
        // Casualties 5 (player 0) v 2 (player 1): the lowest wins, so player 1.
        stalemateBothWars(plain(5), plain(2));
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void siegeCannonCasualtyBeatsAnAssassinCasualty() {
        // Siege Cannon (value 0, player 0) v Assassin (value 1, player 1): 0 < 1, so player 0 wins.
        stalemateBothWars(plain("SiegeCannon", 0, SIEGE_CANNON), plain("Assassin", 1, ASSASSIN));
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void equalCasualtiesAreAGenuineDraw() {
        // Two value-7 Generals: equal Casualties, so neither player wins.
        stalemateBothWars(plain("General", 7, GENERAL_ONE), plain("General", 7, GENERAL_TWO));
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[1]);
    }

    // ---------------------------------------------------------------- copy / equals / hashCode

    @Test
    public void copyKeepsShrineFlagsIndependently() {
        state.shrineFlags[0][0] = 2;
        state.shrineFlags[0][1] = 2;
        state.shrineFlags[1][0] = 1;
        state.shrineFlags[1][1] = 1;
        ToadGameState copy = (ToadGameState) state.copy();
        assertNotNull("copy has no shrineFlags", copy.shrineFlags);
        assertArrayEquals(new int[]{2, 2}, copy.shrineFlags[0]);
        assertArrayEquals(new int[]{1, 1}, copy.shrineFlags[1]);
        // changing the original does not change the copy
        state.shrineFlags[0][0] = 4;
        state.shrineFlags[1][1] = 4;
        assertEquals(2, copy.shrineFlags[0][0]);
        assertEquals(1, copy.shrineFlags[1][1]);
    }

    @Test
    public void statesDifferingOnlyInShrineFlagsAreNotEqualAndHashDifferently() {
        state.shrineFlags[0][0] = 2;
        state.shrineFlags[0][1] = 2;
        ToadGameState copy = (ToadGameState) state.copy();
        copy.shrineFlags = new int[][]{{2, 2}, {0, 0}}; // keep the test independent of _copy
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        // War 1 Flags 2 v 3 each: the states differ only in the Flags
        copy.shrineFlags = new int[][]{{3, 3}, {0, 0}};
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
    }
}

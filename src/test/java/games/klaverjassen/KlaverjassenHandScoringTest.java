package games.klaverjassen;

import org.junit.Before;
import org.junit.Test;

import static games.klaverjassen.KlaverjassenTestUtils.newState;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * KlaverjassenForwardModel.scoreHand: the pit bonus and the nat. The trump team is the trump chooser's team.
 */
public class KlaverjassenHandScoringTest {

    KlaverjassenGameState state;
    KlaverjassenForwardModel fm;

    @Before
    public void setup() {
        state = newState(3);
        fm = new KlaverjassenForwardModel();
        assertEquals(0, state.getTrumpChooser());    // team 0 is the trump team unless a test moves the deal on
    }

    private void score(int[] points, int[] roem, int[] scoresBefore) {
        // tricks consistent with the points, but never all 8 to one team (no pit) unless a test sets them after
        score(points, roem, new int[]{4, 4}, scoresBefore);
    }

    private void score(int[] points, int[] roem, int[] tricks, int[] scoresBefore) {
        state.tricksWon = tricks.clone();
        state.handPoints = points.clone();
        state.handRoem = roem.clone();
        state.teamScores = scoresBefore.clone();
        fm.scoreHand(state);
    }

    @Test
    public void aTrumpTeamWithFewerPointsIsNatAndTheOpponentsScoreBothTotals() {
        score(new int[]{70, 92}, new int[]{0, 0}, new int[]{0, 0});
        // 70 < 92: team 1 scores 70 + 92 = 162, team 0 nothing
        assertArrayEquals(new int[]{0, 162}, state.teamScores);
    }

    @Test
    public void aTrumpTeamWithMorePointsKeepsItsOwnAndScoresAddToEarlierOnes() {
        score(new int[]{92, 70}, new int[]{0, 0}, new int[]{100, 20});
        // 92 > 70: each scores its own; 100 + 92 = 192, 20 + 70 = 90
        assertArrayEquals(new int[]{192, 90}, state.teamScores);

        score(new int[]{60, 102}, new int[]{0, 0}, new int[]{100, 20});
        // 60 < 102: nat; team 1 gets 20 + 60 + 102 = 182, team 0 stays at 100
        assertArrayEquals(new int[]{100, 182}, state.teamScores);
    }

    @Test
    public void equalTotalsAreNotNatAndEachTeamKeepsItsOwn() {
        score(new int[]{81, 81}, new int[]{0, 0}, new int[]{0, 0});
        assertArrayEquals(new int[]{81, 81}, state.teamScores);
    }

    @Test
    public void equalTotalsAreNatWhenTieIsFailure() {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("tieIsFailure", true);
        state = newState(3, params);
        score(new int[]{81, 81}, new int[]{0, 0}, new int[]{0, 0});
        // 81 = 81 fails with tieIsFailure: team 1 scores 81 + 81 = 162, team 0 nothing
        assertArrayEquals(new int[]{0, 162}, state.teamScores);

        score(new int[]{61, 81}, new int[]{20, 0}, new int[]{0, 0});
        // the totals are compared: 61 + 20 = 81 v 81, nat; team 1 scores 81 + 81 = 162
        assertArrayEquals(new int[]{0, 162}, state.teamScores);

        score(new int[]{82, 80}, new int[]{0, 0}, new int[]{0, 0});
        // a greater total still makes it: 82 > 80, each scores its own
        assertArrayEquals(new int[]{82, 80}, state.teamScores);
    }

    @Test
    public void withTieIsFailureTheTrumpTeamIsStillTheChoosersTeam() {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("tieIsFailure", true);
        state = newState(3, params);
        fm.endRound(state, 1);
        assertEquals(1, state.getTrumpChooser());    // arrangement guard: team 1 is the trump team
        score(new int[]{81, 81}, new int[]{0, 0}, new int[]{50, 60});
        // trump team 1 fails on the tie: team 0 scores 50 + 81 + 81 = 212, team 1 stays at 60
        assertArrayEquals(new int[]{212, 60}, state.teamScores);
    }

    @Test
    public void theTrumpTeamIsTheChoosersTeam() {
        // the next hand: dealer 0, so player 1 chooses and team 1 is the trump team
        fm.endRound(state, 1);
        assertEquals(1, state.getTrumpChooser());    // arrangement guard

        score(new int[]{92, 70}, new int[]{0, 0}, new int[]{0, 0});
        // trump team 1 has 70 < 92: nat, team 0 scores 92 + 70 = 162
        assertArrayEquals(new int[]{162, 0}, state.teamScores);

        score(new int[]{70, 92}, new int[]{0, 0}, new int[]{0, 0});
        // trump team 1 has 92 > 70: each keeps its own (with team 0 as trump team this would be nat)
        assertArrayEquals(new int[]{70, 92}, state.teamScores);
    }

    @Test
    public void roemCountsInTheComparisonAndGoesToTheOpponentsOnANat() {
        score(new int[]{90, 72}, new int[]{0, 40}, new int[]{0, 0});
        // totals 90 + 0 = 90 v 72 + 40 = 112: nat, team 1 scores 90 + 112 = 202
        assertArrayEquals(new int[]{0, 202}, state.teamScores);

        score(new int[]{72, 90}, new int[]{20, 0}, new int[]{0, 0});
        // totals 72 + 20 = 92 v 90: the trump team makes it, each scores its own total
        assertArrayEquals(new int[]{92, 90}, state.teamScores);
    }

    // ---- pit: a team taking all 8 tricks gains pitBonus (100) as roem, before the nat comparison ----

    @Test
    public void theTrumpTeamTakingEveryTrickGainsThePitBonusAsRoem() {
        score(new int[]{162, 0}, new int[]{20, 0}, new int[]{8, 0}, new int[]{0, 0});
        // roem 20 + pit 100 = 120; total 162 + 120 = 282 v 0
        assertArrayEquals(new int[]{120, 0}, state.handRoem);
        assertArrayEquals(new int[]{282, 0}, state.teamScores);
    }

    @Test
    public void theOpponentsTakingEveryTrickGainThePitAndScoreItOnTheNat() {
        score(new int[]{0, 162}, new int[]{0, 40}, new int[]{0, 8}, new int[]{0, 0});
        // team 1 roem 40 + pit 100 = 140; totals 0 v 162 + 140 = 302: nat, team 1 scores 0 + 302 = 302
        assertArrayEquals(new int[]{0, 140}, state.handRoem);
        assertArrayEquals(new int[]{0, 302}, state.teamScores);
    }

    @Test
    public void sevenTricksOfEightAreNoPit() {
        score(new int[]{150, 12}, new int[]{20, 0}, new int[]{7, 1}, new int[]{0, 0});
        // no pit: totals 150 + 20 = 170 v 12
        assertArrayEquals(new int[]{20, 0}, state.handRoem);
        assertArrayEquals(new int[]{170, 12}, state.teamScores);
    }

    @Test
    public void thePitBonusComesFromTheParameters() {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("pitBonus", 50);
        state = newState(3, params);
        score(new int[]{162, 0}, new int[]{0, 0}, new int[]{8, 0}, new int[]{0, 0});
        // pit 50: total 162 + 50 = 212
        assertArrayEquals(new int[]{50, 0}, state.handRoem);
        assertArrayEquals(new int[]{212, 0}, state.teamScores);
    }
}

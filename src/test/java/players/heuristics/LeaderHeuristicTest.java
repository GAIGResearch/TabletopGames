package players.heuristics;

import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.spades.SpadesForwardModel;
import games.spades.SpadesGameState;
import games.spades.SpadesParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LeaderHeuristicTest {

    // Spades: 4 players in 2 teams (players 0 & 2 are team 0; players 1 & 3 are team 1)
    SpadesGameState spadesState = new SpadesGameState(new SpadesParameters(), 4);
    SpadesForwardModel spadesFM = new SpadesForwardModel();

    LoveLetterGameState llState = new LoveLetterGameState(new LoveLetterParameters(), 3);
    LoveLetterForwardModel llFM = new LoveLetterForwardModel();

    LeaderHeuristic leader = new LeaderHeuristic();
    CoarseTunableHeuristic coarseLeader = new CoarseTunableHeuristic();

    @Before
    public void setup() {
        spadesFM.setup(spadesState);
        spadesState.setTeamScore(0, 100);
        spadesState.setTeamScore(1, 40);
        llFM.setup(llState);
        coarseLeader.setParameterValue("heuristicType", CoarseTunableHeuristic.HeuristicType.LEADER);
    }

    @Test
    public void leadingTeamComparesAgainstOtherTeamNotTeamMate() {
        assertEquals(60.0, leader.evaluateState(spadesState, 0), 1e-6);
        assertEquals(60.0, leader.evaluateState(spadesState, 2), 1e-6);
    }

    @Test
    public void trailingTeamComparesAgainstOtherTeam() {
        assertEquals(-60.0, leader.evaluateState(spadesState, 1), 1e-6);
        assertEquals(-60.0, leader.evaluateState(spadesState, 3), 1e-6);
    }

    @Test
    public void coarseTunableLeaderComparesAgainstOtherTeam() {
        assertEquals(60.0, coarseLeader.evaluateState(spadesState, 0), 1e-6);
        assertEquals(60.0, coarseLeader.evaluateState(spadesState, 2), 1e-6);
        assertEquals(-60.0, coarseLeader.evaluateState(spadesState, 1), 1e-6);
    }

    @Test
    public void nonTeamGameComparesAgainstBestOtherPlayer() {
        for (int p = 0; p < llState.getNPlayers(); p++) {
            double bestOther = Double.NEGATIVE_INFINITY;
            for (int o = 0; o < llState.getNPlayers(); o++)
                if (o != p) bestOther = Math.max(bestOther, llState.getGameScore(o));
            assertEquals(llState.getGameScore(p) - bestOther, leader.evaluateState(llState, p), 1e-6);
        }
    }
}

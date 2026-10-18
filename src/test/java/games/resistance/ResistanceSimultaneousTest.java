package games.resistance;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.GameType;
import games.resistance.actions.ResTeamBuilding;
import games.resistance.actions.ResVoting;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static games.resistance.ResGameState.ResGamePhase.*;
import static games.resistance.components.ResPlayerCards.CardType.*;
import static org.junit.Assert.*;

/**
 * Resistance as a simultaneous-move game: the team vote is cast by everyone at once, and the mission vote by
 * everyone on the team at once. Votes can arrive one at a time or as one joint action, and an observation hides
 * the other players' votes but keeps our own.
 */
public class ResistanceSimultaneousTest {

    Game game;
    ResGameState state;
    ResForwardModel fm;

    @Before
    public void setup() {
        game = GameType.Resistance.createGameInstance(5, 34, new ResParameters());
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        state = (ResGameState) game.getGameState();
        fm = (ResForwardModel) game.getForwardModel();
        // players 1 and 3 are the spies
        for (int p = 0; p < 5; p++)
            state.setPlayerIdentity(p, p == 1 || p == 3 ? SPY : RESISTANCE);
    }

    /** The leader (player 0) proposes a team of the size needed for the first mission, containing players 0 and 1. */
    private List<Integer> proposeTeam() {
        int size = state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()];
        int[] team = new int[size];
        for (int i = 0; i < size; i++) team[i] = i;
        fm.next(state, new ResTeamBuilding(0, team));
        assertEquals(TeamSelectionVote, state.getGamePhase());
        return Arrays.stream(team).boxed().toList();
    }

    private static SimultaneousAction jointVote(List<Integer> voters, Map<Integer, ?> noVoters) {
        Map<Integer, AbstractAction> votes = new LinkedHashMap<>();
        for (int p : voters)
            votes.put(p, new ResVoting(p, noVoters.containsKey(p) ? No : Yes));
        return new SimultaneousAction(votes);
    }

    @Test
    public void leaderAloneChoosesTheTeam() {
        assertEquals(LeaderSelectsTeam, state.getGamePhase());
        assertEquals(List.of(0), state.getCurrentSimultaneousPlayers());
        assertTrue(state.getPlayersStillToVote().isEmpty());
    }

    @Test
    public void teamVoteActingSetShrinksAsPlayersVote() {
        proposeTeam();
        assertEquals(List.of(0, 1, 2, 3, 4), state.getCurrentSimultaneousPlayers());

        // votes may arrive from any of the players still to vote, not just the turn owner
        fm.next(state, new ResVoting(3, Yes));
        assertEquals(List.of(0, 1, 2, 4), state.getCurrentSimultaneousPlayers());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new ResVoting(0, No));
        assertEquals(List.of(1, 2, 4), state.getCurrentSimultaneousPlayers());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(TeamSelectionVote, state.getGamePhase());

        // every player can vote, whoever holds the turn
        for (int p : List.of(1, 2, 4))
            assertEquals(List.of(new ResVoting(p, Yes), new ResVoting(p, No)), fm.computeAvailableActions(state, null, p));
    }

    @Test
    public void missionVoteAsksOnlyTheTeam() {
        List<Integer> team = proposeTeam();
        fm.next(state, jointVote(List.of(0, 1, 2, 3, 4), Map.of()));
        assertEquals(MissionVote, state.getGamePhase());
        assertEquals(team, state.getCurrentSimultaneousPlayers());
        assertEquals((int) team.get(0), state.getCurrentPlayer());

        // player 1 is a spy, and so may fail the mission; player 0 may not
        assertEquals(List.of(new ResVoting(0, Yes)), fm.computeAvailableActions(state, null, 0));
        assertEquals(List.of(new ResVoting(1, Yes), new ResVoting(1, No)), fm.computeAvailableActions(state, null, 1));

        fm.next(state, new ResVoting(1, No));
        assertEquals(team.stream().filter(p -> p != 1).toList(), state.getCurrentSimultaneousPlayers());
    }

    @Test
    public void jointTeamVoteEqualsSequentialVotes() {
        Game other = GameType.Resistance.createGameInstance(5, 34, new ResParameters());
        other.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        ResGameState sequential = (ResGameState) other.getGameState();
        for (int p = 0; p < 5; p++)
            sequential.setPlayerIdentity(p, p == 1 || p == 3 ? SPY : RESISTANCE);

        List<Integer> team = proposeTeam();
        int[] teamArray = team.stream().mapToInt(i -> i).toArray();
        fm.next(sequential, new ResTeamBuilding(0, teamArray));

        SimultaneousAction joint = jointVote(List.of(0, 1, 2, 3, 4), Map.of(2, true, 4, true));
        fm.next(state, joint);
        for (int i = 0; i < 5; i++)
            fm.next(sequential, joint.getPlayerActions().get(sequential.getCurrentPlayer()));

        for (ResGameState s : List.of(state, sequential)) {
            assertTrue(s.getVoteSuccess());
            assertEquals(MissionVote, s.getGamePhase());
            assertEquals(team, s.getFinalTeam());
            assertEquals(team, s.getCurrentSimultaneousPlayers());
            assertEquals((int) team.get(0), s.getCurrentPlayer());
        }
    }

    @Test
    public void jointMissionVoteResolvesTheMission() {
        List<Integer> team = proposeTeam();
        fm.next(state, jointVote(List.of(0, 1, 2, 3, 4), Map.of()));
        fm.next(state, jointVote(team, Map.of(1, true)));

        assertEquals(1, state.getMissionsSoFar());
        assertFalse(state.getHistoricMissionSuccess(1));
        assertEquals(1, state.getHistoricNoVotes(1));
        assertEquals(LeaderSelectsTeam, state.getGamePhase());
        assertEquals(1, state.getLeaderID());
        assertEquals(List.of(1), state.getCurrentSimultaneousPlayers());
    }

    @Test
    public void observationKeepsOwnVoteHidesOthersAndOwnsTheTurn() {
        proposeTeam();
        assertTrue(state.getCoreGameParameters().partialObservable);
        fm.next(state, new ResVoting(0, Yes));
        fm.next(state, new ResVoting(1, No));
        assertEquals(2, state.getCurrentPlayer());

        // a player who has voted sees their own vote, and nobody else's
        ResGameState seenBy1 = (ResGameState) state.copy(1);
        assertArrayEquals(new Object[]{null, No, null, null, null}, seenBy1.votingChoice);
        assertEquals(List.of(0, 2, 3, 4), seenBy1.getCurrentSimultaneousPlayers());
        assertEquals(2, seenBy1.getCurrentPlayer());

        // a player yet to vote sees no votes at all, and everyone still to vote; they hold the turn
        ResGameState seenBy3 = (ResGameState) state.copy(3);
        assertArrayEquals(new Object[5], seenBy3.votingChoice);
        assertEquals(3, seenBy3.getCurrentPlayer());
        assertEquals(List.of(0, 1, 2, 3, 4), seenBy3.getCurrentSimultaneousPlayers());
        assertEquals(List.of(new ResVoting(3, Yes), new ResVoting(3, No)), fm.computeAvailableActions(seenBy3));

        // the full copy, and the master state, are untouched
        for (ResGameState s : List.of((ResGameState) state.copy(), state)) {
            assertArrayEquals(new Object[]{Yes, No, null, null, null}, s.votingChoice);
            assertEquals(2, s.getCurrentPlayer());
            assertEquals(List.of(2, 3, 4), s.getCurrentSimultaneousPlayers());
        }
    }

    @Test
    public void randomPlayersFinishTheGameAndHistoryRecordsTheVoter() {
        for (int nPlayers = 5; nPlayers <= 10; nPlayers++) {
            for (long seed = 10; seed < 13; seed++) {
                Game g = GameType.Resistance.createGameInstance(nPlayers, seed);
                List<AbstractPlayer> players = new ArrayList<>();
                for (int p = 0; p < nPlayers; p++) players.add(new RandomPlayer(new Random(seed + p)));
                g.reset(players);
                g.run();
                ResGameState s = (ResGameState) g.getGameState();
                assertFalse(s.isNotTerminal());
                int votes = 0;
                for (Pair<Integer, AbstractAction> h : s.getHistory()) {
                    int actor;
                    if (h.b instanceof ResVoting v) {
                        actor = v.playerId;
                        votes++;
                    } else if (h.b instanceof ResTeamBuilding t) actor = t.playerId;
                    else throw new AssertionError("unexpected action in history: " + h.b);
                    assertEquals("history records the wrong player", (int) h.a, actor);
                }
                assertTrue(votes >= nPlayers);
            }
        }
    }
}

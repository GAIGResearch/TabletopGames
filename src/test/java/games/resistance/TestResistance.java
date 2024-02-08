package games.resistance;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGamePhase;
import games.GameType;
import games.resistance.components.ResPlayerCards;
import games.resistance.actions.ResVoting;
import games.resistance.actions.ResTeamBuilding;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;
import java.util.stream.IntStream;

import static games.resistance.components.ResPlayerCards.CardType.RESISTANCE;
import static games.resistance.components.ResPlayerCards.CardType.SPY;
import static org.junit.Assert.*;


public class TestResistance {

    public int atLeastOneHandRearranged = 0;
    Game resistance;
    List<AbstractPlayer> players;
    ResForwardModel fm = new ResForwardModel();
    RandomPlayer rnd = new RandomPlayer();

    private void progressGame(ResGameState state, ResGameState.ResGamePhase requiredGamePhase) {
        while (state.getGamePhase() != requiredGamePhase &&
                state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
    }

    private void progressGameOneRound(ResGameState state) {
        int round = state.getRoundCounter();
        while (state.getRoundCounter() == round && state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
    }

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        ResParameters resParameters = new ResParameters();
        resParameters.setRandomSeed(-274);
        resistance = GameType.Resistance.createGameInstance(5, 34, resParameters);
        resistance.reset(players);
    }

    @Test
    public void checkingActionsForFirstPhaseTest() {
        ResGameState state = (ResGameState) resistance.getGameState();
        do {
            progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam);
            if (state.getGameStatus() == CoreConstants.GameResult.GAME_END) return;
            assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.LeaderSelectsTeam);
            assertEquals(state.getLeaderID(), state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            int[] players = new int[state.getNPlayers()];
            for (int i = 0; i < state.getNPlayers(); i++) {
                players[i] = i;
            }
            ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()]);
            assertEquals(choiceOfTeams.size(), actions.size());
            for (AbstractAction action : actions) {
                assertEquals(action.getClass(), ResTeamBuilding.class);
            }
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING);
    }

    @Test
    public void checkingActionsForSecondPhaseTest() {
        ResGameState state = (ResGameState) resistance.getGameState();
        do {
            progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote);
            if (state.getGameStatus() == CoreConstants.GameResult.GAME_END) return;
            assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.TeamSelectionVote);
            do {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertEquals(2, actions.size());
                assertTrue(actions.stream().allMatch(a -> a instanceof ResVoting));
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
            } while (state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote);
        } while (state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING);
    }

    @Test
    public void checkingActionsForThirdPhaseTest() {
        ResGameState state = (ResGameState) resistance.getGameState();
        do {
            progressGame(state, ResGameState.ResGamePhase.MissionVote);
            if (state.getGameStatus() == CoreConstants.GameResult.GAME_END) return;
            do {
                assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.MissionVote);
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                int possibleActions = state.getPlayerHandCards().get(state.getCurrentPlayer()).get(2).cardType == SPY ? 2 : 1;
                assertEquals(possibleActions, actions.size());
                assertTrue(actions.stream().allMatch(a -> a instanceof ResVoting));
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
            } while (state.getGamePhase() == ResGameState.ResGamePhase.MissionVote && state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING);
        } while (state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING);
    }

    @Test
    public void checkingPhaseTransitionLeaderToVote() {
        ResGameState state = (ResGameState) resistance.getGameState();

        progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam);
        IGamePhase previousGamePhase = state.getGamePhase();

        assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.LeaderSelectsTeam);
        assertEquals(state.leaderID, state.getCurrentPlayer());
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        assertNotEquals(previousGamePhase, state.getGamePhase());
        assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.TeamSelectionVote);

    }

    @Test
    public void checkingPhaseTransitionVoteToMissionVote() {
        ResGameState state = (ResGameState) resistance.getGameState();

        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote);
        IGamePhase previousGamePhase = state.getGamePhase();

        assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.TeamSelectionVote);
        for (int i = 0; i < state.getNPlayers(); i++) {
            assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof ResVoting));
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
        assertNotEquals(previousGamePhase, state.getGamePhase());
        if (state.getGamePhase() == ResGameState.ResGamePhase.MissionVote || state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam) {
            // fine
        } else {
            {
                throw new AssertionError("Neither condition is met.");
            }
        }
    }

    @Test
    public void checkingPhaseTransitionMissionVoteToLeader() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        IGamePhase previousGamePhase = state.getGamePhase();

        if (CoreConstants.GameResult.GAME_END != state.getGameStatus()) {
            for (int i = 0; i < state.finalTeamChoice.size(); i++) {
                assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.MissionVote);
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
            }
            assertNotEquals(previousGamePhase, state.getGamePhase());
            assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.LeaderSelectsTeam);
        }


    }

    @Test
    public void checkingGameOverWithMissionsCriteria() {

        ResGameState state = (ResGameState) resistance.getGameState();
        progressGameOneRound(state);
        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 1);
            progressGameOneRound(state);
        }
        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 2);
            progressGameOneRound(state);
        }
        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 3);
        }

        if (Collections.frequency(state.getGameBoardValues(), true) == 3 || Collections.frequency(state.getGameBoardValues(), false) == 3) {
            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        }

        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            progressGameOneRound(state);
        }
        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 4);
            if (Collections.frequency(state.getGameBoardValues(), true) == 3 || Collections.frequency(state.getGameBoardValues(), false) == 3) {
                assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
            }
        }

        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            progressGameOneRound(state);
        }
        if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 5);
            if (Collections.frequency(state.getGameBoardValues(), true) == 3 || Collections.frequency(state.getGameBoardValues(), false) == 3) {
                assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
            }
        }
    }

    @Test
    public void checkingGameOverWithFailedVotesCriteria() {

        ResGameState state = (ResGameState) resistance.getGameState();
        while (state.getFailedVoteCounter() != 5 && state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
        //fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        if (state.getFailedVoteCounter() == 5) {

            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        }
    }

    @Test
    public void checkingHandInitialisation() {

        ResGameState state = (ResGameState) resistance.getGameState();
        for (int i = 0; i < state.getNPlayers(); i++) {
            assertEquals(state.getPlayerHandCards().get(i).getSize(), 3);
            if (state.getPlayerHandCards().get(i).get(2).cardType != ResPlayerCards.CardType.RESISTANCE && state.getPlayerHandCards().get(i).get(2).cardType != SPY) {
                throw new AssertionError("last card isn't SPY or RESISTANCE");
            }
            if (state.getPlayerHandCards().get(i).get(0).cardType != ResPlayerCards.CardType.No && state.getPlayerHandCards().get(i).get(0).cardType != ResPlayerCards.CardType.Yes) {
                throw new AssertionError("first card isn't yes or no");
            }
            if (state.getPlayerHandCards().get(i).get(1).cardType != ResPlayerCards.CardType.Yes && state.getPlayerHandCards().get(i).get(1).cardType != ResPlayerCards.CardType.No) {
                throw new AssertionError("second card isn't yes or no");
            }
        }

    }

    @Test
    public void checkingCorrectSpyToResistanceRatio() {
        ResGameState state = (ResGameState) resistance.getGameState();
        int spyCount = 0;
        int resistanceCount = 0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            assertEquals(state.getPlayerHandCards().get(i).getSize(), 3);
            if (state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.RESISTANCE) {
                resistanceCount += 1;
            }
            if (state.getPlayerHandCards().get(i).get(2).cardType == SPY) {
                spyCount += 1;
            }
        }
        assertEquals(resistanceCount, state.factions[0]);
        assertEquals(spyCount, state.factions[1]);
    }

    @Test
    public void checkingWinnersAreCorrect() {
        ResGameState state = (ResGameState) resistance.getGameState();
        while (CoreConstants.GameResult.GAME_END != state.getGameStatus()) {
            progressGameOneRound(state);
        }

        boolean resistanceWon = state.getPlayerHandCards().get(0).get(2).cardType == ResPlayerCards.CardType.RESISTANCE &&
                state.getPlayerResults()[0] == CoreConstants.GameResult.WIN_GAME;
        for (int i = 0; i < state.getNPlayers() - 1; i++) {
            if (resistanceWon) {
                if (state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.RESISTANCE) {
                    assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[i]);
                }
                if (state.getPlayerHandCards().get(i).get(2).cardType == SPY) {
                    assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[i]);
                }
            } else {
                if (state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.RESISTANCE) {
                    assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[i]);
                }
                if (state.getPlayerHandCards().get(i).get(2).cardType == SPY) {
                    assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[i]);
                }
            }
        }
    }

    @Test
    public void checkingLeaderMovesAfterFailedTeamVote() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote);
        int previousLeader = state.getLeaderID();

        do {
            fm.next(state, new ResVoting(state.getCurrentPlayer(), ResPlayerCards.CardType.No));
        } while (state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote);
        assertFalse(state.getVoteSuccess());
        assertNotEquals(previousLeader, state.getLeaderID());
    }

    @Test
    public void checkingTeamSize() {
        ResGameState state = (ResGameState) resistance.getGameState();

        //Checking Round 0
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote);
        assertEquals(0, state.getFinalTeam().size());
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        assertEquals(0, state.getRoundCounter());
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()], state.getFinalTeam().size());

        //Checking Round 1
        progressGameOneRound(state);
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        assertEquals(1, state.getRoundCounter());
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()], state.getFinalTeam().size());

        //Checking Round 2
        progressGameOneRound(state);
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        assertEquals(2, state.getRoundCounter());
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()], state.getFinalTeam().size());

        //Checking Round 3
        progressGameOneRound(state);
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        assertEquals(3, state.getRoundCounter());
        if (state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING)
            assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()], state.getFinalTeam().size());
        else return;

        //Checking Round 4
        progressGameOneRound(state);
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        assertEquals(4, state.getRoundCounter());
        if (state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING)
            assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()], state.getFinalTeam().size());
    }

    @Test
    public void checkingLeaderMovesAfterRoundEnds() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        int previousLeader = state.getLeaderID();
        int previousRound = state.getRoundCounter();

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        if (previousRound != state.getRoundCounter()) {
            assertNotEquals(previousLeader, state.getLeaderID());
        }
    }

    @Test
    public void checkingSpiesKnowEveryonesCards() {
        ResGameState state = (ResGameState) resistance.getGameState();
        List<ResPlayerCards.CardType> listOfIdentityCards = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            listOfIdentityCards.add(state.getPlayerHandCards().get(i).get(2).cardType);
        }

        //Checking Player 0
        ResGameState playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 1
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 2
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 3
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 4
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
    }

    @Test
    public void checkingResistanceDontKnowEveryonesCards() {
        //This Test May Fail Due To A Random Arrangement Of Hidden Cards Aligning With Actual Identity Cards, I have minimised this
        // Chance by checking multiple Resistance members views of other players hands
        // If atleast one hand is not equal the default copy/gamestate, the hand redeterminisation works
        ResGameState state = (ResGameState) resistance.getGameState();

        List<ResPlayerCards.CardType> listOfIdentityCards = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            listOfIdentityCards.add(state.getPlayerHandCards().get(i).get(2).cardType);
        }

        //Checking Player 0
        ResGameState playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 1
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 2
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 3
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 4
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state, playerState, listOfIdentityCards);

        assertNotEquals(atLeastOneHandRearranged, 0);
    }


    private void checkingSpiesKnowEveryonesCardsMethod(ResGameState state, ResGameState playerState, List<ResPlayerCards.CardType> listOfIdentityCards) {
        if (state.getPlayerHandCards().get(state.getCurrentPlayer()).get(2).cardType == SPY) {
            List<ResPlayerCards.CardType> listOfSpyKnownIdentityCards = new ArrayList<>();
            for (int j = 0; j < state.getNPlayers(); j++) {
                listOfSpyKnownIdentityCards.add(playerState.getPlayerHandCards().get(j).get(2).cardType);
            }
            assertEquals(listOfSpyKnownIdentityCards, listOfIdentityCards);
        }
    }

    private void checkingResistanceDontKnowEveryonesCardsMethod(ResGameState state, ResGameState playerState, List<ResPlayerCards.CardType> listOfIdentityCards) {
        if (state.getPlayerHandCards().get(state.getCurrentPlayer()).get(2).cardType == ResPlayerCards.CardType.RESISTANCE) {
            List<ResPlayerCards.CardType> listOfResistanceKnownIdentityCards = new ArrayList<>();
            for (int j = 0; j < state.getNPlayers(); j++) {
                listOfResistanceKnownIdentityCards.add(playerState.getPlayerHandCards().get(j).get(2).cardType);
            }

            if (listOfResistanceKnownIdentityCards != listOfIdentityCards) {
                atLeastOneHandRearranged += 1;
            }
        }
    }

    @Test
    public void redeterminisationKeepsSpiesConsistentWithFailedMissionsI() {
        // We set one mission with a failure
        // then confirm that over a number of redeterminisations, there is always at least one spy who went on that mission
        ResGameState state = (ResGameState) resistance.getGameState();
        state.setPlayerIdentity(0, RESISTANCE);
        state.setPlayerIdentity(1, SPY);
        state.setPlayerIdentity(2, SPY);
        state.setPlayerIdentity(3, RESISTANCE);
        state.setPlayerIdentity(4, RESISTANCE);
        state.setMissionData(Arrays.asList(0, 1, 2), 1);
        // we now redeterminise for player 0 (who was on the team, but not a spy)
        int[] spyCounts = new int[5];
        for (int i = 0; i < 100; i++) {
            ResGameState copyState = (ResGameState) state.copy(0);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(0).get(2).cardType);
            if (copyState.getPlayerHandCards().get(1).get(2).cardType == SPY) spyCounts[1]++;
            if (copyState.getPlayerHandCards().get(2).get(2).cardType == SPY) spyCounts[2]++;
            if (copyState.getPlayerHandCards().get(3).get(2).cardType == SPY) spyCounts[3]++;
            if (copyState.getPlayerHandCards().get(4).get(2).cardType == SPY) spyCounts[4]++;
            // at least one person on the team must be a spy
            assertTrue(
                    copyState.getPlayerHandCards().get(1).get(2).cardType == SPY ||
                            copyState.getPlayerHandCards().get(2).get(2).cardType == SPY);
        }
        // and all other players might be the spy
        assertTrue(spyCounts[1] > 10);
        assertTrue(spyCounts[2] > 10);
        assertTrue(spyCounts[3] > 5);
        assertTrue(spyCounts[4] > 5);
        assertTrue(spyCounts[1] + spyCounts[2] > spyCounts[3] + spyCounts[4]);

        // and then for player 1 (who is a spy)
        for (int i = 0; i < 100; i++) {
            ResGameState copyState = (ResGameState) state.copy(1);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(0).get(2).cardType);
            assertEquals(SPY, copyState.getPlayerHandCards().get(1).get(2).cardType);
            assertEquals(SPY, copyState.getPlayerHandCards().get(2).get(2).cardType);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(3).get(2).cardType);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(4).get(2).cardType);
        }

        // and then for player 4, who was not on the team
        spyCounts = new int[5];
        for (int i = 0; i < 100; i++) {
            ResGameState copyState = (ResGameState) state.copy(4);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(4).get(2).cardType);
            if (copyState.getPlayerHandCards().get(1).get(2).cardType == SPY) spyCounts[1]++;
            if (copyState.getPlayerHandCards().get(2).get(2).cardType == SPY) spyCounts[2]++;
            if (copyState.getPlayerHandCards().get(3).get(2).cardType == SPY) spyCounts[3]++;
            if (copyState.getPlayerHandCards().get(0).get(2).cardType == SPY) spyCounts[0]++;
            // at least one person on the team must be a spy
            assertTrue(
                    copyState.getPlayerHandCards().get(0).get(2).cardType == SPY ||
                            copyState.getPlayerHandCards().get(1).get(2).cardType == SPY ||
                            copyState.getPlayerHandCards().get(2).get(2).cardType == SPY);
        }
        // and all other players might be the spy
        assertTrue(spyCounts[0] > 10);
        assertTrue(spyCounts[1] > 10);
        assertTrue(spyCounts[2] > 10);
        assertTrue(spyCounts[3] > 10);
    }

    @Test
    public void redeterminisationKeepsSpiesConsistentWithFailedMissionsII() {
        // We set two missions with a failure (with one overlapping agent)
        // then confirm that over a number of redeterminisations, there is always at least one spy who went on each mission
        // and that not all of these incriminate the overlapping agent
        // We set one mission with a failure
        // then confirm that over a number of redeterminisations, there is always at least one spy who went on that mission
        ResGameState state = (ResGameState) resistance.getGameState();
        state.setPlayerIdentity(0, RESISTANCE);
        state.setPlayerIdentity(1, SPY);
        state.setPlayerIdentity(2, RESISTANCE);
        state.setPlayerIdentity(3, RESISTANCE);
        state.setPlayerIdentity(4, SPY);
        state.setMissionData(Arrays.asList(0, 1, 2), 1);
        state.setMissionData(Arrays.asList(1, 2, 3), 1);
        // we now redeterminise for player 0 (who was on one team, but not a spy)
        int[] spyCounts = new int[5];
        for (int i = 0; i < 100; i++) {
            ResGameState copyState = (ResGameState) state.copy(0);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(0).get(2).cardType);
            if (copyState.getPlayerHandCards().get(1).get(2).cardType == SPY) spyCounts[1]++;
            if (copyState.getPlayerHandCards().get(2).get(2).cardType == SPY) spyCounts[2]++;
            if (copyState.getPlayerHandCards().get(3).get(2).cardType == SPY) spyCounts[3]++;
            if (copyState.getPlayerHandCards().get(4).get(2).cardType == SPY) spyCounts[4]++;
            // at least one person on the team must be a spy
            assertTrue(
                    copyState.getPlayerHandCards().get(1).get(2).cardType == SPY ||
                            copyState.getPlayerHandCards().get(2).get(2).cardType == SPY);
        }
        // and all other players might be the spy
        assertTrue(spyCounts[1] > 10);
        assertTrue(spyCounts[2] > 10);
        assertTrue(spyCounts[3] > 5);
        assertTrue(spyCounts[4] > 5);
        assertTrue(spyCounts[1] + spyCounts[2] > spyCounts[3] + spyCounts[4]);

    }

    @Test
    public void redeterminisationKeepsSpiesConsistentWithFailedMissionsIII() {
        // We have one mission with a failure; but 2 votes, so two of the members must be spies
        ResGameState state = (ResGameState) resistance.getGameState();
        state.setPlayerIdentity(0, RESISTANCE);
        state.setPlayerIdentity(1, SPY);
        state.setPlayerIdentity(2, RESISTANCE);
        state.setPlayerIdentity(3, RESISTANCE);
        state.setPlayerIdentity(4, SPY);
        state.setMissionData(Arrays.asList(0, 1, 4), 2);
        // we now redeterminise for player 0 (who was on the team, but not a spy)
        // Hence they know everything
        for (int i = 0; i < 100; i++) {
            ResGameState copyState = (ResGameState) state.copy(0);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(0).get(2).cardType);
            assertEquals(SPY, copyState.getPlayerHandCards().get(1).get(2).cardType);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(2).get(2).cardType);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(3).get(2).cardType);
            assertEquals(SPY, copyState.getPlayerHandCards().get(4).get(2).cardType);
        }

        // and then for player 2, who was not on the team
        int[] spyCounts = new int[5];
        for (int i = 0; i < 100; i++) {
            ResGameState copyState = (ResGameState) state.copy(2);
            assertEquals(RESISTANCE, copyState.getPlayerHandCards().get(2).get(2).cardType);
            if (copyState.getPlayerHandCards().get(1).get(2).cardType == SPY) spyCounts[1]++;
            if (copyState.getPlayerHandCards().get(0).get(2).cardType == SPY) spyCounts[0]++;
            if (copyState.getPlayerHandCards().get(3).get(2).cardType == SPY) spyCounts[3]++;
            if (copyState.getPlayerHandCards().get(4).get(2).cardType == SPY) spyCounts[4]++;
            // at least two people on the team must be spies
            int nSpies = IntStream.of(0, 1, 4).map(j -> copyState.getPlayerHandCards().get(j).get(2).cardType == SPY ? 1 : 0).sum();
            assertEquals(2, nSpies);
        }
        // and all other players might be the spy
        assertTrue(spyCounts[0] > 20);
        assertTrue(spyCounts[1] > 20);
        assertEquals(0, spyCounts[3]);
        assertTrue(spyCounts[4] > 20);

    }

    @Test
    public void historyOfMissionSuccessesIsCorrect() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        List<Integer> team = new ArrayList<>(state.getFinalTeam());
        do {
            fm.next(state, new ResVoting(state.getCurrentPlayer(), ResPlayerCards.CardType.Yes));
        } while (state.getGamePhase() == ResGameState.ResGamePhase.MissionVote);
        assertEquals(1, state.getMissionsSoFar());
        assertEquals(team, state.getHistoricTeam(1));
        assertTrue(state.getHistoricMissionSuccess(1));
        assertEquals(0, state.getHistoricNoVotes(1));

        progressGame(state, ResGameState.ResGamePhase.MissionVote);
        team = new ArrayList<>(state.getFinalTeam());
        do {
            fm.next(state, new ResVoting(state.getCurrentPlayer(), ResPlayerCards.CardType.No));
        } while (state.getGamePhase() == ResGameState.ResGamePhase.MissionVote);
        assertEquals(2, state.getMissionsSoFar());
        assertNotEquals(team, state.getHistoricTeam(1));
        assertEquals(team, state.getHistoricTeam(2));
        assertTrue(state.getHistoricMissionSuccess(1));
        assertFalse(state.getHistoricMissionSuccess(2));
        assertEquals(0, state.getHistoricNoVotes(1));
        assertEquals(3, state.getHistoricNoVotes(2));
    }
}

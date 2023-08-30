package games.resistance;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGamePhase;
import games.GameType;
import games.resistance.components.ResPlayerCards;
import games.resistance.ResGameState;
import games.resistance.ResForwardModel;
import games.resistance.ResParameters;
import games.resistance.actions.ResMissionVoting;
import games.resistance.actions.ResVoting;
import games.resistance.actions.ResTeamBuilding;
import games.resistance.actions.ResWait;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;

import static org.junit.Assert.*;


public class TestResistance {

    public int atLeastOneHandRearranged = 0;
    Game resistance;
    List<AbstractPlayer> players;
    ResForwardModel fm = new ResForwardModel();
    RandomPlayer rnd = new RandomPlayer();


    private void progressGame(ResGameState state, ResGameState.ResGamePhase requiredGamePhase, int playerTurn) {
            while (state.getGamePhase() != requiredGamePhase && state.getGameStatus() != CoreConstants.GameResult.GAME_END)
            {
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
            }
            while (state.getCurrentPlayer() != playerTurn && state.getGameStatus() != CoreConstants.GameResult.GAME_END)
            {
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
            }
    }

    private void progressGameOneRound(ResGameState state) {
        while (state.getGamePhase() != ResGameState.ResGamePhase.MissionVote && state.getGameStatus() != CoreConstants.GameResult.GAME_END)
        {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
        while (state.getCurrentPlayer() != state.getNPlayers()-1 && state.getGameStatus() != CoreConstants.GameResult.GAME_END)
        {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
    }
    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        resistance = GameType.Resistance.createGameInstance(5, 34, new ResParameters(-274));
        resistance.reset(players);
    }

    @Test
    public void checkingActionsForFirstPhaseTest() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam,0);
        if(state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getLeaderID() != state.getCurrentPlayer()) {
                assertEquals(actions.size(),1);
                assertEquals(actions.get(0).getClass(), ResWait.class);}
            else{int[] players = new int[state.getNPlayers()];
                for (int i = 0; i < state.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()]);
                assertEquals(choiceOfTeams.size(), actions.size());
                for (int i = 0; i < actions.size(); i++) {
                    assertEquals(actions.get(i).getClass(), ResTeamBuilding.class);}}}
        progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam,1);
        if(state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getLeaderID() != state.getCurrentPlayer()) {
                assertEquals(actions.size(),1);
                assertEquals(actions.get(0).getClass(), ResWait.class);}
            else{int[] players = new int[state.getNPlayers()];
                for (int i = 0; i < state.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()]);
                assertEquals(choiceOfTeams.size(), actions.size());
                for (int i = 0; i < actions.size(); i++) {
                    assertEquals(actions.get(i).getClass(), ResTeamBuilding.class);}}}
        progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam,2);
        if(state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getLeaderID() != state.getCurrentPlayer()) {
                assertEquals(actions.size(),1);
                assertEquals(actions.get(0).getClass(), ResWait.class);}
            else{int[] players = new int[state.getNPlayers()];
                for (int i = 0; i < state.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()]);
                assertEquals(choiceOfTeams.size(), actions.size());
                for (int i = 0; i < actions.size(); i++) {
                    assertEquals(actions.get(i).getClass(), ResTeamBuilding.class);}}}
        progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam,3);
        if(state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getLeaderID() != state.getCurrentPlayer()) {
                assertEquals(actions.size(),1);
                assertEquals(actions.get(0).getClass(), ResWait.class);}
            else{int[] players = new int[state.getNPlayers()];
                for (int i = 0; i < state.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()]);
                assertEquals(choiceOfTeams.size(), actions.size());
                for (int i = 0; i < actions.size(); i++) {
                    assertEquals(actions.get(i).getClass(), ResTeamBuilding.class);}}}
        progressGame(state, ResGameState.ResGamePhase.LeaderSelectsTeam,4);
        if(state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getLeaderID() != state.getCurrentPlayer()) {
                assertEquals(actions.size(),1);
                assertEquals(actions.get(0).getClass(), ResWait.class);}
            else{int[] players = new int[state.getNPlayers()];
                for (int i = 0; i < state.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()]);
                assertEquals(choiceOfTeams.size(), actions.size());
                for (int i = 0; i < actions.size(); i++) {
                    assertEquals(actions.get(i).getClass(), ResTeamBuilding.class);}}}
    }

    @Test
    public void checkingActionsForSecondPhaseTest() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote,0);
        if(state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote )
        {List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(actions.get(0).getClass(), ResVoting.class);}
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote,1);
        if(state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote )
        {List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(actions.get(0).getClass(), ResVoting.class);}
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote,2);
        if(state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote )
        {List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(actions.get(0).getClass(), ResVoting.class);}
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote,3);
        if(state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote )
        {List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(actions.get(0).getClass(), ResVoting.class);}
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote,4);
        if(state.getGamePhase() == ResGameState.ResGamePhase.TeamSelectionVote )
        {List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(actions.get(0).getClass(), ResVoting.class);}
    }

    @Test
    public void checkingActionsForThirdPhaseTest() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.MissionVote,0);
        if(state.getGamePhase() == ResGameState.ResGamePhase.MissionVote){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getFinalTeam().contains(state.getCurrentPlayer())) {assertEquals(actions.get(0).getClass(), ResMissionVoting.class);}
            else {assertEquals(actions.get(0).getClass(), ResWait.class);}}
        progressGame(state, ResGameState.ResGamePhase.MissionVote,1);
        if(state.getGamePhase() == ResGameState.ResGamePhase.MissionVote){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getFinalTeam().contains(state.getCurrentPlayer())) {assertEquals(actions.get(0).getClass(), ResMissionVoting.class);}
            else {assertEquals(actions.get(0).getClass(), ResWait.class);}}
        progressGame(state, ResGameState.ResGamePhase.MissionVote,2);
        if(state.getGamePhase() == ResGameState.ResGamePhase.MissionVote){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getFinalTeam().contains(state.getCurrentPlayer())) {assertEquals(actions.get(0).getClass(), ResMissionVoting.class);}
            else {assertEquals(actions.get(0).getClass(), ResWait.class);}}
        progressGame(state, ResGameState.ResGamePhase.MissionVote,3);
        if(state.getGamePhase() == ResGameState.ResGamePhase.MissionVote){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getFinalTeam().contains(state.getCurrentPlayer())) {assertEquals(actions.get(0).getClass(), ResMissionVoting.class);}
            else {assertEquals(actions.get(0).getClass(), ResWait.class);}}
        progressGame(state, ResGameState.ResGamePhase.MissionVote,4);
        if(state.getGamePhase() == ResGameState.ResGamePhase.MissionVote){
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if(state.getFinalTeam().contains(state.getCurrentPlayer())) {assertEquals(actions.get(0).getClass(), ResMissionVoting.class);}
            else {assertEquals(actions.get(0).getClass(), ResWait.class);}}
    }
    @Test
    public void checkingPhaseTransitionLeaderToVote()
    {
        ResGameState state = (ResGameState) resistance.getGameState();

        progressGame(state,ResGameState.ResGamePhase.LeaderSelectsTeam, state.getNPlayers()-1);
        IGamePhase previousGamePhase = state.getGamePhase();

        assertEquals(state.getGamePhase(),ResGameState.ResGamePhase.LeaderSelectsTeam);
        assertEquals(state.getNPlayers()-1,state.getCurrentPlayer() );
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        assertNotEquals(previousGamePhase,state.getGamePhase());
        assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.TeamSelectionVote);

    }
@Test
    public void checkingPhaseTransitionVoteToMissionVote()
    {
        ResGameState state = (ResGameState) resistance.getGameState();

        progressGame(state,ResGameState.ResGamePhase.TeamSelectionVote, state.getNPlayers()-1);
        IGamePhase previousGamePhase = state.getGamePhase();

        assertEquals(state.getGamePhase(),ResGameState.ResGamePhase.TeamSelectionVote);
        assertEquals(state.getNPlayers()-1,state.getCurrentPlayer() );
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        assertNotEquals(previousGamePhase,state.getGamePhase());
        if(state.getGamePhase() == ResGameState.ResGamePhase.MissionVote || state.getGamePhase() == ResGameState.ResGamePhase.LeaderSelectsTeam )
        {
            return;
        }
        else
        {
            {throw new AssertionError("Neither condition is met.");}
        }
    }

    @Test
    public void checkingPhaseTransitionMissionVoteToLeader()
    {
        ResGameState state = (ResGameState) resistance.getGameState();


            progressGame(state,ResGameState.ResGamePhase.MissionVote, state.getNPlayers()-1);
            IGamePhase previousGamePhase = state.getGamePhase();

            if(CoreConstants.GameResult.GAME_END != state.getGameStatus()) {
                assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.MissionVote);
                assertEquals(state.getNPlayers() - 1, state.getCurrentPlayer());
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

                assertNotEquals(previousGamePhase, state.getGamePhase());
                assertEquals(state.getGamePhase(), ResGameState.ResGamePhase.LeaderSelectsTeam);
            }


    }

    @Test
    public void checkingGameOverWithMissionsCriteria() {

        ResGameState state = (ResGameState) resistance.getGameState();
        progressGameOneRound(state);
        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 1);
            progressGameOneRound(state);
        }
        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 2);
            progressGameOneRound(state);
        }
        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
            assertEquals(state.getGameBoardValues().size(), 3);
        }

        if ( Collections.frequency(state.getGameBoardValues(), true) == 3 || Collections.frequency(state.getGameBoardValues(), false) == 3)
        {
            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        }

        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END ){progressGameOneRound(state);}
        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END ) {
            assertEquals(state.getGameBoardValues().size(), 4);
            if ( Collections.frequency(state.getGameBoardValues(), true) == 3 || Collections.frequency(state.getGameBoardValues(), false) == 3) {
                assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
            }
        }

        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END ) {progressGameOneRound(state);}
        if(state.getGameStatus() != CoreConstants.GameResult.GAME_END ) {
            assertEquals(state.getGameBoardValues().size(), 5);
            if (Collections.frequency(state.getGameBoardValues(), true) == 3 || Collections.frequency(state.getGameBoardValues(), false) == 3) {
                assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
            }
        }
    }
    @Test
    public void checkingGameOverWithFailedVotesCriteria() {

        ResGameState state = (ResGameState) resistance.getGameState();
        while ( state.getFailedVoteCounter() != 5 && state.getGameStatus() != CoreConstants.GameResult.GAME_END)
        {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
        //fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        if(state.getFailedVoteCounter() == 5){

            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        }
    }

    @Test
    public void checkingHandInitialisation() {

        ResGameState state = (ResGameState) resistance.getGameState();
        for (int i = 0; i < state.getNPlayers(); i++) {
            assertEquals(state.getPlayerHandCards().get(i).getSize(), 3);
            if (state.getPlayerHandCards().get(i).get(2).cardType != ResPlayerCards.CardType.RESISTANCE && state.getPlayerHandCards().get(i).get(2).cardType != ResPlayerCards.CardType.SPY)
            {throw new AssertionError("last card isn't SPY or RESISTANCE");}
            if (state.getPlayerHandCards().get(i).get(0).cardType != ResPlayerCards.CardType.No && state.getPlayerHandCards().get(i).get(0).cardType != ResPlayerCards.CardType.Yes)
            {throw new AssertionError("first card isn't yes or no");}
            if (state.getPlayerHandCards().get(i).get(1).cardType != ResPlayerCards.CardType.Yes && state.getPlayerHandCards().get(i).get(1).cardType != ResPlayerCards.CardType.No)
            {throw new AssertionError("second card isn't yes or no");}
        }

    }
    @Test
    public void checkingCorrectSpyToResistanceRatio()
    {
        ResGameState state = (ResGameState) resistance.getGameState();
        int spyCount = 0;
        int resistanceCount = 0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            assertEquals(state.getPlayerHandCards().get(i).getSize(), 3);
            if (state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.RESISTANCE)
            {resistanceCount += 1;}
            if (state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.SPY)
            {spyCount += 1;}
        }
        assertEquals(resistanceCount, state.factions[0]);
        assertEquals(spyCount, state.factions[1]);
    }

    @Test
    public void checkingWinnersAreCorrect()
    {
        ResGameState state = (ResGameState) resistance.getGameState();
        while (CoreConstants.GameResult.GAME_END != state.getGameStatus())
        {
            progressGameOneRound(state);
        }

        for (int i = 0; i < state.getNPlayers()-1; i++) {
            if(state.getWinningTeam() == 0)
            {
                if(state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.RESISTANCE)
                {assertEquals( CoreConstants.GameResult.WIN_GAME,state.getPlayerResults()[i]);}
                if(state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.SPY)
                {assertEquals(CoreConstants.GameResult.LOSE_GAME,state.getPlayerResults()[i] );}
            }

            if(state.getWinningTeam() == 1)
            {
                if(state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.RESISTANCE)
                {assertEquals(CoreConstants.GameResult.LOSE_GAME,state.getPlayerResults()[i] );}
                if(state.getPlayerHandCards().get(i).get(2).cardType == ResPlayerCards.CardType.SPY)
                {assertEquals( CoreConstants.GameResult.WIN_GAME,state.getPlayerResults()[i]);}
            }
        }
    }

    @Test
    public void checkingLeaderMovesAfterFailedTeamVote() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote, state.getNPlayers() -1);
        int previousLeader = state.getLeaderID();

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        if(state.getVoteSuccess() == false) {assertNotEquals( previousLeader,state.getLeaderID());}
        else {assertEquals( previousLeader,state.getLeaderID());}
    }

    @Test
    public void checkingTeamSize() {
        ResGameState state = (ResGameState) resistance.getGameState();

        //Checking Round 0
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote, 0);
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()],state.getFinalTeam().size());

        //Checking Round 1
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote, 1);
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()],state.getFinalTeam().size());

        //Checking Round 2
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote, 2);
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()],state.getFinalTeam().size());

        //Checking Round 3
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote, 3);
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()],state.getFinalTeam().size());

        //Checking Round 4
        progressGame(state, ResGameState.ResGamePhase.TeamSelectionVote, 4);
        assertEquals(state.gameBoard.getMissionSuccessValues()[state.getRoundCounter()],state.getFinalTeam().size());
    }

    @Test
    public void checkingLeaderMovesAfterRoundEnds() {
        ResGameState state = (ResGameState) resistance.getGameState();
        progressGame(state, ResGameState.ResGamePhase.MissionVote, state.getNPlayers() -1);
        int previousLeader = state.getLeaderID();
        int previousRound = state.getRoundCounter();

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        if(previousRound != state.getRoundCounter()) {assertNotEquals( previousLeader,state.getLeaderID());}
    }

    @Test
    public void checkingSpiesKnowEveryonesCards() {
        ResGameState state = (ResGameState) resistance.getGameState();
        List<ResPlayerCards.CardType> listOfIdentityCards =  new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            listOfIdentityCards.add(state.getPlayerHandCards().get(i).get(2).cardType);
        }

        //Checking Player 0
        ResGameState playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 1
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 2
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 3
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 4
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingSpiesKnowEveryonesCardsMethod(state, playerState,listOfIdentityCards);
    }

    @Test
    public void checkingResistanceDontKnowEveryonesCards() {
        //This Test May Fail Due To A Random Arrangement Of Hidden Cards Aligning With Actual Identity Cards, I have minimised this
        // Chance by checking multiple Resistance members views of other players hands
        // If atleast one hand is not equal the default copy/gamestate, the hand redeterminisation works
        ResGameState state = (ResGameState) resistance.getGameState();

        List<ResPlayerCards.CardType> listOfIdentityCards =  new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            listOfIdentityCards.add(state.getPlayerHandCards().get(i).get(2).cardType);
        }

        //Checking Player 0
        ResGameState playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state,playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 1
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state,playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 2
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state,playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 3
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state,playerState,listOfIdentityCards);
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        //Checking Player 4
        playerState = (ResGameState) state.copy(state.getCurrentPlayer());
        checkingResistanceDontKnowEveryonesCardsMethod(state,playerState,listOfIdentityCards);

        assertNotEquals(atLeastOneHandRearranged, 0);
    }


private void checkingSpiesKnowEveryonesCardsMethod(ResGameState state,ResGameState playerState, List<ResPlayerCards.CardType> listOfIdentityCards)
{
    if(state.getPlayerHandCards().get(state.getCurrentPlayer()).get(2).cardType == ResPlayerCards.CardType.SPY)
    {
        List<ResPlayerCards.CardType> listOfSpyKnownIdentityCards =  new ArrayList<>();
        for (int j = 0; j < state.getNPlayers(); j++) {
            listOfSpyKnownIdentityCards.add(playerState.getPlayerHandCards().get(j).get(2).cardType);
        }
        assertEquals(listOfSpyKnownIdentityCards,listOfIdentityCards);
    }}

    private void checkingResistanceDontKnowEveryonesCardsMethod(ResGameState state,ResGameState playerState, List<ResPlayerCards.CardType> listOfIdentityCards)
    {
        if (state.getPlayerHandCards().get(state.getCurrentPlayer()).get(2).cardType == ResPlayerCards.CardType.RESISTANCE) {
            List<ResPlayerCards.CardType> listOfResistanceKnownIdentityCards = new ArrayList<>();
            for (int j = 0; j < state.getNPlayers(); j++) {
                listOfResistanceKnownIdentityCards.add(playerState.getPlayerHandCards().get(j).get(2).cardType);
            }

            if(listOfResistanceKnownIdentityCards != listOfIdentityCards) {atLeastOneHandRearranged += 1;}
        }
    }

    //
//    @Test
//    public void testTeamVoteNumber() {
//        SHGameState state = (SHGameState) resistance.getGameState();
//        fm.next(state, new RollDice());
//        do {
//            fm.next(state, fm.computeAvailableActions(state).get(0));
//            fm.next(state, new RollDice());
//            // we keep rolling dice until we go bust
//        } while (!fm.computeAvailableActions(state).get(0).equals(new Pass(true)));
//        fm.next(state, new Pass(true));
//        assertEquals(CantStopGamePhase.Decision, state.getGamePhase());
//    }


}

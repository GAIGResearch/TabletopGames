package games.resistance;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.resistance.actions.*;
import games.resistance.components.ResPlayerCards;
import utilities.Utils;
import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.GameResult.*;
import static games.resistance.ResGameState.ResGamePhase.*;


/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class ResForwardModel extends StandardForwardModel {
    public int counter = 0;
    boolean haveBeenInLoop = false;
    boolean roundEnded = false;

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        // TODO: perform initialization of variables and game setup
        Random rnd = new Random(firstState.getGameParameters().getRandomSeed());
        ResGameState resgs = (ResGameState)firstState;
        ResParameters resp = (ResParameters)firstState.getGameParameters();
        System.out.println(firstState.getNPlayers() +"firststate");
        resgs.votingChoice = new ArrayList<>(firstState.getNPlayers());
        resgs.missionVotingChoice = new ArrayList<>(firstState.getNPlayers());
        resgs.gameBoardValues = new ArrayList<>(5);
        resgs.failedVoteCounter = 0;
        resgs.playerHandCards = new ArrayList<>(firstState.getNPlayers());
        resgs.gameBoard = resp.getPlayerBoard(firstState.getNPlayers());
        if(resgs.gameBoard == null)
        {throw new AssertionError("GameBoard shouldn't be null");};
        resgs.factions = resp.getFactions(firstState.getNPlayers());

        int spyCounter = 0;
        int resistanceCounter = 0;
        for (int i = 0; i < firstState.getNPlayers(); i++) {
            resgs.votingChoice.add(new ArrayList<>());
            resgs.missionVotingChoice.add(new ArrayList<>());
            boolean[] visible = new boolean[firstState.getNPlayers()];
            visible[i] = false;
            PartialObservableDeck<ResPlayerCards> playerCards = new PartialObservableDeck<>("Player Cards", visible);


            // Add identity cards to hand
            if (rnd.nextInt(2) == 0 && spyCounter != resgs.factions[1]) {
                ResPlayerCards SPY = new ResPlayerCards(ResPlayerCards.CardType.SPY);
                SPY.setOwnerId(i);
                playerCards.add(SPY);
                spyCounter += 1;
            }
            else if (resistanceCounter != resgs.factions[0])
            {
                ResPlayerCards resistor = new ResPlayerCards(ResPlayerCards.CardType.RESISTANCE);
                resistor.setOwnerId(i);
                playerCards.add(resistor);
                resistanceCounter += 1;
            }

            else if (spyCounter != resgs.factions[1] && resistanceCounter == resgs.factions[0] )
            {
                ResPlayerCards SPY = new ResPlayerCards(ResPlayerCards.CardType.SPY);
                SPY.setOwnerId(i);
                playerCards.add(SPY);
                spyCounter += 1;
            }
            else if (spyCounter == resgs.factions[1] && resistanceCounter != resgs.factions[0] )
            {
                ResPlayerCards resistor = new ResPlayerCards(ResPlayerCards.CardType.RESISTANCE);
                resistor.setOwnerId(i);
                playerCards.add(resistor);
                resistanceCounter += 1;
            }
            //Add Voting Cards in random order, done so when getting hidden choice at index 0, will be random vote
            if(rnd.nextInt(2) == 0) {
                ResPlayerCards yes = new ResPlayerCards(ResPlayerCards.CardType.Yes);
                yes.setOwnerId(i);
                playerCards.add(yes);

                ResPlayerCards no = new ResPlayerCards(ResPlayerCards.CardType.No);
                no.setOwnerId(i);
                playerCards.add(no);
            }
            else{ResPlayerCards no = new ResPlayerCards(ResPlayerCards.CardType.No);

                no.setOwnerId(i);
                playerCards.add(no);

                ResPlayerCards yes = new ResPlayerCards(ResPlayerCards.CardType.Yes);
                yes.setOwnerId(i);
                playerCards.add(yes);
            }
            resgs.playerHandCards.add(playerCards);
        }

        //Adding leader
        int randomPlayerLeader = rnd.nextInt(resgs.getNPlayers());
        System.out.println("Random Player Leader : " +randomPlayerLeader);

        resgs.leaderID = randomPlayerLeader;
        resgs.setGamePhase(LeaderSelectsTeam);
        resgs.previousGamePhase = LeaderSelectsTeam;

    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {

        ResGameState resgs = (ResGameState) gameState;

        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = resgs.getCurrentPlayer();

        Deck<ResPlayerCards> currentPlayerHand = resgs.getPlayerHandCards().get(currentPlayer);

        if (resgs.getGamePhase() == LeaderSelectsTeam) {
            
            //Leader Creates Team
            System.out.println("Leader Outside Loop | Player ID :" + currentPlayer);
            if (currentPlayer == resgs.leaderID) {
                System.out.println("Leader In Loop | Player ID :" + currentPlayer);
                int[] players = new int[resgs.getNPlayers()];
                for (int i = 0; i < resgs.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, resgs.gameBoard.getMissionSuccessValues()[resgs.getRoundCounter()]);
                for(int[] team : choiceOfTeams) {
                    actions.add(new ResTeamBuilding(currentPlayer, team));
                    if (team.length == 0){throw new AssertionError("Team Size Zero");}
                }
                System.out.println("Action Size of Potential Teams : " + actions.size());
            }
            //Every Other Player Waits
            else{
                actions.add(new ResWait(currentPlayer));
            }
        }


            if(resgs.getGamePhase()== TeamSelectionVote) {

                // All players can do is choose a yes or no card in hand to play.
                actions.add(new ResVoting(currentPlayer, ResPlayerCards.CardType.Yes));
                actions.add(new ResVoting(currentPlayer, ResPlayerCards.CardType.No));

            }

        if(resgs.getGamePhase()== MissionVote) {
            
            if(resgs.finalTeamChoice.contains(currentPlayer)){

                actions.add(new ResMissionVoting(currentPlayer, ResPlayerCards.CardType.Yes));
                actions.add(new ResMissionVoting(currentPlayer, ResPlayerCards.CardType.No));
            }
            else {
                actions.add(new ResWait(currentPlayer));
            }
        }

            return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        ResGameState resgs = (ResGameState) currentState;
        if(resgs.getCurrentPlayer() % 4 == 0) {System.out.println("Current Round : " + resgs.getRoundCounter());}
        System.out.println("Action Taken : " + action.getString(resgs));

        //Leader Selects Team
        if (resgs.getGamePhase() == LeaderSelectsTeam && haveBeenInLoop == false) {
            int turn = resgs.getTurnCounter();

            if ((turn + 1) % (resgs.getNPlayers()) == 0) {
                revealCards(resgs);
                resgs.setGamePhase(TeamSelectionVote);

            }
            else{resgs.previousGamePhase = resgs.getGamePhase();}
            haveBeenInLoop = true;
        }

        if (resgs.getGamePhase() == TeamSelectionVote && haveBeenInLoop == false) {

            int turn = resgs.getTurnCounter();
            if(resgs.teamChoice.size() == 0){throw new AssertionError("Team Choice Size is Zero");}
            if ((turn+1) % resgs.getNPlayers() == 0 && resgs.previousGamePhase == resgs.getGamePhase()) {
            revealCards(resgs);
            if(resgs.failedVoteCounter == 5){
                for (int i = 0; i < resgs.getNPlayers(); i++) {
                    PartialObservableDeck<ResPlayerCards> hand = resgs.playerHandCards.get(i);
                    if (hand.get(2).cardType == ResPlayerCards.CardType.SPY) {
                        resgs.setPlayerResult(CoreConstants.GameResult.WIN, i);
                    } else {
                        resgs.setPlayerResult(CoreConstants.GameResult.LOSE, i);
                    }
                }
                endGame(resgs);
                resgs.winners = 1;
                System.out.println("GAME ENDED BY FAILED TEAMVOTE");
            }

            if(resgs.voteSuccess == true){ resgs.setGamePhase(MissionVote);}
            else{
                resgs.clearCardChoices();
                resgs.clearTeamChoices();
                resgs.clearVoteChoices();

                // CHANGE LEADER
                changeLeader(resgs);

                resgs.setGamePhase(LeaderSelectsTeam);
            }}


            else{resgs.previousGamePhase = resgs.getGamePhase();}
            haveBeenInLoop = true;
        }

        if (resgs.getGamePhase() == MissionVote && haveBeenInLoop == false){
            
            int turn = resgs.getTurnCounter();
            if(resgs.finalTeamChoice.size() == 0) {throw new AssertionError("Final Team Choice Size is Zero");}
            if ((turn + 1) % resgs.getNPlayers() == 0 && resgs.previousGamePhase == resgs.getGamePhase()) {

                revealCards(resgs);
                resgs.clearCardChoices();
                resgs.clearMissionChoices();
                resgs.clearVoteChoices();
                resgs.clearTeamChoices();
                changeLeader(resgs);
                endRound(resgs);
                roundEnded = true;
                _endRound(resgs);

                // Check if the game is over
                int occurrenceCountTrue = Collections.frequency(resgs.gameBoardValues, true);
                int occurrenceCountFalse = Collections.frequency(resgs.gameBoardValues, false);
                System.out.println("Size of gameboard values: " +  resgs.gameBoardValues.size());
                System.out.println("Occurrence True : " + occurrenceCountTrue);
                System.out.println("Occurrence False : " + occurrenceCountFalse);
                if (occurrenceCountTrue == 3) {
                    // Decide winner
                    for (int i = 0; i < resgs.getNPlayers(); i++) {
                        PartialObservableDeck<ResPlayerCards> hand = resgs.playerHandCards.get(i);
                        if (hand.get(2).cardType == ResPlayerCards.CardType.RESISTANCE) {
                            resgs.setPlayerResult(CoreConstants.GameResult.WIN, i);
                        } else {
                            resgs.setPlayerResult(CoreConstants.GameResult.LOSE, i);
                        }
                    }
                    resgs.winners = 0;
                    resgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    endGame(resgs);
                    if(occurrenceCountTrue == 3){ System.out.println("GAME ENDED BY SUCCESSFUL MISSIONS");}

                }

                if (occurrenceCountFalse == 3) {
                    // Decide winner
                    for (int i = 0; i < resgs.getNPlayers(); i++) {
                        PartialObservableDeck<ResPlayerCards> hand = resgs.playerHandCards.get(i);
                        if (hand.get(2).cardType == ResPlayerCards.CardType.SPY) {
                            resgs.setPlayerResult(CoreConstants.GameResult.WIN, i);
                        } else {
                            resgs.setPlayerResult(CoreConstants.GameResult.LOSE, i);
                        }
                    }
                    System.out.println(resgs.getPlayerResults()[0] + "      Player Results");
                    resgs.winners = 1;
                    resgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    endGame(resgs);
                    //roundEnded = true;
                    if(occurrenceCountFalse == 3){ System.out.println("GAME ENDED BY FAILED MISSIONS");}
                    //return;

                }
                if(resgs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
                    resgs.failedVoteCounter = 0;
                    resgs.setGamePhase(LeaderSelectsTeam);
                }
            }

            else{resgs.previousGamePhase = resgs.getGamePhase();}
            haveBeenInLoop = true;
        }
        //End player turn
        if (resgs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && roundEnded == false) {
            endPlayerTurn(resgs);
        }
        haveBeenInLoop = false;
        roundEnded = false;

    }

    void revealCards(ResGameState resgs) {
        if (resgs.getGamePhase() == TeamSelectionVote){

            ArrayList<ResPlayerCards.CardType> allVotes = new ArrayList<>();
        for (int i = 0; i < resgs.getNPlayers(); i++) {
            for (ResVoting cc: resgs.votingChoice.get(i)) {
                allVotes.add(cc.cardType);
                break;
            }
        }

            int occurrenceCount = Collections.frequency(allVotes, ResPlayerCards.CardType.Yes);
            System.out.println("Team Vote :  " + allVotes);
            if(allVotes.contains(ResPlayerCards.CardType.LEADER ) || allVotes.contains(ResPlayerCards.CardType.RESISTANCE ) || allVotes.contains(ResPlayerCards.CardType.SPY ))
            {throw new AssertionError("Incorrect Type in Team Vote");}
            System.out.println("Team Yes Occurrence Count : " + occurrenceCount  );
            if (occurrenceCount > allVotes.size()/2){resgs.voteSuccess = true;}
            else{resgs.voteSuccess = false; resgs.failedVoteCounter += 1;}
        }

        if (resgs.getGamePhase() == LeaderSelectsTeam){
                        for (int[] cc : resgs.teamChoice) {
                            resgs.finalTeamChoice = new ArrayList<>();
                            for (int member : cc){resgs.finalTeamChoice.add(member);}
                        }
                        System.out.println("Final Team :  " + resgs.finalTeamChoice);
        }

        if (resgs.getGamePhase() == MissionVote){
            ArrayList<ResPlayerCards.CardType> allVotes = new ArrayList<>();
            for (int i = 0; i < resgs.getNPlayers(); i++) {
                    for (ResMissionVoting cc : resgs.missionVotingChoice.get(i)) {
                        allVotes.add(cc.cardType);
                        break;
                    }

            }


            System.out.println("Mission Vote :  " + allVotes);
            if(allVotes.contains(ResPlayerCards.CardType.LEADER ) || allVotes.contains(ResPlayerCards.CardType.RESISTANCE ) || allVotes.contains(ResPlayerCards.CardType.SPY ))
            {throw new AssertionError("Incorrect Type in Mission Vote");}
            //System.out.println(allVotes.get(0) == SHPlayerCards.CardType.No);
            int occurrenceCount = Collections.frequency(allVotes, ResPlayerCards.CardType.No);
            System.out.println("Mission No Occurrence Count : " + occurrenceCount  );
            if (occurrenceCount > 0){resgs.gameBoardValues.add(false);}
            else{resgs.gameBoardValues.add(true);}

        }

    }


    public void _endRound(ResGameState resgs) {
        // Apply card end of round rules
//        for (SHPlayerCards.CardType type: SHPlayerCards.CardType.values()) {
//            type.onRoundEnd(resgs);
//        }
        //resgs.setGamePhase(LeaderSelectsTeam);
        //resgs.teamChoice.clear();
        roundEnded = true;


    }
    @Override
    protected void endGame(AbstractGameState gs) {
        gs.setGameStatus(CoreConstants.GameResult.GAME_END);
        if (gs.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gs.getPlayerResults()));
        }
    }
    public void changeLeader(ResGameState resgs) {
        for (int i = 0; i < resgs.getNPlayers(); i++){
            if (i == resgs.leaderID)
            {
                if(i + 1 == resgs.getNPlayers()) {resgs.leaderID = 0;}
                else{resgs.leaderID = i + 1;}
                break;
            }
        }
    }
}

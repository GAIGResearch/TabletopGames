package games.resistance;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.resistance.actions.ResMissionVoting;
import games.resistance.actions.ResTeamBuilding;
import games.resistance.actions.ResVoting;
import games.resistance.components.ResGameBoard;
import games.resistance.components.ResPlayerCards;
import games.sushigo.SGGameState;
//import games.resistance.components.SHGameBoard;

import java.util.*;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class ResGameState extends AbstractGameState {

    int spyCounter = 0;
    int resistanceCounter = 0;
    public int[] factions;
    List<Boolean> gameBoardValues = new ArrayList<>();
    boolean voteSuccess;
    int leaderID;
    int winners = 3; //0 is Resistance , 1 is Spy
    int failedVoteCounter = 0;

    List<List<ResVoting>> votingChoice;

    List<List<ResMissionVoting>> missionVotingChoice;
    List<int[]> teamChoice = new ArrayList<>();
    IGamePhase previousGamePhase = null;


    ArrayList<Integer> finalTeamChoice = new ArrayList<>();

    public enum ResGamePhase implements IGamePhase {
        MissionVote, TeamSelectionVote, LeaderSelectsTeam
    }

    List<PartialObservableDeck<ResPlayerCards>> playerHandCards = new ArrayList<>(10);
    public ResGameBoard gameBoard = new ResGameBoard(new int[nPlayers]);

    @Override
    public int hashCode() {
        int result = super.hashCode() + 31 * Objects.hash(leaderID,playerHandCards,teamChoice, gameBoardValues,
                missionVotingChoice, finalTeamChoice);

        return result;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResGameState)) return false;
        if (!super.equals(o)) return false;
        ResGameState that = (ResGameState) o;
        return
                leaderID == that.leaderID &&
                 Objects.equals(playerHandCards, that.playerHandCards) &&
                Objects.equals(gameBoardValues, that.gameBoardValues) && Objects.equals(missionVotingChoice, that.missionVotingChoice) &&
                Objects.equals(teamChoice, that.teamChoice) &&
                         Objects.equals(votingChoice, that.votingChoice) &&
                Objects.equals(finalTeamChoice, that.finalTeamChoice);

    }

    @Override
    public String toString() {
        return
                leaderID +"leaderID|"+
                gameBoardValues.hashCode() + "gameBoardValues|" +
                        teamChoice.hashCode() + "teamChoice|" +
                        votingChoice.hashCode() + "votingChoice|" +
                        playerHandCards.hashCode() + "|" +
                        missionVotingChoice.hashCode() + "|" +
                        finalTeamChoice.hashCode() + "|" +

                super.hashCode() + "|";
    }



    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */



    public ResGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);


    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        // TODO: replace with game-specific enum value declared in GameType
        return GameType.Resistance;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        // TODO: add all components to the list
        if(gameBoard == null)
        {throw new AssertionError("GameBoard shouldn't be null");};


        return new ArrayList<Component>() {{
            add(gameBoard);
            addAll(playerHandCards);
        }};
    }



    /**
     * <p>Create a deep copy of the game state containing only those components the given player can observe.</p>
     * <p>If the playerID is NOT -1 and If any components are not visible to the given player (e.g. cards in the hands
     * of other players or a face-down deck), then these components should instead be randomized (in the previous examples,
     * the cards in other players' hands would be combined with the face-down deck, shuffled together, and then new cards drawn
     * for the other players).</p>
     * <p>If the playerID passed is -1, then full observability is assumed and the state should be faithfully deep-copied.</p>
     *
     * <p>Make sure the return type matches the class type, and is not AbstractGameState.</p>
     *
     * @param playerId - player observing this game state.
     */

    @Override
    protected ResGameState _copy(int playerId) {

        ResGameState copy = new ResGameState(gameParameters.copy(), getNPlayers());
        copy.gameBoard = gameBoard;
        copy.factions = factions;

        copy.previousGamePhase = previousGamePhase;
        copy.voteSuccess = voteSuccess;
        copy.teamChoice = new ArrayList<>();
        copy.votingChoice = new ArrayList<>();
        copy.missionVotingChoice = new ArrayList<>();
        copy.playerHandCards = new ArrayList<>();
        copy.finalTeamChoice = new ArrayList<>();
        copy.gameBoardValues = new ArrayList<>();

        if (playerId == -1) {
            copy.leaderID = leaderID;
            copy.winners = winners;

            for (int i = 0; i < getNPlayers(); i++) {
                copy.playerHandCards.add(playerHandCards.get(i));
            }

            for(int i = 0; i < gameBoardValues.size(); i++){
                copy.gameBoardValues.add(gameBoardValues.get(i));
            }
            for(int i = 0; i < finalTeamChoice.size(); i++){
                copy.finalTeamChoice.add(finalTeamChoice.get(i));
            }
            for(int i = 0; i < teamChoice.size(); i++){
                copy.teamChoice.add(teamChoice.get(i));
            }

            for (int i = 0; i < getNPlayers(); i++) {

                copy.votingChoice.add(new ArrayList<>(votingChoice.get(i)));

                copy.missionVotingChoice.add(new ArrayList<>(missionVotingChoice.get(i)));
            }


        }
        else {
            spyCounter = 0;
            resistanceCounter = 0;
            for (int i = 0; i < getNPlayers(); i++) {
                //Knowledge of Own Hand/Votes
                if (i == playerId) {
                    copy.leaderID = leaderID;
                    for(int j = 0; j < gameBoardValues.size(); j++){
                        copy.gameBoardValues.add(gameBoardValues.get(j));
                    }
                    for(int j = 0; j < finalTeamChoice.size(); j++){
                        copy.finalTeamChoice.add(finalTeamChoice.get(j));
                    }
                    for(int j = 0; j < teamChoice.size(); j++){
                        copy.teamChoice.add(teamChoice.get(j));
                    }


                    copy.playerHandCards.add(playerHandCards.get(i));
                    copy.leaderID = leaderID;
                    //Checking MissionVote Eligibility
                        copy.votingChoice.add(new ArrayList<>(votingChoice.get(i)));
                        copy.missionVotingChoice.add(new ArrayList<>(missionVotingChoice.get(i)));

                }
                else {
                    //Allowing Spies To Know All Card Types
                    if (playerHandCards.get(playerId).get(playerHandCards.get(playerId).getSize() - 1).cardType == ResPlayerCards.CardType.SPY && i != playerId) {
                        copy.playerHandCards.add(playerHandCards.get(i));

                    } else if (i != playerId) {
                        copy.playerHandCards.add(createHiddenHands(i));


                    }
                }

                if (i != playerId){
                    copy.votingChoice.add(new ArrayList<>());
                    copy.missionVotingChoice.add(new ArrayList<>());}

            }

        }
        return copy;

    }

    public void clearCardChoices() {
        for (int i = 0; i < getNPlayers(); i++) votingChoice.get(i).clear();
    }


    public void addCardChoice(ResVoting ResVoting, int playerId) {
        votingChoice.get(playerId).add(ResVoting);
    }

    public void addMissionChoice(ResMissionVoting ResMissionVoting, int playerId) {
            System.out.println("mission voting choice size      " + missionVotingChoice.size());
            missionVotingChoice.get(playerId).add(ResMissionVoting);

    }

    public void clearMissionChoices() {
        for (int i = 0; i < missionVotingChoice.size(); i++) missionVotingChoice.get(i).clear();

    }

    public List<List<ResVoting>> getvotingChoice() {
        return votingChoice;
    }

    public void clearTeamChoices() {
        for (int i = 0; i < getNPlayers(); i++) teamChoice.clear();
        for (int i = 0; i < getNPlayers(); i++) finalTeamChoice.clear();
    }
    public void clearVoteChoices() {
        for (int i = 0; i < getNPlayers(); i++) votingChoice.get(i).clear();
    }


    public void addTeamChoice(ResTeamBuilding ResTeamBuilding) {
        teamChoice.add(ResTeamBuilding.getTeam());
    }

    public List<int[]> getTeamChoice() {
        return teamChoice;
    }
    /**
     * @param //playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // TODO calculate an approximate value
            return 0;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        // TODO: What is this player's score (if any)?
        return 0;
    }

    public List<PartialObservableDeck<ResPlayerCards>> getPlayerHandCards(){
        return playerHandCards;
    }




    private PartialObservableDeck<ResPlayerCards> createHiddenHands(int i){
        PartialObservableDeck<ResPlayerCards> hiddenPlayerHandCards = new PartialObservableDeck<>("hiddenDeck",i);
        Random rnd = new Random();
        if (rnd.nextInt(2) == 0 && spyCounter != factions[1]) {
            ResPlayerCards SPY = new ResPlayerCards(ResPlayerCards.CardType.SPY);
            SPY.setOwnerId(i);
            hiddenPlayerHandCards.add(SPY);
            spyCounter += 1;
        }
        else if (resistanceCounter != factions[0])
        {
            ResPlayerCards resistor = new ResPlayerCards(ResPlayerCards.CardType.RESISTANCE);
            resistor.setOwnerId(i);
            hiddenPlayerHandCards.add(resistor);
            resistanceCounter += 1;
        }

        else if (spyCounter != factions[1] && resistanceCounter == factions[0])
        {
            ResPlayerCards SPY = new ResPlayerCards(ResPlayerCards.CardType.SPY);
            SPY.setOwnerId(i);
            hiddenPlayerHandCards.add(SPY);
            spyCounter += 1;
        }
        else if (spyCounter == factions[1] && resistanceCounter != factions[0])
        {
            ResPlayerCards resistor = new ResPlayerCards(ResPlayerCards.CardType.RESISTANCE);
            resistor.setOwnerId(i);
            hiddenPlayerHandCards.add(resistor);
            resistanceCounter += 1;
        }
        ResPlayerCards No = new ResPlayerCards(ResPlayerCards.CardType.No);
        No.setOwnerId(i);
        ResPlayerCards Yes = new ResPlayerCards(ResPlayerCards.CardType.Yes);
        No.setOwnerId(i);
        hiddenPlayerHandCards.add(No);
        hiddenPlayerHandCards.add(Yes);


        return hiddenPlayerHandCards;
    }

    public int getLeaderID() {
        return leaderID;
    }
    public ArrayList<Integer> getFinalTeam() {
        return finalTeamChoice;
    }
    public List<Boolean> getGameBoardValues() {
        return gameBoardValues;
    }
    public int getFailedVoteCounter() {
        return failedVoteCounter;
    }
    public int getWinners() {
        return winners;
    }
    public boolean getVoteSuccess() {
        return voteSuccess;
    }

}

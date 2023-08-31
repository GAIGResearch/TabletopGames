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
import org.apache.avro.generic.GenericData;

import java.util.*;

public class ResGameState extends AbstractGameState {

    public int[] factions;
    List<Boolean> gameBoardValues = new ArrayList<>();
    boolean voteSuccess;
    int leaderID;
    int failedVoteCounter = 0;
    Random rnd;

    ResPlayerCards.CardType[] votingChoice;
    ResPlayerCards.CardType[] missionVotingChoice;

    List<Integer> teamChoice;
    IGamePhase previousGamePhase = null;
    List<Integer> finalTeamChoice = new ArrayList<>();
    List<List<Integer>> historicTeams = new ArrayList<>();

    public enum ResGamePhase implements IGamePhase {
        MissionVote, TeamSelectionVote, LeaderSelectsTeam
    }

    List<PartialObservableDeck<ResPlayerCards>> playerHandCards = new ArrayList<>(10);
    public ResGameBoard gameBoard = new ResGameBoard(new int[nPlayers]);

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Objects.hash(leaderID, playerHandCards, gameBoardValues,
                teamChoice, finalTeamChoice, voteSuccess, failedVoteCounter, historicTeams) +
                Arrays.hashCode(factions) + 31 * Arrays.hashCode(votingChoice) + 31 * 31 * Arrays.hashCode(missionVotingChoice);
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
                        Objects.equals(gameBoardValues, that.gameBoardValues) &&
                        Arrays.equals(missionVotingChoice, that.missionVotingChoice) &&
                        Objects.equals(teamChoice, that.teamChoice) &&
                        Arrays.equals(votingChoice, that.votingChoice) &&
                        Objects.equals(finalTeamChoice, that.finalTeamChoice) &&
                        Objects.equals(voteSuccess, that.voteSuccess) &&
                        Objects.equals(failedVoteCounter, that.failedVoteCounter) &&
                        Objects.equals(historicTeams, that.historicTeams) &&
                        Arrays.equals(factions, that.factions);
    }

    @Override
    public String toString() {
        return
                leaderID + "|" +
                        gameBoardValues.hashCode() + "|" +
                        teamChoice.hashCode() + "|" +
                        Arrays.hashCode(votingChoice) + "|" +
                        playerHandCards.hashCode() + "|" +
                        Arrays.hashCode(missionVotingChoice) + "|" +
                        finalTeamChoice.hashCode() + "|" +
                        teamChoice.hashCode() + "|" +
                        voteSuccess + "|" +
                        failedVoteCounter + "|" +
                        Arrays.hashCode(factions) + "|" +
                        historicTeams.hashCode() + "|" +
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
        List<Component> retValue = new ArrayList<>();
        retValue.add(gameBoard);
        retValue.addAll(playerHandCards);
        return retValue;
    }

    @Override
    protected ResGameState _copy(int playerId) {
        ResGameState copy = new ResGameState(gameParameters.copy(), getNPlayers());
        copy.gameBoard = gameBoard;
        copy.factions = factions;

        copy.previousGamePhase = previousGamePhase;
        copy.voteSuccess = voteSuccess;
        copy.failedVoteCounter = failedVoteCounter;
        copy.teamChoice = new ArrayList<>();
        copy.votingChoice = new ResPlayerCards.CardType[getNPlayers()];
        copy.missionVotingChoice = new ResPlayerCards.CardType[getNPlayers()];
        copy.playerHandCards = new ArrayList<>();
        copy.finalTeamChoice = new ArrayList<>();
        copy.gameBoardValues = new ArrayList<>(gameBoardValues);
        copy.rnd = new Random(rnd.nextLong());
        copy.historicTeams = new ArrayList<>(historicTeams);  // we do not need to copy the sub-lists, as they are immutable
        copy.leaderID = leaderID;
        copy.teamChoice = new ArrayList<>(teamChoice);
        copy.finalTeamChoice = new ArrayList<>(finalTeamChoice);

        if (playerId == -1) {
            for (int i = 0; i < getNPlayers(); i++) {
                copy.playerHandCards.add(playerHandCards.get(i));
            }
            for (int i = 0; i < getNPlayers(); i++) {
                copy.votingChoice[i] = votingChoice[i];
                copy.missionVotingChoice[i] = missionVotingChoice[i];
            }
        } else {
            boolean isSpy = playerHandCards.get(playerId).get(2).cardType == ResPlayerCards.CardType.SPY;
            // If the player is a spy, then they know everyone's identity
            // if not, we need to shuffle all the other players
            LinkedList<Boolean> spyAllocation = new LinkedList<>();
            if (!isSpy) {
                spyAllocation = new LinkedList<>(ResParameters.randomiseSpies(factions[1], this, playerId));
            }
            for (int i = 0; i < getNPlayers(); i++) {
                //Knowledge of Own Hand/Votes
                if (i == playerId) {
                    copy.playerHandCards.add(playerHandCards.get(i));
                    //Checking MissionVote Eligibility
                    copy.votingChoice[i] = votingChoice[i];
                    copy.missionVotingChoice[i] = missionVotingChoice[i];
                } else {
                    //Allowing Spies To Know All Card Types
                    if (isSpy) {
                        copy.playerHandCards.add(playerHandCards.get(i)); // refers to whole deck
                    } else {
                        PartialObservableDeck<ResPlayerCards> playerHand = playerHandCards.get(i).copy();
                        ResPlayerCards idCard = new ResPlayerCards(ResPlayerCards.CardType.RESISTANCE);
                        if (spyAllocation.get(i)) {
                            idCard = new ResPlayerCards(ResPlayerCards.CardType.SPY);
                        }
                        idCard.setOwnerId(i);
                        playerHand.remove(2);
                        playerHand.add(idCard, 2);
                        // the other two cards are the voting YES/NO cards
                        copy.playerHandCards.add(playerHand);
                    }
                }
            }
        }
        return copy;

    }

    public void clearCardChoices() {
        votingChoice = new ResPlayerCards.CardType[getNPlayers()];
    }
    public void clearMissionChoices() {
        missionVotingChoice = new ResPlayerCards.CardType[getNPlayers()];
    }
    public void addCardChoice(ResVoting ResVoting, int playerId) {
        votingChoice[playerId] = ResVoting.cardType;
    }
    public void addMissionChoice(ResMissionVoting ResMissionVoting, int playerId) {
        missionVotingChoice[playerId] = ResMissionVoting.cardType;
    }

    public void addTeamChoice(ResTeamBuilding ResTeamBuilding) {
        teamChoice = ResTeamBuilding.getTeam();
    }

    public void clearTeamChoices() {
        teamChoice.clear();
        finalTeamChoice.clear();
    }

    public int getMissionsSoFar() {
        return historicTeams.size();
    }
    public List<Integer> getHistoricTeam(int i) {
        return new ArrayList<>(historicTeams.get(i-1));
    }
    public boolean getHistoricMissionSuccess(int i) {
        return gameBoardValues.get(i-1);
    }
    public List<List<Integer>> getFailedTeams() {
        List<List<Integer>> retValue = new ArrayList<>();
        for (int i = 1; i <= historicTeams.size(); i++) {
            if (!getHistoricMissionSuccess(i)) {
                retValue.add(getHistoricTeam(i));
            }
        }
        return retValue;
    }

    // this method is purely for ease of testing
    public void setMissionData(List<Integer> team, boolean success) {
        historicTeams.add(team);
        gameBoardValues.add(success);
    }
    // for testing only
    public void setPlayerIdentity(int playerID, ResPlayerCards.CardType cardType) {
        playerHandCards.get(playerID).remove(2);
        playerHandCards.get(playerID).add(new ResPlayerCards(cardType), 2);
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        if (isNotTerminal()) {
            return 0;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    public List<PartialObservableDeck<ResPlayerCards>> getPlayerHandCards() {
        return playerHandCards;
    }

    public int getLeaderID() {
        return leaderID;
    }

    public List<Integer> getFinalTeam() {
        return finalTeamChoice;
    }

    public List<Boolean> getGameBoardValues() {
        return gameBoardValues;
    }

    public int getFailedVoteCounter() {
        return failedVoteCounter;
    }

    public boolean getVoteSuccess() {
        return voteSuccess;
    }

}

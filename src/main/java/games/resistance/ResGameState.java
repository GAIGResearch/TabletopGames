package games.resistance;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.resistance.actions.ResTeamBuilding;
import games.resistance.actions.ResVoting;
import games.resistance.components.ResGameBoard;
import games.resistance.components.ResPlayerCards;

import java.util.*;

public class ResGameState extends AbstractGameState {

    public int[] factions;
    List<Boolean> gameBoardValues = new ArrayList<>();
    List<Integer> noVotesPerMission = new ArrayList<>();
    boolean voteSuccess;
    int leaderID;
    int failedVoteCounter = 0;

    ResPlayerCards.CardType[] votingChoice;

    List<Integer> teamChoice;
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
                teamChoice, finalTeamChoice, voteSuccess, failedVoteCounter, historicTeams, noVotesPerMission) +
                Arrays.hashCode(factions) + 31 * Arrays.hashCode(votingChoice);
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
                        Objects.equals(teamChoice, that.teamChoice) &&
                        Arrays.equals(votingChoice, that.votingChoice) &&
                        Objects.equals(finalTeamChoice, that.finalTeamChoice) &&
                        Objects.equals(voteSuccess, that.voteSuccess) &&
                        Objects.equals(failedVoteCounter, that.failedVoteCounter) &&
                        Objects.equals(historicTeams, that.historicTeams) &&
                        Objects.equals(noVotesPerMission, that.noVotesPerMission) &&
                        Arrays.equals(factions, that.factions);
    }

    @Override
    public String toString() {
        return
                leaderID + "|" +
                        gameBoardValues.hashCode() + "|" +
                        Arrays.hashCode(votingChoice) + "|" +
                        playerHandCards.hashCode() + "|" +
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
        nTeams = 2;
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

        copy.voteSuccess = voteSuccess;
        copy.failedVoteCounter = failedVoteCounter;
        copy.gameBoardValues = new ArrayList<>(gameBoardValues);
        copy.historicTeams = new ArrayList<>(historicTeams);  // we do not need to copy the sub-lists, as they are immutable
        copy.noVotesPerMission = new ArrayList<>(noVotesPerMission);
        copy.leaderID = leaderID;
        copy.teamChoice = new ArrayList<>(teamChoice);
        copy.finalTeamChoice = new ArrayList<>(finalTeamChoice);

        copy.votingChoice = votingChoice.clone();
        // the decks are shared with the copy; redeterminise() replaces (rather than alters) those it changes
        copy.playerHandCards = new ArrayList<>(playerHandCards);
        // Hidden information (the other players' identities if we are not a spy, and their votes this turn) is
        // dealt with in redeterminise(), which the superclass calls on the copy when appropriate.
        return copy;
    }

    @Override
    public void redeterminise(int playerId) {
        // A spy knows everyone's identity; a member of the resistance knows only their own, so we reallocate the
        // spies amongst the other players, consistently with the results of previous missions.
        boolean isSpy = playerHandCards.get(playerId).get(2).cardType == ResPlayerCards.CardType.SPY;
        if (!isSpy) {
            List<Boolean> spyAllocation = ResForwardModel.randomiseSpies(factions[1], this, playerId, redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i == playerId)
                    continue;
                PartialObservableDeck<ResPlayerCards> playerHand = playerHandCards.get(i).copy();
                ResPlayerCards idCard = new ResPlayerCards(spyAllocation.get(i) ? ResPlayerCards.CardType.SPY : ResPlayerCards.CardType.RESISTANCE);
                idCard.setOwnerId(i);
                playerHand.remove(2);
                playerHand.add(idCard, 2);
                // the other two cards are the voting YES/NO cards
                playerHandCards.set(i, playerHand);
            }
        }

        // Hide the votes the other players have already cast this turn, but keep our own:
        // getCurrentSimultaneousPlayers() decides who still has to vote from votingChoice, so
        // losing our own vote here would have us asked to vote a second time.
        for (int p = 0; p < getNPlayers(); p++)
            if (p != playerId)
                votingChoice[p] = null;

        // Everyone still to vote does so at once, so each of them sees themselves as the current player.
        // This is what the 2-argument computeAvailableActions() reads.
        if (isNotTerminal() && getPlayersStillToVote().contains(playerId))
            setTurnOwner(playerId);
    }

    /**
     * The players who still have to vote this turn: during TeamSelectionVote everyone who has not yet voted,
     * and during MissionVote the members of the mission team who have not yet voted.
     * Empty during LeaderSelectsTeam, which is a decision for the leader alone.
     */
    public List<Integer> getPlayersStillToVote() {
        List<Integer> retValue = new ArrayList<>();
        if (getGamePhase() == ResGamePhase.TeamSelectionVote) {
            for (int p = 0; p < getNPlayers(); p++)
                if (votingChoice[p] == null)
                    retValue.add(p);
        } else if (getGamePhase() == ResGamePhase.MissionVote) {
            for (int p : finalTeamChoice)
                if (votingChoice[p] == null)
                    retValue.add(p);
        }
        return retValue;
    }

    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        if (isActionInProgress() || !isNotTerminal() || getGamePhase() == ResGamePhase.LeaderSelectsTeam) {
            return super.getCurrentSimultaneousPlayers();
        }
        List<Integer> toVote = getPlayersStillToVote();
        if (toVote.isEmpty()) {
            // every eligible player has voted, so the forward model should already have
            // resolved the vote and cleared the choices. Say so loudly rather than return nobody.
            throw new AssertionError("All players have voted but the vote has not been resolved");
        }
        return toVote;
    }

    public void clearVoteChoices() {
        votingChoice = new ResPlayerCards.CardType[getNPlayers()];
    }
    public void addVoteChoice(ResVoting ResVoting, int playerId) {
        votingChoice[playerId] = ResVoting.cardType;
    }

    public void addTeamChoice(ResTeamBuilding ResTeamBuilding) {
        teamChoice = ResTeamBuilding.getTeam();
    }

    /**
     * Returns 0 if the player is a resistance member, 1 if the player is a spy.
     */
    @Override
    public int getTeam(int player) {
        ResPlayerCards.CardType id = playerHandCards.get(player).get(2).cardType;
        return (id == ResPlayerCards.CardType.SPY) ? 1 : 0;
    }

    public void clearTeamChoices() {
        teamChoice.clear();
        finalTeamChoice.clear();
    }

    /**
     * The number of missions already played.
     */
    public int getMissionsSoFar() {
        return historicTeams.size();
    }

    /**
     * Returns the playerIDs of the team that went on the ith mission.
     */
    public List<Integer> getHistoricTeam(int i) {
        return new ArrayList<>(historicTeams.get(i-1));
    }

    /**
     * Returns the result of the ith mission; true if successful, false if failed.
     */
    public boolean getHistoricMissionSuccess(int i) {
        return gameBoardValues.get(i-1);
    }

    /**
     * Returns the number of failed votes on the ith mission.
     */
    public int getHistoricNoVotes(int i) {
        return noVotesPerMission.get(i-1);
    }
    // this method is purely for ease of testing
    public void setMissionData(List<Integer> team, int noVotes) {
        historicTeams.add(team);
        gameBoardValues.add(noVotes == 0);
        noVotesPerMission.add(noVotes);
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

    // current leader who is selecting the team
    public int getLeaderID() {
        return leaderID;
    }

    // The final team selected for the mission
    public List<Integer> getFinalTeam() {
        return finalTeamChoice;
    }

    // The list of
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

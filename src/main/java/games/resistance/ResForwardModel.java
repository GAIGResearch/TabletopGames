package games.resistance;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.resistance.actions.*;
import games.resistance.components.ResPlayerCards;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult.*;
import static evaluation.metrics.Event.GameEvent.GAME_EVENT;
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
        ResGameState resgs = (ResGameState) firstState;
        ResParameters resp = (ResParameters) firstState.getGameParameters();
        resgs.votingChoice = new ResPlayerCards.CardType[firstState.getNPlayers()];
        resgs.gameBoardValues = new ArrayList<>(5);
        resgs.failedVoteCounter = 0;
        resgs.playerHandCards = new ArrayList<>(firstState.getNPlayers());
        resgs.gameBoard = resp.getPlayerBoard(firstState.getNPlayers());
        resgs.historicTeams = new ArrayList<>();
        resgs.noVotesPerMission = new ArrayList<>();
        resgs.teamChoice = new ArrayList<>();
        if (resgs.gameBoard == null) {
            throw new AssertionError("GameBoard shouldn't be null");
        }
        resgs.factions = resp.getFactions(firstState.getNPlayers());

        List<Boolean> spies = ResForwardModel.randomiseSpies(resgs.factions[1], resgs, -1, firstState.getRnd());
        for (int i = 0; i < firstState.getNPlayers(); i++) {
            boolean[] visible = new boolean[firstState.getNPlayers()];
            visible[i] = false;
            PartialObservableDeck<ResPlayerCards> playerCards = new PartialObservableDeck<>("Player Cards", i, visible);
            if (spies.get(i)) {
                ResPlayerCards idCard = new ResPlayerCards(ResPlayerCards.CardType.SPY);
                idCard.setOwnerId(i);
                playerCards.add(idCard);
            } else {
                ResPlayerCards idCard = new ResPlayerCards(ResPlayerCards.CardType.RESISTANCE);
                idCard.setOwnerId(i);
                playerCards.add(idCard);
            }

            //Add Voting Cards
            ResPlayerCards yes = new ResPlayerCards(ResPlayerCards.CardType.Yes);
            yes.setOwnerId(i);
            playerCards.add(yes);

            ResPlayerCards no = new ResPlayerCards(ResPlayerCards.CardType.No);
            no.setOwnerId(i);
            playerCards.add(no);

            resgs.playerHandCards.add(playerCards);
        }

        resgs.leaderID = 0;
        resgs.setGamePhase(LeaderSelectsTeam);
        resgs.setTurnOwner(resgs.leaderID);
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {

        ResGameState resgs = (ResGameState) gameState;

        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = resgs.getCurrentPlayer();

        if (resgs.getGamePhase() == LeaderSelectsTeam) {

            //Leader Creates Team
            if (currentPlayer == resgs.leaderID) {
                int[] players = new int[resgs.getNPlayers()];
                for (int i = 0; i < resgs.getNPlayers(); i++) {
                    players[i] = i;
                }
                ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, resgs.gameBoard.getMissionSuccessValues()[resgs.getRoundCounter()]);
                for (int[] team : choiceOfTeams) {
                    actions.add(new ResTeamBuilding(currentPlayer, team));
                    if (team.length == 0) {
                        throw new AssertionError("Team Size Zero");
                    }
                }
            }
        } else if (resgs.getGamePhase() == TeamSelectionVote) {

            // All players can do is choose a yes or no card in hand to play.
            actions.add(new ResVoting(currentPlayer, ResPlayerCards.CardType.Yes));
            actions.add(new ResVoting(currentPlayer, ResPlayerCards.CardType.No));

        } else if (resgs.getGamePhase() == MissionVote) {

            if (resgs.finalTeamChoice.contains(currentPlayer)) {
                // Resistance members can only play a success card
                actions.add(new ResVoting(currentPlayer, ResPlayerCards.CardType.Yes));
                if (resgs.playerHandCards.get(currentPlayer).get(2).cardType == ResPlayerCards.CardType.SPY) {
                    // Spies can play either a success or fail card
                    actions.add(new ResVoting(currentPlayer, ResPlayerCards.CardType.No));
                }
            } else {
                throw new AssertionError("Should not be a player's turn if they are not on the mission");
            }
        }

        if (actions.isEmpty())
            throw new AssertionError("No Actions Available");
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        ResGameState resgs = (ResGameState) currentState;

        //Leader Selects Team
        if (resgs.getGamePhase() == LeaderSelectsTeam) {
            resgs.setGamePhase(TeamSelectionVote);
            endPlayerTurn(resgs);
        } else if (resgs.getGamePhase() == TeamSelectionVote) {
            if (resgs.teamChoice.isEmpty()) {
                throw new AssertionError("Team Choice Size is Zero");
            }
            // Now we have to check if all players have voted
            for (int i = 0; i < resgs.getNPlayers(); i++) {
                if (resgs.votingChoice[i] == null) {
                    endPlayerTurn(resgs, i);
                    return;
                }
            }
            // If we reach this point, then all players have voted
            revealCards(resgs);
            if (resgs.failedVoteCounter == 5) {
                endGame(resgs, ResPlayerCards.CardType.SPY);
                return;
            }

            resgs.logEvent(GAME_EVENT, "Vote was " + Arrays.stream(resgs.votingChoice).map(Objects::toString).collect(Collectors.joining(",")));
            resgs.clearVoteChoices();

            if (resgs.voteSuccess) {
                resgs.finalTeamChoice = resgs.teamChoice;
                resgs.setGamePhase(MissionVote);
                endPlayerTurn(resgs, resgs.finalTeamChoice.get(0));
            } else {
;
                resgs.clearTeamChoices();

                // CHANGE LEADER
                changeLeader(resgs);
                resgs.setGamePhase(LeaderSelectsTeam);
                endPlayerTurn(resgs, resgs.leaderID);
            }
        } else if (resgs.getGamePhase() == MissionVote) {

            if (resgs.finalTeamChoice.isEmpty()) {
                throw new AssertionError("Final Team Choice Size is Zero");
            }
            // Now we have to check if all players have voted
            for (int i = 0; i < resgs.finalTeamChoice.size(); i++) {
                int p = resgs.finalTeamChoice.get(i);
                if (resgs.votingChoice[p] == null) {
                    endPlayerTurn(resgs, p);
                    return;
                }
            }
            // If we reach this point, then all players have voted
            revealCards(resgs);
            resgs.clearVoteChoices();
            resgs.clearTeamChoices();
            changeLeader(resgs);
            endRound(resgs);

            // Check if the game is over
            int occurrenceCountTrue = Collections.frequency(resgs.gameBoardValues, true);
            int occurrenceCountFalse = Collections.frequency(resgs.gameBoardValues, false);
            if (occurrenceCountTrue == 3) {
                endGame(resgs, ResPlayerCards.CardType.RESISTANCE);
                return;
            }

            if (occurrenceCountFalse == 3) {
                endGame(resgs, ResPlayerCards.CardType.SPY);
                return;
            }
            resgs.failedVoteCounter = 0;
            resgs.setGamePhase(LeaderSelectsTeam);
            endPlayerTurn(resgs, resgs.leaderID);
        }

    }

    void endGame(ResGameState resgs, ResPlayerCards.CardType winnerType) {
        for (int i = 0; i < resgs.getNPlayers(); i++) {
            PartialObservableDeck<ResPlayerCards> hand = resgs.playerHandCards.get(i);
            if (hand.get(2).cardType == winnerType) {
                resgs.setPlayerResult(WIN_GAME, i);
            } else {
                resgs.setPlayerResult(LOSE_GAME, i);
            }
        }
        endGame(resgs);
    }

    void revealCards(ResGameState resgs) {
        if (resgs.getGamePhase() == TeamSelectionVote) {
            int occurrenceCount = (int) Arrays.stream(resgs.votingChoice).filter(c -> c == ResPlayerCards.CardType.Yes).count();
            if (occurrenceCount > resgs.getNPlayers() / 2) {
                resgs.voteSuccess = true;
            } else {
                resgs.voteSuccess = false;
                resgs.failedVoteCounter += 1;
            }
        }

        if (resgs.getGamePhase() == MissionVote) {
            int occurrenceCount = (int) Arrays.stream(resgs.votingChoice).filter(c -> c == ResPlayerCards.CardType.No).count();
            if (occurrenceCount > 0) {
                resgs.gameBoardValues.add(false);
            } else {
                resgs.gameBoardValues.add(true);
            }
            resgs.historicTeams.add(new ArrayList<>(resgs.finalTeamChoice));
            resgs.noVotesPerMission.add(occurrenceCount);
        }
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        gs.setGameStatus(CoreConstants.GameResult.GAME_END);
        if (gs.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gs.getPlayerResults()));
        }
    }

    public void changeLeader(ResGameState resgs) {
        resgs.leaderID = (resgs.leaderID + 1) % resgs.getNPlayers();
    }

    public static List<Boolean> randomiseSpies(int spies, ResGameState state, int playerID, Random rnd) {
        // We want to randomly assign the number of spies across the total number of players
        // and return a boolean[] with length of total, and spies number of true values
        // we also need to ensure that there is at least one spy per historically failed mission
        boolean valid = true;
        int total = state.getNPlayers();
        boolean[] retValue;
        int count = 0;
        do {
            retValue = new boolean[state.getNPlayers()];
            for (int i = 0; i < spies; i++) {
                boolean done = false;

                while (!done) {
                    int rndIndex = rnd.nextInt(total);
                    if (!retValue[rndIndex] && rndIndex != playerID) {
                        retValue[rndIndex] = true;
                        done = true;
                    }
                }
            }
            // now check constraints
            valid = true;
            for (int previousMission = 1; previousMission <= state.getMissionsSoFar(); previousMission++) {
                int noVotes = state.getHistoricNoVotes(previousMission);
                if (noVotes == 0)
                    continue;
                List<Integer> failedMission = state.getHistoricTeam(previousMission);
                boolean[] finalRetValue = retValue;
                int spiesOnMission = failedMission.stream()
                        .mapToInt(p -> finalRetValue[p] ? 1 : 0)
                        .sum();
                if (spiesOnMission < noVotes)
                    valid = false;
                if (!valid)
                    break;
            }
            count++;
            if (count > 200)
                throw new AssertionError(String.format("Infinite loop allocating %d spies amongst %d players", spies, total));
        } while (!valid);
        List<Boolean> RV = new ArrayList<>();
        for (
                boolean b : retValue) {
            RV.add(b);
        }
        return RV;
    }
}

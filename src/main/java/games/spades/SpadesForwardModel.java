package games.spades;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.spades.actions.Bid;
import games.tricktaking.PlayCard;
import games.tricktaking.PlayRule;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class SpadesForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SpadesGameState state = (SpadesGameState) firstState;
        state.playerHands = new ArrayList<>();
        state.tricksWon = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            Deck<FrenchCard> hand = new Deck<>("Player" + i + "Hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            hand.setOwnerId(i);
            state.playerHands.add(hand);
            state.tricksWon.add(new ArrayList<>());
        }
        Arrays.fill(state.teamScores, 0);
        Arrays.fill(state.teamSandbags, 0);
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerBids[i] = -1;
            state.tricksTaken[i] = 0;
        }
        state.spadesBroken = false;

        startNewRound(state);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SpadesGameState state = (SpadesGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = state.getCurrentPlayer();
        SpadesParameters params = (SpadesParameters) state.getGameParameters();

        if (state.getGamePhase() == SpadesGameState.Phase.BIDDING) {
            int team = state.getTeam(currentPlayer);
            boolean nilAllowed = params.allowNilOverbid || state.getTeamScore(team) < params.winningScore;
            int minBid = 0;
            for (int bid = minBid; bid <= params.maxBid; bid++) {
                if (bid == 0 && !nilAllowed) continue; // restrict Nil if house rule disallows it at high scores
                actions.add(new Bid(bid));
            }
            if (params.allowBlindNil && nilAllowed) {
                // Offer Blind Nil as a distinct bid option (uses Bid with blind flag)
                actions.add(new Bid(0, true));
            }
        } else if (state.getGamePhase() == SpadesGameState.Phase.PLAYING) {
            // spades may not be led until they are broken, unless the leader holds nothing else
            PlayRule rule = state.isSpadesBroken() ? PlayRule.FOLLOW_SUIT : PlayRule.leadRestricted(FrenchCard.Suite.Spades);
            List<FrenchCard> hand = state.getPlayerHand(currentPlayer).getComponents();
            for (FrenchCard card : rule.legalPlays(hand, state.getCurrentTrick())) {
                actions.add(new PlayCard(card));
            }
        }

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        SpadesGameState state = (SpadesGameState) currentState;

        if (actionTaken instanceof Bid) {
            if (state.allPlayersBid()) {
                state.setGamePhase(SpadesGameState.Phase.PLAYING);
            }
            endPlayerTurn(state);
        } else if (actionTaken instanceof PlayCard playAction) {

            if (playAction.card.suite == FrenchCard.Suite.Spades) {
                state.setSpadesBroken(true);
            }

            Trick trick = state.getCurrentTrick();
            if (trick.isComplete()) {
                // trick finished: spades are trumps
                int trickWinner = trick.winner(FrenchCard.Suite.Spades);
                state.incrementTricksTaken(trickWinner);

                Deck<FrenchCard> trickDeck = new Deck<>("Trick", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
                for (FrenchCard card : trick.getComponents())
                    trickDeck.addToBottom(card);
                state.tricksWon.get(trickWinner).add(trickDeck);
                state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), trickWinner);
                endPlayerTurn(state, trickWinner);

                if (state.getPlayerHands().get(0).getSize() == 0) {
                    // all cards played
                    nextRound(state);
                }
            } else {
                endPlayerTurn(state); // otherwise default to next player
            }

        }
    }

    /**
     * Handles end of round scoring and checks for game end
     */
    private void nextRound(SpadesGameState state) {
        SpadesParameters params = (SpadesParameters) state.getGameParameters();

        for (int team = 0; team < state.getNTeams(); team++) {
            // Teams in Spades alternate players
            int finalTeam = team;
            int[] teamPlayers = IntStream.range(0, state.getNPlayers())
                    .filter(p -> state.getTeam(p) == finalTeam)
                    .toArray();

            int teamScore = state.getTeamScore(team);
            int teamTricks = 0;
            int teamBid = 0;
            // Only positive bids contribute to team bid; 0 is Nil and scored separately
            for (int player : teamPlayers) {
                int bid = state.getPlayerBid(player);
                int tricks = state.getTricksTaken(player);
                teamTricks += tricks;
                if (bid > 0) {
                    teamBid += bid;
                } else if (bid == 0) {
                    boolean blind1 = state.playerBlindNil[player];
                    int bonus = blind1 ? params.blindNilBonusPoints : params.nilBonusPoints;
                    int penalty = blind1 ? params.blindNilPenaltyPoints : params.nilPenaltyPoints;
                    teamScore += (tricks == 0) ? bonus : -penalty;
                }
            }

            // Team contract score (for non-nil bids)
            if (teamBid > 0) {
                if (teamTricks >= teamBid) {
                    int basePoints = teamBid * params.pointsPerTrick;
                    int sandBags = teamTricks - teamBid;
                    teamScore += basePoints + sandBags;

                    state.addTeamSandbags(team, sandBags);

                    while (state.getTeamSandbags(team) >= params.sandbagsPerPenalty) {
                        teamScore -= params.sandbagsRandPenalty;
                        state.addTeamSandbags(team, -params.sandbagsPerPenalty);
                    }
                } else {
                    // If any teammate bid Nil, do not double-penalize the team for missing the non-nil contract.
                    // Standard house rules apply contract penalty regardless; keep it simple but bounded.
                    int penalty = teamBid * params.pointsPerTrick;
                    teamScore -= penalty;
                }
            } else {
                // Both players bid Nil: no contract, and by default do NOT count tricks as sandbags
                // (only Nil bonuses/penalties apply)
            }

            state.setTeamScore(team, teamScore);
        }

        boolean gameEnded = false;
        for (int team = 0; team < 2; team++) {
            if (state.getTeamScore(team) >= params.winningScore) {
                gameEnded = true;
                break;
            }
        }

        if (gameEnded) {
            int winningTeam = state.getTeamScore(0) > state.getTeamScore(1) ? 0 : 1;
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (state.getTeam(p) == winningTeam) {
                    state.setPlayerResult(CoreConstants.GameResult.WIN_GAME, p);
                } else {
                    state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, p);
                }
            }
            state.setGameStatus(CoreConstants.GameResult.GAME_END);
        } else {
            // the first player for the round rotates clockwise
            int startPlayer = (state.getRoundCounter() + 1) % state.getNPlayers();
            endRound(state, startPlayer);
            if (state.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
                startNewRound(state);
            }
        }
    }

    /**
     * Starts a new round of play
     */
    private void startNewRound(SpadesGameState state) {
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerBids[i] = -1;
            state.tricksTaken[i] = 0;
            state.tricksWon.get(i).clear();
        }

        // the first player of the round bids first, and then leads the first trick
        state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), state.getFirstPlayer());
        state.setGamePhase(SpadesGameState.Phase.BIDDING);
        state.setSpadesBroken(false);

        // known voids are only valid for the current round, as hands are re-dealt each round
        state.knownVoids.clear();

        for (int i = 0; i < state.getNPlayers(); i++) {
            Deck<FrenchCard> hand = state.getPlayerHands().get(i);
            hand.clear();
        }

        Deck<FrenchCard> deck = FrenchCard.generateDeck("MainDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        deck.shuffle(state.getRnd());

        for (int i = 0; i < 13; i++) {
            for (int p = 0; p < state.getNPlayers(); p++) {
                FrenchCard card = deck.draw();
                state.getPlayerHands().get(p).add(card);
                card.setOwnerId(p);
            }
        }
        state.setGamePhase(SpadesGameState.Phase.BIDDING);
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        // Override to set team-based winners by score when framework triggers end (e.g., maxRounds)
        SpadesGameState state = (SpadesGameState) gs;
        int team0 = state.getTeamScore(0);
        int team1 = state.getTeamScore(1);
        if (team0 == team1) {
            for (int p = 0; p < state.getNPlayers(); p++) state.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, p);
        } else {
            int winningTeam = team0 > team1 ? 0 : 1;
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (state.getTeam(p) == winningTeam) state.setPlayerResult(CoreConstants.GameResult.WIN_GAME, p);
                else state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, p);
            }
        }
        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        if (gs.getCoreGameParameters().verbose) System.out.println(Arrays.toString(gs.getPlayerResults()));
    }
}
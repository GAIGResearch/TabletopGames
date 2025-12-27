package games.spades;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.spades.actions.Bid;
import games.spades.actions.PlayCard;
import utilities.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpadesForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SpadesGameState state = (SpadesGameState) firstState;
        Arrays.fill(state.teamScores, 0);
        Arrays.fill(state.teamSandbags, 0);
        for (int i = 0; i < 4; i++) {
            state.playerBids[i] = -1;
            state.tricksTaken[i] = 0;
            state.tricksWon.get(i).clear();
        }
        state.currentTrick.clear();
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
            boolean nilAllowed = params.allowNilOverbid || state.getTeamScore(team) < 500;
            int minBid = 0;
            int maxBid = Math.min(13, params.maxBid);
            for (int bid = minBid; bid <= maxBid; bid++) {
                if (bid == 0 && !nilAllowed) continue; // restrict Nil if house rule disallows it at high scores
                actions.add(new Bid(bid));
            }
            if (params.allowBlindNil && nilAllowed) {
                // Offer Blind Nil as a distinct bid option (uses Bid with blind flag)
                actions.add(new Bid(0, true));
            }
        } else if (state.getGamePhase() == SpadesGameState.Phase.PLAYING) {
            Deck<FrenchCard> playerHand = state.getPlayerHands().get(currentPlayer);

            for (FrenchCard card : playerHand.getComponents()) {
                if (isValidPlay(state, card)) {
                    actions.add(new PlayCard(card));
                }
            }
        }

        return actions;
    }

    /**
     * Determines legal card plays
     */
    private boolean isValidPlay(SpadesGameState state, FrenchCard card) {
        List<Pair<Integer, FrenchCard>> currentTrick = state.getCurrentTrick();
        int currentPlayer = state.getCurrentPlayer();
        Deck<FrenchCard> playerHand = state.getPlayerHands().get(currentPlayer);

        if (currentTrick.isEmpty()) {
            if (card.suite == FrenchCard.Suite.Spades) {
                if (!state.isSpadesBroken()) {
                    return playerHand.getComponents().stream()
                            .allMatch(c -> c.suite == FrenchCard.Suite.Spades);
                }
            }
            return true;
        } else {
            FrenchCard.Suite leadSuit = state.getLeadSuit();
            if (card.suite == leadSuit) {
                return true;
            }

            boolean hasLeadSuit = playerHand.getComponents().stream()
                    .anyMatch(c -> c.suite == leadSuit);

            return !hasLeadSuit;
        }
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

            if (state.getCurrentTrick().size() == 1) {
                state.setLeadSuit(playAction.card.suite);
            }

            if (playAction.card.suite == FrenchCard.Suite.Spades) {
                state.setSpadesBroken(true);
            }

            if (state.getCurrentTrick().size() == state.getNPlayers()) {
                // trick finished
                int trickWinner = determineTrickWinner(state);
                state.incrementTricksTaken(trickWinner);

                Deck<FrenchCard> trickDeck = new Deck<>("Trick", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
                for (Pair<Integer, FrenchCard> entry : state.getCurrentTrick()) {
                    trickDeck.add(entry.b);
                }
                state.tricksWon.get(trickWinner).add(trickDeck);
                state.clearCurrentTrick();
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
     * Determines the winner of a completed trick
     */
    private int determineTrickWinner(SpadesGameState state) {
        List<Pair<Integer, FrenchCard>> trick = state.getCurrentTrick();
        FrenchCard.Suite leadSuit = state.getLeadSuit();

        int winner = trick.get(0).a;
        FrenchCard winningCard = trick.get(0).b;

        for (Pair<Integer, FrenchCard> entry : trick) {
            FrenchCard card = entry.b;

            if (card.suite == FrenchCard.Suite.Spades && winningCard.suite != FrenchCard.Suite.Spades) {
                winner = entry.a;
                winningCard = card;
            } else if (card.suite == FrenchCard.Suite.Spades) {
                if (getCardValue(card) > getCardValue(winningCard)) {
                    winner = entry.a;
                    winningCard = card;
                }
            } else if (card.suite == leadSuit && winningCard.suite != FrenchCard.Suite.Spades) {
                if (winningCard.suite != leadSuit || getCardValue(card) > getCardValue(winningCard)) {
                    winner = entry.a;
                    winningCard = card;
                }
            }
        }

        return winner;
    }

    /**
     * Gets the value of a card for comparison (higher is better)
     */
    private int getCardValue(FrenchCard card) {
        if (card.type == FrenchCard.FrenchCardType.Ace) return 14;
        if (card.type == FrenchCard.FrenchCardType.King) return 13;
        if (card.type == FrenchCard.FrenchCardType.Queen) return 12;
        if (card.type == FrenchCard.FrenchCardType.Jack) return 11;
        return card.number;
    }

    /**
     * Handles end of round scoring and checks for game end
     */
    private void nextRound(SpadesGameState state) {
        SpadesParameters params = (SpadesParameters) state.getGameParameters();

        for (int team = 0; team < 2; team++) {
            // Teams in Spades are (0,2) and (1,3)
            int[] teamPlayers = new int[] {team, team + 2};

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
                    int basePoints = teamBid * 10;
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
                    int penalty = teamBid * 10;
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
            for (int p = 0; p < 4; p++) {
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
        for (int i = 0; i < 4; i++) {
            state.playerBids[i] = -1;
            state.tricksTaken[i] = 0;
            state.tricksWon.get(i).clear();
        }

        state.currentTrick.clear();
        state.setGamePhase(SpadesGameState.Phase.BIDDING);
        state.setSpadesBroken(false);
        state.leadSuit = null;

        for (int i = 0; i < 4; i++) {
            Deck<FrenchCard> hand = state.getPlayerHands().get(i);
            hand.clear();
        }

        Deck<FrenchCard> deck = FrenchCard.generateDeck("MainDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        deck.shuffle(state.getRnd());

        for (int i = 0; i < 13; i++) {
            for (int p = 0; p < 4; p++) {
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
            for (int p = 0; p < 4; p++) state.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, p);
        } else {
            int winningTeam = team0 > team1 ? 0 : 1;
            for (int p = 0; p < 4; p++) {
                if (state.getTeam(p) == winningTeam) state.setPlayerResult(CoreConstants.GameResult.WIN_GAME, p);
                else state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, p);
            }
        }
        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        if (gs.getCoreGameParameters().verbose) System.out.println(Arrays.toString(gs.getPlayerResults()));
    }
}
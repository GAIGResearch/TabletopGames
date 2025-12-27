package games.spades;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.spades.actions.Bid;
import games.spades.actions.PlayCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpadesForwardModel extends StandardForwardModel {
    
    @Override
    protected void _setup(AbstractGameState firstState) {
        SpadesGameState state = (SpadesGameState) firstState;
        if (firstState.getRoundCounter() == 0) {
            Arrays.fill(state.teamScores, 0);
            Arrays.fill(state.teamSandbags, 0);
            for (int i = 0; i < 4; i++) {
                state.playerBids[i] = -1;
                state.tricksTaken[i] = 0;
                state.tricksWon.get(i).clear();
            }
            state.currentTrick.clear();
            state.spadesBroken = false;
            state.leadSuit = null;
        }

        for (int i = 0; i < 4; i++) {
            Deck<FrenchCard> hand = state.getPlayerHands().get(i);
            hand.clear();
            hand.setOwnerId(i);
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
        
        state.setSpadesGamePhase(SpadesGameState.Phase.BIDDING);

        endPlayerTurn(state, 1);
        state.setLeadPlayer(1);
    }
    
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SpadesGameState state = (SpadesGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = state.getCurrentPlayer();
        SpadesParameters params = (SpadesParameters) state.getGameParameters();
        
        if (state.getSpadesGamePhase() == SpadesGameState.Phase.BIDDING) {
            int team = state.getTeam(currentPlayer);
            boolean nilAllowed = params.allowNilOverbid || state.getTeamScore(team) < 500;
            int minBid = Math.max(0, params.minBid);
            int maxBid = Math.min(13, params.maxBid);
            for (int bid = minBid; bid <= maxBid; bid++) {
                if (bid == 0 && !nilAllowed) continue; // restrict Nil if house rule disallows it at high scores
                actions.add(new Bid(currentPlayer, bid));
            }
            if (params.allowBlindNil && nilAllowed) {
                // Offer Blind Nil as a distinct bid option (uses Bid with blind flag)
                actions.add(new Bid(currentPlayer, 0, true));
            }
        } else if (state.getSpadesGamePhase() == SpadesGameState.Phase.PLAYING) {
            Deck<FrenchCard> playerHand = state.getPlayerHands().get(currentPlayer);
            
            for (FrenchCard card : playerHand.getComponents()) {
                if (isValidPlay(state, card)) {
                    actions.add(new PlayCard(currentPlayer, card));
                }
            }
        }
        
        return actions;
    }
    
    /**
     * Determines legal card plays
     */
    private boolean isValidPlay(SpadesGameState state, FrenchCard card) {
        List<Map.Entry<Integer, FrenchCard>> currentTrick = state.getCurrentTrick();
        int currentPlayer = state.getCurrentPlayer();
        Deck<FrenchCard> playerHand = state.getPlayerHands().get(currentPlayer);

        if (currentTrick.isEmpty()) {
            if (card.suite == FrenchCard.Suite.Spades) {
                if (!state.isSpadesBroken()) {
                    boolean hasOnlySpades = playerHand.getComponents().stream()
                            .allMatch(c -> c.suite == FrenchCard.Suite.Spades);
                    return hasOnlySpades;
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
                state.setSpadesGamePhase(SpadesGameState.Phase.PLAYING);
                endPlayerTurn(state, 1);
                state.setLeadPlayer(1);
            } else {
                endPlayerTurn(state);
            }
        } else if (actionTaken instanceof PlayCard) {
            PlayCard playAction = (PlayCard) actionTaken;
            
            if (state.getCurrentTrick().size() == 1) {
                state.setLeadSuit(playAction.card.suite);
            }
            
            if (playAction.card.suite == FrenchCard.Suite.Spades) {
                state.setSpadesBroken(true);
            }
            
            if (state.getCurrentTrick().size() == 4) {
                int trickWinner = determineTrickWinner(state);
                state.incrementTricksTaken(trickWinner);

                Deck<FrenchCard> trickDeck = new Deck<>("Trick", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
                for (Map.Entry<Integer, FrenchCard> entry : state.getCurrentTrick()) {
                    trickDeck.add(entry.getValue());
                }
                state.tricksWon.get(trickWinner).add(trickDeck);
                
                state.clearCurrentTrick();
                
                endPlayerTurn(state, trickWinner);
                state.setLeadPlayer(trickWinner);
                
                if (state.getPlayerHands().get(0).getSize() == 0) {
                    endRound(state);
                } else {
                    // Continue with next trick
                }
            } else {
                endPlayerTurn(state);
            }
        }
    }
    
    /**
     * Determines the winner of a completed trick
     */
    private int determineTrickWinner(SpadesGameState state) {
        List<Map.Entry<Integer, FrenchCard>> trick = state.getCurrentTrick();
        FrenchCard.Suite leadSuit = state.getLeadSuit();
        
        int winner = trick.get(0).getKey();
        FrenchCard winningCard = trick.get(0).getValue();
        
        for (Map.Entry<Integer, FrenchCard> entry : trick) {
            FrenchCard card = entry.getValue();

            if (card.suite == FrenchCard.Suite.Spades && winningCard.suite != FrenchCard.Suite.Spades) {
                winner = entry.getKey();
                winningCard = card;
            } else if (card.suite == FrenchCard.Suite.Spades && winningCard.suite == FrenchCard.Suite.Spades) {
                if (getCardValue(card) > getCardValue(winningCard)) {
                    winner = entry.getKey();
                    winningCard = card;
                }
            } else if (card.suite == leadSuit && winningCard.suite != FrenchCard.Suite.Spades) {
                if (winningCard.suite != leadSuit || getCardValue(card) > getCardValue(winningCard)) {
                    winner = entry.getKey();
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
    private void endRound(SpadesGameState state) {
        SpadesParameters params = (SpadesParameters) state.getGameParameters();
        
        for (int team = 0; team < 2; team++) {
            // Teams in Spades are (0,2) and (1,3)
            int player1 = team;      // 0 for team 0, 1 for team 1
            int player2 = team + 2;  // 2 for team 0, 3 for team 1
            
            int bid1 = state.getPlayerBid(player1);
            int bid2 = state.getPlayerBid(player2);
            int tricks1 = state.getTricksTaken(player1);
            int tricks2 = state.getTricksTaken(player2);
            int teamTricks = tricks1 + tricks2;

            int teamBid = 0;
            // Only positive bids contribute to team bid; 0 is Nil and scored separately
            if (bid1 > 0) teamBid += bid1;

            if (bid2 > 0) teamBid += bid2;

            int teamScore = state.getTeamScore(team);

            // Score Nil bids per player
            if (bid1 == 0) {
                boolean blind1 = state.playerBlindNil[player1];
                int bonus = blind1 ? params.blindNilBonusPoints : params.nilBonusPoints;
                int penalty = blind1 ? params.blindNilPenaltyPoints : params.nilPenaltyPoints;
                teamScore += (tricks1 == 0) ? bonus : -penalty;
            }
            if (bid2 == 0) {
                boolean blind2 = state.playerBlindNil[player2];
                int bonus = blind2 ? params.blindNilBonusPoints : params.nilBonusPoints;
                int penalty = blind2 ? params.blindNilPenaltyPoints : params.nilPenaltyPoints;
                teamScore += (tricks2 == 0) ? bonus : -penalty;
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
            super.endRound(state);
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
        state.setSpadesGamePhase(SpadesGameState.Phase.BIDDING);
        state.setSpadesBroken(false);
        state.leadSuit = null;
        
        _setup(state);
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
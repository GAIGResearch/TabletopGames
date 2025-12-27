package games.spades;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import core.components.FrenchCard;

public class SpadesHeuristic implements IStateHeuristic {
    
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SpadesGameState state = (SpadesGameState) gs;
        
        if (state.isNotTerminal()) {
            int team = state.getTeam(playerId);
            int opponentTeam = 1 - team;

            double ourScore = state.getTeamScore(team);
            double opponentScore = state.getTeamScore(opponentTeam);

            // Handle score difference more robustly for negative scores
            double scoreDiff = ourScore - opponentScore;
            double scoreAdvantage = 0.0;
            
            // Normalize score difference to [-1, +1] range
            if (Math.abs(scoreDiff) > 0) {
                // Use a sigmoid-like function to handle extreme negative scores
                scoreAdvantage = Math.tanh(scoreDiff / 1000.0);
            }

            // Add tactical evaluation based on current game phase
            if (state.getGamePhase() == SpadesGameState.Phase.PLAYING) {
                int ourBid = state.getPlayerBid(playerId) + state.getPlayerBid((playerId + 2) % 4);
                int ourTricks = state.getTricksTaken(playerId) + state.getTricksTaken((playerId + 2) % 4);

                // Evaluate bid progress
                if (ourBid > 0) {
                    double bidProgress = Math.min(1.0, (double) ourTricks / ourBid);
                    scoreAdvantage += bidProgress * 0.3;

                    // Penalize severe overbidding (too many sandbags)
                    if (ourTricks > ourBid + 3) {
                        scoreAdvantage -= 0.2;
                    }
                }

                // Add hand strength evaluation
                double handStrength = evaluateHandStrength(state, playerId);
                scoreAdvantage += handStrength * 0.2;
            } else if (state.getGamePhase() == SpadesGameState.Phase.BIDDING) {
                // During bidding, focus more on hand strength
                double handStrength = evaluateHandStrength(state, playerId);
                scoreAdvantage += handStrength * 0.4;
            }

            // Convert to [0,1] range for MCTS
            return Math.max(0.0, Math.min(1.0, 0.5 + scoreAdvantage * 0.5));
        } else {
            // Terminal state - use actual game results
            return state.getPlayerResults()[playerId].value;
        }
    }
    
    /**
     * Evaluates the strength of a player's hand and suggests appropriate bid
     */
    private double evaluateHandStrength(SpadesGameState state, int playerId) {
        if (playerId < 0 || playerId >= state.getPlayerHands().size()) {
            return 0.0;
        }
        
        double strength = 0.0;
        int spadeCount = 0;
        
        for (FrenchCard card : state.getPlayerHands().get(playerId).getComponents()) {
            if (card.suite == FrenchCard.Suite.Spades) {
                spadeCount++;
                if (card.type == FrenchCard.FrenchCardType.Ace) {
                    strength += 1.5;
                } else if (card.type == FrenchCard.FrenchCardType.King) {
                    strength += 1.2;
                } else if (card.type == FrenchCard.FrenchCardType.Queen) {
                    strength += 1.0;
                } else if (card.type == FrenchCard.FrenchCardType.Jack) {
                    strength += 0.8;
                } else if (card.number >= 10) {
                    strength += 0.5;
                } else {
                    strength += 0.2;
                }
            } else {
                if (card.type == FrenchCard.FrenchCardType.Ace) {
                    strength += 0.8;
                } else if (card.type == FrenchCard.FrenchCardType.King) {
                    strength += 0.5;
                } else if (card.type == FrenchCard.FrenchCardType.Queen) {
                    strength += 0.3;
                } else if (card.type == FrenchCard.FrenchCardType.Jack) {
                    strength += 0.2;
                } else if (card.number >= 10) {
                    strength += 0.1;
                }
            }
        }

        if (spadeCount >= 5) strength += 0.5;
        if (spadeCount >= 7) strength += 0.5;

        return Math.min(1.0, strength / 8.0);
    }
    
    /**
     * Suggests a reasonable bid based on hand strength
     * This helps AI make better bidding decisions
     */
    public int suggestBid(SpadesGameState state, int playerId) {
        if (playerId < 0 || playerId >= state.getPlayerHands().size()) {
            return 1;
        }
        
        double handStrength = evaluateHandStrength(state, playerId);
        int spadeCount = 0;
        int highCards = 0;
        
        for (FrenchCard card : state.getPlayerHands().get(playerId).getComponents()) {
            if (card.suite == FrenchCard.Suite.Spades) {
                spadeCount++;
            }

            if (card.type == FrenchCard.FrenchCardType.Ace) {
                highCards++;
            } else if (card.type == FrenchCard.FrenchCardType.King) {
                highCards++;
            } else if (card.suite == FrenchCard.Suite.Spades && card.type == FrenchCard.FrenchCardType.Queen) {
                highCards++;
            }
        }

        int suggestedBid = Math.max(1, highCards + (spadeCount / 3));

        if (handStrength < 0.3) {
            suggestedBid = Math.max(1, suggestedBid - 1);
        } else if (handStrength > 0.7) {
            suggestedBid = Math.min(6, suggestedBid + 1);
        }

        return Math.min(6, Math.max(1, suggestedBid));
    }
} 
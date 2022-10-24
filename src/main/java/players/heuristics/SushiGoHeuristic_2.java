package players.heuristics;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.util.ArrayList;
import java.util.List;

public class SushiGoHeuristic_2 implements IStateHeuristic {

    double[] gameScores;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState sggs = (SGGameState) gs;
        int nPlayers = sggs.getNPlayers();
        // Get player game scores
        gameScores = new double[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            gameScores[i] = sggs.getGameScore(i);
        }
        return evaluateDiff2BestPlayer(sggs, playerId) / 50;
    }

    double evaluateDiff2BestPlayer(SGGameState sggs, int playerId) {
        // Find the best player score (besides current player)
        double bestOtherPlayerScore = -1;
        int nPlayers = sggs.getNPlayers();
        for (int i = 0; i < nPlayers; i++) {
            if (i == playerId) continue;
            double playerScore = sggs.getGameScore(i);
            if (playerScore > bestOtherPlayerScore) {
                bestOtherPlayerScore = playerScore;
            }
        }
        // Maximize difference to best other player
        double bestPlayerDif = sggs.getGameScore(playerId) - bestOtherPlayerScore;
        return bestPlayerDif;
    }

    void evaluateUncompletedCards(SGGameState sggs, int playerId) {
        int nPlayers = sggs.getNPlayers();
        List<Deck<SGCard>> decks = sggs.getPlayerDecks();
        for (int i = 0; i < nPlayers; i++) {
            // Evaluate uncompleted Tempura Cards
            int nTempuraCards = sggs.getPlayerTempuraAmount(i);
            if (nTempuraCards % 2 == 1) {
                // Check if tempura cards in hands
                // TODO: Tell lecturer we changed access to public
                ArrayList<SGCard> deck;
                if (sggs.hasSeenHand(playerId, i)) {
                    deck = (ArrayList) decks.get(i).getComponents();
                    // Find out if player has tempura pair ready to play
                    boolean playerHasTempuraPair = false;
                    for (SGCard card : deck) {
                        if (card.type == SGCard.SGCardType.Tempura) {
                            playerHasTempuraPair = true;
                            break;
                        }
                    }
                    // If player has 2nd tempura card ready to play, we can ignore it
                    if (playerHasTempuraPair) continue;
                }
                // Otherwise check if other tempura cards are in play
                int nTempuraLeftToPlay = 0;
                for (int j = 0; j < nPlayers; j++) {
                    if (j == i) continue;
                    if (!sggs.hasSeenHand(playerId, j)) continue;
                    deck = (ArrayList) decks.get(j).getComponents();
                    for (SGCard card : deck)
                        if (card.type == SGCard.SGCardType.Tempura) nTempuraLeftToPlay++;
                }
                if (nTempuraLeftToPlay > 0) gameScores[i] += 2.5;
            }
        }
    }

}

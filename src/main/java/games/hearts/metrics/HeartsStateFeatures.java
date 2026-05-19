package games.hearts.metrics;

import core.AbstractGameState;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IStateFeatureVector;
import games.hearts.HeartsGameState;
import games.hearts.HeartsParameters;

import java.util.Map;

public class HeartsStateFeatures implements IStateFeatureVector {

    static String[] names = new String[] {
            "heartsBroken",
            "currentScore",
            "opponentBestScore",
            "opponentWorstScore",
            "roundPoints",
            "roundPointsOpponentMax",
            "tricksTaken",
            "cardsInHand",
            "suitCountClubs",
            "suitCountDiamonds",
            "suitCountHearts",
            "suitCountSpades",
            "hasQoS",
            "currentTrickSize",
            "isFirstPlayer",
            "isClubsLead",
            "isDiamondsLead",
            "isHeartsLead",
            "isSpadesLead",
            "ownCardHigh",
            "handHighCardsCount",
            "handLowCardsCount"
    };

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        HeartsGameState hgs = (HeartsGameState) state;
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();
        double[] features = new double[names.length];

        features[0] = hgs.heartsBroken ? 1.0 : 0.0;

        // currentScore (normalized by matchScore)
        features[1] = hgs.getPlayerPoints(playerID) / (double) params.matchScore;

        double bestOpponentScore = -1e9;
        double worstOpponentScore = 1e9;
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            if (i != playerID) {
                double score = hgs.getPlayerPoints(i);
                if (score > bestOpponentScore) bestOpponentScore = score;
                if (score < worstOpponentScore) worstOpponentScore = score;
            }
        }
        features[2] = bestOpponentScore / (double) params.matchScore;
        features[3] = worstOpponentScore / (double) params.matchScore;

        // roundPoints (normalized by shootTheMoon)
        int roundPoints = 0;
        if (hgs.trickDecks != null && hgs.trickDecks.size() > playerID) {
            Deck<FrenchCard> playerTrickDeck = hgs.trickDecks.get(playerID);
            for (FrenchCard card : playerTrickDeck.getComponents()) {
                if (card.suite == FrenchCard.Suite.Hearts) roundPoints += params.heartCard;
                else if (card.equals(params.qosCard)) roundPoints += params.queenOfSpades;
            }
        }
        features[4] = roundPoints / (double) params.shootTheMoon;

        int maxOpponentRoundPoints = 0;
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            if (i != playerID) {
                int oppRoundPoints = 0;
                if (hgs.trickDecks != null && hgs.trickDecks.size() > i) {
                    Deck<FrenchCard> oppTrickDeck = hgs.trickDecks.get(i);
                    for (FrenchCard card : oppTrickDeck.getComponents()) {
                        if (card.suite == FrenchCard.Suite.Hearts) oppRoundPoints += params.heartCard;
                        else if (card.equals(params.qosCard)) oppRoundPoints += params.queenOfSpades;
                    }
                }
                if (oppRoundPoints > maxOpponentRoundPoints) maxOpponentRoundPoints = oppRoundPoints;
            }
        }
        features[5] = maxOpponentRoundPoints / (double) params.shootTheMoon;

        if (hgs.playerTricksTaken != null && hgs.playerTricksTaken.length > playerID) {
            features[6] = hgs.playerTricksTaken[playerID];
        }

        Deck<FrenchCard> hand = hgs.getPlayerDecks().get(playerID);
        int handSize = hand.getSize();
        features[7] = handSize;

        int[] suitCounts = new int[4];
        int highCards = 0;
        int lowCards = 0;
        boolean hasQoS = false;
        for (FrenchCard card : hand.getComponents()) {
            suitCounts[card.suite.ordinal()]++;
            // Value 10+ or face cards
            if (card.number >= 10 || card.type != FrenchCard.FrenchCardType.Number) highCards++;
            // Value 5- or Number cards
            if (card.number <= 5 && card.type == FrenchCard.FrenchCardType.Number) lowCards++;
            if (card.equals(params.qosCard)) hasQoS = true;
        }
        features[8] = suitCounts[FrenchCard.Suite.Clubs.ordinal()];
        features[9] = suitCounts[FrenchCard.Suite.Diamonds.ordinal()];
        features[10] = suitCounts[FrenchCard.Suite.Hearts.ordinal()];
        features[11] = suitCounts[FrenchCard.Suite.Spades.ordinal()];
        features[12] = hasQoS ? 1.0 : 0.0;

        features[13] = hgs.currentPlayedCards.size();
        features[14] = hgs.getCurrentPlayer() == hgs.getFirstPlayer() ? 1.0 : 0.0;
        
        if (hgs.firstCardSuit != null) {
            features[15] = hgs.firstCardSuit == FrenchCard.Suite.Clubs ? 1.0 : 0.0;
            features[16] = hgs.firstCardSuit == FrenchCard.Suite.Diamonds ? 1.0 : 0.0;
            features[17] = hgs.firstCardSuit == FrenchCard.Suite.Hearts ? 1.0 : 0.0;
            features[18] = hgs.firstCardSuit == FrenchCard.Suite.Spades ? 1.0 : 0.0;

            int highestInTrick = -1;
            for (Map.Entry<Integer, FrenchCard> entry : hgs.currentPlayedCards) {
                if (entry.getValue().suite == hgs.firstCardSuit) {
                    if (entry.getValue().number > highestInTrick) {
                        highestInTrick = entry.getValue().number;
                    }
                }
            }
            boolean canBeat = false;
            for (FrenchCard card : hand.getComponents()) {
                if (card.suite == hgs.firstCardSuit && card.number > highestInTrick) {
                    canBeat = true;
                    break;
                }
            }
            features[19] = canBeat ? 1.0 : 0.0;
        } else {
            features[15] = 0.0;
            features[16] = 0.0;
            features[17] = 0.0;
            features[18] = 0.0;
            features[19] = 0.0;
        }

        features[20] = highCards;
        features[21] = lowCards;

        return features;
    }
}

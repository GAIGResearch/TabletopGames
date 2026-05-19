package games.hearts.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.interfaces.IActionFeatureVector;
import games.hearts.HeartsGameState;
import games.hearts.HeartsParameters;
import games.hearts.actions.Pass;
import games.hearts.actions.Play;

import java.util.Map;

/**
 * HeartsActionFeatures provides a feature vector for actions in the game of Hearts.
 * It implements IActionFeatureVector and uses the doubleVector method to return a double[].
 */
public class HeartsActionFeatures implements IActionFeatureVector {
    String[] names = new String[]{
            "isPass",
            "suitDiamonds",
            "suitHearts",
            "suitClubs",
            "suitSpades",
            "rank",
            "pointsValue",
            "isQoS",
            "isLeadSuit",
            "winsTrick"
    };

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        HeartsGameState hgs = (HeartsGameState) state;
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();
        double[] features = new double[names.length];

        FrenchCard card = null;
        boolean isPass = false;

        if (action instanceof Play play) {
            card = play.card;
        } else if (action instanceof Pass pass) {
            card = pass.card1;
            isPass = true;
        }

        if (card != null) {
            features[0] = isPass ? 1.0 : 0.0;
            features[1] = card.suite == FrenchCard.Suite.Diamonds ? 1.0 : 0.0;
            features[2] = card.suite == FrenchCard.Suite.Hearts ? 1.0 : 0.0;
            features[3] = card.suite == FrenchCard.Suite.Clubs ? 1.0 : 0.0;
            features[4] = card.suite == FrenchCard.Suite.Spades ? 1.0 : 0.0;
            features[5] = card.number;

            double points = 0;
            if (card.suite == FrenchCard.Suite.Hearts) points = params.heartCard;
            else if (card.equals(params.qosCard)) points = params.queenOfSpades;
            features[6] = points;

            boolean isQoS = card.equals(params.qosCard);
            features[7] = isQoS ? 1.0 : 0.0;

            if (!isPass) {
                features[8] = (hgs.firstCardSuit != null && card.suite == hgs.firstCardSuit) ? 1.0 : 0.0;

                // Wins trick?
                boolean wins = false;
                if (hgs.firstCardSuit == null) {
                    wins = true; // Leading a card "wins" so far
                } else if (card.suite == hgs.firstCardSuit) {
                    int highestInTrick = -1;
                    for (Map.Entry<Integer, FrenchCard> entry : hgs.currentPlayedCards) {
                        if (entry.getValue().suite == hgs.firstCardSuit) {
                            if (entry.getValue().number > highestInTrick) {
                                highestInTrick = entry.getValue().number;
                            }
                        }
                    }
                    if (card.number > highestInTrick) {
                        wins = true;
                    }
                }
                features[9] = wins ? 1.0 : 0.0;
            } else {
                features[8] = 0.0;
                features[9] = 0.0;
            }
        }
        return features;
    }
}

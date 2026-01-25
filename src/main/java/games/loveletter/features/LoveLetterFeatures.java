package games.loveletter.features;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IStateFeatureVector;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class LoveLetterFeatures implements IStateFeatureVector {
    private int nFeatures = 20;

    @Override
    public String[] names() {
        // TODO add names
        return new String[nFeatures];
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {

        // Schema
        // [0-7]: Player hand cards (per card type)
        // [8]: Number of cards in draw pile
        // [9-16]: discarded card types
        // [16-20]: Affection tokens per player
        LoveLetterGameState llgs = (LoveLetterGameState) state;
        double[] observationSpace = new double[nFeatures];
        PartialObservableDeck<LoveLetterCard> playerHandCards = llgs.getPlayerHandCards().get(playerID);

        // Player Hand Cards
        for (LoveLetterCard card : playerHandCards.getComponents()) {
            observationSpace[card.cardType.getValue() - 1] = 1;
        }

        // Draw Pile

        observationSpace[8] = llgs.getDrawPile().getSize();

        // Discard Piles
        int i = 9;
        for (Deck<LoveLetterCard> deck : llgs.getPlayerDiscardCards()) {
            for (LoveLetterCard card : deck.getComponents()) {
                observationSpace[i + card.cardType.getValue() - 1] += 1;
            }
//            observationSpace[i] += deck.getSize();
            i++;
        }

        // Affection Tokens
        for (int j = 0; j < llgs.getNPlayers(); j++) {
            observationSpace[16 + j] = llgs.getGameScore(j);
        }

        return observationSpace;
    }
//
//    @Override
//    public double[] getNormalizedObservationVector() {
//        final double maxCards = 16;
//        double[] results = getObservationVector();
//        results[8] = results[8] / maxCards;
//        for (int i = 0; i < CardType.values().length; i++) {
//            // todo 5 is the max, which is guard other cards only have 1 each - should get it somehow
//            results[9+i] = results[9+i] / 5; // ((LoveLetterParameters) gameParameters).cardCounts.get(CardType.values()[i]);
////            results[i] = CardType.values()[i]
//        }
//        int nTokensWin = ((LoveLetterParameters) gameParameters).nTokensWin2;
//        switch (nPlayers) {
//            case 3:
//                nTokensWin = ((LoveLetterParameters) gameParameters).nTokensWin3;
//                break;
//            case 4:
//                nTokensWin = ((LoveLetterParameters) gameParameters).nTokensWin4;
//                break;
//        }
//        for (int i = 0; i < 4; i++) {
//            results[16+i] = results[16+i] / nTokensWin;
//        }
//
//        return results;
//    }
}

package games.loveletter.features;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IStateFeatureVector;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static games.loveletter.cards.CardType.*;

/**
 * A set of features designed to tie in exactly with those used in LoveLetterHeuristic
 */
public class LLStateFeaturesReduced implements IStateFeatureVector {

    String[] names = new String[]{"CARDS", "AFFECTION", "COUNTESS", "BARON",
            "GUARD", "HANDMAID", "KING", "PRIEST", "PRINCE", "PRINCESS", "HIDDEN",
            "ADVANTAGE"};

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        LoveLetterParameters llp = (LoveLetterParameters) gs.getGameParameters();

        double[] retValue = new double[names.length];

        double cardValues = 0;

        Set<CardType> cardTypes = new HashSet<>();
        for (LoveLetterCard card : llgs.getPlayerHandCards().get(playerId).getComponents()) {
            cardValues += card.cardType.getValue();
            cardTypes.add(card.cardType);
        }

        double maxCardValue = 1 + llgs.getPlayerHandCards().get(playerId).getSize() * getMaxCardValue();
        double nRequiredTokens = (llgs.getNPlayers() == 2 ? llp.nTokensWin2 : llgs.getNPlayers() == 3 ? llp.nTokensWin3 : llp.nTokensWin4);
        if (nRequiredTokens < llgs.getGameScore(playerId)) nRequiredTokens = llgs.getGameScore(playerId);

        retValue[0] = cardValues / maxCardValue;
        retValue[1] = llgs.getGameScore(playerId) / nRequiredTokens;

        if (cardTypes.contains(Countess)) retValue[2] = 1.0;
        if (cardTypes.contains(King)) retValue[6] = 1.0;
        if (cardTypes.contains(Baron)) retValue[3] = 1.0;
        if (cardTypes.contains(Handmaid)) retValue[5] = 1.0;
        if (cardTypes.contains(Guard)) retValue[4] = 1.0;
        if (cardTypes.contains(Priest)) retValue[7] = 1.0;
        if (cardTypes.contains(Prince)) retValue[8] = 1.0;
        if (cardTypes.contains(Princess)) retValue[9] = 1.0;

        int visibleCards = 0;
        for (int player = 0; player < llgs.getNPlayers(); player++) {
            if (player != playerId) {
                PartialObservableDeck<LoveLetterCard> deck = llgs.getPlayerHandCards().get(player);
                visibleCards += (int) IntStream.range(0, deck.getSize()).filter(i -> deck.getVisibilityForPlayer(i, playerId)).count();
            }
        }
        retValue[10] = visibleCards / (llgs.getNPlayers() - 1.0);

        int maxOtherScore = IntStream.range(0, llgs.getNPlayers())
                .filter(p -> p != playerId)
                .map(p -> (int) llgs.getGameScore(p)).max().orElseThrow(() -> new AssertionError("??"));
        retValue[11] = (llgs.getGameScore(playerId) - maxOtherScore) / nRequiredTokens;

        return retValue;

    }

    @Override
    public String[] names() {
        return names;
    }

}

package games.loveletter.features;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IStateFeatureVector;
import evaluation.optimisation.TunableParameters;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.cards.LoveLetterCard;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static games.loveletter.cards.LoveLetterCard.CardType.*;

/**
 * A set of features designed to tie in exactly with those used in LoveLetterHeuristic
 */
public class LLStateFeaturesTunable extends TunableParameters implements IStateFeatureVector {

    boolean[] active = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};

    String[] allNames = new String[]{"CARDS", "AFFECTION", "COUNTESS", "BARON",
            "GUARD", "HANDMAID", "KING", "PRIEST", "PRINCE", "PRINCESS", "HIDDEN",
            "ADVANTAGE"};

    String[] namesUsed;

    public LLStateFeaturesTunable() {
        for (String name : allNames) {
            addTunableParameter(name, true);
        }
    }

    @Override
    public void _reset() {
        for (int i = 0; i < allNames.length; i++) {
            active[i] = (Boolean) getParameterValue(allNames[i]);
        }
        namesUsed = IntStream.range(0, allNames.length).filter(i -> active[i]).mapToObj(i -> allNames[i]).toArray(String[]::new);
    }

    @Override
    public double[] featureVector(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        LoveLetterParameters llp = (LoveLetterParameters) gs.getGameParameters();

        double[] data = new double[allNames.length];

        double cardValues = 0;

        Set<LoveLetterCard.CardType> cardTypes = new HashSet<>();
        if (active[0]) {
            for (LoveLetterCard card : llgs.getPlayerHandCards().get(playerId).getComponents()) {
                cardValues += card.cardType.getValue();
                cardTypes.add(card.cardType);
            }
            double maxCardValue = 1 + llgs.getPlayerHandCards().get(playerId).getSize() * getMaxCardValue();
            data[0] = cardValues / maxCardValue;
        }

        double nRequiredTokens = (llgs.getNPlayers() == 2 ? llp.nTokensWin2 : llgs.getNPlayers() == 3 ? llp.nTokensWin3 : llp.nTokensWin4);
        if (nRequiredTokens < llgs.getGameScore(playerId)) nRequiredTokens = llgs.getGameScore(playerId);

        data[1] = llgs.getGameScore(playerId) / nRequiredTokens;

        if (cardTypes.contains(Countess)) data[2] = 1.0;
        if (cardTypes.contains(King)) data[6] = 1.0;
        if (cardTypes.contains(Baron)) data[3] = 1.0;
        if (cardTypes.contains(Handmaid)) data[5] = 1.0;
        if (cardTypes.contains(Guard)) data[4] = 1.0;
        if (cardTypes.contains(Priest)) data[7] = 1.0;
        if (cardTypes.contains(Prince)) data[8] = 1.0;
        if (cardTypes.contains(Princess)) data[9] = 1.0;

        if (active[10]) {
            int visibleCards = 0;
            for (int player = 0; player < llgs.getNPlayers(); player++) {
                if (player != playerId) {
                    PartialObservableDeck<LoveLetterCard> deck = llgs.getPlayerHandCards().get(player);
                    visibleCards += (int) IntStream.range(0, deck.getSize()).filter(i -> deck.getVisibilityForPlayer(i, playerId)).count();
                }
            }
            data[10] = visibleCards / (llgs.getNPlayers() - 1.0);
        }

        if (active[11]) {
            int maxOtherScore = IntStream.range(0, llgs.getNPlayers())
                    .filter(p -> p != playerId)
                    .map(p -> (int) llgs.getGameScore(p)).max().orElseThrow(() -> new AssertionError("??"));
            data[11] = (llgs.getGameScore(playerId) - maxOtherScore) / nRequiredTokens;
        }

        double[] retValue = new double[namesUsed.length];
        int count = 0;
        for (int i = 0; i < allNames.length; i++) {
            if (active[i]) {
                retValue[count] = data[i];
                count++;
            }
        }
        return retValue;
    }

    @Override
    public String[] names() {
        return namesUsed;
    }

    @Override
    protected LLStateFeaturesTunable _copy() {
        return new LLStateFeaturesTunable();
        // setting of values is done in TunableParameters
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof LLStateFeaturesTunable) {
            LLStateFeaturesTunable other = (LLStateFeaturesTunable) o;
            if (other.active.length != active.length)
                return false;
            for (int i = 0; i < active.length; i++) {
                if (other.active[i] != active[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public LLStateFeaturesTunable instantiate() {
        return this._copy();
    }

}

package games.loveletter.features;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static utilities.Utils.enumNames;
import static utilities.Utils.enumToOneHot;

public class LLActionFeaturesTiny implements IActionFeatureVector {

    final String[] localNames;
    final int featuresPerGroup = LoveLetterCard.CardType.values().length;

    public LLActionFeaturesTiny() {
        // For each card type, is this the card played
        localNames = enumNames(LoveLetterCard.CardType.class).stream().map(s -> s + "_PLAY").toArray(String[]::new);
    }

    @Override
    public String[] names() {
        return localNames;
    }

    @Override
    public double[] featureVector(AbstractAction a, AbstractGameState state, int playerID) {
        double[] retValue = new double[names().length];
        if (!(a instanceof PlayCard))
            return retValue;
        LoveLetterCard.CardType cardPlayed = ((PlayCard) a).getCardType();
        System.arraycopy(enumToOneHot(cardPlayed), 0, retValue, 0, featuresPerGroup);
        return retValue;
    }


}

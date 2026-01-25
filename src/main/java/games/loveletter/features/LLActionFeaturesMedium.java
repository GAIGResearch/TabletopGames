package games.loveletter.features;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.CardType;

import java.util.ArrayList;
import java.util.List;

import static utilities.Utils.enumNames;
import static utilities.Utils.enumToOneHot;

public class LLActionFeaturesMedium implements IActionFeatureVector {

    final String[] localNames;
    final int featuresPerGroup = CardType.values().length;

    public LLActionFeaturesMedium() {
        List<String> allNames = new ArrayList<>();
        // For each card type, is this the card played
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_PLAY").toList());
        // For each card type, is this the card guessed (where that is relevant - for GUARD)
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_GUESS").toList());
        localNames = allNames.toArray(new String[0]);
    }

    @Override
    public String[] names() {
        return localNames;
    }

    @Override
    public double[] doubleVector(AbstractAction a, AbstractGameState state, int playerID) {
        double[] retValue = new double[names().length];
        if (!(a instanceof PlayCard))
            return retValue;
        CardType cardPlayed = ((PlayCard) a).getCardType();
        CardType cardGuessed = ((PlayCard) a).getTargetCardType();
        System.arraycopy(enumToOneHot(cardPlayed), 0, retValue, 0, featuresPerGroup);
        if (cardGuessed != null)
            System.arraycopy(enumToOneHot(cardGuessed), 0, retValue, featuresPerGroup, featuresPerGroup);

        return retValue;
    }


}

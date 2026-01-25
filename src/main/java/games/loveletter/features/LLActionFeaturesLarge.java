package games.loveletter.features;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IActionFeatureVector;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.*;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;

import static utilities.Utils.enumNames;
import static utilities.Utils.enumToOneHot;

public class LLActionFeaturesLarge implements IActionFeatureVector {


    final String[] localNames;
    final int featuresPerGroup = CardType.values().length;

    public LLActionFeaturesLarge() {
        List<String> allNames = new ArrayList<>();
        // For each card type, is this the card played
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_PLAY").toList());
        // For each card type, is this the card guessed (where that is relevant - for GUARD)
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_GUESS").toList());
        // For each card type, does the target player have this card (to our knowledge)
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_HAS").toList());
        // current position of target player
        allNames.add("TARGET_ORDINAL");
        localNames = allNames.toArray(new String[0]);
    }

    @Override
    public String[] names() {
        return localNames;
    }

    @Override
    public double[] doubleVector(AbstractAction a, AbstractGameState state, int playerID) {
        double[] retValue = new double[names().length];
        LoveLetterGameState llgs = (LoveLetterGameState) state;
        if (!(a instanceof PlayCard))
            return retValue;
        CardType cardPlayed = ((PlayCard) a).getCardType();
        CardType cardGuessed = ((PlayCard) a).getTargetCardType();
        System.arraycopy(enumToOneHot(cardPlayed), 0, retValue, 0, featuresPerGroup);
        if (cardGuessed != null)
            System.arraycopy(enumToOneHot(cardGuessed), 0, retValue, featuresPerGroup, featuresPerGroup);
        // Now for our knowledge of the target player's hand
        int targetPlayer = ((PlayCard) a).getTargetPlayer();
        if (targetPlayer > -1) {
            PartialObservableDeck<LoveLetterCard> hand = llgs.getPlayerHandCards().get(targetPlayer);
            for (int i = 0; i < hand.getSize(); i++) {
                if (hand.isComponentVisible(i, playerID)) {
                    int index = hand.get(i).cardType.ordinal();
                    retValue[2 * featuresPerGroup + index] = 1.0;
                }
            }
            retValue[3 * featuresPerGroup] = llgs.getOrdinalPosition(targetPlayer);
        }
        return retValue;
    }


}

package games.loveletter;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IActionFeatureVector;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static utilities.Utils.enumNames;
import static utilities.Utils.enumToOneHot;

public class LLActionFeaturesLarge implements IActionFeatureVector {


    final String[] localNames;
    final int featuresPerGroup = LoveLetterCard.CardType.values().length;

    public LLActionFeaturesLarge() {
        List<String> allNames = new ArrayList<>();
        // For each card type, is this the card played
        allNames.addAll(enumNames(LoveLetterCard.CardType.class).stream().map(s -> s + "_PLAY").collect(toList()));
        // For each card type, is this the card guessed (where that is relevant - for GUARD)
        allNames.addAll(enumNames(LoveLetterCard.CardType.class).stream().map(s -> s + "_GUESS").collect(toList()));
        // For each card type, does the target player have this card (to our knowledge)
        allNames.addAll(enumNames(LoveLetterCard.CardType.class).stream().map(s -> s + "_HAS").collect(toList()));
        // current position of target player
        allNames.add("TARGET_ORDINAL");
        localNames = allNames.toArray(new String[0]);
    }

    @Override
    public String[] names() {
        return localNames;
    }

    @Override
    public double[] featureVector(AbstractAction a, AbstractGameState state, int playerID) {
        double[] retValue = new double[names().length];
        LoveLetterGameState llgs = (LoveLetterGameState) state;
        if (!(a instanceof PlayCard))
            return retValue;
        LoveLetterCard.CardType cardPlayed = ((PlayCard) a).getCardType();
        LoveLetterCard.CardType cardGuessed = ((PlayCard) a).getTargetCardType();
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

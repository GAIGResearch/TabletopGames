package games.toads.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.toads.ToadCard;
import games.toads.ToadConstants.ToadCardType;
import games.toads.ToadGameState;
import games.toads.actions.*;

import java.util.*;

public class ToadQFeatures001 implements IActionFeatureVector {

    final String[] localNames;

    public ToadQFeatures001() {
        List<String> names = new ArrayList<>();
        names.add("FLANK");
        names.add("ATTACKER");
        for (ToadCardType cardName : ToadCardType.values()) {
            names.add(cardName + "_PLAY");
            names.add(cardName + "_OPPOSITE");
            names.add(cardName + "_DIAGONAL");
            names.add(cardName + "_GUESS"); // for AssaultCannon
        }
        localNames = names.toArray(new String[0]);
    }

    @Override
    public double[] featureVector(AbstractAction action, AbstractGameState ags, int playerID) {
        double[] retValue = new double[names().length];
        ToadGameState state = (ToadGameState) ags;
        ToadCard oppField = state.getFieldCard(1 - playerID);
        // attacker goes first, so turn is even (starting with 0)
        retValue[1] = ags.getTurnCounter() % 2 == 0 ? 1 : 0;

        List<ToadCardType> allValues = List.of(ToadCardType.values());
        ToadCardType cardPlayed;
        if (action instanceof PlayFieldCard pfc) {
            cardPlayed = pfc.card.type;
            int indexOfCard = allValues.indexOf(cardPlayed);
            retValue[2 + indexOfCard] = 1;
            if (oppField != null) {
                int indexOfOppField = allValues.indexOf(oppField.type);
                retValue[2 + allValues.size() + indexOfOppField] = 1;
            }
        } else if (action instanceof PlayFlankCard pfc) {
            retValue[0] = 1;
            cardPlayed = pfc.card.type;
            int indexOfCard = allValues.indexOf(cardPlayed);
            retValue[2 + indexOfCard] = 1;
            if (oppField != null) {
                int indexOfOppField = allValues.indexOf(oppField.type);
                retValue[2 + 2 * allValues.size() + indexOfOppField] = 1;
            }
        } else if (action instanceof ForceOpponentDiscard fod) {
            cardPlayed = fod.type;
            int indexOfCard = allValues.indexOf(cardPlayed);
            retValue[2 + 3 * allValues.size() + indexOfCard] = 1;
        }

        return retValue;
    }

    @Override
    public String[] names() {
        return localNames;
    }
}

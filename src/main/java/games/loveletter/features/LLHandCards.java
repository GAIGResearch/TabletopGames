package games.loveletter.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class LLHandCards implements IStateFeatureVector {
    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        LoveLetterGameState llgs = (LoveLetterGameState) state;
        if (!llgs.isNotTerminalForPlayer(playerID)) {
            return new double[]{0.0, 0.0};
        }
        double[] retValue = new double[2];
        LoveLetterCard card1 = llgs.getPlayerHandCards().get(playerID).peek(0);
        LoveLetterCard card2 = llgs.getPlayerHandCards().get(playerID).peek(1);
        // we take the highest value card first
        if (card1.cardType.getValue() > card2.cardType.getValue()) {
            retValue[0] = card1.cardType.getValue();
            retValue[1] = card2.cardType.getValue();
        } else {
            retValue[0] = card2.cardType.getValue();
            retValue[1] = card1.cardType.getValue();
        }
        return retValue;
    }

    @Override
    public String[] names() {
        return new String[] {"HighCard", "LowCard"};
    }
}

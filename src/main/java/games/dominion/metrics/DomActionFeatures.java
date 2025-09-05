package games.dominion.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IActionFeatureVector;
import games.dominion.actions.*;
import games.dominion.cards.CardType;

public class DomActionFeatures implements IActionFeatureVector {
    String[] names = new String[]{
            "actionType",
            "cardType"
    };
    Class<?>[] types = new Class[]{
            ActionType.class,
            CardType.class
    };

    public enum ActionType {
        PLAY,
        BUY,
        TRASH,
        GAIN,
        DISCARD,
        PASS
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Object[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        Object[] retValue = new Object[names.length];
        if (action instanceof BuyCard buy) {
            retValue[0] = ActionType.BUY;
            retValue[1] = buy.cardType;
        } else if (action instanceof TrashCard trash) {
            retValue[0] = ActionType.TRASH;
            retValue[1] = trash.trashedCard;
        } else if (action instanceof GainCard gain) {
            retValue[0] = ActionType.GAIN;
            retValue[1] = gain.cardType;
        } else if (action instanceof DiscardCard discard) {
            retValue[0] = ActionType.DISCARD;
            retValue[1] = discard.type;
        } else if (action instanceof DominionAction play) {
            retValue[0] = ActionType.PLAY;
            retValue[1] = play.type;
        } else if (action instanceof DoNothing || action instanceof EndPhase) {
            retValue[0] = ActionType.PASS;
            retValue[1] = null; // No card type for DoNothing or EndPhase
        }
        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public Class[] types() {
        return types;
    }
}

package games.uno.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IActionFeatureVector;
import games.uno.UnoGameState;
import games.uno.actions.NoCards;
import games.uno.actions.PlayCard;
import games.uno.cards.UnoCard;

import java.util.Arrays;

public class UnoActionFeatures implements IActionFeatureVector {

    static String[] cardTypeNames = Arrays.stream(UnoCard.UnoCardType.values()).map(type -> "Play" + type.name()).toArray(String[]::new);
    static String[] allNames;
    static {
        allNames = new String[cardTypeNames.length + 3];
        allNames[0] = "PassDraw";
        System.arraycopy(cardTypeNames, 0, allNames, 1, cardTypeNames.length);
        allNames[allNames.length - 2] = "SameNumberInHand";
        allNames[allNames.length - 1] = "SameColorInHand";
    }

    @Override
    public String[] names() {
        return allNames;
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        double[] data = new double[allNames.length];
        if (action instanceof NoCards) {
            data[0] = 1.0;
        } else if (action instanceof PlayCard) {
            UnoCard card = (UnoCard) ((PlayCard) action).getCard(state);
            int typeIndex = card.unoCardType.ordinal();
            data[1 + typeIndex] = 1.0;

            if (card.unoCardType == UnoCard.UnoCardType.Number) {
                UnoGameState ugs = (UnoGameState) state;
                Deck<UnoCard> playerHand = ugs.getPlayerDecks().get(playerID);
                int sameNumber = 0;
                int sameColor = 0;
                for (UnoCard handCard : playerHand.getComponents()) {
                    if (handCard.getComponentID() != card.getComponentID()) {
                        if (handCard.unoCardType == UnoCard.UnoCardType.Number && handCard.number == card.number) {
                            sameNumber++;
                        }
                        if (handCard.color.equals(card.color)) {
                            sameColor++;
                        }
                    }
                }
                data[allNames.length - 2] = sameNumber;
                data[allNames.length - 1] = sameColor;
            }
        }
        return data;
    }
}

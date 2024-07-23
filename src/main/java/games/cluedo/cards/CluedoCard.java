package games.cluedo.cards;

import core.components.Card;
import games.cluedo.CluedoConstants;

public class CluedoCard extends Card {

    public Object cardName;

    protected CluedoCard(Object cardName) {
        super(cardName.toString());
        this.cardName = cardName;
    }

    protected CluedoCard(Object cardType, int ID) {
        super(cardType.toString(), ID);
        this.cardName = cardType;
    }

    @Override
    public Card copy() {
        return new CluedoCard(cardName, componentID);
    }

    @Override
    public String toString() {
        return cardName.toString();
    }
}

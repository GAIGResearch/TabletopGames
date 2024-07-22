package games.cluedo.cards;

import core.components.Card;
import games.cluedo.CluedoConstants;

public class CluedoCard extends Card {

    public CluedoConstants.Card cardType;

    public CluedoCard(CluedoConstants.Card cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public CluedoCard(CluedoConstants.Card cardType, int ID) {
        super(cardType.toString(), ID);
        this.cardType = cardType;
    }

    @Override
    public Card copy() {
        return new CluedoCard(cardType, componentID);
    }

    @Override
    public String toString() {
        return cardType.name();
    }
}

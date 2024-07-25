package games.cluedo.cards;

import core.components.Card;
import games.cluedo.CluedoConstants;

public class RoomCard extends CluedoCard {

    CluedoConstants.Room cardName;

    public RoomCard(CluedoConstants.Room cardName) {
        super(cardName);
        this.cardName = cardName;
    }

    @Override
    public String getName() {
        return cardName.name();
    }
}

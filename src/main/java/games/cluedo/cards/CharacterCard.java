package games.cluedo.cards;

import core.components.Card;
import games.cluedo.CluedoConstants;

public class CharacterCard extends CluedoCard {

    CluedoConstants.Character cardName;

    public CharacterCard(CluedoConstants.Character cardName) {
        super(cardName);
        this.cardName = cardName;
    }
}

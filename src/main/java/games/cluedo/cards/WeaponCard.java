package games.cluedo.cards;

import core.components.Card;
import games.cluedo.CluedoConstants;

public class WeaponCard extends CluedoCard {

    CluedoConstants.Weapon cardName;

    public WeaponCard(CluedoConstants.Weapon cardName) {
        super(cardName);
        this.cardName = cardName;
    }
}

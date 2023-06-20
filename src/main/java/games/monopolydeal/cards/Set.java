package games.monopolydeal.cards;

import core.CoreConstants;
import core.components.Deck;

public class Set extends Deck<MonopolyDealCard> {
    SetType type;

    public Set(String name, CoreConstants.VisibilityMode visibility,SetType type) {
        super(name, visibility);
        this.type = type;
    }
}

package games.catan.components;

import core.components.Card;
import games.catan.CatanParameters;

public class CatanCard extends Card {
    public final CardType cardType;
    public int roundCardWasBought = -1;  // -1 is not bought

    public CatanCard(CardType cardType) {
        super(cardType.name());
        this.cardType = cardType;
    }
    private CatanCard(CardType cardType, int id) {
        super(cardType.name(), id);
        this.cardType = cardType;
    }

    @Override
    public CatanCard copy() {
        CatanCard card = new CatanCard(cardType, componentID);
        card.roundCardWasBought = -1;  // Assigned in game state copy of the deck
        return card;
    }

    public enum CardType {
        KNIGHT_CARD,
        MONOPOLY,
        YEAR_OF_PLENTY,
        ROAD_BUILDING,
        VICTORY_POINT_CARD;

        public int nDeepSteps(CatanParameters params) {
            switch(this) {
                case KNIGHT_CARD:
                    return 2; // select location, select target
                case MONOPOLY:
                    return 1;  // select resource
                case YEAR_OF_PLENTY:
                    return params.nResourcesYoP; // select 1 resource at a time
                case ROAD_BUILDING:
                    return params.nRoadsRB;  // select 1 road at a time
            }
            return 0; // nothing to do
        }
    }
}

package games.catan.components;

import core.components.Card;

public class CatanCard extends Card {
    public final CardType cardType;
    public int turnCardWasBought = -1;  // -1 is not bought

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
        card.turnCardWasBought = turnCardWasBought;
        return card;
    }

    public enum CardType {
        KNIGHT_CARD,
        MONOPOLY,
        YEAR_OF_PLENTY,
        ROAD_BUILDING,
        VICTORY_POINT_CARD
    }
}

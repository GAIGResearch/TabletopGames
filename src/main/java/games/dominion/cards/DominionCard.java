package games.dominion.cards;

import core.components.*;

public class DominionCard extends Card {

    CardType type;

    private DominionCard(CardType type) {
        super(type.name());
        this.type = type;
    }

    private static DominionCard gold = new DominionCard(CardType.GOLD);
    private static DominionCard silver = new DominionCard(CardType.SILVER);
    private static DominionCard copper = new DominionCard(CardType.COPPER);
    private static DominionCard province = new DominionCard(CardType.PROVINCE);
    private static DominionCard duchy = new DominionCard(CardType.DUCHY);
    private static DominionCard estate = new DominionCard(CardType.ESTATE);

    public static DominionCard create(CardType type) {
        switch (type) {
            case GOLD:
                return gold;
            case COPPER:
                return copper;
            case SILVER:
                return silver;
            case ESTATE:
                return estate;
            case DUCHY:
                return duchy;
            case PROVINCE:
                return province;
            default:
                throw new AssertionError("Not yet implemented : " + type);
        }
    }

    public boolean isVictoryCard() {
        return type.getVictory() > 0;
    }

    public boolean isTreasureCard() {
        return type.getTreasure() > 0;
    }

    public int victoryPoints() {
        return type.getVictory();
    }

    public int treasureValue() {
        return type.getTreasure();
    }

    public int getCost() {
        return type.getCost();
    }

    public CardType cardType() {return type; }

    @Override
    public Card copy() {
        // Currently all cards are immutable - so we can save resources when copying
        return this;
    }

}



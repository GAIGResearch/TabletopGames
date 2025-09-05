package games.diamant.cards;

import core.components.Card;

// Diamant Cards
public class DiamantCard extends Card {

    public enum DiamantCardType {
        Treasure,
        Hazard,
        Relic
    }

    public enum HazardType {
        Scorpions,
        Snakes,
        PoisonGas,
        Explosions,
        Rockfalls,
        None
    }

    private final DiamantCardType cardType;
    private final HazardType      hazardType;
    private final int value;

    public DiamantCard(DiamantCardType cardType, HazardType hazardType, int NumberOfGems) {
        super(cardType.toString());
        this.cardType     = cardType;
        this.hazardType   = hazardType;
        this.value = NumberOfGems;
    }

    public DiamantCard(DiamantCardType cardType, HazardType hazardType, int NumberOfGems, int ID) {
        super(cardType.toString(), ID);
        this.cardType     = cardType;
        this.hazardType   = hazardType;
        this.value = NumberOfGems;
    }

    public DiamantCardType getCardType()     { return cardType;     }
    public HazardType      getHazardType()   { return hazardType;   }
    public int getValue() { return value; }

    @Override
    public String toString() {
        return switch(cardType) {
            case Treasure -> "Treasure " + value;
            case Hazard -> hazardType.toString();
            case Relic -> "Relic " + value;
        };
    }

    @Override
    public Card copy() {
        return this;
    }
}

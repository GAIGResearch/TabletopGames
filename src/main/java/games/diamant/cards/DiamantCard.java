package games.diamant.cards;

import core.components.Card;

public class DiamantCard extends Card {

    public enum DiamantCardType {
        Treasure,
        Hazard,
        Artifact
    }

    public enum HazardType {
        Scorpions,
        Snakes,
        PoissonGas,
        Explosions,
        Rockfalls,
        None
    }

    private final DiamantCardType cardType;
    private final HazardType      hazardType;
    private final int             NumberOfGems;

    public DiamantCard(DiamantCardType cardType, HazardType hazardType, int NumberOfGems) {
        super(cardType.toString());
        this.cardType = cardType;
        this.hazardType = hazardType;
        this.NumberOfGems = NumberOfGems;
    }

    public DiamantCard(DiamantCardType cardType, HazardType hazardType, int NumberOfGems, int ID) {
        super(cardType.toString(), ID);
        this.cardType = cardType;
        this.hazardType = hazardType;
        this.NumberOfGems = NumberOfGems;
    }

    public DiamantCardType getCardType()     { return cardType;     }
    public HazardType      getHazardType()   { return hazardType;   }
    public int             getNumberOfGems() { return NumberOfGems; }

    @Override
    public String toString() {
        String str = "";
        if      (cardType == DiamantCardType.Treasure) str = "DiamantCard { Treasure : " + NumberOfGems + "}";
        else if (cardType == DiamantCardType.Hazard)   str = "DiamantCard { Hazard : " + hazardType.toString() + "}";
        else if (cardType == DiamantCardType.Artifact) str = "DiamantCard { Treasure : Artifact }";

        return str;
    }

    @Override
    public Card copy() {
        return new DiamantCard(cardType, hazardType, NumberOfGems, componentID);
    }
}

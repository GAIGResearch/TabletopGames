package games.dominion.cards;

import core.components.*;
import games.dominion.actions.DominionAction;
import games.dominion.actions.Laboratory;
import games.dominion.actions.Smithy;
import games.dominion.actions.Village;

import java.util.*;

public class DominionCard extends Card {

    private static Map<CardType, DominionAction> typeToAction = new HashMap<>();
    static {
        typeToAction.put(CardType.VILLAGE, new Village());
        typeToAction.put(CardType.LABORATORY, new Laboratory());
        typeToAction.put(CardType.SMITHY, new Smithy());
    }


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
    private static DominionCard village = new DominionCard(CardType.VILLAGE);

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
            case VILLAGE:
                return village;
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

    public boolean isActionCard() {
        return type.isActionCard();
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

    public CardType cardType() {
        return type;
    }

    public DominionAction getAction() {
        DominionAction retValue = typeToAction.get(type);
        if (retValue == null)
            throw new AssertionError("No action found for " + type);
        return retValue;
    }

    @Override
    public Card copy() {
        // Currently all cards are immutable - so we can save resources when copying
        return this;
    }

}



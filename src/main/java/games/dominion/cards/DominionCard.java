package games.dominion.cards;

import core.actions.AbstractAction;
import core.components.*;
import games.dominion.DominionConstants;
import games.dominion.DominionGame;
import games.dominion.actions.*;

import java.util.Objects;

public class DominionCard extends Card {

    CardType type;

    protected DominionCard(CardType type) {
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
            case VILLAGE:
            case SMITHY:
            case LABORATORY:
            case WOODCUTTER:
            case MARKET:
            case FESTIVAL:
            case CELLAR:
            case MILITIA:
            case MOAT:
            case REMODEL:
                return new DominionCard(type);
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

    public DominionAction getAction(int playerId) {
        switch (type) {
            case VILLAGE:
                return new Village(playerId);
            case SMITHY:
                return new Smithy(playerId);
            case LABORATORY:
                return new Laboratory(playerId);
            case WOODCUTTER:
                return new Woodcutter(playerId);
            case FESTIVAL:
                return new Festival(playerId);
            case MARKET:
                return new Market(playerId);
            case CELLAR:
                return new Cellar(playerId);
            case MILITIA:
                return new Militia(playerId);
            case MOAT:
                return new Moat(playerId);
            case REMODEL:
                return new Remodel(playerId);
            default:
                throw new AssertionError("No action for : " + type);
        }
    }

    public boolean hasAttackReaction() {return type.hasAttackReaction();}

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

    @Override
    public DominionCard copy() {
        // Currently all cardTypes are immutable - so we can save resources when copying
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DominionCard) {
            DominionCard other = (DominionCard) obj;
            return other.type == type;
        }
        return false;
    }
}



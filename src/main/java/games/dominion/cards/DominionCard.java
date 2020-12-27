package games.dominion.cards;

import core.actions.AbstractAction;
import core.components.*;
import games.dominion.DominionConstants;
import games.dominion.DominionGame;
import games.dominion.DominionGameState;
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
    private static DominionCard curse = new DominionCard(CardType.CURSE);

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
            case CURSE:
                return curse;
            case VILLAGE:
            case SMITHY:
            case LABORATORY:
            case MARKET:
            case FESTIVAL:
            case CELLAR:
            case MILITIA:
            case MOAT:
            case REMODEL:
            case MERCHANT:
            case MINE:
            case WORKSHOP:
            case ARTISAN:
            case MONEYLENDER:
            case POACHER:
            case WITCH:
            case CHAPEL:
            case HARBINGER:
            case THRONE_ROOM:
            case BANDIT:
            case BUREAUCRAT:
                return new DominionCard(type);
            case GARDENS:
                return new Gardens();
            default:
                throw new AssertionError("Not yet implemented : " + type);
        }
    }

    public boolean isTreasureCard() {
        return type.isTreasure;
    }

    public boolean isActionCard() {
        return type.isAction;
    }

    public boolean isVictoryCard() {
        return type.isVictory;
    }

    public DominionAction getAction(int playerId) {
        switch (type) {
            case VILLAGE:
                return new Village(playerId);
            case SMITHY:
                return new Smithy(playerId);
            case LABORATORY:
                return new Laboratory(playerId);
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
            case MERCHANT:
                return new Merchant(playerId);
            case MINE:
                return new Mine(playerId);
            case WORKSHOP:
                return new Workshop(playerId);
            case ARTISAN:
                return new Artisan(playerId);
            case MONEYLENDER:
                return new Moneylender(playerId);
            case POACHER:
                return new Poacher(playerId);
            case WITCH:
                return new Witch(playerId);
            case CHAPEL:
                return new Chapel(playerId);
            case HARBINGER:
                return new Harbinger(playerId);
            case THRONE_ROOM:
                return new ThroneRoom(playerId);
            case BANDIT:
                return new Bandit(playerId);
            case BUREAUCRAT:
                return new Bureaucrat(playerId);
            default:
                throw new AssertionError("No action for : " + type);
        }
    }

    public boolean hasAttackReaction() {
        return type.isReaction;
    }

    public AbstractAction getAttackReaction(int playerId) {
        switch (type) {
            case MOAT:
                return new MoatReaction(playerId);
            default:
                throw new AssertionError("Nope - no Attack Reaction for " + this);
        }
    }

    public int victoryPoints(int player, DominionGameState context) {
        return type.victory;
    }

    public int treasureValue() {
        return type.treasure;
    }

    public int getCost() {
        return type.cost;
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



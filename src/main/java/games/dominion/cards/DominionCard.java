package games.dominion.cards;

import core.actions.AbstractAction;
import core.components.Card;
import games.dominion.DominionGameState;
import games.dominion.actions.*;

public class DominionCard extends Card {

    CardType type;

    protected DominionCard(CardType type) {
        super(type.name());
        this.type = type;
    }

    public static DominionCard create(CardType type) {
        return switch (type) {
            case GOLD, COPPER, SILVER, ESTATE, DUCHY, PROVINCE, CURSE, VILLAGE, SMITHY, LABORATORY, MARKET, FESTIVAL,
                 CELLAR, MILITIA, MOAT, REMODEL, MERCHANT, MINE, WORKSHOP, ARTISAN, MONEYLENDER, POACHER, WITCH, CHAPEL,
                 HARBINGER, THRONE_ROOM, BANDIT, BUREAUCRAT, SENTRY, VASSAL, LIBRARY, COUNCIL_ROOM -> new DominionCard(type);
            case GARDENS -> new Gardens();
            default -> throw new AssertionError("Not yet implemented : " + type);
        };
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
        return getAction(playerId, false);
    }

    public DominionAction getAction(int playerId, boolean dummy) {
        switch (type) {
            case VILLAGE:
            case SMITHY:
            case LABORATORY:
            case FESTIVAL:
            case MARKET:
            case MOAT:
                return new SimpleAction(type, playerId, dummy);
            case CELLAR:
                return new Cellar(playerId, dummy);
            case MILITIA:
                return new Militia(playerId, dummy);
            case REMODEL:
                return new Remodel(playerId, dummy);
            case MERCHANT:
                return new Merchant(playerId, dummy);
            case MINE:
                return new Mine(playerId, dummy);
            case WORKSHOP:
                return new Workshop(playerId, dummy);
            case ARTISAN:
                return new Artisan(playerId, dummy);
            case MONEYLENDER:
                return new Moneylender(playerId, dummy);
            case POACHER:
                return new Poacher(playerId, dummy);
            case WITCH:
                return new Witch(playerId, dummy);
            case CHAPEL:
                return new Chapel(playerId, dummy);
            case HARBINGER:
                return new Harbinger(playerId, dummy);
            case THRONE_ROOM:
                return new ThroneRoom(playerId, dummy);
            case BANDIT:
                return new Bandit(playerId, dummy);
            case BUREAUCRAT:
                return new Bureaucrat(playerId, dummy);
            case SENTRY:
                return new Sentry(playerId, dummy);
            case LIBRARY:
                return new Library(playerId, dummy);
            case VASSAL:
                return new Vassal(playerId, dummy);
            case COUNCIL_ROOM:
                return new CouncilRoom(playerId, dummy);
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



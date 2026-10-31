package games.dominion.cards;

import core.actions.AbstractAction;
import core.components.Card;
import games.dominion.DominionGameState;
import games.dominion.actions.*;

public class DominionCard extends Card {

    CardType cardType;

    protected DominionCard(CardType cardType) {
        super(cardType.name());
        this.cardType = cardType;
    }

    public static DominionCard create(CardType cardType) {
        return switch (cardType) {
            case GOLD, COPPER, SILVER, ESTATE, DUCHY, PROVINCE, CURSE, VILLAGE, SMITHY, LABORATORY, MARKET, FESTIVAL,
                 CELLAR, MILITIA, MOAT, REMODEL, MERCHANT, MINE, WORKSHOP, ARTISAN, MONEYLENDER, POACHER, WITCH, CHAPEL,
                 HARBINGER, THRONE_ROOM, BANDIT, BUREAUCRAT, SENTRY, VASSAL, LIBRARY, COUNCIL_ROOM -> new DominionCard(cardType);
            case GARDENS -> new Gardens();
            default -> throw new AssertionError("Not yet implemented : " + cardType);
        };
    }

    public boolean isTreasureCard() {
        return cardType.isTreasure;
    }

    public boolean isActionCard() {
        return cardType.isAction;
    }

    public boolean isVictoryCard() {
        return cardType.isVictory;
    }

    public DominionAction getAction(int playerId) {
        return getAction(playerId, false);
    }

    public DominionAction getAction(int playerId, boolean dummy) {
        switch (cardType) {
            case VILLAGE:
            case SMITHY:
            case LABORATORY:
            case FESTIVAL:
            case MARKET:
            case MOAT:
                return new SimpleAction(cardType, playerId, dummy);
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
                throw new AssertionError("No action for : " + cardType);
        }
    }

    public boolean hasAttackReaction() {
        return cardType.isReaction;
    }

    public AbstractAction getAttackReaction(int playerId) {
        switch (cardType) {
            case MOAT:
                return new MoatReaction(playerId);
            default:
                throw new AssertionError("Nope - no Attack Reaction for " + this);
        }
    }

    public int victoryPoints(int player, DominionGameState context) {
        return cardType.victory;
    }

    public int treasureValue() {
        return cardType.treasure;
    }

    public int getCost() {
        return cardType.cost;
    }

    public CardType cardType() {
        return cardType;
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
            return other.cardType == cardType;
        }
        return false;
    }
}



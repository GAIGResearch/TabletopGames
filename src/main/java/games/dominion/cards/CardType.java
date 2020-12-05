package games.dominion.cards;

import core.actions.*;
import games.dominion.actions.IBuyPhaseEffect;
import games.dominion.actions.MerchantBuyEffect;
import games.dominion.actions.MoatReaction;

import java.util.*;

public enum CardType {
    CURSE(0, 0, -1, false),
    ESTATE(2, 0, 1, false),
    DUCHY(5, 0, 3, false),
    PROVINCE(8, 0, 6, false),
    COPPER(0, 1, 0, false),
    SILVER(3, 2, 0, false),
    GOLD(6, 3, 0, false),
    CHANCELLOR(3, 0, 0),
    WOODCUTTER(3, 0, 0),
    FEAST(5, 0, 0),
    SPY(5, 0, 0),
    THIEF(4, 0, 0),
    ADVENTURER(6, 0, 0),
    CELLAR(2, 0, 0),
    CHAPEL(2, 0, 0),
    MOAT(2, 0, 0),
    HARBINGER(4, 0, 0),
    MERCHANT(3, 0, 0),
    VASSAL(5, 0, 0),
    VILLAGE(3, 0, 0),
    WORKSHOP(3, 0, 0),
    BUREAUCRAT(4, 0, 0),
    GARDENS(4, 0, 0),
    MILITIA(4, 0, 0),
    MONEYLENDER(4, 0, 0),
    POACHER(4, 0, 0),
    REMODEL(4, 0, 0),
    SMITHY(4, 0, 0),
    THRONE_ROOM(5, 0, 0),
    BANDIT(5, 0, 0),
    COUNCIL_ROOM(5, 0, 0),
    FESTIVAL(5, 0, 0),
    LABORATORY(5, 0, 0),
    LIBRARY(5, 0, 0),
    MARKET(5, 0, 0),
    MINE(5, 0, 0),
    SENTRY(3, 0, 0),
    WITCH(5, 0, 0),
    ARTISAN(4, 0, 0);

    private final int cost;
    private final int treasure;
    private final int victory;
    private final boolean action;

    private CardType(int cost, int treasure, int victory) {
        this.cost = cost;
        this.treasure = treasure;
        this.victory = victory;
        this.action = true;
    }
    private CardType(int cost, int treasure, int victory, boolean action) {
        this.cost = cost;
        this.treasure = treasure;
        this.victory = victory;
        this.action = action;
    }

    public int getCost(){return cost;}
    public int getTreasure(){return treasure;}
    public boolean isTreasure() {
        return treasure > 0;
    }
    public int getVictory(){return victory;}
    public boolean isActionCard(){return action;}

    public boolean hasAttackReaction() {
        switch (this) {
            case MOAT:
                return true;
            default:
                return false;
        }
    }

    public boolean hasBuyEffect() {
        switch (this) {
            case MERCHANT:
                return true;
            default:
                return false;
        }
    }

    public AbstractAction getAttackReaction(int playerId) {
        switch (this) {
            case MOAT:
                return new MoatReaction(playerId);
            default:
                throw new AssertionError("Nope - no Attack Reaction for " + this);
        }
    }

    public IBuyPhaseEffect getBuyEffect() {
        switch (this) {
            case MERCHANT:
                return new MerchantBuyEffect();
            default:
                throw new AssertionError("Nope - no Buy Effect for " + this);
        }
    }


}

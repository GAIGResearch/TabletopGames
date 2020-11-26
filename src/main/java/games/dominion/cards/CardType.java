package games.dominion.cards;

import core.actions.*;

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
    MERCHANT(5, 0, 0),
    VASSAL(5, 0, 0),
    VILLAGE(3, 0, 0),
    WORKSHOP(4, 0, 0),
    BUREAUCRAT(4, 0, 0),
    GARDENS(4, 0, 0),
    MILITIA(4, 0, 0),
    MONEYLENDER(4, 0, 0),
    POACHER(4, 0, 0),
    REMODEL(5, 0, 0),
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


}

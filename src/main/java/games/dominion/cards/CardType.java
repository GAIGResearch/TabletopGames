package games.dominion.cards;


public enum CardType {
    CURSE(0, 0, -1),
    ESTATE(2, 0, 1),
    DUCHY(5, 0, 3),
    PROVINCE(8, 0, 6),
    COPPER(0, 1, 0),
    SILVER(3, 2, 0),
    GOLD(6, 3, 0),
    CELLAR(2, 0, 0, 1, 0, 0, 0),
    CHAPEL(2, 0, 0),
    MOAT(2, 0, 0, false, false, true, true, false, 0, 2, 0, 0),
    HARBINGER(3, 0, 0, 1, 1, 0, 0),
    MERCHANT(3, 0, 0, 1, 1, 0, 0),
    VASSAL(3, 0, 0, false, false, true, false, false, 0, 0, 0, 2),
    VILLAGE(3, 0, 0, 2, 1, 0, 0),
    WORKSHOP(3, 0, 0),
    BUREAUCRAT(4, 0, 0, false, false, true, false, true),
    GARDENS(4, 0, 0, true, false, false, false, false),
    MILITIA(4, 0, 0, false, false, true, false, true, 0, 0, 0, 2),
    MONEYLENDER(4, 0, 0),
    POACHER(4, 0, 0, 1, 1, 0, 1),
    REMODEL(4, 0, 0),
    SMITHY(4, 0, 0, 0, 3, 0, 0),
    THRONE_ROOM(4, 0, 0),
    BANDIT(5, 0, 0, false, false, true, false, true),
    COUNCIL_ROOM(5, 0, 0, false, false ,true, false, false, 0, 4, 1, 0),
    FESTIVAL(5, 0, 0, 2, 0, 1, 2),
    LABORATORY(5, 0, 0, 1, 2, 0, 0),
    LIBRARY(5, 0, 0, false, false, true, false, false, 0, 0, 0, 0),
    MARKET(5, 0, 0, 1, 1, 1, 1),
    MINE(5, 0, 0),
    SENTRY(5, 0, 0, 1, 1, 0, 0),
    WITCH(5, 0, 0, false, false, true, false, true, 0, 2, 0, 0),
    ARTISAN(6, 0, 0);

    public final int cost;
    public final int treasure;
    public final int victory;
    public final boolean isAction;
    public final boolean isVictory;
    public final boolean isTreasure;
    public final boolean isReaction;
    public final boolean isAttack;
    public final int plusActions;
    public final int plusDraws;
    public final int plusBuys;
    public final int plusMoney;


    CardType(int cost, int treasure, int victory,
             boolean isVictory, boolean isTreasure, boolean isAction, boolean isReaction, boolean isAttack,
             int plusActions, int plusDraws, int plusBuys, int plusMoney) {
        this.cost = cost;
        this.treasure = treasure;
        this.victory = victory;
        this.isAction = isAction;
        this.isReaction = isReaction;
        this.isAttack = isAttack;
        this.isVictory = isVictory;
        this.isTreasure = isTreasure;
        this.plusActions = plusActions;
        this.plusBuys = plusBuys;
        this.plusDraws = plusDraws;
        this.plusMoney = plusMoney;
    }

    CardType(int cost, int treasure, int victory,
             boolean isVictory, boolean isTreasure, boolean isAction, boolean isReaction, boolean isAttack) {
        this(cost, treasure, victory, isVictory, isTreasure, isAction, isReaction, isAttack, 0, 0, 0, 0);
    }

    CardType(int cost, int treasure, int victory, int plusActions, int plusDraws, int plusBuys, int plusMoney) {
        this(cost, treasure, victory,
                victory > 0, treasure > 0, treasure == 0 && victory == 0, false, false,
                plusActions, plusDraws, plusBuys, plusMoney);
    }

    CardType(int cost, int treasure, int victory) {
        this(cost, treasure, victory, 0, 0, 0, 0);
    }

}

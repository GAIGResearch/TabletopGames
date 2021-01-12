package games.dominion.cards;


public enum CardType {
    CURSE(0, 0, -1),
    ESTATE(2, 0, 1),
    DUCHY(5, 0, 3),
    PROVINCE(8, 0, 6),
    COPPER(0, 1, 0),
    SILVER(3, 2, 0),
    GOLD(6, 3, 0),
    CELLAR(2, 0, 0),
    CHAPEL(2, 0, 0),
    MOAT(2, 0, 0, false, false, true, true, false),
    HARBINGER(3, 0, 0),
    MERCHANT(3, 0, 0),
    VASSAL(3, 0, 0),
    VILLAGE(3, 0, 0),
    WORKSHOP(3, 0, 0),
    BUREAUCRAT(4, 0, 0, false, false, true, false, true),
    GARDENS(4, 0, 0, true, false, false, false, false),
    MILITIA(4, 0, 0, false, false, true, false, true),
    MONEYLENDER(4, 0, 0),
    POACHER(4, 0, 0),
    REMODEL(4, 0, 0),
    SMITHY(4, 0, 0),
    THRONE_ROOM(4, 0, 0),
    BANDIT(5, 0, 0, false, false, true, false, true),
    COUNCIL_ROOM(5, 0, 0),
    FESTIVAL(5, 0, 0),
    LABORATORY(5, 0, 0),
    LIBRARY(5, 0, 0),
    MARKET(5, 0, 0),
    MINE(5, 0, 0),
    SENTRY(5, 0, 0),
    WITCH(5, 0, 0, false, false, true, false, true),
    ARTISAN(6, 0, 0);

    public final int cost;
    public final int treasure;
    public final int victory;
    public final boolean isAction;
    public final boolean isVictory;
    public final boolean isTreasure;
    public final boolean isReaction;
    public final boolean isAttack;

    CardType(int cost, int treasure, int victory) {
        this.cost = cost;
        this.treasure = treasure;
        this.victory = victory;
        this.isAction = treasure == 0 && victory == 0;
        this.isTreasure = treasure > 0;
        this.isVictory = victory > 0;
        this.isReaction = false;
        this.isAttack = false;
    }
    CardType(int cost, int treasure, int victory, boolean isVictory, boolean isTreasure, boolean isAction, boolean isReaction, boolean isAttack) {
        this.cost = cost;
        this.treasure = treasure;
        this.victory = victory;
        this.isAction = isAction;
        this.isReaction = isReaction;
        this.isAttack = isAttack;
        this.isVictory = isVictory;
        this.isTreasure = isTreasure;
    }

}

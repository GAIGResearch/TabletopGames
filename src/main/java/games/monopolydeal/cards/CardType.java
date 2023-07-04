package games.monopolydeal.cards;

public enum CardType {

    // All card Types
    Money10(10),
    Money5(5),
    Money4(4),
    Money3(3),
    Money2(2),
    Money1(1),

    PassGo(1,true),
    DoubleTheRent(1,true),

    ItsMyBirthday(2,true),
    DebtCollector(5,true),

    SlyDeal(3,true),
    ForcedDeal(3,true),

    DealBreaker(5,true),
    JustSayNo(4,true),

    MulticolorRent(1,true),
    GreenBlueRent(1,true),
    BrownLightBlueRent(1,true),
    PinkOrangeRent(1,true),
    RailRoadUtilityRent(1,true),
    RedYellowRent(1,true),

    House(3,true),
    Hotel(4,true),

    BrownProperty(1,true,false),
    BlueProperty(1,true,false),
    GreenProperty(1,true,false),
    LightBlueProperty(1,true,false),
    OrangeProperty(1,true,false),
    PinkProperty(1,true,false),
    RailRoadProperty(1,true,false),
    UtilityProperty(1,true,false),
    RedProperty(1,true,false),
    YellowProperty(1,true,false),

    MulticolorWild(-1,true,true),
    GreenBlueWild(4,true,true),
    BrownLightBlueWild(1,true,true),
    PinkOrangeWild(2,true,true),
    RailRoadGreenWild(4,true,true),
    RailRoadLightBlueWild(4,true,true),
    RailRoadUtilityWild(2,true,true),
    RedYellowWild(3,true,true);

    public final int moneyValue;
    public final boolean isAction;
    public final boolean isProperty;
    public final boolean isPropertyWild;

    CardType(int moneyValue,
             boolean isProperty, boolean isAction, boolean isPropertyWild) {
        this.moneyValue = moneyValue;
        this.isAction = isAction;
        this.isPropertyWild = isPropertyWild;
        this.isProperty = isProperty;
    }
    // Money cards
    CardType(int moneyValue){
        this(moneyValue,false,false,false);

    }
    CardType(int moneyValue,boolean isAction){
        this(moneyValue,false,isAction,false);

    }
    CardType(int moneyValue, boolean isProperty, boolean isPropertyWild){
        this(moneyValue,isProperty,false,isPropertyWild);
    }
}

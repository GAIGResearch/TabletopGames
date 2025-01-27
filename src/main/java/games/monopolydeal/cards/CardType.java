package games.monopolydeal.cards;

public enum CardType {

    // All card Types
    Money10(10),
    Money5(5),
    Money4(4),
    Money3(3),
    Money2(2),
    Money1(1),

    PassGo(1,true),  //y
    DoubleTheRent(1,true),
    ItsMyBirthday(2,true), //y
    DebtCollector(5,true), //y
    SlyDeal(3,true), //y
    ForcedDeal(3,true), //y
    DealBreaker(5,true), //y
    JustSayNo(4,true),
    MulticolorRent(1,true), //y
    GreenBlueRent(1,true), //y/5
    BrownLightBlueRent(1,true), //y/5
    PinkOrangeRent(1,true),  //y/5
    RailRoadUtilityRent(1,true), //y/5
    RedYellowRent(1,true), //y/5
    House(3,true),
    Hotel(4,true),

    BrownProperty(1,true,false),
    BlueProperty(4,true,false),
    GreenProperty(4,true,false),
    LightBlueProperty(1,true,false),
    OrangeProperty(2,true,false),
    PinkProperty(2,true,false),
    RailRoadProperty(2,true,false),
    UtilityProperty(2,true,false),
    RedProperty(3,true,false),
    YellowProperty(3,true,false),

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

    public SetType getSetType(){
        SetType sType;
        switch (this){
            case BrownProperty:
            case BrownLightBlueWild:
                sType = SetType.Brown;
                break;
            case BlueProperty:
                sType = SetType.Blue;
                break;
            case GreenProperty:
            case GreenBlueWild:
                sType = SetType.Green;
                break;
            case LightBlueProperty:
                sType = SetType.LightBlue;
                break;
            case OrangeProperty:
                sType = SetType.Orange;
                break;
            case PinkProperty:
            case PinkOrangeWild:
                sType = SetType.Pink;
                break;
            case RailRoadProperty:
            case RailRoadGreenWild:
            case RailRoadLightBlueWild:
            case RailRoadUtilityWild:
                sType = SetType.RailRoad;
                break;
            case RedProperty:
                sType = SetType.Red;
                break;
            case UtilityProperty:
                sType = SetType.Utility;
                break;
            case YellowProperty:
            case RedYellowWild:
                sType = SetType.Yellow;
                break;
            default:
                sType = SetType.UNDEFINED;
                break;
        }
        return sType;
    }
}

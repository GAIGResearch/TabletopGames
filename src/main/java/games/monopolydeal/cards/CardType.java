package games.monopolydeal.cards;

public enum CardType {

    Money10(10),
    Money5(5),
    Money4(4),
    Money3(3),
    Money2(2),
    Money1(1);


    public final int moneyValue;
    public final int property;
    public final boolean isAction;
    public final boolean isProperty;
    public final boolean isRent;
    public final boolean isPropertyWild;



    CardType(int moneyValue, int property,
             boolean isProperty, boolean isRent, boolean isAction, boolean isPropertyWild) {
        this.moneyValue = moneyValue;
        this.property = property;
        this.isAction = isAction;
        this.isPropertyWild = isPropertyWild;
        this.isProperty = isProperty;
        this.isRent = isRent;
    }
    // Money cards
    CardType(int moneyValue){
        this(moneyValue,-1,false,false,false,false);

    }
}

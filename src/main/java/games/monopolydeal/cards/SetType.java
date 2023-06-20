package games.monopolydeal.cards;

public enum SetType {

    Blue(2,new int[]{3,8}),
    LightBlue(3);

    public final int setSize;
    public final int[] rent;

    SetType(){
        this(3);
    }
    SetType(int setSize){
        this(3,new int[]{1,2,3});
    }
    SetType(int setSize, int[] rent){
        this.setSize = setSize;
        this.rent = rent;
    }
}

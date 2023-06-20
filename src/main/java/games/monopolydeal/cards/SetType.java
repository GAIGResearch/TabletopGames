package games.monopolydeal.cards;

public enum SetType {

    Blue(2,new int[]{3,8}),
    LightBlue(3);

    public final int setSize;
    public final boolean hasWild;
    public final boolean hasHouse;
    public final boolean hasHotel;
    public final int[] rent;

    SetType(){
        this(3);
    }
    SetType(int setSize){
        this(3,new int[]{1,2,3});
    }
    SetType(int setSize, int[] rent){
        this.setSize = setSize;
        this.hasWild = false;
        this.hasHouse = false;
        this.hasHotel = false;
        this.rent = rent;
    }
}

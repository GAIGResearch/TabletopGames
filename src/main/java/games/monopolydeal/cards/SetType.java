package games.monopolydeal.cards;

public enum SetType {

    Brown(2,new int[]{1,2}),
    Blue(2,new int[]{3,8}),
    Green(3,new int[]{2,4,7}),
    LightBlue(3,new int[]{1,2,3}),
    Orange(3,new int[]{1,3,5}),
    Pink(3,new int[]{1,2,4}),
    RailRoad(4,new int[]{1,2,3,4}),
    Red(3,new int[]{2,3,6}),
    Utility(2,new int[]{1,2}),
    Yellow(3,new int[]{2,4,6}),
    UNDEFINED(100,new int[]{0,0,0,0});

    public final int setSize;
    public final int[] rent;

    SetType(int setSize, int[] rent){
        this.setSize = setSize;
        this.rent = rent;
    }
}

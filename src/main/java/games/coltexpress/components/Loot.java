package games.coltexpress.components;

public class Loot {
    public enum LootType {
        Purse,
        Jewel,
        Strongbox
    }

    private int value;
    private LootType type;

    public Loot(LootType type, int value){
        this.type = type;
        this.value = value;
    }

    public static Loot createJewel(){
        return new Loot(LootType.Jewel, 500);
    }

    public static Loot createStrongbox(){
        return new Loot(LootType.Strongbox, 1000);
    }

    public int getValue(){
        return value;
    }
    public LootType getType(){
        return type;
    }

    public String toString(){
        return this.type.toString() + "_" + this.value;
    }
}

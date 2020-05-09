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

    public int getValue(){
        return value;
    }

    public String toString(){
        return this.type.toString() + "_" + this.value;
    }
}

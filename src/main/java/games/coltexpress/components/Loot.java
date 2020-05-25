package games.coltexpress.components;

import core.components.Component;
import utilities.Utils;

public class Loot extends Component {

    public Loot(LootType type, int value) {
        super(Utils.ComponentType.TOKEN, type.toString());
        this.type = type;
        this.value = value;
    }

    @Override
    public Component copy() {
        return null;
    }

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

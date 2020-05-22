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

    public int getValue(){
        return value;
    }
}

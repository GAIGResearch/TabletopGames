package games.coltexpress.components;

import core.CoreConstants;
import core.components.Component;
import games.coltexpress.ColtExpressTypes;

public class Loot extends Component {

    private int value;
    private ColtExpressTypes.LootType type;

    public Loot(ColtExpressTypes.LootType type, int value){
        super(CoreConstants.ComponentType.TOKEN, type.toString());
        this.type = type;
        this.value = value;
    }

    private Loot(ColtExpressTypes.LootType type, int value, int ID){
        super(CoreConstants.ComponentType.TOKEN, type.toString(), ID);
        this.type = type;
        this.value = value;
    }

    public static Loot createJewel(){
        return new Loot(ColtExpressTypes.LootType.Jewel, 500);
    }

    public static Loot createStrongbox(){
        return new Loot(ColtExpressTypes.LootType.Strongbox, 1000);
    }

    // point value of the loot
    public int getValue(){
        return value;
    }
    public ColtExpressTypes.LootType getLootType(){
        return type;
    }

    @Override
    public String toString(){
        return this.type + "_" + this.value;
    }

    @Override
    public Component copy() {
        return new Loot(type, value, componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loot)) return false;
        if (!super.equals(o)) return false;
        Loot loot = (Loot) o;
        return value == loot.value &&
                type == loot.type;
    }

}

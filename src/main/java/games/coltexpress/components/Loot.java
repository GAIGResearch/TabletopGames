package games.coltexpress.components;

import core.components.Component;
import games.coltexpress.ColtExpressParameters;
import utilities.Utils;

public class Loot extends Component {

    private int value;
    private ColtExpressParameters.LootType type;

    public Loot(ColtExpressParameters.LootType type, int value){

        super(Utils.ComponentType.TOKEN, type.toString());
        this.type = type;
        this.value = value;
    }

    public static Loot createJewel(){
        return new Loot(ColtExpressParameters.LootType.Jewel, 500);
    }

    public static Loot createStrongbox(){
        return new Loot(ColtExpressParameters.LootType.Strongbox, 1000);
    }

    public int getValue(){
        return value;
    }
    public ColtExpressParameters.LootType getLootType(){
        return type;
    }

    @Override
    public String toString(){
        return this.type + "_" + this.value;
    }

    @Override
    public Component copy() {
        return null;
    }

}

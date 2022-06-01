package games.descent2e.components;

import core.properties.Property;
import core.properties.PropertyStringArray;

import java.util.Map;

import static games.descent2e.DescentConstants.*;

public class Monster extends Figure {

    int orientation;  // medium monsters might be vertical (0) or horizontal (1)

    public Monster() {
        super("Monster");
    }

    public void setProperties(Map<Integer, Property> props) {
        for (Property p: props.values()) {
            setProperty(p);
        }
        String[] attack = ((PropertyStringArray) getProperty(attackHash)).getValues();
        attackDice = DicePool.constructDicePool(attack);
        String[] defence = ((PropertyStringArray) getProperty(defenceHash)).getValues();
        defenceDice = DicePool.constructDicePool(defence);
    }

    protected Monster(String name, int ID) {
        super(name, ID);
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    @Override
    public Monster copy() {
        Monster copy = new Monster(componentName, componentID);
        copy.orientation = orientation;
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster();
        copy.orientation = orientation;
        super.copyComponentTo(copy);
        return copy;
    }

}

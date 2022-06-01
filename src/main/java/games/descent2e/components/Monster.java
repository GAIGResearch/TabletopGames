package games.descent2e.components;

import core.components.Counter;
import core.properties.Property;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;

import java.util.HashMap;
import java.util.Map;

import static games.descent2e.DescentConstants.*;

public class Monster extends Figure {

    int orientation;  // medium monsters might be vertical (0) or horizontal (1)
    DicePool attackDice;
    DicePool defenceDice;

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
        copy.attackDice = attackDice.copy();
        copy.defenceDice = defenceDice.copy();
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster();
        copy.orientation = orientation;
        if (attackDice != null) {
            copy.attackDice = attackDice.copy();
        }
        if (defenceDice != null) {
            copy.defenceDice = defenceDice.copy();
        }
        super.copyComponentTo(copy);
        return copy;
    }

    public DicePool getAttackDice() { return attackDice;}
    public DicePool getDefenceDice() {return defenceDice;}
}

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

    public Monster(String name, Map<Integer, Property> props) {
        super(name);
        properties.clear();
        properties.putAll(props);

        int mp = ((PropertyInt)getProperty(movementHash)).value;
        this.setAttribute(Attribute.MovePoints, new Counter(0, 0, mp, "Move points"));
        int hp = ((PropertyInt)getProperty(healthHash)).value;
        this.setAttribute(Attribute.Health, new Counter(hp, 0, hp, "Health"));

        String[] attack = ((PropertyStringArray) getProperty(attackHash)).getValues();
        attackDice = DicePool.constructDicePool(attack);
        String[] defence = ((PropertyStringArray) getProperty(defenceHash)).getValues();
        defenceDice = DicePool.constructDicePool(defence);
        ownerId = 0; // All monsters belong to the overlord player
        tokenType = "Monster";
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

    public DicePool getAttackDice() { return attackDice;}
    public DicePool getDefenceDice() {return defenceDice;}
}

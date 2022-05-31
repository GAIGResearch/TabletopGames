package games.descent2e.components;

import core.components.Card;
import core.properties.Property;
import core.properties.PropertyStringArray;
import games.descent2e.DescentConstants.AttackType;
import games.descent2e.DescentGameState;

import java.util.*;

public class Item {

    int referenceComponent;
    protected AttackType attackType = AttackType.NONE;
    protected DicePool dicePool;

    public Item(Card data) {
        referenceComponent = data.getComponentID();
        Property at = data.getProperty("attackType");
        if (at != null) {
            attackType = AttackType.valueOf(at.toString().toUpperCase(Locale.ROOT));
            PropertyStringArray attPower = (PropertyStringArray) data.getProperty("attackPower");
            Map<DiceType, Integer> attackDice = new HashMap<>();
            for (String d : attPower.getValues()) {
                DiceType dt = DiceType.valueOf(d.toUpperCase(Locale.ROOT));
                if (attackDice.containsKey(dt))
                    attackDice.put(dt, attackDice.get(dt) + 1);
                else
                    attackDice.put(dt, 1);
            }
            dicePool = DicePool.constructDicePool(attackDice);
        }

    }

    public AttackType getAttackType() {
        return attackType;
    }

    public DicePool getDicePool() {
        return dicePool.copy();
    }

    public boolean isMeleeAttack() {
        return attackType == AttackType.MELEE;
    }

    public boolean isRangedAttack() {
        return attackType == AttackType.RANGED;
    }

    public boolean isAttack() {
        return attackType != AttackType.NONE;
    }

    public boolean isAOE() {
        return attackType == AttackType.BLAST;
    }

    public int getComponentID() {
        return referenceComponent;
    }
}

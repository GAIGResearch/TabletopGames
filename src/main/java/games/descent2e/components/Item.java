package games.descent2e.components;

import core.components.Card;
import core.properties.Property;
import core.properties.PropertyStringArray;
import games.descent2e.DescentConstants.AttackType;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.Surge;

import java.util.*;
import java.util.stream.Collectors;

public class Item {

    // Currently this is immutable, with no internal state
    // Items are not stored as such, but are a useful wrapper around the Component
    // to give programmatic access to key properties

    final int referenceComponent;
    protected AttackType attackType = AttackType.NONE;
    protected DicePool dicePool = DicePool.empty;
    protected List<Surge> weaponSurges = new ArrayList<>();

    public Item(Card data) {
        referenceComponent = data.getComponentID();
        Property at = data.getProperty("attackType");
        if (at != null) {
            attackType = AttackType.valueOf(at.toString().toUpperCase(Locale.ROOT));
            PropertyStringArray attPower = (PropertyStringArray) data.getProperty("attackPower");
            dicePool = DicePool.constructDicePool(attPower.getValues());
        }
        Property ws = data.getProperty("weaponSurges");
        if (ws != null) {
            PropertyStringArray wSurges = (PropertyStringArray) data.getProperty("weaponSurges");
            weaponSurges = Arrays.stream(wSurges.getValues())
                    .map(d -> Surge.valueOf(d.toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toList());
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

package games.descent2e.components;

import core.components.Card;
import core.properties.Property;
import core.properties.PropertyStringArray;
import games.descent2e.DescentTypes.AttackType;
import games.descent2e.actions.attack.Surge;

import java.util.*;
import java.util.stream.Collectors;

public class DescentCard extends Card {
    // Currently this is immutable, with no internal state
    // Items are not stored as such, but are a useful wrapper around the Card
    // to give programmatic access to key properties

    protected AttackType attackType = AttackType.NONE;
    protected DicePool dicePool = DicePool.empty;
    protected List<Surge> weaponSurges = new ArrayList<>();

    public DescentCard(Card data) {
        super(data.getComponentName());

        this.setProperties(data.getProperties());
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

    public List<Surge> getWeaponSurges() {return new ArrayList<>(weaponSurges);}

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

    @Override
    public DescentCard copy() {
        return this;
    }
}

package games.descent2e.components;

import core.components.Card;
import games.descent2e.DescentConstants.AttackType;

public class Item {

    int referenceComponent;

    protected AttackType attackType = AttackType.NONE;

    public Item(Card data) {
        referenceComponent = data.getComponentID();
        String at = data.getProperty("attackType").toString();
        if (!at.isEmpty())
            attackType = AttackType.valueOf(at);
    }

    public AttackType getAttackType() {
        return attackType;
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
}

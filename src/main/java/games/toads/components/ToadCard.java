package games.toads.components;

import core.components.Card;
import games.toads.ToadConstants.ToadCardType;
import games.toads.abilities.ToadAbility;

public class ToadCard extends Card {

    public final int value;
    public final ToadAbility ability;
    public final ToadAbility tactics;
    public final ToadCardType type;


    public ToadCard(String name, int value, ToadCardType type, ToadAbility ability, ToadAbility tactics) {
        super(name);
        this.value = value;
        this.ability = ability;
        this.tactics = tactics;
        this.type = type;
    }

    public ToadCard(String name, int value) {
        this(name, value, null, null, null);
    }
    public ToadCard(String name, int value, ToadCardType type) {
        this(name, value, type, type == null ? null : type.defaultAbility, type == null ? null : type.defaultAbility);
    }
    public ToadCard(String name, int value, ToadCardType type, ToadAbility ability) {
        this(name, value, type, ability, ability);
    }
//    public ToadCard(String name, int value, int ID) {
//        this(name, value, null, null, ID);
//    }

    @Override
    public ToadCard copy() {
        return this;  // currently immutable
    }

    public ToadAbility getAbility() {
        return ability;
    }
    public ToadAbility getTactics() {
        return tactics;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ToadCard otherCard)) return false;
        return super.equals(otherCard) && this.value == otherCard.value && this.ability.equals(otherCard.ability);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + value + (ability != null ? ability.hashCode()  * 31 : 0);
    }


}

package games.toads;

import core.components.Card;

public class ToadCard extends Card {

    public final int value;
    protected final ToadAbility ability;  // TODO: Don't know what this does yet


    public ToadCard(String name, int value, ToadAbility ability) {
        super(name);
        this.value = value;
        this.ability = ability;
    }
    public ToadCard(String name, int value) {
        this(name, value, null);
    }

    public ToadCard(String name, int value, ToadAbility ability, int ID) {
        super(name, ID);
        this.value = value;
        this.ability = ability;
    }
    public ToadCard(String name, int value, int ID) {
        this(name, value, null, ID);
    }

    @Override
    public ToadCard copy() {
        return this;  // currently immutable
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

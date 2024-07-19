package games.toads;

import core.components.Card;

public class ToadCard extends Card {

    public final int value;
    protected final ToadAbility ability;
    protected final ToadAbility tactics;


    public ToadCard(String name, int value, ToadAbility ability, ToadAbility tactics) {
        super(name);
        this.value = value;
        this.ability = ability;
        this.tactics = tactics;
    }
    public ToadCard(String name, int value) {
        this(name, value, null, null);
    }

    public ToadCard(String name, int value, ToadAbility ability, ToadAbility tactics, int ID) {
        super(name, ID);
        this.value = value;
        this.ability = ability;
        this.tactics = tactics;
    }
    public ToadCard(String name, int value, ToadAbility ability) {
        this(name, value, ability, ability);
    }
    public ToadCard(String name, int value, int ID) {
        this(name, value, null, null, ID);
    }

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

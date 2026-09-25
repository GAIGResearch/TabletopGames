package games.toads.components;

import core.components.Card;
import games.toads.ToadConstants.ToadCardType;
import games.toads.abilities.ToadAbility;

import java.util.Objects;

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

    @Override
    public ToadCard copy() {
        return this;  // currently immutable
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ToadCard otherCard)) return false;
        return super.equals(otherCard) && this.value == otherCard.value &&
                Objects.equals(this.ability, otherCard.ability) &&
                Objects.equals(this.tactics, otherCard.tactics) && this.type == otherCard.type;
    }

    @Override
    public int hashCode() {
        // the abilities are left out: they have identity hash codes, which differ between runs
        return 31 * super.hashCode() + value + (type != null ? type.ordinal() * 31 : 0);
    }


}

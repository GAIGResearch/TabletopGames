package games.descent2e.actions.items;

import com.google.common.collect.Iterables;
import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Hero;

import java.util.Objects;

public class ChangeWeapon extends DescentAction {

    int userID;
    int weaponID;

    public ChangeWeapon(int userID, int weaponID) {
        super(Triggers.ANYTIME);
        this.userID = userID;
        this.weaponID = weaponID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getComponentById(userID);
        DescentCard weapon = (DescentCard) dgs.getComponentById(weaponID);

        DescentCard oldWeapon = f.getPrimaryWeapon();
        DescentHelper.enableSurges(f, oldWeapon, false);
        f.setPrimaryWeapon(weapon);
        DescentHelper.enableSurges(f, weapon, true);

        return true;
    }

    @Override
    public DescentAction copy() {
        return new ChangeWeapon(userID, weaponID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ChangeWeapon that = (ChangeWeapon) o;
        return that.userID == userID && that.weaponID == weaponID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, weaponID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Change current weapon to " + gameState.getComponentById(weaponID).toString();
    }

    @Override
    public String toString() {
        return "Change current weapon to " + weaponID;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (Iterables.getLast(dgs.getHistory()).b instanceof ChangeWeapon) return false;

        Hero f = (Hero) dgs.getComponentById(userID);
        if (f == null || !dgs.getActingFigure().equals(f)) return false;

        DescentCard weapon = (DescentCard) dgs.getComponentById(weaponID);
        if (weapon == null) return false;
        if (!f.getHandEquipment().contains(weapon)) return false;

        // If it's the only weapon equipped, don't allow changes
        if (f.getWeapons().size() == 1) return false;

        return !f.getPrimaryWeapon().equals(weapon);
    }
}

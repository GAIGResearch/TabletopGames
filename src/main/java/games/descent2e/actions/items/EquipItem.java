package games.descent2e.actions.items;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EquipItem extends DescentAction implements IExtendedSequence {

    public List<Integer> equipped = new ArrayList<>();

    public int userID = -1;
    public int itemID = -1;
    public boolean unequip = false;
    private boolean complete = false;

    public EquipItem() {
        super(Triggers.ANYTIME);
    }

    public EquipItem(int userID) {
        super(Triggers.ANYTIME);
        this.userID = userID;
    }

    public EquipItem(int userID, boolean unequip) {
        super(Triggers.ANYTIME);
        this.userID = userID;
        this.unequip = unequip;
    }

    public EquipItem(int userID, int itemID, boolean unequip, List<Integer> equipped) {
        super(Triggers.ANYTIME);
        this.userID = userID;
        this.itemID = itemID;
        this.unequip = unequip;
        this.equipped = new ArrayList<>(equipped);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        List<EquipItem> equips = new ArrayList<>();

        DescentGameState dgs = (DescentGameState) state;
        Hero user = (Hero) dgs.getComponentById(userID);

        for (DescentCard item : user.getInventory()) {
            EquipItem equip = new EquipItem(userID, item.getComponentID(), false, equipped);
            if (equip.canExecute(dgs))
                equips.add(equip);
        }

        for (DescentCard item : user.getHandEquipment()) {
            EquipItem equip = new EquipItem(userID, item.getComponentID(), true, equipped);
            if (equip.canExecute(dgs))
                equips.add(equip);
        }

        if (user.getArmor() != null) {
            EquipItem equip = new EquipItem(userID, user.getArmor().getComponentID(), true, equipped);
            if (equip.canExecute(dgs))
                equips.add(equip);
        }

        for (DescentCard item : user.getOtherEquipment()) {
            EquipItem equip = new EquipItem(userID, item.getComponentID(), true, equipped);
            if (equip.canExecute(dgs))
                equips.add(equip);
        }

        EquipItem endEquip = new EquipItem();
        if (endEquip.canExecute(dgs))
            equips.add(endEquip);

        return new ArrayList<>(equips);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(userID).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof EquipItem equip) {
            if (equip.userID == -1) {
                complete = true;
                ((Hero) state.getComponentById(userID)).setEquipped(true);
            }
            DescentCard item = (DescentCard) state.getComponentById(equip.itemID);
            if (item != null)
                equipped.add(equip.itemID);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        if (userID == -1) {
            complete = true;
            if (dgs.getActingFigure() instanceof Hero hero)
                hero.setEquipped(true);
            return true;
        }

        Hero user = (Hero) dgs.getComponentById(userID);
        user.addActionTaken(toString());

        if (itemID != -1) {
            DescentCard item = (DescentCard) dgs.getComponentById(itemID);

            if (unequip) {
                user.unequip(item);
            }
            else {
                user.equip(item);
            }
            complete = true;
            return true;
        }

        dgs.setActionInProgress(this);


        return true;
    }

    @Override
    public EquipItem copy() {
        EquipItem trade = new EquipItem(userID, itemID, unequip, equipped);
        trade.complete = complete;
        return trade;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EquipItem other)) return false;
        return userID == other.userID && itemID == other.itemID &&
                complete == other.complete && unequip == other.unequip;
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), userID, itemID, complete, unequip, equipped);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (itemID == -1)
            return toString();

        DescentCard item = (DescentCard) gameState.getComponentById(itemID);

        if (unequip)
            return "Unequip " + item.toString();
        return "Equip " + item.toString();

    }

    @Override
    public String toString() {
        if (itemID == -1) {
            if (userID == -1)
                return "End Equipment Decision";
            else
                return "Change Equipment";
        }
        if (unequip)
            return "Unequip " + itemID;
        return "Equip " + itemID;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (userID == -1) return true;

        Hero user = (Hero) dgs.getComponentById(userID);
        if (user == null) return false;

        if (user.isEquipped()) return false;

        if (itemID != -1) {
            DescentCard item = (DescentCard) dgs.getComponentById(itemID);
            if (item == null) return false;

            if (unequip) {
                return user.canUnequip(item);
            }
            return user.getInventory().contains(item) &&
                    user.canEquip(item);
        }
        return user.hasEquipment();
    }
}

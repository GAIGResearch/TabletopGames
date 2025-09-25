package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.components.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class UseFireFlask extends DescentAction implements IExtendedSequence {
    final int userID;
    final int enemyID;
    final int itemID;
    private boolean complete;
    private final String name = "Fire Flask";

    Deck<DescentCard> oldWeapons = new Deck<>("Hands", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);;
    DicePool oldAttackDice = new DicePool(new ArrayList<>());
    List<DescentAction> oldSurges = new ArrayList<>();

    public UseFireFlask(int userID, int enemyID, int itemID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.userID = userID;
        this.enemyID = enemyID;
        this.itemID = itemID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RangedAttack attack = new RangedAttack(userID, enemyID);
        return name +": " + attack.getString(gameState);
    }

    @Override
    public String toString() {
        RangedAttack attack = new RangedAttack(userID, enemyID);
        return name + ": " + attack.toString();
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);
        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        card.setProperty(new PropertyBoolean("used", true));

        Hero f = (Hero) dgs.getComponentById(userID);

        // Just for this attack, we need to unequip our current weapons
        oldWeapons = f.getHandEquipment();
        Deck<DescentCard> newWeapons = new Deck<>("Hands", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        newWeapons.add(card);
        f.setHandEquipment(newWeapons);

        oldAttackDice = f.getAttackDice();
        f.setAttackDice(card.getDicePool());

        oldSurges = f.getAbilities();
        List<DescentAction> newSurges = getDescentActions(oldSurges, oldWeapons, card);
        f.setAbilities(newSurges);

        // We will hand back the old weapons, dice and abilities after we conclude the Ranged Attack
        f.addActionTaken(toString());

        return true;
    }

    private @NotNull List<DescentAction> getDescentActions(List<DescentAction> oldSurges, Deck<DescentCard> oldWeapons, DescentCard card) {
        List<DescentAction> newSurges = new java.util.ArrayList<>(List.copyOf(oldSurges));

        for (DescentCard weapon : oldWeapons) {
            for(Surge s : weapon.getWeaponSurges()) {
                SurgeAttackAction surge = new SurgeAttackAction(s, userID);
                newSurges.remove(surge);
            }
        }

        for(Surge s : card.getWeaponSurges()) {
            SurgeAttackAction surge = new SurgeAttackAction(s, userID);
            newSurges.add(surge);
        }
        return newSurges;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> retVal = new ArrayList<>();
        RangedAttack attack = new RangedAttack(userID, enemyID);
        if (attack.canExecute((DescentGameState) state))
            retVal.add(attack);
        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(userID).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof RangedAttack) {

            Hero f = (Hero) state.getComponentById(userID);

            // Now, reequip our old weapons
            f.setHandEquipment(oldWeapons);
            f.setAttackDice(oldAttackDice);
            f.setAbilities(oldSurges);

            complete = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public UseFireFlask copy() {
        UseFireFlask retVal = new UseFireFlask(userID, enemyID, itemID);
        retVal.oldWeapons = oldWeapons.copy();
        retVal.oldAttackDice = oldAttackDice.copy();
        retVal.oldSurges = List.copyOf(oldSurges);
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure target = (Figure) dgs.getComponentById(enemyID);
        if (!(target instanceof Monster)) return false;
        Hero user = (Hero) dgs.getActingFigure();
        if (user == null) return false;
        if (user.getComponentID() != userID) return false;

        Deck<DescentCard> heroInventory = user.getInventory();
        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        if (card == null) return false;
        if (heroInventory.contains(card))
            if (((PropertyString) card.getProperty("name")).value.equals(name))
                if (((PropertyBoolean) card.getProperty("used")).value.equals(false))
                {
                    RangedAttack attack = new RangedAttack(userID, enemyID);
                    return attack.canExecute(dgs);
                }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UseFireFlask other)) return false;
        return userID == other.userID && itemID == other.itemID &&
                enemyID == other.enemyID && complete == other.complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, enemyID, itemID, complete, name);
    }
}

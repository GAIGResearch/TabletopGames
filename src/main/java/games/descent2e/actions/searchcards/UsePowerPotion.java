package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.items.RerollAttackDice;
import games.descent2e.components.DescentCard;
import games.descent2e.components.DescentDice;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.*;

public class UsePowerPotion extends DescentAction implements IExtendedSequence {
    int userID;
    int itemID;
    boolean complete = false;

    private final String name = "Power Potion";

    public UsePowerPotion(int userID, int itemID) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.itemID = itemID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
        }

    @Override
    public String toString() {
        return "Use Power Potion";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);

        Hero user = (Hero) dgs.getComponentById(userID);
        user.addActionTaken(toString());

        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        card.setProperty(new PropertyBoolean("used", true));
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<RerollAttackDice> retVal = new ArrayList<>();

        DescentGameState dgs = (DescentGameState) state;
        List<DescentDice> dice = dgs.getAttackDicePool().getComponents();

        List<List<Integer>> combinations = getDiceCombinations(dgs, dice);

        for (List<Integer> combo : combinations)
        {
            RerollAttackDice reroll = new RerollAttackDice(userID, combo);
            if (reroll.canExecute(dgs))
                retVal.add(reroll);
        }

        retVal.sort(Comparator.comparingInt(RerollAttackDice::getDiceSize));

        List<AbstractAction> finish = new ArrayList<>();
        finish.addAll(retVal);

        return finish;
    }

    public List<List<Integer>> getDiceCombinations(DescentGameState dgs, List<DescentDice> dice) {
        List<Integer> diceIndex = new ArrayList<>();
        for (DescentDice die : dice) {
            diceIndex.add(dice.indexOf(die));
        }
        List<List<Integer>> retVal = new ArrayList<>();

        for (int i = 0; i < diceIndex.size(); i++)
        {
            List<Integer> combos = new ArrayList<>();
            getCombinations(0, i+1, combos, retVal, diceIndex);
        }
        return retVal;
    }

    public void getCombinations(int index, int max, List<Integer> combos, List<List<Integer>> retVal, List<Integer> indexToTrack) {
        if (combos.size() == max) {
            retVal.add(new ArrayList<>(combos));
            return;
        }

        for (int i = index; i < indexToTrack.size(); i++)
        {
            combos.add(indexToTrack.get(i));
            getCombinations(i+1, max, combos, retVal, indexToTrack);
            combos.remove(combos.size() - 1);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(userID).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof RerollAttackDice)
            complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public UsePowerPotion copy() {
        UsePowerPotion retVal = new UsePowerPotion(userID, itemID);
        retVal.complete = complete;
        return retVal;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero user = (Hero) dgs.getActingFigure();
        if (user == null) return false;
        if (user.getComponentID() != userID) return false;

        Deck<DescentCard> heroInventory = user.getInventory();
        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        if (card == null) return false;
        if (heroInventory.contains(card))
            if (((PropertyString) card.getProperty("name")).value.equals(name))
                if (((PropertyBoolean) card.getProperty("used")).value.equals(false))
                    return canUse(dgs);
        return false;
    }

    private boolean canUse(DescentGameState dgs)
    {
        IExtendedSequence currentAction = dgs.currentActionInProgress();
        if (!(currentAction instanceof MeleeAttack melee)) return false;
        if (melee.getCurrentPlayer(dgs) != dgs.getActingFigure().getOwnerId()) return false;
        return !melee.getSkip() && melee.getPhase() == MeleeAttack.AttackPhase.POST_ATTACK_ROLL;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UsePowerPotion other)) return false;
        return userID == other.userID && itemID == other.itemID &&
                complete == other.complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, itemID, name, complete);
    }
}

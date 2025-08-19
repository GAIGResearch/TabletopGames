package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.tokens.AcolyteAction;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Sacrifice extends DescentAction implements IExtendedSequence {

    int elizaID;
    int targetID;
    int damage;
    boolean complete = false;
    int maxDamage = 5;
    public Sacrifice(int elizaID, int targetID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.elizaID = elizaID;
        this.targetID = targetID;
        this.damage = -1;
    }

    public Sacrifice(int elizaID, int targetID, int damage) {
        super(Triggers.ACTION_POINT_SPEND);
        this.elizaID = elizaID;
        this.targetID = targetID;
        this.damage = damage;
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String elizaName = ((Figure) gameState.getComponentById(elizaID)).getName().replace("Hero: ", "");
        String victimName = ((Figure) gameState.getComponentById(targetID)).getName().replace("Hero: ", "");

        if (damage < 0)
            return "Sacrifice by " + elizaName + " on " + victimName;
        return "Sacrifice by " + elizaName + " on " + victimName + " for " + damage + " damage";
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        List<AbstractAction> actions = new ArrayList<>();
        DescentGameState dgs = (DescentGameState) state;

        // Only allow the initial Sacrifice action to have subsequent actions
        if (damage < 0)
        {
            for (int i = 0; i < maxDamage; i++) {
                Sacrifice newSacrifice = new Sacrifice(elizaID, targetID, i+1);
                if (newSacrifice.canExecute(dgs)) {
                    actions.add(newSacrifice);
                }
            }
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(elizaID).getOwnerId();
    }

    @Override
    public boolean execute(DescentGameState gs) {
        if (damage != -1)
        {
            Figure eliza = (Figure) gs.getComponentById(elizaID);
            Figure victim = (Figure) gs.getComponentById(targetID);

            // Sacrifice health from the target to Eliza
            eliza.getAttribute(Figure.Attribute.Health).increment(damage);
            victim.getAttribute(Figure.Attribute.Health).decrement(damage);

            if (victim.getAttribute(Figure.Attribute.Health).isMinimum())
            {
                // Death
                DescentHelper.figureDeath(gs, victim);

                // Add to the list of defeated figures this turn
                gs.addDefeatedFigure(victim, targetID, eliza, elizaID);
            }

            eliza.addActionTaken(getString(gs));
            eliza.getNActionsExecuted().increment();
            eliza.setHasAttacked(true);
            //complete = true;
        }
        else
        {
            gs.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        // Lady Eliza can only Sacrifice adjacent Monsters
        Figure victim = (Figure) dgs.getComponentById(targetID);
        if (victim == null) return false;
        if (!(victim instanceof Monster) || victim.getAttribute(Figure.Attribute.Health).isMinimum()) return false;

        Figure eliza = (Figure) dgs.getComponentById(elizaID);

        // No point in using it if she's already at full health
        // if (eliza.getAttribute(Figure.Attribute.Health).isMaximum()) return false;

        // Only bother with this section during the second part
        if (damage > 0)
        {
            /*
            int difference = eliza.getAttributeMax(Figure.Attribute.Health) - eliza.getAttribute(Figure.Attribute.Health).getValue();
            if (difference < damage) {
                // No point in sacrificing more than she needs to recover
                return false;
            }
            */

            if (victim.getAttribute(Figure.Attribute.Health).getValue() < damage) {
                // She cannot sacrifice more than the target has health
                return false;
            }
        }

        // Make sure the target is actually adjacent to Lady Eliza
        List<Integer> targets = DescentHelper.getAdjacentTargets(dgs, eliza, true);

        return targets.contains(targetID);
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public Sacrifice copy() {
        Sacrifice sacrifice = new Sacrifice(elizaID, targetID, damage);
        sacrifice.complete = complete;
        return sacrifice;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof Sacrifice &&
               elizaID == ((Sacrifice) o).elizaID &&
               targetID == ((Sacrifice) o).targetID &&
               damage == ((Sacrifice) o).damage &&
               complete == ((Sacrifice) o).complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elizaID, targetID, damage, complete);
    }
}

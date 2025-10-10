package games.descent2e.actions.items;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Revive;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.archetypeskills.Heal;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class FortunasDice extends DescentAction implements IExtendedSequence {

    int userID = -1;
    int itemID = -1;
    int diceID = -1;
    String diceType = "";
    boolean complete = false;

    public FortunasDice(int userID, int itemID) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.itemID = itemID;
    }

    public FortunasDice(int userID, int itemID, int diceID, String diceType) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.itemID = itemID;
        this.diceID = diceID;
        this.diceType = diceType.toUpperCase();
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(userID);
        f.addActionTaken(toString());

        if (diceID == -1) {
            dgs.setActionInProgress(this);
            return true;
        }

        DescentCard item = (DescentCard) dgs.getComponentById(itemID);
        f.exhaustCard(item);
        f.getAttribute(Figure.Attribute.Fatigue).increment();

        DescentDice dice = getDice(dgs, diceID, diceType);

        dice.setFace(DescentHelper.reroll(dgs, dice));

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        DescentGameState dgs = (DescentGameState) state;
        List<AbstractAction> retVal = new ArrayList<>();

        Stack<IExtendedSequence> actions = dgs.getActionsInProgress();
        IExtendedSequence current = actions.get(actions.size() - 2);

        String diceType = "";
        int dicePoolSize = 0;

        if (current instanceof MeleeAttack melee) {
            if (melee.getAttackingFigure() == userID) {
                diceType = "ATTACK";
                dicePoolSize = dgs.getAttackDicePool().getSize();
            }
            if (melee.getDefendingFigure() == userID) {
                diceType = "DEFENCE";
                dicePoolSize = dgs.getDefenceDicePool().getSize();
            }
        }
        if (current instanceof AttributeTest test) {
            if (test.getTestingFigure() == userID) {
                diceType = "ATTRIBUTE";
                dicePoolSize = dgs.getAttributeDicePool().getSize();
            }
        }
        if (current instanceof Heal heal) {
            if (dgs.getActingFigure().getComponentID() == userID) {
                diceType = "HEAL";
                dicePoolSize = DicePool.heal.getSize();
            }
        }
        if (current instanceof Revive revive) {
            if (dgs.getActingFigure().getComponentID() == userID) {
                diceType = "REVIVE";
                dicePoolSize = DicePool.revive.getSize();
            }
        }

        for (int i = 0; i < dicePoolSize; i++) {
            FortunasDice reroll = new FortunasDice(userID, itemID, i, diceType);
            if (reroll.canExecute(dgs))
                retVal.add(reroll);
        }

        // Just in case we decide against using Fortuna's Dice after all
        retVal.add(new EndCurrentPhase());

        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(userID).getOwnerId();
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
    public FortunasDice copy() {
        return new FortunasDice(userID, itemID, diceID, diceType);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FortunasDice that) {
            return super.equals(that) && userID == that.userID && itemID == that.itemID
                    && diceID == that.diceID && diceType.equals(that.diceType);
        }
        return false;

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, itemID, diceID, diceType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (diceID == -1) return "Fortuna's Dice: Reroll 1 Dice";

        DescentGameState dgs = (DescentGameState) gameState;
        DescentDice dice = getDice(dgs, diceID, diceType);

        if (diceType.equals("ATTACK"))
            return "Reroll Dice: " + dice.getColour() + " (Face: " + dice.getFace() + ", Range: " + dice.getRange() + ", Damage: " + dice.getDamage() + ", Surge: " + dice.getSurge() + ")";
        if (diceType.equals("DEFENCE") || diceType.equals("ATTRIBUTE"))
            return "Reroll Dice: " + dice.getColour() + " (Shield: " + dice.getShielding() + ")";
        return "Reroll Dice: " + dice.getColour() + " (Health: " + dice.getDamage() + ")";
    }

    @Override
    public String toString() {
        if (diceID == -1) return "Fortuna's Dice: Reroll 1 Dice";
        return "Reroll " + diceType + "Dice #" + (diceID + 1);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(userID);
        if (f == null) return false;
        if (dgs.getActingFigure().getComponentID() != userID) return false;
        DescentCard item = (DescentCard) dgs.getComponentById(itemID);
        if (item == null) return false;
        if (diceID == -1) {
            if (dgs.getActionsInProgress().isEmpty()) return false;
            if (f.isExhausted(item)) return false;
            if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

            // Check that we aren't just going in a loop of wanting to use it, then declining using it
            List<Pair<Integer, AbstractAction>> history = dgs.getHistory();
            if (history.size() > 1) {
                Pair<Integer, AbstractAction> previous = history.get(history.size() - 2);
                return previous.a != f.getOwnerId() || !(previous.b instanceof FortunasDice);
            }
            return true;
        }

        Stack<IExtendedSequence> actions = dgs.getActionsInProgress();
        if (actions.size() < 2) return false;

        // As the general Fortuna's Dice request is already in progress, we need to check what the action it interrupted is
        IExtendedSequence current = actions.get(actions.size() - 2);

        DescentDice dice = (getDice(dgs, diceID, diceType));

        if (dice == null) return false;
        return canUse(dgs, current, dice);
    }

    public DescentDice getDice(DescentGameState dgs, int diceID, String diceType) {
        switch (diceType) {
            case "ATTACK" -> {
                if (dgs.getAttackDicePool().getSize() > diceID)
                    return dgs.getAttackDicePool().getDice(diceID);
            }
            case "DEFENCE" -> {
                if (dgs.getDefenceDicePool().getSize() > diceID)
                    return dgs.getDefenceDicePool().getDice(diceID);
            }
            case "ATTRIBUTE" -> {
                if (dgs.getAttributeDicePool().getSize() > diceID)
                    return dgs.getAttributeDicePool().getDice(diceID);
            }
            case "HEAL" -> {
                if (DicePool.heal.getSize() > diceID)
                    return DicePool.heal.getDice(diceID);
            }
            case "REVIVE" -> {
                if (DicePool.revive.getSize() > diceID)
                    return DicePool.revive.getDice(diceID);
            }
        }
        return null;
    }

    protected boolean canUse(DescentGameState dgs, IExtendedSequence currentAction, DescentDice dice)
    {
        // For Defence and Test dice, we prevent rerolling if we got the best result they could get
        // We don't do this for Attack dice or Healing dice as there are too many factors to consider why we reroll

        if (currentAction instanceof MeleeAttack melee) {
            // Always allow rerolling for valid attacks
            if (melee.getAttackingFigure() == userID)
                return melee.getPhase() == MeleeAttack.AttackPhase.POST_ATTACK_ROLL;

            // Only allow rerolling for defending if we'd take damage
            if (melee.getDefendingFigure() == userID) {
                if (melee.getPhase() != MeleeAttack.AttackPhase.POST_DEFENCE_ROLL) return false;

                // No point in rerolling if we got the best result possible
                if (dice.isMaxShield()) return false;

                // If the defender already has enough defence to block the attack, there's no point in exhausting the shield
                int damage = dgs.getAttackDicePool().getDamage() + melee.getExtraDamage();
                int defence = dgs.getDefenceDicePool().getShields() + melee.getExtraDefence() - melee.getPierce();
                return damage > defence;
            }
        }

        // For Attribute Tests, only allow if we failed the test
        if (currentAction instanceof AttributeTest test) {
            // There is no point in rerolling a die that already has the lowest result possible
            if (dice.getShielding() == 0) return false;

            if (test.getTestingFigure() != userID) return false;
            Figure f = (Figure) dgs.getComponentById(userID);
            int result = dgs.getAttributeDicePool().getShields();
            return (result > f.getAttributeValue(test.getAttribute()));
        }

        return dgs.getActingFigure().getComponentID() == userID;
    }
}

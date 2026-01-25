package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Component;
import core.components.GridBoard;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.List;
import java.util.Objects;

import static utilities.Utils.getNeighbourhood;

public class Heal extends DescentAction {

    int targetID;
    int range = 1;
    boolean isAction = false;
    int cardID = -1;
    int healthRecovered = 0;

    // Prayer of Healing
    public Heal(int targetID, int cardID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.targetID = targetID;
        this.cardID = cardID;
    }

    // Flesh Moulder's Heal Action
    public Heal(int targetID, int range, boolean isAction) {
        super(Triggers.ACTION_POINT_SPEND);
        this.targetID = targetID;
        this.range = range;
        this.isAction = isAction;
    }

    public Heal(int targetID, int range, boolean isAction, int cardID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.targetID = targetID;
        this.range = range;
        this.isAction = isAction;
        this.cardID = cardID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String targetName = gameState.getComponentById(targetID).getComponentName().replace("Hero: ", "");
        String string = "Heal " + targetName + " for 1 Red Power Die";
        Component card = gameState.getComponentById(cardID);
        if (card != null) {
            string = card.getProperty("name").toString() + ": " + string;
        }
        if (healthRecovered > 0)
            string += " (" + healthRecovered + " Health)";
        return string;
    }

    @Override
    public String toString() {
        return "Heal " + targetID + " (" + cardID + ")";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure target = (Figure) dgs.getComponentById(targetID);

        // Health recovery: roll 1 red die
        DicePool.heal.roll(dgs.getRnd());

        healthRecovered = DicePool.heal.getDamage();

        target.incrementAttribute(Figure.Attribute.Health, healthRecovered);
        //System.out.println(target.getComponentName() + " healed for " + DicePool.heal.getDamage() + " health.");

        if (target instanceof Hero && ((Hero) target).isDefeated())
            ((Hero) target).setDefeated(dgs, false);

        Figure f = dgs.getActingFigure();

        if (isAction)
        {
            f.getNActionsExecuted().increment();
            if (f instanceof Monster)
            {
                f.setHasAttacked(true);
            }
        }

        // Prayer of Healing
        if (!isAction && f instanceof Hero)
        {
            f.getAttribute(Figure.Attribute.Fatigue).increment();
        }

        Component card = dgs.getComponentById(cardID);
        if (card != null)
        {
            f.exhaustCard((DescentCard) dgs.getComponentById(cardID));
        }

        f.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        Figure target = (Figure) dgs.getComponentById(targetID);

        if (isAction)
        {
            if (f.getNActionsExecuted().isMaximum())
                return false;
            if (f instanceof Monster && f.hasAttacked())
                return false;
        }

        // Prayer of Healing
        if (!isAction && f instanceof Hero)
        {
            if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum())
                return false;
        }

        Component card = dgs.getComponentById(cardID);
        if (card == null)
        {
            if (target.getAttribute(Figure.Attribute.Health).getValue() >= target.getAttribute(Figure.Attribute.Health).getMaximum())
                return false;
        }

        if (card != null)
        {
            if (f.isExhausted((DescentCard) dgs.getComponentById(cardID)))
                return false;
            // Normally Prayer of Healing can be upgraded to have additional effects,
            // so we would not care if the target is at full health if we are exhausting a card
            // However, at this current level, we do not have such abilities, so we still need to check
            if (target.getAttribute(Figure.Attribute.Health).getValue() >= target.getAttribute(Figure.Attribute.Health).getMaximum())
                return false;
        }

        // We can always heal ourselves
        if (target.equals(f)) return true;

        if (range == 1)
        {
            Vector2D loc = f.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            return neighbours.contains(target.getPosition()) || target.getPosition().equals(loc);
        }

        if (range > 1)
        {
            return DescentHelper.inRange(f.getPosition(), target.getPosition(), range);
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Heal heal = (Heal) o;
        return targetID == heal.targetID && range == heal.range && isAction == heal.isAction && cardID == heal.cardID && healthRecovered == heal.healthRecovered;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetID, range, isAction, cardID, healthRecovered);
    }

    @Override
    public DescentAction copy()
    {
        Heal heal = new Heal(targetID, range, isAction, cardID);
        heal.healthRecovered = healthRecovered;
        return heal;
    }
}

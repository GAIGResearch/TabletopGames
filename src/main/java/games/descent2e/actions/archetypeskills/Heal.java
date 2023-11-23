package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
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
    DescentCard card = null;

    // Prayer of Healing
    public Heal(int targetID, DescentCard card) {
        super(Triggers.ACTION_POINT_SPEND);
        this.targetID = targetID;
        this.card = card;
    }

    // Flesh Moulder's Heal Action
    public Heal(int targetID, int range, boolean isAction) {
        super(Triggers.ACTION_POINT_SPEND);
        this.targetID = targetID;
        this.range = range;
        this.isAction = isAction;
    }

    public Heal(int targetID, int range, boolean isAction, DescentCard card) {
        super(Triggers.ACTION_POINT_SPEND);
        this.targetID = targetID;
        this.range = range;
        this.isAction = isAction;
        this.card = card;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String targetName = gameState.getComponentById(targetID).getComponentName().replace("Hero: ", "");
        String string = "Heal " + targetName + " for 1 Red Power Die";
        if (card != null)
            string = card.getProperty("name").toString() + ": " + string;
        return string;
    }

    @Override
    public String toString() {
        if (card != null)
            return card.getProperty("name").toString() + ": Heal " + targetID;
        return "Heal " + targetID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure target = (Figure) dgs.getComponentById(targetID);

        // Health recovery: roll 1 red die
        DicePool.heal.roll(dgs.getRandom());

        target.incrementAttribute(Figure.Attribute.Health, DicePool.heal.getDamage());
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

        if (card != null)
        {
            f.exhaustCard(card);
        }

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

        if (card == null)
        {
            // As Prayer of Healing can be upgraded to have additional effects,
            // we do not care if the target is at full health if we are exhausting a card
            if (target.getAttribute(Figure.Attribute.Health).getValue() >= target.getAttribute(Figure.Attribute.Health).getMaximum())
                return false;
        }

        if (card != null)
        {
            if (f.isExhausted(card))
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
        return targetID == heal.targetID && range == heal.range && isAction == heal.isAction && card == heal.card;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetID, range, isAction, card);
    }

    @Override
    public DescentAction copy()
    {
        return new Heal(targetID, range, isAction, card);
    }
}

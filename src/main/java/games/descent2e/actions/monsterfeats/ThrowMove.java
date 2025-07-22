package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import static games.descent2e.DescentHelper.figureDeath;

public class ThrowMove extends ForcedMove{
    public ThrowMove(int target, int source, Vector2D whereTo) {
        super(target, source, whereTo);
    }

    public ThrowMove(int target, int source, Vector2D whereTo, Monster.Direction orientation) {
        super(target, source, whereTo, orientation);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        super.execute(dgs);
        Figure f = (Figure) dgs.getComponentById(this.figureID);
        f.getAttribute(Figure.Attribute.MovePoints).setToMin();
        f.getAttribute(Figure.Attribute.Health).decrement();
        if (f.getAttribute(Figure.Attribute.Health).getValue() <= 0) {
            figureDeath(dgs, f);
        }
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        if (!super.canExecute(dgs)) {
            return false;
        }

        Figure f = (Figure) dgs.getComponentById(figureID);
        return f.isOffMap() && (f.getAttribute(Figure.Attribute.MovePoints).getValue() == 3);
    }

    public ThrowMove copy() {
        ThrowMove throwMove = new ThrowMove(figureID, sourceID, whereTo, orientation);
        throwMove.startPosition = startPosition.copy();
        throwMove.directionID = directionID;
        return throwMove;
    }

    @Override
    public String getString(AbstractGameState gameState) {

        Figure f = (Figure) gameState.getComponentById(figureID);
        Figure source = (Figure) gameState.getComponentById(sourceID);

        String name = f.getName().replace("Hero: ", "");
        String sourceName = source.getName().replace("Hero: ", "");

        if (startPosition.equals(new Vector2D(0,0)))
        {
            // If the Start Position has not been changed from initiation, we save it here
            startPosition = f.getPosition();
        }

        String movement = "Throw Move: " + sourceName + " throws " + name + " from " + startPosition.toString() + " to " + whereTo.toString();

        movement = movement + (f.getSize().a > 1 || f.getSize().b > 1 ? "; Orientation: " + orientation : "");
        return movement;
    }

    @Override
    public String toString() {
        return "Throw Move by " + sourceID + " upon " + figureID + " from" + startPosition.toString() + " to " + whereTo.toString();
    }
}

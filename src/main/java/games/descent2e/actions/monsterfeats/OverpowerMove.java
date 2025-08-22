package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import static games.descent2e.actions.Move.remove;

public class OverpowerMove extends ForcedMove{
    public OverpowerMove(int target, int source, Vector2D startPosition, Vector2D whereTo) {
        super(target, source, startPosition, whereTo, 0);
    }

    public OverpowerMove(int target, int source, Vector2D startPosition, Vector2D whereTo, Monster.Direction orientation) {
        super(target, source, startPosition, whereTo, orientation, 0);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.figureID);
        Figure source = (Figure) dgs.getComponentById(this.sourceID);

        // Swap the positions of the two figures
        remove(dgs, f);
        remove(dgs, source);
        forcedMove(dgs, f, whereTo, orientation);
        forcedMove(dgs, source, startPosition, orientation);

        f.setHasMoved(true);
        f.setOffMap(false);
        source.setHasMoved(true);
        source.setOffMap(false);

        String test = "Overpower of " + source.getName();
        DescentHelper.forcedFatigue(dgs, f, test);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        if (!super.canExecute(dgs)) {
            return false;
        }
        Figure f = (Figure) dgs.getComponentById(figureID);
        return f.isOffMap();
    }

    public OverpowerMove copy() {
        OverpowerMove overpowerMove = new OverpowerMove(figureID, sourceID, startPosition, whereTo, orientation);
        overpowerMove.startPosition = startPosition.copy();
        overpowerMove.directionID = directionID;
        return overpowerMove;
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

        String movement = "Overpower Move: " + sourceName + " forces " + name + " to swap from " + startPosition.toString() + " to " + whereTo.toString();

        movement = movement + (f.getSize().a > 1 || f.getSize().b > 1 ? "; Orientation: " + orientation : "");
        return movement;
    }

    @Override
    public String toString() {
        return "Overpower Move by " + sourceID + " forces " + figureID + " to swap from" + startPosition.toString() + " to " + whereTo.toString();
    }
}

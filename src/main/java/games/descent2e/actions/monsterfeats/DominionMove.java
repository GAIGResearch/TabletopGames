package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

public class DominionMove extends ForcedMove{
    public DominionMove(int target, int source, Vector2D whereTo) {
        super(target, source, whereTo);
    }

    public DominionMove(int target, int source, Vector2D whereTo, Monster.Direction orientation) {
        super(target, source, whereTo, orientation);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        super.execute(dgs);
        Figure f = (Figure) dgs.getComponentById(this.figureID);
        f.getAttribute(Figure.Attribute.MovePoints).setToMin();
        f.setOffMap(false);
        f.addCondition(DescentTypes.DescentCondition.Stun);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        if (!super.canExecute(dgs)) {
            return false;
        }

        Figure f = (Figure) dgs.getComponentById(figureID);
        return f.isOffMap() && (f.getAttribute(Figure.Attribute.MovePoints).getValue() == 1);
    }

    public DominionMove copy() {
        DominionMove dominionMove = new DominionMove(figureID, sourceID, whereTo, orientation);
        dominionMove.startPosition = startPosition.copy();
        dominionMove.directionID = directionID;
        return dominionMove;
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

        String movement = "Dominion Move: " + sourceName + " beguiles " + name + " from " + startPosition.toString() + " to " + whereTo.toString();

        movement = movement + (f.getSize().a > 1 || f.getSize().b > 1 ? "; Orientation: " + orientation : "");
        return movement;
    }

    @Override
    public String toString() {
        return "Dominion Move by " + sourceID + " upon " + figureID + " from" + startPosition.toString() + " to " + whereTo.toString();
    }
}

package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.Objects;

public class Knockback extends ForcedMove{

    public static String name = "Knockback";
    public Knockback(int target, int source, Vector2D startPosition, Vector2D whereTo) {
        super(target, source, startPosition, whereTo, 3);
    }

    public Knockback(int target, int source, Vector2D startPosition, Vector2D whereTo, int distance) {
        super(target, source, startPosition, whereTo, distance);
    }

    public Knockback(int target, int source, Vector2D startPosition, Vector2D whereTo, Monster.Direction orientation, int distance) {
        super(target, source, startPosition, whereTo, orientation, distance);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        // As this is an Interrupt Attack, we do not need to disable all Interrupt Attacks
        // Only decrement how many times we can use Knockback now that we have used it
        dgs.removeInterruptAttack(name + ":" + maxDistance);
        super.execute(dgs);
        Figure f = (Figure) dgs.getComponentById(this.figureID);
        f.getAttribute(Figure.Attribute.MovePoints).setToMin();
        f.setOffMap(false);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        if (!dgs.hasInterruptAttack(name + ":" + maxDistance)) return false;
        if (!super.canExecute(dgs)) {
            return false;
        }
        Figure f = (Figure) dgs.getComponentById(figureID);
        return f.isOffMap();
    }

    public Knockback copy() {
        Knockback knockback = new Knockback(figureID, sourceID, startPosition, whereTo, orientation, maxDistance);
        knockback.directionID = directionID;
        return knockback;
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

        String movement = "Knockback: " + sourceName + " knocks back " + name + " from " + startPosition.toString() + " to " + whereTo.toString();

        movement = movement + (f.getSize().a > 1 || f.getSize().b > 1 ? "; Orientation: " + orientation : "");
        return movement;
    }

    @Override
    public String toString() {
        return "Knockback by " + sourceID + " upon " + figureID + " from" + startPosition.toString() + " to " + whereTo.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}

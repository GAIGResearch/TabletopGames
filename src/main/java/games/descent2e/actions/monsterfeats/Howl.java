package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.List;
import java.util.stream.Collectors;

public class Howl extends DescentAction {

    List<Hero> heroes;
    public Howl() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Howl";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Monster monster = (Monster)dgs.getActingFigure();

        Vector2D position = monster.getPosition();

        heroes = dgs.getHeroes();

        for (Hero h : heroes) {
            Vector2D other = h.getPosition();
            if (Math.abs(position.getX() - other.getX()) <= 3 && Math.abs(position.getY() - other.getY()) <= 3) {
                System.out.println(h.getName() + " must make a Howl Test!");
            }
        }

        monster.getNActionsExecuted().increment();
        return true;
    }

    public boolean isNearHeroes (DescentGameState dgs)
    {
        Monster monster = (Monster)dgs.getActingFigure();

        Vector2D position = monster.getPosition();

        heroes = dgs.getHeroes();

        for (Hero h : heroes) {
            Vector2D other = h.getPosition();
            //System.out.println("Comparing " + position + " to " + other);
            if (Math.abs(position.getX() - other.getX()) <= 3 && Math.abs(position.getY() - other.getY()) <= 3) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        boolean canHowl = isNearHeroes(dgs);
        return f instanceof Monster && (((Monster) f).hasAction("Howl")) && !f.getNActionsExecuted().isMaximum() && canHowl;
    }
}

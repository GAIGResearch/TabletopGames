package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.ArrayList;
import java.util.List;

public class Sneaky extends DescentAction {

    public static List<Integer> linesOfSight = new ArrayList<>();

    public Sneaky(Triggers triggerPoint) {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public boolean execute(DescentGameState gs) {
        // TODO: Implement Doors
        return false;
    }

    @Override
    public DescentAction copy() {
        return null;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return false;
    }

    public static void setLinesOfSight(DescentGameState dgs, Figure f) {
        clearLinesOfSight();
        linesOfSight = DescentHelper.getAllLinesOfSight(dgs, f, "Monsters");
    }

    public static void clearLinesOfSight() {
        linesOfSight.clear();
    }

    public static List<Integer> getLinesOfSight()
    {
        return linesOfSight;
    }

    public static boolean beSneaky(int target) {
        if (linesOfSight.isEmpty()) return true;
        return linesOfSight.contains(target);
    }
}

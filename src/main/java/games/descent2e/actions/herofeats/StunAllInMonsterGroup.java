package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StunAllInMonsterGroup extends DescentAction {

    // Ashrian Heroic Feat
    List<Integer> monsters = new ArrayList<>();
    String monsterName;
    int range;
    public StunAllInMonsterGroup(List<Monster> monsters, int range) {
        super(Triggers.ACTION_POINT_SPEND);
        for (Monster monster : monsters) {
            this.monsters.add(monster.getComponentID());
        }
        this.monsterName = monsters.get(0).getName().replace(" master", "").replace(" minion", "");
        this.range = range;
    }

    public StunAllInMonsterGroup(List<Integer> monsters, String monsterName, int range) {
        super(Triggers.ACTION_POINT_SPEND);
        this.monsters = monsters;
        this.monsterName = monsterName;
        this.range = range;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        for (int monster : monsters) {
            ((Monster) dgs.getComponentById(monster)).addCondition(DescentTypes.DescentCondition.Stun);
        }
        if (f instanceof Hero) {((Hero) f).setFeatAvailable(false);}
        f.getNActionsExecuted().increment();
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public StunAllInMonsterGroup copy() {
        return new StunAllInMonsterGroup(monsters, monsterName, range);
    }

    boolean canStunMonsters(DescentGameState dgs) {
        // Check all monsters in our chosen group
        // If at least one of them is within range, we can stun the whole group
        Vector2D position = dgs.getActingFigure().getPosition();
        for(int monster : monsters) {
            Vector2D monPos = ((Monster) dgs.getComponentById(monster)).getPosition();
            if(DescentHelper.inRange(position, monPos, range)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        boolean canStunMonsters = canStunMonsters(dgs);
        return  !f.getNActionsExecuted().isMaximum() && canStunMonsters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StunAllInMonsterGroup other) {
            return monsters.equals(other.monsters) && other.monsterName.equals(monsterName) && other.range == range;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), monsters, monsterName, range);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (monsters.size() > 1)
            return "Heroic Feat: Stun all " + monsterName +"s";
        else
            return "Heroic Feat: Stun " + monsterName;
    }
    @Override
    public String toString() {
        return "Heroic Feat: Stun Monster Group (" + monsterName + "s)";
    }
}

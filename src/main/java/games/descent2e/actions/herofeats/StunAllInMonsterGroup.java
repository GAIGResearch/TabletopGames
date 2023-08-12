package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StunAllInMonsterGroup extends DescentAction {

    // Ashrian Heroic Feat
    String heroName = "Ashrian";
    List<Monster> monsters;
    String monsterName;
    int range;
    public StunAllInMonsterGroup(List<Monster> monsters, int range) {
        super(Triggers.ACTION_POINT_SPEND);
        this.monsters = monsters;
        this.monsterName = monsters.get(0).getName().replace(" master", "").replace(" minion", "");
        this.range = range;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();
        for (Monster monster : monsters) {
            monster.addCondition(DescentTypes.DescentCondition.Stun);
        }
        f.setFeatAvailable(false);
        f.getNActionsExecuted().increment();
        return true;
    }

    @Override
    public StunAllInMonsterGroup copy() {
        StunAllInMonsterGroup retVal = new StunAllInMonsterGroup(monsters, range);
        retVal.monsterName = monsterName;
        return retVal;
    }

    boolean canStunMonsters(DescentGameState dgs) {
        // Check all monsters in our chosen group
        // If at least one of them is within range, we can stun the whole group
        Vector2D position = dgs.getActingFigure().getPosition();
        for(Monster monster : monsters) {
            if((Math.abs(position.getX() - monster.getPosition().getX()) <= range && Math.abs(position.getY() - monster.getPosition().getY()) <= range)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();
        boolean canStunMonsters = canStunMonsters(dgs);
        return  f.getName().contains(heroName) && f.isFeatAvailable() && !f.getNActionsExecuted().isMaximum() && canStunMonsters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StunAllInMonsterGroup) {
            StunAllInMonsterGroup other = (StunAllInMonsterGroup) obj;
            return other.monsters == monsters && other.monsterName == monsterName && other.range == range;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(monsters, monsterName, range);
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
        return "Heroic Feat: Ashrian - Group Stun";
    }
}

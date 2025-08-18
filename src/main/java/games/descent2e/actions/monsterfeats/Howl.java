package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import org.apache.spark.sql.sources.In;

import java.util.List;
import java.util.Objects;

public class Howl extends DescentAction implements IExtendedSequence {

    final List<Integer> heroes;
    final int attackingFigure;
    int currentIndex;

    public Howl(int attackingFigure, List<Integer> targets) {
        super(Triggers.ACTION_POINT_SPEND);
        this.attackingFigure = attackingFigure;
        this.heroes = targets;
        currentIndex = 0;
    }

    @Override
    public String toString() {
        return "Howl";
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure monster = (Figure) state.getComponentById(attackingFigure);
        monster.getNActionsExecuted().increment();
        monster.addActionTaken(toString());
        state.setActionInProgress(this);
        return true;
    }


    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int defendingFigure = heroes.get(currentIndex);
        return List.of(new HowlTest(defendingFigure, Figure.Attribute.Willpower, attackingFigure));
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // We find the player for the next hero
        Figure h = (Figure) state.getComponentById(heroes.get(currentIndex));
        return h.getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof HowlTest) {
            currentIndex++;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return currentIndex == heroes.size();
    }

    @Override
    public Howl copy() {
        Howl retValue = new Howl(attackingFigure, List.copyOf(heroes));
        retValue.currentIndex = currentIndex;
        return retValue;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.HOWL));
    }

    public List<Integer> getTargets() {
        return List.copyOf(heroes);
    }

    public int currentTarget() {
        if (currentIndex < heroes.size()) {
            return heroes.get(currentIndex);
        } else {
            return -1; // No current target
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Howl howl = (Howl) o;
        return attackingFigure == howl.attackingFigure && currentIndex == howl.currentIndex &&
                Objects.equals(heroes, howl.heroes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroes, attackingFigure, currentIndex);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Howl by " + ((Figure) gameState.getComponentById(attackingFigure)).getName() +
                " at " + heroes.stream().map(id -> ((Figure) gameState.getComponentById(id)).getName()).toList();
    }
}

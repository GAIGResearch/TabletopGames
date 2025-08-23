package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Utils;

/**
 * Monsters with the Fly passive ignore the effects of terrain when moving, but must land at the end of their movement.
 * Though they can still interrupt their movement to perform an action as usual,
 * they must land first before that action is legal.
 * Afterwards, they can still move as normal.
 * This action simply applies the effects, if any, of the terrain position they've landed on.
 */

public class Land extends DescentAction {
    public Land() {
        super(Triggers.MOVE_INTO_SPACE);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Monster f = (Monster) gs.getActingFigure();
        f.setFlying(false);
        BoardNode tile = gs.getMasterBoard().getElement(f.getPosition());
        DescentTypes.TerrainType terrain = Utils.searchEnum(DescentTypes.TerrainType.class, tile.getComponentName());
        Move.terrainPenalty(gs, f, terrain);
        return true;
    }

    @Override
    public Land copy() {
        return new Land();
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f == null) return false;
        if (!f.hasMoved()) return false; // Must have moved to land
        return f instanceof Monster && ((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.FLY) && ((Monster) f).isFlying();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Land;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() {
        return "Land (End Movement)";
    }
}

package games.descent.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent.DescentGameState;
import games.descent.DescentParameters;
import games.descent.DescentTurnOrder;
import games.descent.components.Figure;
import games.descent.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;


public class Move extends AbstractAction {
    Vector2D location;

    public Move(Vector2D whereTo) {
        this.location = whereTo;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState)gs;
        DescentParameters dp = (DescentParameters)gs.getGameParameters();
        int currentPlayer = gs.getCurrentPlayer();

        Figure f;
        if (currentPlayer == 0) {
            // Move monsters
            int monsterGroupIdx = ((DescentTurnOrder)dgs.getTurnOrder()).getMonsterGroupActingNext();
            ArrayList<Monster> monsterGroup = dgs.getMonsters().get(monsterGroupIdx);
            f = monsterGroup.get(((DescentTurnOrder)dgs.getTurnOrder()).getMonsterActingNext());
        }
        else {
            // Move corresponding hero player
            f = dgs.getHeroes().get(currentPlayer-1);
        }

        String currentTile = dgs.getMasterBoard().getElement(f.getLocation().getX(), f.getLocation().getY());
        String destinationTile = dgs.getMasterBoard().getElement(location.getX(), location.getY());

        dgs.getMasterBoardOccupancy().setElement(f.getLocation().getX(), f.getLocation().getY(), -1);
        dgs.getMasterBoardOccupancy().setElement(location.getX(), location.getY(), f.getComponentID());
        f.setLocation(location);

        if (destinationTile.equals("water") && !currentTile.equals("pit")) {
            f.setMovePoints(f.getMovePoints() - dp.waterMoveCost);  // Difficult terrain
        } else {
            if (!currentTile.equals("pit")) {
                f.setMovePoints(f.getMovePoints() - 1);  // Normal move
            }
            if (destinationTile.equals("pit")) {
                f.setHp(f.getHp() - dp.pitFallHpCost);  // Hurts
            }
            if (destinationTile.equals("lava") || destinationTile.equals("hazard")) {
                f.setHp(f.getHp() - dp.lavaHpCost);  // Hurts
            }
        }

        // Check if move action finished
        if (f.getMovePoints() == 0 || currentTile.equals("pit")) f.setNActionsExecuted(f.getNActionsExecuted() + 1);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new Move(location.copy());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Move;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Move";
    }
}

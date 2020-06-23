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
        // Update location
        Vector2D oldLocation = f.getLocation().copy();
        f.setLocation(location.copy());

        // TODO: maybe change orientation if monster doesn't fit vertically
        int w = 1;
        int h = 1;
        if (f.getSize()!= null) {
            w = f.getSize().a;
            h = f.getSize().b;
        }

        boolean toWater = false;
        boolean inPit = false;
        boolean toPit = false;
        boolean toLava = false;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                String currentTile = dgs.getMasterBoard().getElement(oldLocation.getX() + j, oldLocation.getY() + i);
                String destinationTile = dgs.getMasterBoard().getElement(location.getX() + j, location.getY() + i);

                dgs.getMasterBoardOccupancy().setElement(f.getLocation().getX() + j, f.getLocation().getY() + i, -1);
                dgs.getMasterBoardOccupancy().setElement(location.getX() + j, location.getY() + i, f.getComponentID());

                if (currentTile.equals("pit")) {
                    inPit = true;
                }
                switch (destinationTile) {
                    case "water":
                        toWater = true;
                        break;
                    case "pit":
                        toPit = true;
                        break;
                    case "lava":
                    case "hazard":
                        toLava = true;
                        break;
                }
            }
        }

        if (!inPit) {
            // Can't spend move points in pit, it's just one action
            if (toWater) {
                f.setMovePoints(f.getMovePoints() - dp.waterMoveCost);  // Difficult terrain
            } else {
                f.setMovePoints(f.getMovePoints() - 1);  // Normal move
            }
        }

        if (toPit) {
            f.setHp(f.getHp() - dp.pitFallHpCost);  // Hurts
        }
        if (toLava) {
            f.setHp(f.getHp() - dp.pitFallHpCost);  // Hurts
        }

        // Check if move action finished
        if (f.getMovePoints() == 0 || inPit) f.setNActionsExecuted(f.getNActionsExecuted() + 1);
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

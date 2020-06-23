package games.descent.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.properties.PropertyInt;
import games.descent.DescentGameState;
import games.descent.components.Figure;
import utilities.Vector2D;

import static games.descent.DescentConstants.movementHash;

public class Move extends AbstractAction {
    Vector2D location;

    public Move(Vector2D whereTo) {
        this.location = whereTo;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        int currentPlayer = gs.getCurrentPlayer();
        if (currentPlayer == 0) {
            // TODO: move monsters
            return false;
        }
        else {
            DescentGameState dgs = (DescentGameState)gs;
            Figure f = dgs.getHeroes().get(currentPlayer-1);
            PropertyInt moveSpeed = (PropertyInt)f.getProperty(movementHash);
            f.setLocation(location);
            String destinationTile = dgs.getMasterBoard().getElement(location.getX(), location.getY());
            if (destinationTile.equals("water")) {
                f.setMovePoints(f.getMovePoints() - 2);  // Difficult terrain
            } else if (destinationTile.equals("pit")) {
                f.setMovePoints(moveSpeed.value);  // Finish movement
                f.setHp(f.getHp() - 1);
            } else {
                f.setMovePoints(f.getMovePoints() - 1);
                if (destinationTile.equals("lava") || destinationTile.equals("hazard")) {
                    f.setHp(f.getHp() - 1);  // Hurts
                }
            }
            return true;
        }
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

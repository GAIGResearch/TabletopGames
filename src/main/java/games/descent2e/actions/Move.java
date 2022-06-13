package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

public class Move extends AbstractAction {

    public enum Direction{
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    Vector2D position;
    Vector2D adjacentPosition;


    public Move(Vector2D position, Vector2D adjacentPosition){
        this.position = position;
        this.adjacentPosition = adjacentPosition;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState)gs;
        DescentParameters dp = (DescentParameters)gs.getGameParameters();

        Figure f = ((DescentGameState) gs).getActingFigure();

        Vector2D oldLocation = f.getPosition().copy();

        f.setPosition(position.copy());

        BoardNode currentTile = dgs.getMasterBoard().getElement(oldLocation.getX(), oldLocation.getY());
        BoardNode destinationTile = dgs.getMasterBoard().getElement(position.getX(), position.getY());

        PropertyInt prop1 = new PropertyInt("players", -1);
        PropertyInt prop2 = new PropertyInt("players", f.getComponentID());

        currentTile.setProperty(prop1);

        if (f instanceof Monster){
            if (f.getSize() != null && (f.getSize().a > 1 || f.getSize().b > 1)) {
                Vector2D oldAdjacentLocation = ((Monster) f).getAdjacentLocation();
                ((Monster) f).setAdjacentLocation(adjacentPosition.copy());

                BoardNode originalAdjacentTile = dgs.getMasterBoard().getElement(oldAdjacentLocation.getX(), oldAdjacentLocation.getY());
                BoardNode destinationAdjacentTile = dgs.getMasterBoard().getElement(adjacentPosition.getX(), adjacentPosition.getY());

                originalAdjacentTile.setProperty(prop1);
                destinationAdjacentTile.setProperty(prop2);
            }
        }

        destinationTile.setProperty(prop2);

        return true;


        /*
        // TODO: maybe change orientation if monster doesn't fit vertically
        int w = 1;
        int h = 1;
        if (f.getSize()!= null) {
            w = f.getSize().a;
            h = f.getSize().b;
        }


        // TODO: find old locations,
        boolean toWater = false;
        boolean inPit = false;
        boolean toPit = false;
        boolean toLava = false;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                BoardNode currentTile = dgs.getMasterBoard().getElement(oldLocation.getX() + j, oldLocation.getY() + i);
                BoardNode destinationTile = dgs.getMasterBoard().getElement(position.getX() + j, position.getY() + i);

                PropertyInt prop1 = new PropertyInt("players", -1);
                PropertyInt prop2 = new PropertyInt("players", f.getComponentID());
                currentTile.setProperty(prop1);
                destinationTile.setProperty(prop2);

                if (currentTile.getComponentName().equals("pit")) {
                    inPit = true;
                }
                switch (destinationTile.getComponentName()) {
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
        // TODO: Calculate this in computeAvailableActions() in DescentForwardModel
        if (!inPit) {
            // Can't spend move points in pit, it's just one action
            if (toWater) {
                f.incrementAttribute(Figure.Attribute.MovePoints, - dp.waterMoveCost);  // Difficult terrain
            } else {
                f.incrementAttribute(Figure.Attribute.MovePoints, - 1);  // Normal move
            }
        }

        if (toPit) {
            f.incrementAttribute(Figure.Attribute.Health, - dp.pitFallHpCost);  // Hurts
        }
        if (toLava) {
            f.incrementAttribute(Figure.Attribute.Health, - dp.pitFallHpCost);  // Hurts
        }

        // Check if move action finished
        if (f.getAttribute(Figure.Attribute.MovePoints).getValue() == 0 || inPit) f.setNActionsExecuted(f.getNActionsExecuted() + 1);
        return true;

         */
    }

    @Override
    public AbstractAction copy() {
        return new Move(position.copy(), adjacentPosition.copy());
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

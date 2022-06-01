package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.components.Figure;
import utilities.Vector2D;


public class Move extends AbstractAction {
    Vector2D position;

    public Move(Vector2D whereTo) {
        this.position = whereTo;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState)gs;
        DescentParameters dp = (DescentParameters)gs.getGameParameters();

        Figure f = ((DescentGameState) gs).getActingFigure();
        // Update location
        Vector2D oldLocation = f.getPosition().copy();
        f.setPosition(position.copy());

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
    }

    @Override
    public AbstractAction copy() {
        return new Move(position.copy());
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

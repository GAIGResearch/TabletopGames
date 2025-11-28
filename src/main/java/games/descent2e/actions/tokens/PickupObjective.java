package games.descent2e.actions.tokens;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import games.descent2e.components.tokens.DToken;

public class PickupObjective extends InteractObjective {

    public PickupObjective() {
        super();
    }

    public PickupObjective(int objectiveID) {
        super(objectiveID);
    }

    public PickupObjective(int objectiveID, int figureID) {
        super(objectiveID, figureID);
    }

    @Override
    public PickupObjective _copy() {
        return new PickupObjective(tokenID, figureID);
    }

    @Override
    protected void interact(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        DToken token = (DToken) dgs.getComponentById(tokenID);
        token.setOwnerId(f.getComponentID());
        token.setPosition(null);

        //System.out.println("Obtained " + token.getComponentName());

        complete = true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Interact with", "Pick up");
    }

    @Override
    public String toString() {
        return super.toString().replace("Interact with", "Pick up");
    }
}

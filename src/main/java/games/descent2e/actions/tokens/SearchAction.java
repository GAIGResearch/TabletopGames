package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.InterruptPoints;

// Draw random search card and add to player
public class SearchAction extends DescentAction {
    public SearchAction() {
        super(InterruptPoints.ACTION_POINT_SPEND);
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        // TODO put card randomly drawn into currentHero.otherEquipment
        return false;
    }
}

package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.wonders7.Wonders7GameState;

import java.util.Objects;

public class ChooseCard extends AbstractAction {

    // actionChosen must be immutable for this to work!
    // (All 7-Wonders actions are as of 30-Oct-23)
    public final AbstractAction actionChosen;

    public ChooseCard(AbstractAction actionChosen) {
        this.actionChosen = actionChosen;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Wonders7GameState wgs = (Wonders7GameState) gs;
        wgs.setTurnAction(wgs.getCurrentPlayer(), actionChosen); // PLAYER CHOOSES ACTION
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ChooseCard && ((ChooseCard) obj).actionChosen.equals(actionChosen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionChosen) + 373;
    }


    @Override
    public String toString() {
        return "Chooses action " + actionChosen.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

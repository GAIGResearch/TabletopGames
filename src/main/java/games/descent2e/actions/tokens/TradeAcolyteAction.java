package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.Acolyte;

import java.util.List;
import java.util.Objects;

public class TradeAcolyteAction extends TokenAction implements IExtendedSequence {
    final int receivingPlayerID;

    public TradeAcolyteAction() {
        super(-1, Triggers.ACTION_POINT_SPEND);
        receivingPlayerID = -1;
    }
    public TradeAcolyteAction(int acolyteComponentID) {
        super(acolyteComponentID, Triggers.ACTION_POINT_SPEND);
        this.receivingPlayerID = -1;
    }
    public TradeAcolyteAction(int acolyteComponentID, int receivingPlayerID) {
        super(acolyteComponentID, Triggers.ACTION_POINT_SPEND);
        this.receivingPlayerID = receivingPlayerID;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // TODO: trade with adjacent players
        DescentGameState dgs = (DescentGameState) state;
        Acolyte acolyte = (Acolyte) dgs.getComponentById(tokenID);
        Hero hero = dgs.getHeroes().get(acolyte.getOwnerId()-1);
        return null;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(tokenID).getOwnerId();
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return false;
    }

    @Override
    public TradeAcolyteAction copy() {
        return new TradeAcolyteAction(tokenID, receivingPlayerID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeAcolyteAction)) return false;
        TradeAcolyteAction that = (TradeAcolyteAction) o;
        return receivingPlayerID == that.receivingPlayerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(receivingPlayerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Trade acolyte with " + receivingPlayerID;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        if (receivingPlayerID != -1) {
            Acolyte acolyte = (Acolyte) gs.getComponentById(tokenID);
            acolyte.setOwnerId(receivingPlayerID, gs);
        }
        return false;
    }
}

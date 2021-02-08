package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class AcceptTrade extends AbstractAction {
    protected OfferPlayerTrade offeredTrade;

    public AcceptTrade(OfferPlayerTrade offeredTrade) {
        this.offeredTrade = offeredTrade;
    }

    @Override
    public boolean execute(AbstractGameState gs) {


        return true;
    }

    @Override
    public AbstractAction copy() {
        return new AcceptTrade((OfferPlayerTrade) offeredTrade.copy());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AcceptTrade){
            AcceptTrade otherAction = (AcceptTrade)obj;
            return offeredTrade.equals(otherAction.offeredTrade);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        //todo expand string
        return "Player " + offeredTrade.getOtherPlayerID() + " trading with "
                + offeredTrade.getOtherPlayerID();
    }
}

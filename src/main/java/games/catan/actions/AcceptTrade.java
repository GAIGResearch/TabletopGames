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
        return false;
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
        return "Player " + offeredTrade.getOtherPlayerID() + " giving "
                + offeredTrade.getResourcesOffered().size() + " "
                + offeredTrade.getResourcesOffered().get(0) + " in exchange for "
                + offeredTrade.getResourcesRequested().size() + " "
                + offeredTrade.getResourcesRequested().get(0) + " from player "
                + offeredTrade.getOtherPlayerID();
    }
}

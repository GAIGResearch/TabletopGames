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
}

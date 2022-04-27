package games.catan.actions;

import core.AbstractGameState;
import core.actions.DoNothing;
import games.catan.CatanGameState;

public class EndNegotiation extends DoNothing {
    // This is used as an indication that we are stopping this round of negotiation

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        cgs.setCurrentTradeOffer(null);
        return true;
    }

    @Override
    public String toString() {
        return "Ends Negotiation";
    }
}

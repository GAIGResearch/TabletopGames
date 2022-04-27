package games.catan.actions;

import core.actions.DoNothing;

public class EndNegotiation extends DoNothing {
    // This is used as an indication that we are stopping this round of negotiation

    @Override
    public String toString() {
        return "Ends Negotiation";
    }
}

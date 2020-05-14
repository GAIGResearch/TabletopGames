package games.coltexpress;

import core.turnorder.AlternatingTurnOrder;
import core.turnorder.TurnOrder;
import games.coltexpress.cards.roundcards.RoundCard;
import games.coltexpress.cards.roundcards.RoundCardBridge;

import java.util.ArrayList;
import java.util.List;


public class ColtExpressTurnOrder extends AlternatingTurnOrder {

    List<RoundCard> rounds = new ArrayList<>(5);

    public ColtExpressTurnOrder(int nPlayers){
        super(nPlayers);
        setStartingPlayer(0);
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
    }

    @Override
    public TurnOrder copy() {
        ColtExpressTurnOrder to = (ColtExpressTurnOrder) super.copy();
        return copyTo(to);
    }
}

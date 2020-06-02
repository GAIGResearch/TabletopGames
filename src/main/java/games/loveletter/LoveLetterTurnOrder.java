package games.loveletter;

import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;


public class LoveLetterTurnOrder extends AlternatingTurnOrder {

    public LoveLetterTurnOrder(int nPlayers){
        super(nPlayers);
        setStartingPlayer(0);
    }

    @Override
    protected TurnOrder _copy() {
        LoveLetterTurnOrder to = new LoveLetterTurnOrder(nPlayers);
        to.direction = direction;
        return to;
    }
}

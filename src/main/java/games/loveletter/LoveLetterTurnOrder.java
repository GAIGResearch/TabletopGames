package games.loveletter;

import core.turnorder.AlternatingTurnOrder;
import core.turnorder.TurnOrder;


public class LoveLetterTurnOrder extends AlternatingTurnOrder {

    public LoveLetterTurnOrder(int nPlayers){
        super(nPlayers);
    }

    @Override
    public TurnOrder copy() {
        LoveLetterTurnOrder to = (LoveLetterTurnOrder) super.copy();
        return copyTo(to);
    }
}

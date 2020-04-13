package explodingkittens.actions;

import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensGameState;

public class AttackAction extends PlayCard implements IsNopeable{
    int target;

    public AttackAction(int playerID, Card card, int target) {
        super(playerID, card);
        this.target = target;
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);
        ((ExplodingKittensGameState) gs).setActivePlayer(target);
        if (((ExplodingKittensGameState) gs).remainingDraws == 1)
        {
            ((ExplodingKittensGameState) gs).remainingDraws = 2;
        } else{
            ((ExplodingKittensGameState) gs).remainingDraws += 2;
        }
        return false;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d attacks player %d", playerID, target);
    }

    @Override
    public boolean nopedExecute(GameState gs) {
        return super.execute(gs);
    }
}

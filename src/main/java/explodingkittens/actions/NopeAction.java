package explodingkittens.actions;

import components.Card;
import core.GameState;

public class NopeAction extends PlayCard implements IsNope {

    public NopeAction(int playerID, Card card) {
        super(playerID, card);
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);
        //((ExplodingKittensGame)gs)
        return false;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d nopes the previous action", playerID);
    }

    @Override
    public boolean isNope() {
        return true;
    }
}

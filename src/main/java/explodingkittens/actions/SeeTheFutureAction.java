package explodingkittens.actions;

import components.Card;
import core.GameState;

public class SeeTheFutureAction extends PlayCard implements IsNopeable {
    public SeeTheFutureAction(int playerID, Card card) {
        super(playerID, card);
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);
        return false;
    }

    @Override
    public String toString() {//overriding the toString() method
        return String.format("Player %d sees the future // Not Implemented Yet", playerID);
    }

    public boolean nopedExecute(GameState gs) {
        return super.execute(gs);
    }
}

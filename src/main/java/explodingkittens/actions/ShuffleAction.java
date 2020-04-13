package explodingkittens.actions;

import components.Card;
import core.GameState;

public class ShuffleAction extends PlayCard implements IsNopeable {
    public ShuffleAction(int playerID, Card card) {
        super(playerID, card);
    }

    @Override
    public boolean execute(GameState gs) {
        gs.findDeck("DrawDeck").shuffle();
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return String.format("Player %d shuffles the deck", playerID);
    }

    public boolean nopedExecute(GameState gs) {
        return super.execute(gs);
    }

}

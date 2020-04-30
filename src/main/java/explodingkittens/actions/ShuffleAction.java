package explodingkittens.actions;

import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensGameState;

public class ShuffleAction extends PlayCard implements IsNopeable {
    public ShuffleAction(int playerID, Card card) {
        super(playerID, card);
    }

    @Override
    public boolean execute(GameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        ekgs.getDrawDeck().shuffle();
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

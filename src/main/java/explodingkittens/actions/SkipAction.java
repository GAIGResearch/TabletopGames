package explodingkittens.actions;

import actions.Action;
import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensGameState;

public class SkipAction extends PlayCard implements IsNopeable {

    int nextPlayer;

    public SkipAction(int playerID, Card card, int nextPlayer)
    {
        super(playerID, card);
        this.nextPlayer = nextPlayer;
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);
        int nextPlayer = ((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID);
        if (nextPlayer != playerID)
            ((ExplodingKittensGameState) gs).remainingDraws = 1;
        else
            ((ExplodingKittensGameState) gs).remainingDraws -= 1;

        ((ExplodingKittensGameState) gs).setActivePlayer(nextPlayer);
        return true;
    }

    @Override
    public String toString(){
        return String.format("Player %d skips its draw", playerID);
    }

    @Override
    public boolean nopedExecute(GameState gs) {
        return super.execute(gs);
    }
}

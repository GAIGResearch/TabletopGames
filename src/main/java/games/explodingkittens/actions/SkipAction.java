package games.explodingkittens.actions;

import core.components.Card;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import core.turnorder.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.GamePhase.PlayerMove;

public class SkipAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {

    public SkipAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck)
    {
        super(card, playerDeck, discardDeck);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        ((ExplodingKittensGameState) gs).setGamePhase(PlayerMove);
        ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        //int nextPlayer = ((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID);
        //if (nextPlayer != playerID)
        //    ((ExplodingKittensGameState) gs).remainingDraws = 1;
        //else
        //    ((ExplodingKittensGameState) gs).remainingDraws -= 1;

        //((ExplodingKittensGameState) gs).setActivePlayer(nextPlayer);
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){
        return "Player skips its draw";
    }

    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

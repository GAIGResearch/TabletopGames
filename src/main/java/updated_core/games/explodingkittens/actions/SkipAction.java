package updated_core.games.explodingkittens.actions;

import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;
import updated_core.actions.IPrintable;
import updated_core.games.explodingkittens.ExplodingKittensGamePhase;
import updated_core.games.explodingkittens.ExplodingKittensGameState;
import updated_core.games.explodingkittens.cards.ExplodingKittenCard;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class SkipAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {

    public SkipAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck)
    {
        super(card, playerDeck, discardDeck);
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);
        ((ExplodingKittensGameState) gs).gamePhase = ExplodingKittensGamePhase.PlayerMove;
        turnOrder.endPlayerTurn(gs);
        //int nextPlayer = ((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID);
        //if (nextPlayer != playerID)
        //    ((ExplodingKittensGameState) gs).remainingDraws = 1;
        //else
        //    ((ExplodingKittensGameState) gs).remainingDraws -= 1;

        //((ExplodingKittensGameState) gs).setActivePlayer(nextPlayer);
        return true;
    }

    @Override
    public String toString(){
        return "Player skips its draw";
    }

    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.Execute(gs, turnOrder);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

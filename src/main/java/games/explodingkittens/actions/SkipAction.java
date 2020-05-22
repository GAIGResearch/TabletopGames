package games.explodingkittens.actions;

import core.components.Card;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import core.turnorder.TurnOrder;

public class SkipAction<T extends Component> extends PlayCard<T> implements IsNopeable, IPrintable {

    public SkipAction(T card, Deck<T> playerDeck, Deck<T> discardDeck)
    {
        super(card, playerDeck, discardDeck);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        gs.setMainGamePhase();
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

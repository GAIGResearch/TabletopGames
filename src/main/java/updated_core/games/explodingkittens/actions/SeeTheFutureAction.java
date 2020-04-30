package updated_core.games.explodingkittens.actions;

import components.Deck;
import components.IDeck;
import updated_core.actions.IPrintable;
import updated_core.components.IPartialObservableDeck;
import updated_core.games.explodingkittens.ExplodingKittensGamePhase;
import updated_core.games.explodingkittens.ExplodingKittensGameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class SeeTheFutureAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    private final IPartialObservableDeck<T> drawPile;
    private final int playerID;

    public SeeTheFutureAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck, int playerID, IPartialObservableDeck<T> drawPile) {
        super(card, playerDeck, discardDeck);
        this.drawPile = drawPile;
        this.playerID = playerID;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);
        ((ExplodingKittensGameState)gs).gamePhase = ExplodingKittensGamePhase.SeeTheFuturePhase;
        return false;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player wants to see the future // Not Implemented Yet";
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.Execute(gs, turnOrder);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

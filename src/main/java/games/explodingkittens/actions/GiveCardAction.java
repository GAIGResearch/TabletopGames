package updated_core.games.explodingkittens.actions;

import components.Deck;
import components.IDeck;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.games.explodingkittens.ExplodingKittenTurnOrder;
import updated_core.games.explodingkittens.ExplodingKittensGamePhase;
import updated_core.games.explodingkittens.ExplodingKittensGameState;
import updated_core.games.explodingkittens.cards.ExplodingKittenCard;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;


public class GiveCardAction implements IAction, IPrintable {

    final ExplodingKittenCard card;
    final IDeck<ExplodingKittenCard> giverDeck;
    final IDeck<ExplodingKittenCard> receiverDeck;

    public GiveCardAction(ExplodingKittenCard card, IDeck<ExplodingKittenCard> giverDeck, IDeck<ExplodingKittenCard> receiverDeck) {
        this.card = card;
        this.giverDeck = giverDeck;
        this.receiverDeck = receiverDeck;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        giverDeck.remove(card);
        receiverDeck.add(card);
        ((ExplodingKittensGameState) gs).gamePhase = ExplodingKittensGamePhase.PlayerMove;
        ((ExplodingKittenTurnOrder) turnOrder).endPlayerTurn(gs);
        ((ExplodingKittenTurnOrder) turnOrder).currentPlayer = ((ExplodingKittensGameState) gs).playerGettingAFavor;
        return true;
    }

    @Override
    public String toString(){
        String cardtype = card.cardType.toString();
        return String.format("Player gives %s Card for a favor", cardtype);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }

}

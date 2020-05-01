package games.explodingkittens.actions;

import actions.IAction;
import components.IDeck;
import core.AbstractGameState;
import observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGamePhase;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;
import turnorder.TurnOrder;


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
        turnOrder.endPlayerTurn(gs);
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

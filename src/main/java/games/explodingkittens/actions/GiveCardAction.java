package games.explodingkittens.actions;

import core.actions.IAction;
import core.components.Card;
import core.AbstractGameState;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;

public class GiveCardAction implements IAction, IPrintable {

    final ExplodingKittenCard card;
    final Deck<ExplodingKittenCard> giverDeck;
    final Deck<ExplodingKittenCard> receiverDeck;

    public GiveCardAction(ExplodingKittenCard card, Deck<ExplodingKittenCard> giverDeck, Deck<ExplodingKittenCard> receiverDeck) {
        this.card = card;
        this.giverDeck = giverDeck;
        this.receiverDeck = receiverDeck;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        ExplodingKittenTurnOrder ekto = ((ExplodingKittenTurnOrder) gs.getTurnOrder());
        giverDeck.remove(card);
        receiverDeck.add(card);
        gs.setMainGamePhase();
        ekto.endPlayerTurnStep(gs);
        ekto.addReactivePlayer(ekgs.getPlayerGettingAFavor());
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){
        String cardtype = card.cardType.toString();
        return String.format("Player gives %s Card for a favor", cardtype);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }

}

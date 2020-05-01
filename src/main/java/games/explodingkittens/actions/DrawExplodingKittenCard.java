package games.explodingkittens.actions;

import actions.IAction;
import components.IDeck;
import core.AbstractGameState;
import observations.IPrintable;
import games.explodingkittens.ExplodingKittensGamePhase;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;
import turnorder.TurnOrder;

public class DrawExplodingKittenCard implements IAction, IPrintable {

    int playerID;
    final IDeck<ExplodingKittenCard> deckFrom;
    final IDeck<ExplodingKittenCard>  deckTo;

    public DrawExplodingKittenCard (int playerID, IDeck<ExplodingKittenCard>  deckFrom, IDeck<ExplodingKittenCard> deckTo) {
        this.playerID = playerID;
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public String toString(){
        return String.format("Player %d draws a card", playerID);
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        ExplodingKittenCard c = deckFrom.draw();
        ExplodingKittenCard.CardType type = c.cardType;
        if (type == ExplodingKittenCard.CardType.EXPLODING_KITTEN) {
            ExplodingKittenCard defuseCard = null;
            for (ExplodingKittenCard card : deckTo.getCards()){
                if (card.cardType == ExplodingKittenCard.CardType.DEFUSE){
                    defuseCard = card;
                    break;
                }
            }
            if (defuseCard != null){
                ((ExplodingKittensGameState) gs).gamePhase = ExplodingKittensGamePhase.DefusePhase;
                deckTo.remove(defuseCard);
                deckTo.add(c);
            } else {
                System.out.println("Player " + playerID + " died");
                ((ExplodingKittensGameState) gs).killPlayer(this.playerID);
                IDeck<ExplodingKittenCard> discardDeck = ((ExplodingKittensGameState)gs).discardPile;
                for (ExplodingKittenCard card : deckTo.getCards()){
                    discardDeck.add(card);
                }
                deckTo.clear();
                discardDeck.add(c);
                turnOrder.endPlayerTurn(gs);
                //((ExplodingKittensGameState) gs).setActivePlayer(((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID));
                //((ExplodingKittensGameState) gs).remainingDraws = 1;
            }
        } else {
            deckTo.add(c);
            turnOrder.endPlayerTurn(gs);
        }
        return true;
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

package updated_core.games.explodingkittens.actions;

import components.IDeck;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.games.explodingkittens.ExplodingKittensGamePhase;
import updated_core.games.explodingkittens.ExplodingKittensGameState;
import updated_core.games.explodingkittens.cards.ExplodingKittenCard;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

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

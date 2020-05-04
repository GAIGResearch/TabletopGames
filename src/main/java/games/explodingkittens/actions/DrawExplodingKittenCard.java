package games.explodingkittens.actions;

import core.actions.IAction;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;

import static games.explodingkittens.ExplodingKittensGameState.GamePhase.DefusePhase;

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
    public boolean execute(AbstractGameState gs) {
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
                ((ExplodingKittensGameState) gs).setGamePhase(DefusePhase);
                deckTo.remove(defuseCard);
                deckTo.add(c);
            } else {
                System.out.println("Player " + playerID + " died");
                ((ExplodingKittensGameState) gs).killPlayer(this.playerID);
                IDeck<ExplodingKittenCard> discardDeck = ((ExplodingKittensGameState)gs).getDiscardPile();
                for (ExplodingKittenCard card : deckTo.getCards()){
                    discardDeck.add(card);
                }
                deckTo.clear();
                discardDeck.add(c);
                gs.getTurnOrder().endPlayerTurnStep(gs);
                //((ExplodingKittensGameState) gs).setActivePlayer(((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID));
                //((ExplodingKittensGameState) gs).remainingDraws = 1;
            }
        } else {
            deckTo.add(c);
            gs.getTurnOrder().endPlayerTurnStep(gs);
        }
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

package games.explodingkittens.actions;

import core.actions.IAction;
import core.components.Card;
import core.AbstractGameState;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Defuse;

public class DrawExplodingKittenCard implements IAction, IPrintable {

    int playerID;
    final Deck<ExplodingKittenCard> deckFrom;
    final Deck<ExplodingKittenCard>  deckTo;

    public DrawExplodingKittenCard (int playerID, Deck<ExplodingKittenCard>  deckFrom, Deck<ExplodingKittenCard> deckTo) {
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
            for (ExplodingKittenCard card : deckTo.getComponents()){
                if (card.cardType == ExplodingKittenCard.CardType.DEFUSE){
                    defuseCard = card;
                    break;
                }
            }
            if (defuseCard != null){
                gs.setGamePhase(Defuse);
                deckTo.remove(defuseCard);
                deckTo.add(c);
            } else {
                System.out.println("Player " + playerID + " died");
                ((ExplodingKittensGameState) gs).killPlayer(this.playerID);
                Deck<ExplodingKittenCard> discardDeck = ((ExplodingKittensGameState)gs).getDiscardPile();
                for (ExplodingKittenCard card : deckTo.getComponents()){
                    discardDeck.add(card);
                }
                deckTo.clear();
                discardDeck.add(c);
                ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
                //((ExplodingKittensGameState) gs).setActivePlayer(((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID));
                //((ExplodingKittensGameState) gs).remainingDraws = 1;
            }
        } else {
            deckTo.add(c);
            ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        }
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

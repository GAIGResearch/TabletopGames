package explodingkittens.actions;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;
import explodingkittens.ExplodingKittenCard;
import explodingkittens.ExplodingKittensCardTypeProperty;
import explodingkittens.ExplodingKittensGamePhase;
import explodingkittens.ExplodingKittensGameState;

public class DrawExplodingKittenCard implements Action{

    int playerID;
    IDeck<Card> deckFrom;
    IDeck<Card>  deckTo;

    public DrawExplodingKittenCard (int playerID, IDeck<Card>  deckFrom, IDeck<Card> deckTo) {
        this.playerID = playerID;
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(GameState gs) {
        Card c = deckFrom.draw();
        ExplodingKittensCardTypeProperty type = (ExplodingKittensCardTypeProperty) c.getProperty(ExplodingKittensGameState.cardTypeHash);
        if (type.value == ExplodingKittenCard.EXPLODING_KITTEN) {
            Card defuseCard = null;
            for (Card card : deckTo.getCards()){
                if (((ExplodingKittensCardTypeProperty) card.getProperty(ExplodingKittensGameState.cardTypeHash)).value == ExplodingKittenCard.DEFUSE){
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
                IDeck<Card> discardDeck = gs.findDeck("DiscardDeck");
                for (Card card : deckTo.getCards()){
                    discardDeck.add(card);
                }
                deckTo.clear();
                discardDeck.add(c);
                ((ExplodingKittensGameState) gs).setActivePlayer(((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID));
                ((ExplodingKittensGameState) gs).remainingDraws = 1;
            }
        } else {
            deckTo.add(c);
            int nextPlayer = ((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID);
            if (nextPlayer != playerID)
                ((ExplodingKittensGameState) gs).remainingDraws = 1;
            else
                ((ExplodingKittensGameState) gs).remainingDraws -= 1;

            ((ExplodingKittensGameState) gs).setActivePlayer(nextPlayer);

        }
        return true;
    }

    @Override
    public String toString(){
        return String.format("Player %d draws a card", playerID);
    }

}

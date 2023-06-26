package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.Objects;

public class DiscardCard extends DrawCard {
    public String cardName;

    public DiscardCard(String cardName){
        this.cardName = cardName;
    }

    public boolean execute(AbstractGameState gameState){
        super.execute(gameState);
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Finds card being removed in player Hand
        int index=0; // The index of the card in hand
        for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++){ // Goes through each card in the playerHand
            if (cardName.equals(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName)){ // If cardName is the one searching for (being played)
                index = i;
            }
        }
        Wonder7Card card = wgs.getPlayerHand(wgs.getCurrentPlayer()).get(index); // Card being removed

        // Player gets 3 coins from discarding card
        int playerValue = wgs.getPlayerResources(wgs.getCurrentPlayer()).get(Wonders7Constants.Resource.coin); // No. Coins player has
        wgs.getPlayerResources(wgs.getCurrentPlayer()).put(Wonders7Constants.Resource.coin,  playerValue+3); // Adds 3 coins to player coin count

        // Removes card from player hand and adds to discarded cards deck
        wgs.getPlayerHand(wgs.getCurrentPlayer()).remove(card); // remove
        wgs.getDiscardPile().add(card); // add

        return true;
    }



    @Override
    public String toString() {
        return "Discard card " + cardName;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardCard)) return false;
        if (!super.equals(o)) return false;
        DiscardCard that = (DiscardCard) o;
        return Objects.equals(cardName, that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName);
    }

    @Override
    public AbstractAction copy(){return new DiscardCard(cardName);}

}

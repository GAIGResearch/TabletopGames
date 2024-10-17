package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.Objects;

public class DiscardCard extends DrawCard {
    public final String cardName;
    public final int player;

    public DiscardCard(String cardName, int player){
        this.cardName = cardName;
        this.player = player;
    }

    public boolean execute(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Finds card being removed in player Hand
        Wonder7Card card = null;
        for (Wonder7Card cardSearch: wgs.getPlayerHand(player).getComponents()){ // Goes through each card in the playerHand
            if (cardName.equals(cardSearch.cardName)){ // If cardName is the one searching for (being played)
                card = cardSearch;
                break;
            }
        }

        if (card == null) {
            throw new AssertionError("Card not found in player hand");
        }

        // Player gets 3 coins from discarding card
        int playerValue = wgs.getPlayerResources(player).get(Wonders7Constants.Resource.Coin); // No. Coins player has
        wgs.getPlayerResources(player).put(Wonders7Constants.Resource.Coin,  playerValue+ ((Wonders7GameParameters)wgs.getGameParameters()).nCoinsDiscard); // Adds 3 coins to player coin count

        // Removes card from player hand and adds to discarded cards deck
        boolean cardFound = wgs.getPlayerHand(player).remove(card); // remove
        if (!cardFound) {
            throw new AssertionError("Card not found in player hand");
        }
        wgs.getDiscardPile().add(card); // add

        return true;
    }



    @Override
    public String toString() {
        return "Player " + player + " discards card " + cardName;
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
        return player == that.player && Objects.equals(cardName, that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName, player);
    }

    @Override
    public DiscardCard copy(){return this;}

}

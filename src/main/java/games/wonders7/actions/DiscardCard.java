package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.Objects;

public class DiscardCard extends DrawCard {
    public final Wonder7Card.CardType cardType;
    public final int player;

    public DiscardCard(Wonder7Card.CardType cardType, int player){
        this.cardType = cardType;
        this.player = player;
    }

    public boolean execute(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Finds card being removed in player Hand
        Wonder7Card card = wgs.findCardInHand(player, cardType);

        // Player gets 3 coins from discarding card
        int playerValue = wgs.getPlayerResources(player).get(Wonders7Constants.Resource.Coin); // No. Coins player has
        wgs.getPlayerResources(player).put(Wonders7Constants.Resource.Coin,  playerValue+ ((Wonders7GameParameters)wgs.getGameParameters()).nCoinsDiscard); // Adds 3 coins to player coin count

        // Removes card from player hand and adds to discarded cards deck
        wgs.getPlayerHand(player).remove(card); // remove
        wgs.getDiscardPile().add(card); // add

        return true;
    }



    @Override
    public String toString() {
        return "Player " + player + " discards card " + cardType;
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
        return player == that.player && cardType == that.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardType.ordinal(), player);
    }

    @Override
    public DiscardCard copy(){return this;}

}

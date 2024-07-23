package games.cluedo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.cluedo.CluedoGameState;
import games.cluedo.cards.CluedoCard;

import java.util.Objects;

public class ShowHintCard extends AbstractAction {

    int fromPlayer = -1;
    int toPlayer = -1;
    int cardToShow = -1;

    public ShowHintCard(int currentPlayerId, int recipientPlayerId, int cardIndex) {
        fromPlayer = currentPlayerId;
        toPlayer = recipientPlayerId;
        cardToShow = cardIndex;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CluedoGameState cgs = (CluedoGameState) gs;

        if (fromPlayer != -1 && toPlayer != -1 && cardToShow != -1) {
            PartialObservableDeck<CluedoCard> fromPlayerHand = cgs.getPlayerHandCards().get(fromPlayer);
            fromPlayerHand.setVisibilityOfComponent(cardToShow, toPlayer, true);
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new ShowHintCard(fromPlayer, toPlayer, cardToShow);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShowHintCard)) return false;
        ShowHintCard that = (ShowHintCard) o;
        return fromPlayer == that.fromPlayer && toPlayer == that.toPlayer && cardToShow == that.cardToShow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPlayer, toPlayer, cardToShow);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "ShowHintCard{" +
                "fromPlayer=" + (fromPlayer != -1? fromPlayer : "from-player-not-found") +
                "toPlayer=" + (toPlayer != -1? toPlayer : "to-player-not-found") +
                "cardToShow=" + (cardToShow != -1? cardToShow : "card-to-show-not-found");
    }
}

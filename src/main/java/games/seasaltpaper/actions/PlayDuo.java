package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.Arrays;

public class PlayDuo extends AbstractAction {

    int playerId;
    int[] cardsIdx;

    public PlayDuo(int playerId, int[] cardsIdx) {
        this.playerId = playerId;
        this.cardsIdx = cardsIdx;
    }
    // TODO PLACEHODLER, REMOVE THIS LATER
    public PlayDuo(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SeaSaltPaperGameState sspg = (SeaSaltPaperGameState) gs;
        // make the duo cards visible
        PartialObservableDeck<SeaSaltPaperCard> playerHand = sspg.getPlayerHands().get(playerId);
        boolean[] visibility = new boolean[sspg.getNPlayers()];
        Arrays.fill(visibility, true);
        playerHand.setVisibilityOfComponent(cardsIdx[0], visibility);
        playerHand.setVisibilityOfComponent(cardsIdx[1], visibility);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "PlaceHolder for Duo Action";
    }
}

package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;

import java.util.Arrays;
import java.util.Objects;

public class LastChance extends AbstractAction {

    int playerId;


    public LastChance(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        sspgs.setLastChance(playerId);
//        // Reveal playerId hand
//        sspgs.getPlayerHands().get(playerId).setVisibility(CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LastChance that = (LastChance) o;
        return playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + playerId + " declares \"LAST CHANCE!\"";
    }
}

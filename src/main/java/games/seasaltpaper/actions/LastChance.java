package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;

import java.util.Arrays;

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
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + playerId + " declares \"LAST CHANCE!\"";
    }
}

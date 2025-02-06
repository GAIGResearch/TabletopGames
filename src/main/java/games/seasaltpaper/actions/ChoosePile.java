package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.HandManager;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.List;
import java.util.Objects;

public class ChoosePile extends AbstractAction {

    public final int playerId;

    public final int pileId;

    ChoosePile(int pileId, int playerId) {
        this.pileId = pileId;
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // make the entire pile visible to the playerId
        Deck<SeaSaltPaperCard> chosenPile = (Deck<SeaSaltPaperCard>) gs.getComponentById(pileId);
//        HandManager.setDeckVisibility(chosenPile, List.of(playerId), true);
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
        ChoosePile that = (ChoosePile) o;
        return playerId == that.playerId && pileId == that.pileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, pileId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "CHOOSE " + gameState.getComponentById(pileId).getComponentName();
    }

}

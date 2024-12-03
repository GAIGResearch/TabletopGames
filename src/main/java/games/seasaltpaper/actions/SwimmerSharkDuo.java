package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.interfaces.IExtendedSequence;
import games.seasaltpaper.SeaSaltPaperGameState;

import java.util.ArrayList;
import java.util.List;

public class SwimmerSharkDuo extends PlayDuo implements IExtendedSequence {

    boolean executed = false;

    public SwimmerSharkDuo(int playerId, int[] cardsIdx) {
        super(playerId, cardsIdx);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) state;
        int playerHandId = sspgs.getPlayerHands().get(playerId).getComponentID();
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int i = 0; i < sspgs.getNPlayers(); i++)
        {
            if (i == playerId)
            {
                continue;
            }
            int targetHandId = sspgs.getPlayerHands().get(i).getComponentID();
            actions.add(new DrawCard(targetHandId, playerHandId));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public SwimmerSharkDuo copy() { return this; }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Sailor-Shark Duo Actions: Pick a player then get a random card from their hand";
    }
}

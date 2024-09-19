package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.toads.ToadConstants;
import games.toads.ToadGameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScoutCards  implements IExtendedSequence {

    protected int player;
    protected boolean complete = false;
    protected int CARDS_TO_SHOW = 3;

    public ScoutCards(int player) {
        this.player = player;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // we have to show three cards
        ToadGameState toadState = (ToadGameState) state;
        List<ToadConstants.ToadCardType> inHand = toadState.getPlayerHand(1 - player).stream().map(a -> a.type).toList();
        if (inHand.size() <= CARDS_TO_SHOW) {
            // no decision to make
            return Collections.singletonList(new ShowCards());
        }
        // we have to choose three cards to show
        if (inHand.size() > CARDS_TO_SHOW + 1)
            throw new AssertionError("We expect 4 cards in hand...if more then rules elsewhere have changed ans we need to generalise here");
        return inHand.stream().map(ShowCards::new).collect(Collectors.toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return 1 - player;  // the other player has to decide what cards to show
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }


    @Override
    public ScoutCards copy() {
        ScoutCards retValue = new ScoutCards(player);
        retValue.complete = complete;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ScoutCards && ((ScoutCards) obj).player == player && ((ScoutCards) obj).complete == complete;
    }

    @Override
    public int hashCode() {
        return 3402 + 31 * player + (complete ? 1 : 0);
    }

}

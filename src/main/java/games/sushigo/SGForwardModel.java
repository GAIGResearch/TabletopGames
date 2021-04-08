package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import games.sushigo.actions.DebugAction;

import java.util.ArrayList;
import java.util.List;

public class SGForwardModel extends AbstractForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState sggs = (SGGameState) firstState;

        sggs.playerScore = new int[firstState.getNPlayers()];
        sggs.testDecks = new ArrayList<>();
        for (int i = 0; i < sggs.getNPlayers(); i++){
            sggs.testDecks.add(new Deck<>("Player" + i + " deck", i));
        }
        //sggs.testDecks.get(0).add(new Card());
        sggs.getTurnOrder().setStartingPlayer(0);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DebugAction());
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new SGForwardModel();
    }
}

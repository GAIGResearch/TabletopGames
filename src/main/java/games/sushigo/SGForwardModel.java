package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.actions.DebugAction;
import games.sushigo.cards.SGCard;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class SGForwardModel extends AbstractForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState SGGS = (SGGameState) firstState;
        SGGS.playerScore = new int[firstState.getNPlayers()];
        SGGS.playerHands = new ArrayList<>();
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            SGGS.playerHands.add(new Deck<>("Player" + i + " hand", i));
            SGGS.playerHands.get(i).add(new SGCard(SGCard.SGCardType.EggNigiri, 0));
            SGGS.playerHands.get(i).add(new SGCard(SGCard.SGCardType.Maki_1, 0));
            SGGS.playerHands.get(i).add(new SGCard(SGCard.SGCardType.Wasabi, 0));

        }

        SGGS.getTurnOrder().setStartingPlayer(0);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        SGGameState SGGS = (SGGameState)currentState;
        int turn = SGGS.getTurnOrder().getTurnCounter();
        if(turn % 4 == 0) System.out.println("Show cards!");
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

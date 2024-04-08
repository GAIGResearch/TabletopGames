package games.toads;

import core.*;
import core.actions.AbstractAction;
import core.components.Deck;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
import java.util.List;


public class ToadForwardModel extends StandardForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        ToadGameState state = (ToadGameState) firstState;
        ToadParameters params = (ToadParameters) state.getGameParameters();

        state.battlesWon = new int[state.getNPlayers()];
        state.fieldCards = new ToadCard[state.getNPlayers()];
        state.hiddenFlankCards = new ToadCard[state.getNPlayers()];
        state.playerDiscards = new ArrayList<>();
        state.playerHands = new ArrayList<>();
        state.playerDecks = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerDecks.add(new Deck<>("Player " + i + " Deck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerHands.add(new Deck<>("Player " + i + " Hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerDiscards.add(new Deck<>("Player " + i + " Discard", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            List<ToadCard> cards = params.getCardDeck();
            state.playerDecks.get(i).add(cards);
            state.playerDecks.get(i).shuffle(state.getRnd());
            for (int j = 0; j < params.handSize; j++) {
                state.playerHands.get(i).add(state.playerDecks.get(i).draw());
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.
        return actions;
    }
}

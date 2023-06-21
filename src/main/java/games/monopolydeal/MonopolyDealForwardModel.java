package games.monopolydeal;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import games.monopolydeal.actions.MonopolyDealAction;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class MonopolyDealForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        // initialization of variables and game setup
        MonopolyDealGameState state = (MonopolyDealGameState) firstState;
        MonopolyDealParameters params = state.params;

        // Add cards to Deck
        state.drawPile.add(MonopolyDealCard.create(CardType.Money10));
        for (CardType cT:state.params.cardsIncludedInGame.keySet()) {
            for(int i =0;i<state.params.cardsIncludedInGame.get(cT);i++){
                state.drawPile.add(MonopolyDealCard.create(cT));
            }
        }
        //Shuffle Deck
        state.drawPile.shuffle(state.rnd);
        //Deal 5 cards to each player
        for(int i=0;i< state.getNPlayers();i++) {
            for (int j = 0; j < state.params.INITIAL_DEAL; j++)
                state.playerHands[i].add(state.drawPile.draw());
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.
        actions.add(new MonopolyDealAction());
        return actions;
    }
}

package games.monopolydeal;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.DominionCard;
import games.monopolydeal.actions.*;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

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
        MonopolyDealGameState state = (MonopolyDealGameState) gameState;
        int playerID = state.getCurrentPlayer();

        switch (state.getGamePhase().toString()){
            case "Play":
                if (state.actionsLeft > 0){
                    List<AbstractAction> availableActions = new ArrayList<>();
                    availableActions.add(new PlayActionCard(playerID));
                    availableActions.add(new AddToBoard(playerID));
                    if(state.canModifyBoard(playerID)){
                        availableActions.add(new ModifyBoard(playerID));
                    }
                    availableActions.add(new EndPhase());
                }
                return Collections.singletonList(new EndPhase());
            case "Discard":
                if(state.playerHands[playerID].stream().count()>state.params.HAND_SIZE){
                    List<AbstractAction> availableActions = new ArrayList<>();

                }
            default:
                throw new AssertionError("Unknown Game Phase " + state.getGamePhase());
        }
        //List<AbstractAction> actions = new ArrayList<>();
        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.

        //actions.add(new MonopolyDealAction());
        //return actions;
    }

    // Draw cards at start of turn
    @Override
    protected void _beforeAction(AbstractGameState currentState, AbstractAction actionChosen) {
        MonopolyDealGameState state = (MonopolyDealGameState) currentState;
        if(state.turnStart){
            int currentPlayer = state.getCurrentPlayer();
            if(state.playerHands[currentPlayer].stream().count() == 0){
                state.drawCard(currentPlayer,state.params.DRAWS_WHEN_EMPTY);
            }
            else{
                state.drawCard(currentPlayer,state.params.DRAWS_PER_TURN);
            }
            state.turnStart = false;
        }
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        MonopolyDealGameState state = (MonopolyDealGameState) currentState;
        int playerID = state.getCurrentPlayer();

        switch (state.getGamePhase().toString()) {
            case "Play":
                if ((state.actionsLeft < 1 || actionTaken instanceof EndPhase) && !state.isActionInProgress()) {
                    state.setGamePhase(MonopolyDealGameState.MonopolyDealGamePhase.Discard);
                }
            case "Discard":
                state.endTurn();
            default:
                throw new AssertionError("Unknown Game Phase " + state.getGamePhase());
        }
    }
}

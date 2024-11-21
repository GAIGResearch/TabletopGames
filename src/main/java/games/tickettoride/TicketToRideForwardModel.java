package games.tickettoride;

import core.AbstractGameData;
import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.*;
import core.properties.PropertyString;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.pandemic.PandemicConstants;
import games.tickettoride.actions.ClaimRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;
import static games.tickettoride.TicketToRideConstants.*;

import games.tickettoride.actions.DrawTrainCards;
import org.apache.hadoop.yarn.state.Graph;
import utilities.Hash;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */


public class TicketToRideForwardModel extends StandardForwardModel {

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

        TicketToRideGameState state = (TicketToRideGameState) firstState;
        state._reset();
        TicketToRideParameters tp = (TicketToRideParameters) state.getGameParameters();

        state.tempDeck = new Deck<>("Temp Deck", VISIBLE_TO_ALL);
        state.areas = new HashMap<>();
        state.scores = new int[state.getNPlayers()];
        AbstractGameData _data = new AbstractGameData();
        _data.load(tp.getDataPath());

        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = new Area(i, "Player Area");
            //Player Train Card hand setup
            Deck<Card> playerTrainCardHand = new Deck<>("Player Train Card Hand", VISIBLE_TO_ALL);
            playerTrainCardHand.setOwnerId(i);
            playerArea.putComponent(playerHandHash, playerTrainCardHand);

            System.out.println("Setup: made deck for player " + i + " has " + playerTrainCardHand);

            state.areas.put(i, playerArea);
        }

        Area gameArea = new Area(-1, "Game Area");
        state.areas.put(-1, gameArea);

        state.world = _data.findGraphBoard("cities");
        gameArea.putComponent(ticketToRideBoardHash, state.world);


        // setup train car card deck
        Deck<Card> trainCardDeck = new Deck<>("Train Card Deck", HIDDEN_TO_ALL);
        Deck<Card> trainCardTypes = _data.findDeck("TrainCars");
        for (Card c: trainCardTypes) { //Add x number of each train card type
            for (int i = 0; i < 25; i++) {
                trainCardDeck.add(c);
            }

        }


        trainCardDeck.shuffle(firstState.getRnd());
        gameArea.putComponent(TicketToRideConstants.trainCardDeckHash, trainCardDeck);

        state.addComponents();


        //draw initial cards
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = state.getArea(i);
            Deck<Card> playerTrainCardHandDeck = (Deck<Card>) playerArea.getComponent(playerHandHash);
            for (int j = 0; j < tp.nInitialTrainCards; j++) {
                new DrawCard(trainCardDeck.getComponentID(), playerTrainCardHandDeck.getComponentID()).execute(state);
            }

            System.out.println("Setup: player " + i + " has " + playerTrainCardHandDeck);
        }


        state.setFirstPlayer(0);



    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;

        List<AbstractAction> actions = new ArrayList<>();
        int playerId = tg.getCurrentPlayer();
        System.out.println(playerId + " in compute action");
        actions.add(new DrawTrainCards(playerId));

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress()) return;
        TicketToRideGameState gs = (TicketToRideGameState) currentState;
        TicketToRideParameters params = (TicketToRideParameters) gs.getGameParameters();

        endPlayerTurn(gs);

    }

    void checkRoutesAvailable(AbstractGameState gameState) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;

        Area gameArea = tg.getArea(-1);

        GraphBoard world = (GraphBoard) gameArea.getComponent(ticketToRideBoardHash);
    }
}

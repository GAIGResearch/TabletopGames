package games.serveTheKing;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.loveletter.cards.LoveLetterCard;
import games.serveTheKing.actions.ThrashPlate;
import games.serveTheKing.components.KingCard;
import games.serveTheKing.components.PlateCard;

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
public class STKForwardModel extends StandardForwardModel {

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
        STKGameState stkgs = (STKGameState)firstState;

        stkgs.mainDeck = new Deck<PlateCard>("mainDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        stkgs.discardPile = new Deck<PlateCard>("discardPile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        stkgs.playersHands= new ArrayList<>(stkgs.getNPlayers());
        //TODO : implement the different kings.
        setupRound(stkgs,null,0);
    }

    private void setupRound(STKGameState stkgs, KingCard king, int roundNumber){
        // Reset player status
        for (int i = 0; i < stkgs.getNPlayers(); i++) {
            stkgs.setPlayerResult(CoreConstants.GameResult.GAME_ONGOING, i);
        }
        // Create Draw Pile
        stkgs.mainDeck.clear();
        for (int i =0 ; i<11;i++) {
            for (int j = 0; j < 4; j++) {
                PlateCard card = new PlateCard(i);
                stkgs.mainDeck.add(card);
            }
        }
        for (int j = 0; j < 3; j++) {
            PlateCard card = new PlateCard(-1);
            stkgs.mainDeck.add(card);
            PlateCard card2 = new PlateCard(15);
            stkgs.mainDeck.add(card2);
        }

        //Create Player Hands
        for (int i = 0; i < stkgs.getNPlayers(); i++) {
            boolean[] visible = new boolean[stkgs.getNPlayers()];
            // add random cards to the player's hand
            PartialObservableDeck<PlateCard> playerCards = new PartialObservableDeck<>("playerHand" + i, i, visible);
            for (int j = 0; j < 4; j++) {
                playerCards.add(stkgs.mainDeck.draw());
            }
            stkgs.playersHands.add(playerCards);
        }

        // Create discard pile
        stkgs.discardPile.clear();

        // Reset the player that called the Serve
        stkgs.playerCalledServe =-1;
    };

    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        if (gameState.isActionInProgress()) return;
        // each turn begins with the player drawing a card after which one card will be played
        STKGameState stkgs = (STKGameState) gameState;

        if(action instanceof ThrashPlate ){
            if (!checkEndOfRound(stkgs, action)) {
                // move turn to the next player who has not already lost the round
                int nextPlayer = gameState.getCurrentPlayer()+1 % stkgs.getNPlayers();
                endPlayerTurn(stkgs, nextPlayer);
            }
        }
        else{
            

        }
    }
    private boolean checkEndOfRound(STKGameState state, AbstractAction action){
        boolean result;
        if(state.playerCalledServe <0){
            result = false;
        }
        else {
            if((action instanceof ThrashPlate) && (state.getCurrentPlayer()+1 == state.playerCalledServe)){
                result = true;
            }
        }
        return
    }
    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.
        actions.add(new ThrashPlate());
        return actions;
    }
}

package games.explodingkittens;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.ForwardModel;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.explodingkittens.actions.IsNope;
import games.explodingkittens.actions.IsNopeable;
import games.explodingkittens.cards.ExplodingKittenCard;
import utilities.CoreConstants;

import java.util.*;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Nope;
import static utilities.CoreConstants.VERBOSE;

public class ExplodingKittensForwardModel extends ForwardModel {

    /**
     * Performs initial game setup according to game rules.
     * @param firstState - the state to be modified to the initial game state.
     */
    public void setup(AbstractGameState firstState) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState)firstState;
        ExplodingKittenParameters ekp = (ExplodingKittenParameters)firstState.getGameParameters();

        // Set up draw pile deck
        PartialObservableDeck<ExplodingKittenCard> drawPile = new PartialObservableDeck<>("Draw Pile", firstState.getNPlayers());
        ekgs.setDrawPile(drawPile);

        // Add all cards but defuse and exploding kittens
        for (HashMap.Entry<ExplodingKittenCard.CardType, Integer> entry : ekp.cardCounts.entrySet()) {
            if (entry.getKey() == ExplodingKittenCard.CardType.DEFUSE || entry.getKey() == ExplodingKittenCard.CardType.EXPLODING_KITTEN)
                continue;
            for (int i = 0; i < entry.getValue(); i++) {
                ExplodingKittenCard card = new ExplodingKittenCard(entry.getKey());
                drawPile.add(card);
            }
        }
        ekgs.getDrawPile().shuffle(rnd);

        // Set up player hands
        List<PartialObservableDeck<ExplodingKittenCard>> playerHandCards = new ArrayList<>(firstState.getNPlayers());
        for (int i = 0; i < firstState.getNPlayers(); i++) {
            // Set up visibility
            boolean[] visibility = new boolean[firstState.getNPlayers()];
            Arrays.fill(visibility, !CoreConstants.PARTIAL_OBSERVABLE);
            visibility[i] = true;

            PartialObservableDeck<ExplodingKittenCard> playerCards = new PartialObservableDeck<>("Player Cards", visibility);
            playerHandCards.add(playerCards);

            // Add defuse card
            ExplodingKittenCard defuse =  new ExplodingKittenCard(ExplodingKittenCard.CardType.DEFUSE);
            playerCards.add(defuse);

            // Add N random cards from the deck
            for (int j = 0; j < ekp.nCardsPerPlayer; j++) {
                playerCards.add(ekgs.getDrawPile().draw());
            }
        }
        ekgs.setPlayerHandCards(playerHandCards);
        ekgs.setDiscardPile(new Deck<>("Discard Pile"));

        // Add remaining defuse cards and exploding kitten cards to the deck and shuffle again
        for (int i = ekgs.getNPlayers(); i < ekp.nDifuseCards; i++){
            ExplodingKittenCard defuse = new ExplodingKittenCard(ExplodingKittenCard.CardType.DEFUSE);
            drawPile.add(defuse);
        }
        for (int i = 0; i < ekgs.getNPlayers() + ekp.cardCounts.get(ExplodingKittenCard.CardType.EXPLODING_KITTEN); i++){
            ExplodingKittenCard explodingKitten = new ExplodingKittenCard(ExplodingKittenCard.CardType.EXPLODING_KITTEN);
            drawPile.add(explodingKitten);
        }
        drawPile.shuffle(rnd);

        ekgs.setActionStack(new Stack<>());
    }

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param gameState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    @Override
    public void next(AbstractGameState gameState, AbstractAction action) {
        if (VERBOSE) {
            System.out.println(action.toString());
        }

        ExplodingKittenTurnOrder ekTurnOrder = (ExplodingKittenTurnOrder) gameState.getTurnOrder();
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
        Stack<AbstractAction> actionStack = ekgs.getActionStack();

        if (actionStack.size() == 0){
            if (action instanceof IsNopeable) {
                actionStack.add(action);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
            } else {
                action.execute(gameState);
            }
        } else {
            // action is either nope or pass
            if (((IsNope) action).isNope()) {
                actionStack.add(action);
                action.execute(gameState);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
            } else {
                ekTurnOrder.endPlayerTurnStep(gameState);

                if (ekTurnOrder.reactionsFinished()){
                    // apply stack
                    if (actionStack.size()%2 == 0){
                        while (actionStack.size() > 1) {
                            actionStack.pop();
                        }
                        //Action was successfully noped
                        ((IsNopeable) actionStack.pop()).nopedExecute(gameState, ekTurnOrder);
                        if (VERBOSE) {
                            System.out.println("Action was successfully noped");
                        }
                    } else {
                        if (actionStack.size() > 2 && VERBOSE) {
                            System.out.println("All nopes were noped");
                        }

                        while (actionStack.size() > 1) {
                            actionStack.pop();
                        }

                        //Action can be played
                        AbstractAction stackedAction = actionStack.get(0);
                        stackedAction.execute(gameState);
                    }
                    actionStack.clear();
                    if (ekgs.getGamePhase() == Nope) {
                        ekgs.setMainGamePhase();
                    }
                }
            }
        }
    }
}

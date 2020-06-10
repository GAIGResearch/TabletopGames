package games.explodingkittens;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.AbstractForwardModel;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.explodingkittens.actions.*;
import games.explodingkittens.cards.ExplodingKittenCard;
import core.CoreConstants;
import utilities.Utils;

import java.util.*;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Nope;
import static core.CoreConstants.VERBOSE;
import static utilities.Utils.generatePermutations;

public class ExplodingKittensForwardModel extends AbstractForwardModel {

    /**
     * Performs initial game setup according to game rules.
     * @param firstState - the state to be modified to the initial game state.
     */
    protected void _setup(AbstractGameState firstState) {
        Random rnd = new Random(firstState.getGameParameters().getGameSeed());

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
            defuse.setOwnerId(i);
            playerCards.add(defuse);

            // Add N random cards from the deck
            for (int j = 0; j < ekp.nCardsPerPlayer; j++) {
                ExplodingKittenCard c = ekgs.getDrawPile().draw();
                c.setOwnerId(i);
                playerCards.add(c);
            }
        }
        ekgs.setPlayerHandCards(playerHandCards);
        ekgs.setDiscardPile(new Deck<>("Discard Pile"));

        // Add remaining defuse cards and exploding kitten cards to the deck and shuffle again
        for (int i = ekgs.getNPlayers(); i < ekp.nDefuseCards; i++){
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
    protected void _next(AbstractGameState gameState, AbstractAction action) {
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
            if (action instanceof NopeAction) {
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
                        ((IsNopeable) actionStack.pop()).nopedExecute(gameState, ekTurnOrder); // TODO: this just executes super action (again), why?
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


    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     * Exploding kittens updates the status of players still alive as winners.
     */
    @Override
    protected void endGame(AbstractGameState gameState) {
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        for (int i = 0; i < gameState.getNPlayers(); i++){
            if (gameState.getPlayerResults()[i] == Utils.GameResult.GAME_ONGOING)
                gameState.setPlayerResult(Utils.GameResult.WIN, i);
        }

        // Print end game result
        if (VERBOSE) {
            System.out.println(Arrays.toString(gameState.getPlayerResults()));
            for (int j = 0; j < gameState.getNPlayers(); j++) {
                System.out.println("Player " + j + ": " + gameState.getPlayerResults()[j]);
            }
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
        ArrayList<AbstractAction> actions;
        // todo the actions per player do not change a lot in between two turns
        // i would strongly recommend to update an existing list instead of generating a new list everytime we query this function
        int player = ekgs.getTurnOrder().getCurrentPlayer(ekgs);
        if (AbstractGameState.DefaultGamePhase.Main.equals(ekgs.getGamePhase())) {
            actions = playerActions(ekgs, player);
        } else if (ExplodingKittensGameState.ExplodingKittensGamePhase.Defuse.equals(ekgs.getGamePhase())) {
            actions = placeKittenActions(ekgs, player);
        } else if (ExplodingKittensGameState.ExplodingKittensGamePhase.Nope.equals(ekgs.getGamePhase())) {
            actions = nopeActions(ekgs, player);
        } else if (ExplodingKittensGameState.ExplodingKittensGamePhase.Favor.equals(ekgs.getGamePhase())) {
            actions = favorActions(ekgs, player);
        } else if (ExplodingKittensGameState.ExplodingKittensGamePhase.SeeTheFuture.equals(ekgs.getGamePhase())) {
            actions = seeTheFutureActions(ekgs, player);
        } else {
            actions = new ArrayList<>();
        }

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new ExplodingKittensForwardModel();
    }

    private ArrayList<AbstractAction> playerActions(ExplodingKittensGameState ekgs, int playerID){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = ekgs.playerHandCards.get(playerID);

        // todo: only add unique actions
        for (int c = 0; c < playerDeck.getSize(); c++) {
            ExplodingKittenCard card = playerDeck.getComponents().get(c);
            switch (card.cardType) {
                case DEFUSE:
                case MELONCAT:
                case RAINBOWCAT:
                case FURRYCAT:
                case BEARDCAT:
                case TACOCAT:
                case NOPE:
                case EXPLODING_KITTEN:
                    break;
                case SKIP:
                    actions.add(new SkipAction(playerDeck.getComponentID(), ekgs.discardPile.getComponentID(), c));
                    break;
                case FAVOR:
                    for (int player = 0; player < ekgs.getNPlayers(); player++) {
                        if (player == playerID)
                            continue;
                        if (ekgs.playerHandCards.get(player).getSize() > 0)
                            actions.add(new FavorAction(playerDeck.getComponentID(), ekgs.discardPile.getComponentID(), c, player));
                    }
                    break;
                case ATTACK:
                    for (int targetPlayer = 0; targetPlayer < ekgs.getNPlayers(); targetPlayer++) {

                        if (targetPlayer == playerID || ekgs.getPlayerResults()[targetPlayer] != Utils.GameResult.GAME_ONGOING)
                            continue;

                        actions.add(new AttackAction(playerDeck.getComponentID(), ekgs.discardPile.getComponentID(), c, targetPlayer));
                    }
                    break;
                case SHUFFLE:
                    actions.add(new ShuffleAction(playerDeck.getComponentID(), ekgs.discardPile.getComponentID(), c));
                    break;
                case SEETHEFUTURE:
                    actions.add(new SeeTheFuture());
                    break;
                default:
                    System.out.println("No actions known for cardtype: " + card.cardType.toString());
            }
        }
        /* todo add special combos
        // can take any card from anyone
        for (int i = 0; i < nPlayers; i++){
            if (i != activePlayer){
                Deck otherDeck = (Deck)this.areas.get(activePlayer).getComponent(playerHandHash);
                for (Card card: otherDeck.getCards()){
                    core.actions.add(new TakeCard(card, i));
                }
            }
        }*/

        // add end turn by drawing a card
        actions.add(new DrawExplodingKittenCard(ekgs.drawPile.getComponentID(), playerDeck.getComponentID()));
        return actions;
    }

    private ArrayList<AbstractAction> placeKittenActions(ExplodingKittensGameState ekgs, int playerID){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = ekgs.playerHandCards.get(playerID);
        int explodingKittenCard = -1;
        for (int i = 0; i < playerDeck.getSize(); i++) {
            if (playerDeck.getComponents().get(i).cardType == ExplodingKittenCard.CardType.EXPLODING_KITTEN) {
                explodingKittenCard = i;
                break;
            }
        }
        if (explodingKittenCard != -1) {
            for (int i = 0; i <= ekgs.drawPile.getSize(); i++) {
                actions.add(new PlaceExplodingKitten(playerDeck.getComponentID(), ekgs.drawPile.getComponentID(), explodingKittenCard, i));
            }
        }
        return actions;
    }

    private ArrayList<AbstractAction> nopeActions(ExplodingKittensGameState ekgs, int playerID){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = ekgs.playerHandCards.get(playerID);
        for (int c = 0; c < playerDeck.getSize(); c++) {
            if (playerDeck.getComponents().get(c).cardType == ExplodingKittenCard.CardType.NOPE) {
                actions.add(new NopeAction(playerDeck.getComponentID(), ekgs.discardPile.getComponentID(), c));
                break;
            }
        }
        actions.add(new PassAction());
        return actions;
    }

    private ArrayList<AbstractAction> favorActions(ExplodingKittensGameState ekgs, int playerID){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = ekgs.playerHandCards.get(playerID);
        Deck<ExplodingKittenCard> receiverDeck = ekgs.playerHandCards.get(ekgs.playerGettingAFavor);
        for (int card = 0; card < playerDeck.getSize(); card++) {
            actions.add(new GiveCard(playerDeck.getComponentID(), receiverDeck.getComponentID(), card));
        }
        return actions;
    }

    private ArrayList<AbstractAction> seeTheFutureActions(ExplodingKittensGameState ekgs, int playerID){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = ekgs.playerHandCards.get(playerID);
        List<ExplodingKittenCard> cards = ekgs.drawPile.getComponents();
        int numberOfCards = ekgs.drawPile.getSize();
        int n = Math.min(((ExplodingKittenParameters)ekgs.getGameParameters()).nSeeFutureCards, numberOfCards);
        ArrayList<int[]> permutations = new ArrayList<>();
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        generatePermutations(n, order, permutations);
        for (int c = 0; c < playerDeck.getSize(); c++) {
            if (playerDeck.getComponents().get(c).cardType == ExplodingKittenCard.CardType.SEETHEFUTURE) {
                for (int[] perm : permutations) {
                    actions.add(new ChooseSeeTheFutureOrder(playerDeck.getComponentID(),
                            ekgs.discardPile.getComponentID(), c, ekgs.drawPile.getComponentID(), perm));
                }
            }
        }
        return actions;
    }
}

package games.explodingkittens;

import core.gamephase.GamePhase;
import core.gamephase.DefaultGamePhase;
import core.ForwardModel;
import core.actions.IAction;
import core.components.Deck;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.explodingkittens.cards.ExplodingKittenCard;
import games.explodingkittens.actions.*;
import core.observations.IObservation;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ExplodingKittensGameState extends AbstractGameState {

    // Exploding kittens adds 4 phases on top of default ones.
    public enum ExplodingKittensGamePhase implements GamePhase {
        Nope,
        Defuse,
        Favor,
        SeeTheFuture
    }

    // Cards in each player's hand, index corresponds to player ID
    private List<PartialObservableDeck<ExplodingKittenCard>> playerHandCards;
    // Cards in the draw pile
    private PartialObservableDeck<ExplodingKittenCard> drawPile;
    // Cards in the discard pile
    private Deck<ExplodingKittenCard> discardPile;
    // Player ID of the player currently getting a favor
    private int playerGettingAFavor = -1;
    // Current stack of actions
    private Stack<IAction> actionStack;

    public ExplodingKittensGameState(ExplodingKittenParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, new ExplodingKittenTurnOrder(nPlayers));
    }

    /**
     * Retrieves an observation specific to the given player from this game state object. Components which are not
     * observed by the player are removed, the rest are copied.
     * @param player - player observing this game state.
     * @return - IObservation, the observation for this player.
     */
    @Override
    public IObservation getObservation(int player) {
        return new ExplodingKittenObservation(playerHandCards, drawPile, discardPile, player);
    }

    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     * Exploding kittens updates the status of players still alive as winners.
     */
    @Override
    public void endGame() {
        this.gameStatus = Utils.GameResult.GAME_END;
        for (int i = 0; i < getNPlayers(); i++){
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING)
                playerResults[i] = Utils.GameResult.GAME_WIN;
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    @Override
    public List<IAction> computeAvailableActions() {

        ArrayList<IAction> actions;
        // todo the actions per player do not change a lot in between two turns
        // i would strongly recommend to update an existing list instead of generating a new list everytime we query this function
        int player = getTurnOrder().getCurrentPlayer(this);
        if (DefaultGamePhase.Main.equals(gamePhase)) {
            actions = playerActions(player);
        } else if (ExplodingKittensGamePhase.Defuse.equals(gamePhase)) {
            actions = defuseActions(player);
        } else if (ExplodingKittensGamePhase.Nope.equals(gamePhase)) {
            actions = nopeActions(player);
        } else if (ExplodingKittensGamePhase.Favor.equals(gamePhase)) {
            actions = favorActions(player);
        } else if (ExplodingKittensGamePhase.SeeTheFuture.equals(gamePhase)) {
            actions = seeTheFutureActions(player);
        } else {
            actions = new ArrayList<>();
        }

        return actions;
    }

    private ArrayList<IAction> playerActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);

        // todo: only add unique actions
        for (ExplodingKittenCard card : playerDeck.getComponents()) {
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
                    actions.add(new SkipAction<>(card, playerDeck, discardPile));
                    break;
                case FAVOR:
                    for (int player = 0; player < getNPlayers(); player++) {
                        if (player == playerID)
                            continue;
                        if (playerHandCards.get(player).getSize() > 0)
                            actions.add(new FavorAction<>(card, playerDeck, discardPile, player, playerID));
                    }
                    break;
                case ATTACK:
                    for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {

                        if (targetPlayer == playerID || playerResults[targetPlayer] != Utils.GameResult.GAME_ONGOING)
                            continue;

                        actions.add(new AttackAction<>(card, playerDeck, discardPile, targetPlayer));
                    }
                    break;
                case SHUFFLE:
                    actions.add(new ShuffleAction<>(card, playerDeck, discardPile, drawPile));
                    break;
                case SEETHEFUTURE:
                    actions.add(new SeeTheFutureAction<>(card, playerDeck, discardPile, playerID, drawPile));
                    break;
                default:
                    System.out.println("No core.actions known for cardtype: " + card.cardType.toString());
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
        actions.add(new DrawExplodingKittenCard(playerID, drawPile, playerDeck));
        return actions;
    }

    private ArrayList<IAction> defuseActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);
        ExplodingKittenCard kitten = playerDeck.peek();
        for (int i = 0; i <= drawPile.getSize(); i++){
            actions.add(new PlaceExplodingKittenAction<>(kitten, playerDeck, drawPile, i));
        }
        return actions;
    }

    private ArrayList<IAction> nopeActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);
        for (ExplodingKittenCard card : playerDeck.getComponents()) {
            if (card.cardType == ExplodingKittenCard.CardType.NOPE) {
                actions.add(new NopeAction<>(card, playerDeck, discardPile, playerID));
            }
            break;
        }
        actions.add(new PassAction(playerID));
        return actions;
    }

    private ArrayList<IAction> favorActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);
        Deck<ExplodingKittenCard> receiverDeck = playerHandCards.get(playerGettingAFavor);
        for (ExplodingKittenCard card : playerDeck.getComponents()) {
            actions.add(new GiveCardAction(card, playerDeck, receiverDeck));
        }
        return actions;
    }

    private ArrayList<IAction> seeTheFutureActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        ArrayList<ExplodingKittenCard> cards = drawPile.getComponents();
        int numberOfCards = drawPile.getSize();
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 1 >= numberOfCards ? null : cards.get(1),
                2 >= numberOfCards ? null : cards.get(2), 3 >= numberOfCards? null : cards.get(3), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 1 >=numberOfCards ? null : cards.get(1),
                3 >= numberOfCards ? null : cards.get(3), 2 >= numberOfCards? null : cards.get(2), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 2 >=numberOfCards ? null : cards.get(2),
                1 >= numberOfCards ? null : cards.get(1), 3 >= numberOfCards? null : cards.get(3), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 2 >=numberOfCards ? null : cards.get(2),
                3 >= numberOfCards ? null : cards.get(3), 1 >= numberOfCards? null : cards.get(1), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 3 >=numberOfCards ? null : cards.get(3),
                1 >= numberOfCards ? null : cards.get(1), 2 >= numberOfCards? null : cards.get(2), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 3 >=numberOfCards ? null : cards.get(3),
                2 >= numberOfCards ? null : cards.get(2), 1 >= numberOfCards? null : cards.get(1), playerID));

        return actions;
    }

    /**
     * Marks a player as dead.
     * @param playerID - player who was killed in a kitten explosion.
     */
    public void killPlayer(int playerID){
        setPlayerResult(Utils.GameResult.GAME_LOSE, playerID);
        int nPlayersActive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        if (nPlayersActive == 1) {
            this.gameStatus = Utils.GameResult.GAME_END;
        }
    }

    // Getters, setters
    public int getPlayerGettingAFavor() {
        return playerGettingAFavor;
    }
    public PartialObservableDeck<ExplodingKittenCard> getDrawPile() {
        return drawPile;
    }
    public void setPlayerGettingAFavor(int playerGettingAFavor) {
        this.playerGettingAFavor = playerGettingAFavor;
    }
    public Deck<ExplodingKittenCard> getDiscardPile() {
        return discardPile;
    }
    public Stack<IAction> getActionStack() {
        return actionStack;
    }

    // Protected, only accessible in this package and subclasses
    protected void setDiscardPile(Deck<ExplodingKittenCard> discardPile) {
        this.discardPile = discardPile;
    }
    protected void setDrawPile(PartialObservableDeck<ExplodingKittenCard> drawPile) {
        this.drawPile = drawPile;
    }
    protected void setPlayerHandCards(List<PartialObservableDeck<ExplodingKittenCard>> playerHandCards) {
        this.playerHandCards = playerHandCards;
    }
    protected void setActionStack(Stack<IAction> actionStack) {
        this.actionStack = actionStack;
    }

    // Printing functions for the game state and decks.

    public void print(ExplodingKittenTurnOrder turnOrder) {
        System.out.println("Exploding Kittens Game-State");
        System.out.println("============================");

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++){
            if (currentPlayer == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ":");
            printDeck(playerHandCards.get(i));
        }

        System.out.print("DrawPile" + ":");
        printDeck(drawPile);

        System.out.print("DiscardPile" + ":");
        printDeck(discardPile);

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println("Missing Draws: " + turnOrder.requiredDraws);
    }

    public void printDeck(Deck<ExplodingKittenCard> deck){
        StringBuilder sb = new StringBuilder();
        for (ExplodingKittenCard card : deck.getComponents()){
            sb.append(card.cardType.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.println(sb.toString());
        //System.out.println();
    }

    private void printActionStack(){
        System.out.print("Action Stack:");
        for (IAction a : actionStack) {
            System.out.print(a.toString() + ",");
        }
    }
}

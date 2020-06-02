package games.explodingkittens;

import core.components.Component;
import core.interfaces.IGamePhase;
import core.actions.AbstractAction;
import core.components.Deck;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import core.observations.VectorObservation;
import games.explodingkittens.cards.ExplodingKittenCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class ExplodingKittensGameState extends AbstractGameState implements IPrintable {

    // Exploding kittens adds 4 phases on top of default ones.
    public enum ExplodingKittensGamePhase implements IGamePhase {
        Nope,
        Defuse,
        Favor,
        SeeTheFuture
    }

    // Cards in each player's hand, index corresponds to player ID
    List<PartialObservableDeck<ExplodingKittenCard>> playerHandCards;
    // Cards in the draw pile
    PartialObservableDeck<ExplodingKittenCard> drawPile;
    // Cards in the discard pile
    Deck<ExplodingKittenCard> discardPile;
    // Player ID of the player currently getting a favor
    int playerGettingAFavor = -1;
    // Current stack of actions
    Stack<AbstractAction> actionStack;

    public ExplodingKittensGameState(ExplodingKittenParameters gameParameters, int nPlayers) {
        super(gameParameters, new ExplodingKittenTurnOrder(nPlayers));
    }

    @Override
    public List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(drawPile);
            add(discardPile);
            addAll(playerHandCards);
        }};
    }

    @Override
    protected AbstractGameState copy(int playerId) {
        ExplodingKittensGameState ekgs = new ExplodingKittensGameState((ExplodingKittenParameters) gameParameters.copy(), getNPlayers());
        ekgs.drawPile = drawPile.copy();
        ekgs.discardPile = discardPile.copy();
        ekgs.playerGettingAFavor = playerGettingAFavor;
        ekgs.actionStack = new Stack<>();
        for (AbstractAction a: actionStack) {
            ekgs.actionStack.add(a.copy());
        }
        ekgs.playerHandCards = new ArrayList<>();
        for (PartialObservableDeck<ExplodingKittenCard> d: playerHandCards) {
            ekgs.playerHandCards.add(d.copy());
        }
        return ekgs;
    }

    @Override
    public VectorObservation getVectorObservation() {
        // TODO
        return null;
    }

    @Override
    public double[] getDistanceFeatures(int playerId) {
        // TODO
        return new double[0];
    }

    @Override
    public HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId) {
        // TODO
        return null;
    }

    @Override
    public double getScore(int playerId) {
        // TODO heuristic
        return 0;
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
    public Stack<AbstractAction> getActionStack() {
        return actionStack;
    }
    public List<PartialObservableDeck<ExplodingKittenCard>> getPlayerHandCards() {
        return playerHandCards;
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
    protected void setActionStack(Stack<AbstractAction> actionStack) {
        this.actionStack = actionStack;
    }

    // Printing functions for the game state and decks.

    public void printToConsole() {
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
        System.out.println("Missing Draws: " + ((ExplodingKittenTurnOrder)turnOrder).requiredDraws);
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
        for (AbstractAction a : actionStack) {
            System.out.print(a.toString() + ",");
        }
    }
}

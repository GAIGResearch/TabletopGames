package games.explodingkittens;

import core.AbstractParameters;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.actions.AbstractAction;
import core.components.Deck;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.explodingkittens.cards.ExplodingKittensCard;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.PARTIAL_OBSERVABLE;

public class ExplodingKittensGameState extends AbstractGameState implements IPrintable {

    // Exploding kittens adds 4 phases on top of default ones.
    public enum ExplodingKittensGamePhase implements IGamePhase {
        Nope,
        Defuse,
        Favor,
        SeeTheFuture
    }

    // Cards in each player's hand, index corresponds to player ID
    List<Deck<ExplodingKittensCard>> playerHandCards;
    // Cards in the draw pile
    PartialObservableDeck<ExplodingKittensCard> drawPile;
    // Cards in the discard pile
    Deck<ExplodingKittensCard> discardPile;
    // Player ID of the player currently getting a favor
    int playerGettingAFavor;
    // Current stack of actions
    Stack<AbstractAction> actionStack;

    public ExplodingKittensGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new ExplodingKittensTurnOrder(nPlayers));
        playerGettingAFavor = -1;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(drawPile);
            add(discardPile);
            addAll(playerHandCards);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        ExplodingKittensGameState ekgs = new ExplodingKittensGameState(gameParameters.copy(), getNPlayers());
        ekgs.discardPile = discardPile.copy();
        ekgs.playerGettingAFavor = playerGettingAFavor;
        ekgs.actionStack = new Stack<>();
        for (AbstractAction a: actionStack) {
            ekgs.actionStack.add(a.copy());
        }
        ekgs.playerHandCards = new ArrayList<>();
        for (Deck<ExplodingKittensCard> d: playerHandCards) {
            ekgs.playerHandCards.add(d.copy());
        }
        ekgs.drawPile = drawPile.copy();
        if (PARTIAL_OBSERVABLE && playerId != -1) {
            // Other player hands + draw deck are hidden, combine in draw pile and shuffle
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    ekgs.drawPile.add(ekgs.playerHandCards.get(i));
                    ekgs.playerHandCards.get(i).clear();
                }
            }
            Random r = new Random(ekgs.gameParameters.getRandomSeed());

            // Shuffles only hidden cards, if player knows what's on top those will stay in place
            ekgs.drawPile.shuffleVisible(r, playerId, false);
            Deck<ExplodingKittensCard> explosive = new Deck<>("tmp");
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerHandCards.get(i).getSize(); j++) {
                        boolean added = false;
                        while (!added) {
                            ExplodingKittensCard card = ekgs.drawPile.draw();
                            if (card.cardType != ExplodingKittensCard.CardType.EXPLODING_KITTEN) {
                                ekgs.playerHandCards.get(i).add(card);
                                added = true;
                            } else {
                                explosive.add(card);
                            }
                        }
                    }
                }
            }
            ekgs.drawPile.add(explosive);
        }
        return ekgs;
    }

    @Override
    protected double _getScore(int playerId) {
        return new ExplodingKittensHeuristic().evaluateState(this, playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    add(playerHandCards.get(i).getComponentID());
                }
            }
            add(drawPile.getComponentID());
        }};
    }

    @Override
    protected void _reset() {
        playerHandCards = new ArrayList<>();
        drawPile = null;
        discardPile = null;
        playerGettingAFavor = -1;
        actionStack = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExplodingKittensGameState)) return false;
        if (!super.equals(o)) return false;
        ExplodingKittensGameState gameState = (ExplodingKittensGameState) o;
        return playerGettingAFavor == gameState.playerGettingAFavor &&
                Objects.equals(playerHandCards, gameState.playerHandCards) &&
                Objects.equals(drawPile, gameState.drawPile) &&
                Objects.equals(discardPile, gameState.discardPile) &&
                Objects.equals(actionStack, gameState.actionStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHandCards, drawPile, discardPile, playerGettingAFavor, actionStack);
    }

    /**
     * Marks a player as dead.
     * @param playerID - player who was killed in a kitten explosion.
     */
    public void killPlayer(int playerID){
        setPlayerResult(Utils.GameResult.LOSE, playerID);
        int nPlayersActive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        if (nPlayersActive == 1) {
            this.gameStatus = Utils.GameResult.GAME_END;
        }
        ((ExplodingKittensTurnOrder)getTurnOrder()).endPlayerTurnStep(this);

    }

    // Getters, setters
    public int getPlayerGettingAFavor() {
        return playerGettingAFavor;
    }
    public PartialObservableDeck<ExplodingKittensCard> getDrawPile() {
        return drawPile;
    }
    public void setPlayerGettingAFavor(int playerGettingAFavor) {
        this.playerGettingAFavor = playerGettingAFavor;
    }
    public Deck<ExplodingKittensCard> getDiscardPile() {
        return discardPile;
    }
    public Stack<AbstractAction> getActionStack() {
        return actionStack;
    }
    public List<Deck<ExplodingKittensCard>> getPlayerHandCards() {
        return playerHandCards;
    }

    // Protected, only accessible in this package and subclasses
    protected void setDiscardPile(Deck<ExplodingKittensCard> discardPile) {
        this.discardPile = discardPile;
    }
    protected void setDrawPile(PartialObservableDeck<ExplodingKittensCard> drawPile) {
        this.drawPile = drawPile;
    }
    protected void setPlayerHandCards(List<Deck<ExplodingKittensCard>> playerHandCards) {
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
        System.out.println("Missing Draws: " + ((ExplodingKittensTurnOrder)turnOrder).requiredDraws);
    }

    public void printDeck(Deck<ExplodingKittensCard> deck){
        StringBuilder sb = new StringBuilder();
        for (ExplodingKittensCard card : deck.getComponents()){
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

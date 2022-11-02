package games.explodingkittens;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.explodingkittens.cards.ExplodingKittensCard;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.VisibilityMode;

public class ExplodingKittensGameState extends AbstractGameState implements IPrintable {

    // Exploding kittens adds 4 phases on top of default ones.
    public enum ExplodingKittensGamePhase implements IGamePhase {
        Nope,
        Defuse,
        Favor,
        SeeTheFuture
    }

    // Cards in each player's hand, index corresponds to player ID
    List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards;
    // Cards in the draw pile
    PartialObservableDeck<ExplodingKittensCard> drawPile;
    // Cards in the discard pile
    Deck<ExplodingKittensCard> discardPile;
    // Player ID of the player currently getting a favor
    int playerGettingAFavor;
    // Current stack of actions
    Stack<AbstractAction> actionStack;
    int[] orderOfPlayerDeath;

    public ExplodingKittensGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new ExplodingKittensTurnOrder(nPlayers), GameType.ExplodingKittens);
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
        for (AbstractAction a : actionStack) {
            ekgs.actionStack.add(a.copy());
        }
        ekgs.orderOfPlayerDeath = orderOfPlayerDeath.clone();
        ekgs.playerHandCards = new ArrayList<>();
        for (PartialObservableDeck<ExplodingKittensCard> d : playerHandCards) {
            ekgs.playerHandCards.add(d.copy());
        }
        ekgs.drawPile = drawPile.copy();
        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Other player hands + draw deck are hidden, combine in draw pile and shuffle
            // Note: this considers the agent to track opponent's cards that are known to him by itself
            // e.g. in case the agent has previously given a favor card to its opponent
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    // Take all cards the player can't see from other players and put them in the draw pile.
                    ArrayList<ExplodingKittensCard> cs = new ArrayList<>();
                    for (int j = 0; j < ekgs.playerHandCards.get(i).getSize(); j++) {
                        if (!ekgs.playerHandCards.get(i).isComponentVisible(j, playerId)) {
                            ExplodingKittensCard c = ekgs.playerHandCards.get(i).get(j);
                            ekgs.drawPile.add(c, ekgs.playerHandCards.get(i).getVisibilityOfComponent(j).clone());
                            cs.add(c);
                        }
                    }
                    for (ExplodingKittensCard c : cs) {
                        ekgs.playerHandCards.get(i).remove(c);
                    }
                }
            }
            Random r = new Random(ekgs.gameParameters.getRandomSeed());

            // Shuffles only hidden cards in draw pile, if player knows what's on top those will stay in place
            ekgs.drawPile.shuffleVisible(r, playerId, false);
            Deck<ExplodingKittensCard> explosive = new Deck<>("tmp", VisibilityMode.HIDDEN_TO_ALL);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerHandCards.get(i).getSize(); j++) {
                        // Add back random cards for all components not visible to this player
                        if (playerHandCards.get(i).isComponentVisible(j, playerId)) continue;
                        boolean added = false;
                        int cardIndex = 0;
                        while (!added) {
                            // if the card is visible to the player we cannot move it somewhere else
                            if (ekgs.drawPile.getVisibilityForPlayer(cardIndex, playerId)) {
                                cardIndex++;
                                continue;
                            }
                            ExplodingKittensCard card = ekgs.drawPile.pick(cardIndex);
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

    private void moveHiddenCards(PartialObservableDeck<?> from, PartialObservableDeck<?> to) {

    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new ExplodingKittensHeuristic().evaluateState(this, playerId);
    }

    /**
     * This provides the current score in game turns. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     *
     * @param playerId
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        return playerResults[playerId].value;
    }

    @Override
    public int getOrdinalPosition(int playerId) {
        if (playerResults[playerId] == Utils.GameResult.WIN)
            return 1;
        if (playerResults[playerId] == Utils.GameResult.LOSE)
            return getNPlayers() - orderOfPlayerDeath[playerId] + 1;
        return 1;  // anyone still alive is jointly winning
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
     *
     * @param playerID - player who was killed in a kitten explosion.
     */
    public void killPlayer(int playerID) {
        setPlayerResult(Utils.GameResult.LOSE, playerID);
        int nPlayersActive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        orderOfPlayerDeath[playerID] = getNPlayers() - nPlayersActive;
        if (nPlayersActive == 1) {
            this.gameStatus = Utils.GameResult.GAME_END;
        }
        ((ExplodingKittensTurnOrder) getTurnOrder()).endPlayerTurnStep(this);

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

    public List<PartialObservableDeck<ExplodingKittensCard>> getPlayerHandCards() {
        return playerHandCards;
    }

    // Protected, only accessible in this package and subclasses
    protected void setDiscardPile(Deck<ExplodingKittensCard> discardPile) {
        this.discardPile = discardPile;
    }

    protected void setDrawPile(PartialObservableDeck<ExplodingKittensCard> drawPile) {
        this.drawPile = drawPile;
    }

    protected void setPlayerHandCards(List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards) {
        this.playerHandCards = playerHandCards;
    }

    protected void setActionStack(Stack<AbstractAction> actionStack) {
        this.actionStack = actionStack;
    }

    // Printing functions for the game state and decks.

    public void printToConsole() {
        System.out.println(toString());
    }

    @Override
    public String toString() {
        String s = "============================\n";

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++) {
            if (currentPlayer == i)
                s += ">>> Player " + i + ":";
            else
                s += "Player " + i + ":";
            s += playerHandCards.get(i).toString() + "\n";
        }

        s += "\nDrawPile: ";
        s += drawPile.toString() + "\n";

        s += "DiscardPile: ";
        s += discardPile.toString() + "\n";

        s += "Action stack: ";
        for (AbstractAction a : actionStack) {
            s += a.toString() + ",";
        }
        s = s.substring(0, s.length() - 1);
        s += "\n\n";

        s += "Current GamePhase: " + gamePhase + "\n";
        s += "Missing Draws: " + ((ExplodingKittensTurnOrder) turnOrder).requiredDraws + "\n";
        s += "============================\n";
        return s;
    }
}

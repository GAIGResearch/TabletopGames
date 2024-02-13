package games.explodingkittens;

import core.AbstractGameStateWithTurnOrder;
import core.AbstractParameters;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import core.interfaces.IStateFeatureJSON;
import core.turnorders.TurnOrder;
import games.GameType;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.*;

import static core.CoreConstants.VisibilityMode;

public class ExplodingKittensGameState extends AbstractGameStateWithTurnOrder implements IPrintable {

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
        super(gameParameters, nPlayers);
        playerGettingAFavor = -1;
        playerHandCards = new ArrayList<>();
    }
    @Override
    protected TurnOrder _createTurnOrder(int nPlayers) {
        return new ExplodingKittensTurnOrder(nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.ExplodingKittens;
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
    protected AbstractGameStateWithTurnOrder __copy(int playerId) {
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

            // Shuffles only hidden cards in draw pile, if player knows what's on top those will stay in place
            ekgs.drawPile.shuffleVisible(redeterminisationRnd, playerId, false);
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
        if (playerResults[playerId] == CoreConstants.GameResult.LOSE_GAME)
            // knocked out
            return orderOfPlayerDeath[playerId];
        // otherwise our current score is the number knocked out + 1
        return Arrays.stream(playerResults).filter(status -> status == CoreConstants.GameResult.LOSE_GAME).count() + 1;
    }

    @Override
    public int getOrdinalPosition(int playerId) {
        if (playerResults[playerId] == CoreConstants.GameResult.WIN_GAME)
            return 1;
        if (playerResults[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return getNPlayers() - orderOfPlayerDeath[playerId] + 1;
        return 1;  // anyone still alive is jointly winning
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
        setPlayerResult(CoreConstants.GameResult.LOSE_GAME, playerID);
        int nPlayersActive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] == CoreConstants.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        orderOfPlayerDeath[playerID] = getNPlayers() - nPlayersActive;
        if (nPlayersActive == 1) {
            this.gameStatus = CoreConstants.GameResult.GAME_END;
            for (int i = 0; i < getNPlayers(); i++) {
                // set winner
                if (playerResults[i] == CoreConstants.GameResult.GAME_ONGOING) {
                    playerResults[i] = CoreConstants.GameResult.WIN_GAME;
                }
            }
            turnOrder.endGame(this);
        }

        ((ExplodingKittensTurnOrder) getTurnOrder()).endPlayerTurnStep(this);

    }

    // Getters, setters
    public int getPlayerGettingAFavor() {
        return playerGettingAFavor;
    }

    public void setPlayerGettingAFavor(int playerGettingAFavor) {
        this.playerGettingAFavor = playerGettingAFavor;
    }

    public PartialObservableDeck<ExplodingKittensCard> getDrawPile() {
        return drawPile;
    }

    protected void setDrawPile(PartialObservableDeck<ExplodingKittensCard> drawPile) {
        this.drawPile = drawPile;
    }

    public Deck<ExplodingKittensCard> getDiscardPile() {
        return discardPile;
    }

    // Protected, only accessible in this package and subclasses
    protected void setDiscardPile(Deck<ExplodingKittensCard> discardPile) {
        this.discardPile = discardPile;
    }

    public Stack<AbstractAction> getActionStack() {
        return actionStack;
    }

    protected void setActionStack(Stack<AbstractAction> actionStack) {
        this.actionStack = actionStack;
    }

    public List<PartialObservableDeck<ExplodingKittensCard>> getPlayerHandCards() {
        return playerHandCards;
    }

    protected void setPlayerHandCards(List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards) {
        this.playerHandCards = playerHandCards;
    }

    public void printToConsole() {
        System.out.println(toString());
    }


    // Printing functions for the game state and decks.

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

    // Exploding kittens adds 4 phases on top of default ones.
    public enum ExplodingKittensGamePhase implements IGamePhase {
        Nope,
        Defuse,
        Favor,
        SeeTheFuture
    }
}

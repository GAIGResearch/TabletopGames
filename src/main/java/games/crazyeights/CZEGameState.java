package games.crazyeights;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Game state for Crazy Eights. Data-only: all initialisation and rule logic lives in {@link CZEForwardModel}.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>drawDeck - the face-down stock, HIDDEN_TO_ALL</li>
 *     <li>discardPile - face-up played cards, VISIBLE_TO_ALL. The top card is at index 0
 *     (Deck.add() inserts at 0 and Deck.peek() reads index 0)</li>
 *     <li>currentSuit - the suit that must be matched. This equals the suit of the top discard, except when
 *     the top discard is an Eight, when it is the suit nominated by whoever played it</li>
 *     <li>consecutivePasses - Pass actions taken in succession since a card was last played or drawn (public)</li>
 * </ul>
 */
public class CZEGameState extends AbstractGameState {

    List<Deck<FrenchCard>> playerHands;
    Deck<FrenchCard> drawDeck;
    Deck<FrenchCard> discardPile;
    FrenchCard.Suite currentSuit;
    int consecutivePasses;

    public CZEGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.CrazyEights;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(drawDeck);
        components.add(discardPile);
        return components;
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public FrenchCard getTopCard() {
        return discardPile.peek();
    }

    public FrenchCard.Suite getCurrentSuit() {
        return currentSuit;
    }

    public void setCurrentSuit(FrenchCard.Suite suit) {
        currentSuit = suit;
    }

    /**
     * True if a card can be drawn: the stock has cards, or there are discards under the top card that
     * would be shuffled to form a new stock.
     */
    public boolean canDraw() {
        return drawDeck.getSize() > 0 || discardPile.getSize() > 1;
    }

    /**
     * The number of Pass actions taken in succession since the last card was played or drawn.
     * When this reaches the number of players the game is blocked and ends.
     */
    public int getConsecutivePasses() {
        return consecutivePasses;
    }

    /**
     * Set the number of Pass actions taken in succession. Used by the forward model; also used by tests to
     * arrange a position where every other player has already passed.
     */
    public void setConsecutivePasses(int passes) {
        consecutivePasses = passes;
    }

    public static boolean isEight(FrenchCard card) {
        return card.type == FrenchCard.FrenchCardType.Number && card.number == 8;
    }

    /**
     * An Eight may always be played. Otherwise the card must match the current suit, or match the rank of the
     * top discard when that is not an Eight (an Eight's rank has been 'replaced' by its nominated suit).
     */
    public boolean canPlay(FrenchCard card) {
        if (isEight(card)) return true;
        if (card.suite == currentSuit) return true;
        FrenchCard top = getTopCard();
        return !isEight(top) && card.type == top.type && card.number == top.number;
    }

    /**
     * Penalty points for the cards currently in the player's hand.
     */
    public int handPenalty(int playerId) {
        CZEParameters params = (CZEParameters) gameParameters;
        int total = 0;
        for (FrenchCard card : playerHands.get(playerId).getComponents()) {
            if (isEight(card))
                total += params.eightPenalty;
            else switch (card.type) {
                case Ace -> total += params.acePenalty;
                case Jack, Queen, King -> total += params.pictureCardPenalty;
                case Number -> total += card.number;
            }
        }
        return total;
    }

    @Override
    protected CZEGameState _copy(int playerId) {
        CZEGameState copy = new CZEGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands) {
            copy.playerHands.add(hand.copy());
        }
        copy.drawDeck = drawDeck.copy();
        copy.discardPile = discardPile.copy();
        copy.currentSuit = currentSuit;
        copy.consecutivePasses = consecutivePasses;

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // Other players' hands and the stock are unknown: shuffle them together, keeping hand sizes.
            List<Deck<FrenchCard>> decks = new ArrayList<>(copy.playerHands);
            decks.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    /**
     * Fewer penalty points in hand is better. Scaled into [0, 1) while the game is ongoing.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            return 1.0 / (1.0 + handPenalty(playerId));
        } else {
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * Penalty points are bad, so the score is their negative: the player who went out scores 0, the maximum.
     */
    @Override
    public double getGameScore(int playerId) {
        return -handPenalty(playerId);
    }

    /**
     * Normally players are ranked by penalty points (via getGameScore): the player who went out is 1st, then the
     * lowest penalty. In a blocked game (every player passed in succession) players are instead ranked by the number
     * of cards in hand, fewest first, with tied players sharing a position.
     */
    @Override
    public int getOrdinalPosition(int playerId) {
        if (consecutivePasses >= getNPlayers())
            // players on the same number of cards share a position
            return getOrdinalPosition(playerId, p -> (double) -playerHands.get(p).getSize(), null);
        return super.getOrdinalPosition(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CZEGameState that)) return false;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardPile, that.discardPile) &&
                currentSuit == that.currentSuit &&
                consecutivePasses == that.consecutivePasses;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, drawDeck, discardPile, currentSuit, consecutivePasses);
    }
}

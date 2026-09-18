package games.agram;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * <p>Game state for Agram. Data-only: all initialisation and rule logic lives in {@link AgramForwardModel}.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>drawDeck - the cards left undealt, HIDDEN_TO_ALL. Never used in play</li>
 *     <li>currentTrick - the cards played to the trick in progress, VISIBLE_TO_ALL, in the order played:
 *     index 0 is the lead card (cards are added with addToBottom), and the card at index i was played by
 *     player (trickLeader + i) % nPlayers</li>
 *     <li>discardPile - cards from completed tricks, VISIBLE_TO_ALL</li>
 *     <li>trickLeader - the player who led the current trick. Once a trick is complete this is its winner, who
 *     leads the next; after the last trick of a deal it is the winner of the deal</li>
 *     <li>knownVoids - for each player, the suits they are publicly known not to hold, from having failed to
 *     follow suit. Redeterminisation never deals them a card of those suits</li>
 *     <li>dealsWon - for each player, the deals of the match they have won (nDeals in AgramParameters)</li>
 * </ul>
 */
public class AgramGameState extends AbstractGameState {

    List<Deck<FrenchCard>> playerHands;
    Deck<FrenchCard> drawDeck;
    Deck<FrenchCard> currentTrick;
    Deck<FrenchCard> discardPile;
    int trickLeader;
    List<Set<FrenchCard.Suite>> knownVoids;
    int[] dealsWon;

    public AgramGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Agram;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(drawDeck);
        components.add(currentTrick);
        components.add(discardPile);
        return components;
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getCurrentTrick() {
        return currentTrick;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public int getTrickLeader() {
        return trickLeader;
    }

    /**
     * The number of completed deals in this match that the player has won (by winning the deal's last trick).
     */
    public int getDealsWon(int playerId) {
        return dealsWon[playerId];
    }

    /**
     * The suits the player is publicly known to be void in: suits led to a trick to which they did not follow suit.
     */
    public Set<FrenchCard.Suite> getKnownVoids(int playerId) {
        return knownVoids.get(playerId);
    }

    /**
     * The suit led to the current trick, or null if no card has been played to it yet.
     */
    public FrenchCard.Suite getLeadSuit() {
        return currentTrick.getSize() == 0 ? null : currentTrick.get(0).suite;
    }

    /**
     * The player who played the card at the given index of the current trick.
     */
    public int playerOfTrickCard(int index) {
        return (trickLeader + index) % getNPlayers();
    }

    /**
     * The player currently winning the trick in progress: whoever played the highest card of the suit led.
     * Aces are high (FrenchCard numbers them 14). The trick must not be empty.
     */
    public int currentTrickWinner() {
        FrenchCard.Suite lead = getLeadSuit();
        int best = 0;
        for (int i = 1; i < currentTrick.getSize(); i++) {
            FrenchCard card = currentTrick.get(i);
            if (card.suite == lead && card.number > currentTrick.get(best).number)
                best = i;
        }
        return playerOfTrickCard(best);
    }


    @Override
    protected AgramGameState _copy(int playerId) {
        AgramGameState copy = new AgramGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands) {
            copy.playerHands.add(hand.copy());
        }
        copy.drawDeck = drawDeck.copy();
        copy.currentTrick = currentTrick.copy();
        copy.discardPile = discardPile.copy();
        copy.trickLeader = trickLeader;
        copy.dealsWon = dealsWon.clone();
        copy.knownVoids = new ArrayList<>();
        for (Set<FrenchCard.Suite> voids : knownVoids) {
            copy.knownVoids.add(voids.isEmpty() ? EnumSet.noneOf(FrenchCard.Suite.class) : EnumSet.copyOf(voids));
        }

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // Other players' hands and the undealt cards are unknown: shuffle them together, keeping hand sizes,
            // and never giving a player a card of a suit they are known to be void in.
            List<Deck<FrenchCard>> decks = new ArrayList<>(copy.playerHands);
            decks.add(copy.drawDeck);
            BiPredicate<Deck<FrenchCard>, FrenchCard> notVoid =
                    (deck, card) -> deck.getOwnerId() < 0 || !knownVoids.get(deck.getOwnerId()).contains(card.suite);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd, notVoid);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            return 0.0;
        } else {
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * The number of deals won. With a single deal this is 1 for the winner of the last trick and 0 for everyone else.
     */
    @Override
    public double getGameScore(int playerId) {
        return dealsWon[playerId];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgramGameState that)) return false;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(discardPile, that.discardPile) &&
                trickLeader == that.trickLeader &&
                Objects.equals(knownVoids, that.knownVoids) &&
                Arrays.equals(dealsWon, that.dealsWon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, drawDeck, currentTrick, discardPile, trickLeader, knownVoids)
                + 31 * Arrays.hashCode(dealsWon);
    }
}

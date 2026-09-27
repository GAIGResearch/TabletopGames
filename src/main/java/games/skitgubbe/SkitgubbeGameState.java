package games.skitgubbe;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Skitgubbe: in phase one players win cards in two-card tricks; in phase two they try to get rid of those cards by
 * beating the last card played. The last player left holding cards loses.
 */
public class SkitgubbeGameState extends AbstractGameState {

    public enum Phase implements IGamePhase {
        PHASE_ONE,
        PHASE_TWO
    }

    // phase one: each player's hand
    List<Deck<FrenchCard>> playerHands;
    // phase one: the face-down draw deck
    Deck<FrenchCard> drawDeck;
    // phase one: the last card of the draw deck, kept face down by the player who drew it (its owner)
    Deck<FrenchCard> trumpCard;
    // the cards each player has won in phase one; in phase two, the cards the player must get rid of (face up)
    List<Deck<FrenchCard>> collectedCards;
    // phase one: each player's cards from tied tricks (bounces), not yet won by anyone
    List<Deck<FrenchCard>> heldCards;
    // the current trick, the last card played on top: two cards in phase one (the leader's at the bottom)
    Deck<FrenchCard> trick;
    // phase two: the cards of completed tricks, out of the game
    Deck<FrenchCard> discardPile;
    // the suit of the trump card, or null until phase one ends
    FrenchCard.Suite trumpSuit;
    // the player who drew the trump card, or -1 until it is drawn
    int trumpPlayer = -1;
    // phase two: the number of cards that completes the current trick (the players holding cards when it began)
    int trickSize;
    // phase two: each player's score once out of cards, 0 while they still hold cards
    int[] exitScores;
    // the number of actions taken in phase two, for SkitgubbeParameters.maxPhaseTwoActions
    int phaseTwoActions;
    // for SkitgubbeParameters.exitOrderTiebreak: phaseTwoActions when each player went out (0 for a player out when
    // phase two begins, and while they hold cards)
    int[] exitActions;

    public SkitgubbeGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        playerHands = new ArrayList<>();
        collectedCards = new ArrayList<>();
        heldCards = new ArrayList<>();
        exitScores = new int[nPlayers];
        exitActions = new int[nPlayers];
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Skitgubbe;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.addAll(collectedCards);
        components.addAll(heldCards);
        if (drawDeck != null) components.add(drawDeck);
        if (trumpCard != null) components.add(trumpCard);
        if (trick != null) components.add(trick);
        if (discardPile != null) components.add(discardPile);
        return components;
    }

    @Override
    protected SkitgubbeGameState _copy(int playerId) {
        SkitgubbeGameState copy = new SkitgubbeGameState(gameParameters, getNPlayers());
        for (Deck<FrenchCard> hand : playerHands) copy.playerHands.add(hand.copy());
        for (Deck<FrenchCard> pile : collectedCards) copy.collectedCards.add(pile.copy());
        for (Deck<FrenchCard> pile : heldCards) copy.heldCards.add(pile.copy());
        copy.drawDeck = drawDeck == null ? null : drawDeck.copy();
        copy.trumpCard = trumpCard == null ? null : trumpCard.copy();
        copy.trick = trick == null ? null : trick.copy();
        copy.discardPile = discardPile == null ? null : discardPile.copy();
        copy.trumpSuit = trumpSuit;
        copy.trumpPlayer = trumpPlayer;
        copy.trickSize = trickSize;
        copy.exitScores = exitScores.clone();
        copy.phaseTwoActions = phaseTwoActions;
        copy.exitActions = exitActions.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other players' hands, the draw deck and (unless playerId drew it) the trump card are hidden
            List<Deck<FrenchCard>> hidden = new ArrayList<>(copy.playerHands);
            hidden.add(copy.drawDeck);
            hidden.add(copy.trumpCard);
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        if (exitScores[playerId] > 0)
            return (double) exitScores[playerId] / getNPlayers();
        return 0.0;
    }

    @Override
    public double getGameScore(int playerId) {
        return exitScores[playerId];
    }

    @Override
    public double getTiebreak(int playerId, int tier) {
        // without SkitgubbeParameters.exitOrderTiebreak, players on equal scores share a place
        if (!((SkitgubbeParameters) gameParameters).exitOrderTiebreak)
            return super.getTiebreak(playerId, tier);
        // the earlier out ranks higher; players still holding cards stay tied
        return exitScores[playerId] > 0 ? -exitActions[playerId] : Integer.MIN_VALUE;
    }

    @Override
    public int getTiebreakLevels() {
        return 1;
    }

    public Deck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getTrumpCard() {
        return trumpCard;
    }

    public Deck<FrenchCard> getCollectedCards(int player) {
        return collectedCards.get(player);
    }

    public Deck<FrenchCard> getHeldCards(int player) {
        return heldCards.get(player);
    }

    public Deck<FrenchCard> getTrick() {
        return trick;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public FrenchCard.Suite getTrumpSuit() {
        return trumpSuit;
    }

    public int getTrumpPlayer() {
        return trumpPlayer;
    }

    public int getTrickSize() {
        return trickSize;
    }

    public int getExitScore(int player) {
        return exitScores[player];
    }

    public int getPhaseTwoActions() {
        return phaseTwoActions;
    }

    public int getExitActions(int player) {
        return exitActions[player];
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof SkitgubbeGameState that)) return false;
        return trumpPlayer == that.trumpPlayer &&
                trickSize == that.trickSize &&
                phaseTwoActions == that.phaseTwoActions &&
                trumpSuit == that.trumpSuit &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(trumpCard, that.trumpCard) &&
                Objects.equals(collectedCards, that.collectedCards) &&
                Objects.equals(heldCards, that.heldCards) &&
                Objects.equals(trick, that.trick) &&
                Objects.equals(discardPile, that.discardPile) &&
                Arrays.equals(exitScores, that.exitScores) &&
                Arrays.equals(exitActions, that.exitActions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), trumpPlayer, trickSize, phaseTwoActions,
                trumpSuit == null ? -1 : trumpSuit.ordinal(), playerHands,
                drawDeck, trumpCard, collectedCards, heldCards, trick, discardPile);
        result = 31 * result + Arrays.hashCode(exitScores);
        result = 31 * result + Arrays.hashCode(exitActions);
        return result;
    }
}

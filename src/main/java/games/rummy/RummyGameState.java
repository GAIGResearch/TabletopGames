package games.rummy;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.rummy.actions.LayOff;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Rummy: players draw and discard, laying down sets and runs, until one of them has no cards or the draw deck is empty.
 */
public class RummyGameState extends AbstractGameState {

    // a card taken from the discard pile is visible to every player while it stays in the hand
    List<PartialObservableDeck<FrenchCard>> playerHands;
    Deck<FrenchCard> drawDeck;
    // face up, the latest discard on top
    Deck<FrenchCard> discardPile;
    // the melds on the table, in the order they were laid down
    List<RummyMeld> melds;
    // the card the current player took from the discard pile this turn, or null
    FrenchCard takenCard;
    // whether the current player has laid down a meld this turn
    boolean meldedThisTurn;
    // the points each player has scored in earlier deals, when playing to RummyParameters.targetScore
    int[] playerScores;

    public enum Phase implements IGamePhase {
        // the current player draws from the draw deck or the discard pile
        DRAW,
        // the current player melds, lays off or discards
        PLAY
    }

    public RummyGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        playerHands = new ArrayList<>();
        melds = new ArrayList<>();
        playerScores = new int[nPlayers];
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Rummy;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        if (drawDeck != null) components.add(drawDeck);
        if (discardPile != null) components.add(discardPile);
        components.addAll(melds);
        return components;
    }

    @Override
    protected RummyGameState _copy(int playerId) {
        RummyGameState copy = new RummyGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (PartialObservableDeck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.drawDeck = drawDeck == null ? null : drawDeck.copy();
        copy.discardPile = discardPile == null ? null : discardPile.copy();
        copy.melds = new ArrayList<>();
        for (RummyMeld meld : melds)
            copy.melds.add(meld.copy());
        copy.takenCard = takenCard;
        copy.meldedThisTurn = meldedThisTurn;
        copy.playerScores = playerScores.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable && copy.drawDeck != null) {
            // the other players' hands and the draw deck are hidden from playerId
            List<Deck<FrenchCard>> hidden = new ArrayList<>();
            for (int p = 0; p < getNPlayers(); p++)
                if (p != playerId) hidden.add(copy.playerHands.get(p));
            hidden.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // the fewer points in hand the better, against a hand of court cards and tens
        RummyParameters params = (RummyParameters) getGameParameters();
        // when playing to a target score, the share of the target reached
        if (params.targetScore > 0)
            return Math.min(1.0, (double) playerScores[playerId] / params.targetScore);
        int worst = 10 * (params.handSize(getNPlayers()) + 1);
        return Math.max(0.0, 1.0 - (double) handPoints(playerId) / worst);
    }

    @Override
    public double getGameScore(int playerId) {
        if (((RummyParameters) getGameParameters()).targetScore > 0)
            return playerScores[playerId];
        // in a single deal the player with the fewest points in hand wins
        return -handPoints(playerId);
    }

    public int getPlayerScore(int player) {
        return playerScores[player];
    }

    public int handPoints(int playerId) {
        return RummyUtils.points(playerHands.get(playerId));
    }

    public PartialObservableDeck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public List<PartialObservableDeck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public List<RummyMeld> getMelds() {
        return melds;
    }

    /**
     * @return the meld on the table that the card can be laid off onto at the given position, or null if none
     */
    public RummyMeld meldFor(FrenchCard card, LayOff.Position position) {
        for (RummyMeld meld : melds)
            if (RummyUtils.fits(meld.getComponents(), card, position))
                return meld;
        return null;
    }

    public FrenchCard getTakenCard() {
        return takenCard;
    }

    public void setTakenCard(FrenchCard card) {
        takenCard = card;
    }

    public boolean hasMeldedThisTurn() {
        return meldedThisTurn;
    }

    public void setMeldedThisTurn(boolean melded) {
        meldedThisTurn = melded;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof RummyGameState that)) return false;
        return meldedThisTurn == that.meldedThisTurn &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardPile, that.discardPile) &&
                Objects.equals(melds, that.melds) &&
                Objects.equals(takenCard, that.takenCard) &&
                Arrays.equals(playerScores, that.playerScores);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(super.hashCode(), meldedThisTurn, playerHands, drawDeck, discardPile, melds, takenCard)
                + Arrays.hashCode(playerScores);
    }
}

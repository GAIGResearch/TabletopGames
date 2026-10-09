package games.president;

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
 * President: players shed sets of equal-ranked cards, each set beating the last, until their hand is empty.
 */
public class PresidentGameState extends AbstractGameState {

    List<Deck<FrenchCard>> playerHands;
    // the sets played to the current trick, the latest on top
    Deck<FrenchCard> playPile;
    // the cards of the tricks already cleared this deal
    Deck<FrenchCard> discardPile;
    // the player who played the top set of the play pile, or -1 while it is empty
    int lastPlayer = -1;
    // the number of cards in each set of the current trick, or 0 while the play pile is empty
    int setSize;
    // the passes since the last set was played
    int passesInRow;
    // the players who have emptied their hands this deal, in the order they went out
    List<Integer> finishingOrder = new ArrayList<>();
    int[] playerScores;
    // the Scum of the last deal while the President gives cards back, otherwise -1
    int scum = -1;
    // the cards the President has still to give back to the Scum
    int cardsToGive;

    public enum Phase implements IGamePhase {
        PLAY,
        EXCHANGE
    }

    public PresidentGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        playerHands = new ArrayList<>();
        playerScores = new int[nPlayers];
    }

    @Override
    protected GameType _getGameType() {
        return GameType.President;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        if (playPile != null) components.add(playPile);
        if (discardPile != null) components.add(discardPile);
        return components;
    }

    @Override
    protected PresidentGameState _copy(int playerId) {
        PresidentGameState copy = new PresidentGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.playPile = playPile == null ? null : playPile.copy();
        copy.discardPile = discardPile == null ? null : discardPile.copy();
        copy.lastPlayer = lastPlayer;
        copy.setSize = setSize;
        copy.passesInRow = passesInRow;
        copy.finishingOrder = new ArrayList<>(finishingOrder);
        copy.playerScores = playerScores.clone();
        copy.scum = scum;
        copy.cardsToGive = cardsToGive;

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other players' hands are hidden from playerId. The cards exchanged between the President and the
            // Scum are shuffled with the rest, although each of them knows where the cards they gave went.
            List<Deck<FrenchCard>> hidden = new ArrayList<>();
            for (int p = 0; p < getNPlayers(); p++)
                if (p != playerId) hidden.add(copy.playerHands.get(p));
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // the share of the cards dealt that the player has got rid of (the President may briefly hold more after the
        // exchange)
        int dealt = (52 + getNPlayers() - 1) / getNPlayers();
        return Math.max(0.0, 1.0 - (double) playerHands.get(playerId).getSize() / dealt);
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScores[playerId];
    }

    /**
     * Players on equal scores are ranked by the order in which they went out.
     */
    @Override
    public double getTiebreak(int playerId, int tier) {
        int place = finishingOrder.indexOf(playerId);
        return place < 0 ? 0 : getNPlayers() - place;
    }

    @Override
    public int getTiebreakLevels() {
        return 1;
    }

    public Deck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    public Deck<FrenchCard> getPlayPile() {
        return playPile;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public int getLastPlayer() {
        return lastPlayer;
    }

    public int getSetSize() {
        return setSize;
    }

    public int getPassesInRow() {
        return passesInRow;
    }

    public void setLastPlayer(int player) {
        lastPlayer = player;
    }

    public void setSetSize(int size) {
        setSize = size;
    }

    public void setPassesInRow(int passes) {
        passesInRow = passes;
    }

    public List<Integer> getFinishingOrder() {
        return finishingOrder;
    }

    public int getPlayerScore(int player) {
        return playerScores[player];
    }

    public int getScum() {
        return scum;
    }

    public int getCardsToGive() {
        return cardsToGive;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof PresidentGameState that)) return false;
        return lastPlayer == that.lastPlayer &&
                setSize == that.setSize &&
                passesInRow == that.passesInRow &&
                scum == that.scum &&
                cardsToGive == that.cardsToGive &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(playPile, that.playPile) &&
                Objects.equals(discardPile, that.discardPile) &&
                Objects.equals(finishingOrder, that.finishingOrder) &&
                Arrays.equals(playerScores, that.playerScores);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), lastPlayer, setSize, passesInRow, scum, cardsToGive, playerHands, playPile,
                discardPile, finishingOrder);
        result = 31 * result + Arrays.hashCode(playerScores);
        return result;
    }
}

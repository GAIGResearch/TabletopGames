package games.schwimmen;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one PartialObservableDeck per player: the owner sees every card, and every player sees a card
 *     taken from the table</li>
 *     <li>extraHand - the extra hand dealt face down for the dealer's choice, HIDDEN_TO_ALL; empty once the dealer
 *     has chosen</li>
 *     <li>table - the face-up cards that players exchange with, VISIBLE_TO_ALL</li>
 *     <li>drawDeck - the undealt cards, HIDDEN_TO_ALL</li>
 *     <li>discardPile - table cards replaced after every player passed, VISIBLE_TO_ALL</li>
 *     <li>dealer - the player who dealt this deal</li>
 *     <li>consecutivePasses - the passes since the last exchange (or since the table cards were replaced)</li>
 *     <li>closer - the player who closed, or -1</li>
 *     <li>chips - each player's chips in the chips game (0 is swimming); below 0 once out, -1 minus the number of
 *     players still in when they dropped out</li>
 * </ul>
 */
public class SchwimmenGameState extends AbstractGameState {

    List<PartialObservableDeck<FrenchCard>> playerHands;
    Deck<FrenchCard> extraHand;
    Deck<FrenchCard> table;
    Deck<FrenchCard> drawDeck;
    Deck<FrenchCard> discardPile;
    int dealer;
    int consecutivePasses;
    int closer = -1;
    int[] chips;

    public SchwimmenGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Schwimmen;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(extraHand);
        components.add(table);
        components.add(drawDeck);
        components.add(discardPile);
        return components;
    }

    public PartialObservableDeck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public Deck<FrenchCard> getExtraHand() {
        return extraHand;
    }

    public Deck<FrenchCard> getTable() {
        return table;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public int getDealer() {
        return dealer;
    }

    public int getConsecutivePasses() {
        return consecutivePasses;
    }

    public int getCloser() {
        return closer;
    }

    public int getChips(int player) {
        return chips[player];
    }

    /**
     * False once the player has dropped out of the chips game.
     */
    public boolean isInGame(int player) {
        return chips[player] >= 0;
    }

    public int getNPlayersInGame() {
        int n = 0;
        for (int p = 0; p < getNPlayers(); p++)
            if (isInGame(p))
                n++;
        return n;
    }

    public boolean isDealerChoicePending() {
        return !extraHand.getComponents().isEmpty();
    }

    public double getHandValue(int player) {
        return SchwimmenUtils.handValue(playerHands.get(player).getComponents(),
                (SchwimmenParameters) gameParameters);
    }

    @Override
    protected SchwimmenGameState _copy(int playerId) {
        SchwimmenGameState copy = new SchwimmenGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (PartialObservableDeck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.extraHand = extraHand.copy();
        copy.table = table.copy();
        copy.drawDeck = drawDeck.copy();
        copy.discardPile = discardPile.copy();
        copy.dealer = dealer;
        copy.consecutivePasses = consecutivePasses;
        copy.closer = closer;
        copy.chips = chips.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the cards playerId cannot see in other players' hands, the extra hand and the draw deck are shuffled
            // together; cards taken from the table stay where they are
            List<Deck<FrenchCard>> hidden = new ArrayList<>(copy.playerHands);
            hidden.add(copy.extraHand);
            hidden.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        SchwimmenParameters params = (SchwimmenParameters) gameParameters;
        if (params.livesGame)
            return isInGame(playerId) ? (chips[playerId] + 1.0) / (params.startingChips + 1.0) : 0;
        return getHandValue(playerId) / params.threeAcesValue;
    }

    /**
     * The value of the player's hand, or in the chips game their chips.
     */
    @Override
    public double getGameScore(int playerId) {
        if (((SchwimmenParameters) gameParameters).livesGame)
            return chips[playerId];
        return getHandValue(playerId);
    }

    @Override
    public double getTiebreak(int playerId, int tier) {
        // in the chips game, equal chips share a place
        if (((SchwimmenParameters) gameParameters).livesGame)
            return Double.MAX_VALUE;
        return SchwimmenUtils.tiebreak(playerHands.get(playerId).getComponents(), (SchwimmenParameters) gameParameters);
    }

    @Override
    public int getTiebreakLevels() {
        return 1;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchwimmenGameState that)) return false;
        return dealer == that.dealer && consecutivePasses == that.consecutivePasses && closer == that.closer &&
                Arrays.equals(chips, that.chips) &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(extraHand, that.extraHand) &&
                Objects.equals(table, that.table) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardPile, that.discardPile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, extraHand, table, drawDeck, discardPile,
                dealer, consecutivePasses, closer) + 31 * Arrays.hashCode(chips);
    }
}

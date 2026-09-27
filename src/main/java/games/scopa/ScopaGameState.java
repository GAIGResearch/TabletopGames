package games.scopa;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.TarotCard;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>drawDeck - the cards not yet dealt, HIDDEN_TO_ALL</li>
 *     <li>table - the face-up cards in the middle, VISIBLE_TO_ALL</li>
 *     <li>capturedCards - the cards each player has captured this deal. VISIBLE_TO_ALL, as every card in it was seen
 *     when captured</li>
 *     <li>scopas - the sweeps each player has made this deal</li>
 *     <li>lastCapturer - the player who made the last capture this deal, or -1</li>
 *     <li>bankedScores - the points each player scored in the deals already completed</li>
 * </ul>
 */
public class ScopaGameState extends AbstractGameState {

    List<Deck<TarotCard>> playerHands;
    Deck<TarotCard> drawDeck;
    Deck<TarotCard> table;
    List<Deck<TarotCard>> capturedCards;
    int[] scopas;
    int lastCapturer;
    int[] bankedScores;

    public ScopaGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Scopa;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(drawDeck);
        components.add(table);
        components.addAll(capturedCards);
        return components;
    }

    public Deck<TarotCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public Deck<TarotCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<TarotCard> getTable() {
        return table;
    }

    public Deck<TarotCard> getCapturedCards(int player) {
        return capturedCards.get(player);
    }

    public int getScopas(int player) {
        return scopas[player];
    }

    public int getLastCapturer() {
        return lastCapturer;
    }

    public int getBankedScore(int player) {
        return bankedScores[player];
    }

    public boolean isDealOver() {
        return drawDeck.getSize() == 0 && playerHands.stream().allMatch(h -> h.getSize() == 0);
    }

    public int getDealer() {
        // the last player deals first, so player 0 plays first; the deal then alternates
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    @Override
    protected ScopaGameState _copy(int playerId) {
        ScopaGameState copy = new ScopaGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<TarotCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.drawDeck = drawDeck.copy();
        copy.table = table.copy();
        copy.capturedCards = new ArrayList<>();
        for (Deck<TarotCard> captured : capturedCards)
            copy.capturedCards.add(captured.copy());
        copy.scopas = scopas.clone();
        copy.lastCapturer = lastCapturer;
        copy.bankedScores = bankedScores.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other player's hand and the draw deck are unknown
            List<Deck<TarotCard>> hidden = new ArrayList<>(copy.playerHands);
            hidden.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // the player's share of the points scored so far
        double total = 0;
        for (int p = 0; p < getNPlayers(); p++)
            total += getGameScore(p);
        return total == 0 ? 0.5 : getGameScore(playerId) / total;
    }

    /**
     * The points banked from the deals completed, plus those scored so far in this one.
     */
    @Override
    public double getGameScore(int playerId) {
        return bankedScores[playerId] + getDealScore(playerId);
    }

    /**
     * The player's points this deal, judged on the captured piles as they stand.
     */
    public int getDealScore(int playerId) {
        List<TarotCard> mine = capturedCards.get(playerId).getComponents();
        List<TarotCard> theirs = capturedCards.get(1 - playerId).getComponents();  // two players
        // a point for each scopa, then a point each for more cards, more Coins, the 7 of Coins and the higher
        // primiera; a tie scores nothing
        int score = scopas[playerId];
        if (mine.size() > theirs.size())
            score++;
        if (ScopaUtils.coins(mine) > ScopaUtils.coins(theirs))
            score++;
        if (mine.contains(ScopaParameters.SETTEBELLO))
            score++;
        if (ScopaUtils.primiera(mine) > ScopaUtils.primiera(theirs))
            score++;
        return score;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScopaGameState that)) return false;
        return lastCapturer == that.lastCapturer &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(table, that.table) &&
                Objects.equals(capturedCards, that.capturedCards) &&
                Arrays.equals(scopas, that.scopas) &&
                Arrays.equals(bankedScores, that.bankedScores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, drawDeck, table, capturedCards, lastCapturer,
                Arrays.hashCode(scopas), Arrays.hashCode(bankedScores));
    }
}

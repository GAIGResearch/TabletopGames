package games.leducpoker;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Leduc Poker (Leduc Hold'em): two players, a six-card deck, one private card each, two betting rounds with a
 * public board card revealed between them. A game is LeducPokerParameters.nHands hands; the score is net chips.
 * <p>
 * Hidden information: the opponent's private card and the order of the draw deck.
 */
public class LeducPokerGameState extends AbstractGameState {

    // Each player's private card (VISIBLE_TO_OWNER)
    List<Deck<FrenchCard>> hands;
    // The undealt cards (HIDDEN_TO_ALL)
    Deck<FrenchCard> drawDeck;
    // The public board card: empty in the first betting round, one card in the second (VISIBLE_TO_ALL)
    Deck<FrenchCard> board;
    // The chips each player has put in the pot this hand, ante included
    int[] contributions;
    // Each player's net chips won (or lost, if negative) over the hands finished so far
    int[] netChips;
    // The number of raises made in the current betting round
    int raisesThisRound;
    // The number of actions taken in the current betting round
    int actionsThisRound;

    public LeducPokerGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.LeducPoker;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(hands);
        components.add(drawDeck);
        components.add(board);
        return components;
    }

    /**
     * The current betting round: 0 before the board card is revealed, 1 after.
     */
    public int getBettingRound() {
        return board.getSize() == 0 ? 0 : 1;
    }

    public int getPot() {
        return Arrays.stream(contributions).sum();
    }

    /**
     * The chips the player must add to match the opponent's contribution (0 if they can check).
     */
    public int amountToCall(int player) {
        return contributions[1 - player] - contributions[player];
    }

    public boolean canRaise() {
        return raisesThisRound < ((LeducPokerParameters) gameParameters).maxRaisesPerRound;
    }

    public void addToPot(int player, int chips) {
        contributions[player] += chips;
    }

    public void recordRaise() {
        raisesThisRound++;
    }

    public Deck<FrenchCard> getHand(int player) {
        return hands.get(player);
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getBoard() {
        return board;
    }

    public int getContribution(int player) {
        return contributions[player];
    }

    public int getNetChips(int player) {
        return netChips[player];
    }

    public int getRaisesThisRound() {
        return raisesThisRound;
    }

    public int getActionsThisRound() {
        return actionsThisRound;
    }

    @Override
    protected LeducPokerGameState _copy(int playerId) {
        LeducPokerGameState copy = new LeducPokerGameState(gameParameters, getNPlayers());
        copy.hands = new ArrayList<>();
        for (Deck<FrenchCard> hand : hands)
            copy.hands.add(hand.copy());
        copy.drawDeck = drawDeck.copy();
        copy.board = board.copy();
        copy.contributions = contributions.clone();
        copy.netChips = netChips.clone();
        copy.raisesThisRound = raisesThisRound;
        copy.actionsThisRound = actionsThisRound;
        return copy;
    }

    @Override
    public void redeterminise(int playerId) {
        // The opponent's card and the draw deck are shuffled together
        List<Deck<FrenchCard>> decks = new ArrayList<>(hands);
        decks.add(drawDeck);
        DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd);
    }

    /**
     * Net chips won over the game so far.
     */
    @Override
    public double getGameScore(int playerId) {
        return netChips[playerId];
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            // WIN 1, DRAW 0, LOSE -1 mapped to 1, 0.5, 0
            return (getPlayerResults()[playerId].value + 1) / 2;
        LeducPokerParameters params = (LeducPokerParameters) gameParameters;
        double maxSwing = (double) params.maxContribution() * params.nHands;
        return (netChips[playerId] + maxSwing) / (2 * maxSwing);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeducPokerGameState that)) return false;
        return raisesThisRound == that.raisesThisRound &&
                actionsThisRound == that.actionsThisRound &&
                hands.equals(that.hands) &&
                drawDeck.equals(that.drawDeck) &&
                board.equals(that.board) &&
                Arrays.equals(contributions, that.contributions) &&
                Arrays.equals(netChips, that.netChips);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), hands, drawDeck, board, raisesThisRound, actionsThisRound);
        result = 31 * result + Arrays.hashCode(contributions);
        result = 31 * result + Arrays.hashCode(netChips);
        return result;
    }
}

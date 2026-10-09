package games.goofspiel;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Goofspiel (GOPS). Each player holds one suit; each round a prize card from the prize deck is turned up,
 * every player bids a card face down at the same time, and the highest bid wins the prizes on offer.
 * <p>
 * Hidden information: the order of the prize deck, and the bids other players have made this round.
 * A player's hand is private, but it is always the player's suit less the bids they have played.
 */
public class GoofspielGameState extends AbstractGameState {

    // Each player's remaining cards (VISIBLE_TO_OWNER)
    List<Deck<FrenchCard>> hands;
    // Each player's face-down bid this round: empty until they bid, then one card (VISIBLE_TO_OWNER)
    List<Deck<FrenchCard>> bids;
    // Each player's revealed bids, the most recent on top (VISIBLE_TO_ALL)
    List<Deck<FrenchCard>> playedBids;
    // The face-down prize deck (HIDDEN_TO_ALL)
    Deck<FrenchCard> prizeDeck;
    // The prize card turned up this round, plus any carried over from tied rounds (VISIBLE_TO_ALL)
    Deck<FrenchCard> prizesOnOffer;
    // The prize cards each player has won (VISIBLE_TO_ALL)
    List<Deck<FrenchCard>> wonPrizes;
    // Prize cards won by nobody (VISIBLE_TO_ALL)
    Deck<FrenchCard> discardedPrizes;

    public GoofspielGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Goofspiel;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(hands);
        components.addAll(bids);
        components.addAll(playedBids);
        components.add(prizeDeck);
        components.add(prizesOnOffer);
        components.addAll(wonPrizes);
        components.add(discardedPrizes);
        return components;
    }

    /**
     * The players who have not yet bid this round.
     */
    public List<Integer> getPlayersStillToBid() {
        List<Integer> players = new ArrayList<>();
        for (int p = 0; p < getNPlayers(); p++)
            if (bids.get(p).getSize() == 0)
                players.add(p);
        return players;
    }

    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        if (gameStatus != CoreConstants.GameResult.GAME_ONGOING)
            return super.getCurrentSimultaneousPlayers();
        List<Integer> players = getPlayersStillToBid();
        if (players.isEmpty())
            throw new AssertionError("Every player has bid but the round has not been resolved");
        return players;
    }

    @Override
    protected GoofspielGameState _copy(int playerId) {
        GoofspielGameState copy = new GoofspielGameState(gameParameters, getNPlayers());
        copy.hands = copyDecks(hands);
        copy.bids = copyDecks(bids);
        copy.playedBids = copyDecks(playedBids);
        copy.prizeDeck = prizeDeck.copy();
        copy.prizesOnOffer = prizesOnOffer.copy();
        copy.wonPrizes = copyDecks(wonPrizes);
        copy.discardedPrizes = discardedPrizes.copy();
        return copy;
    }

    private static List<Deck<FrenchCard>> copyDecks(List<Deck<FrenchCard>> decks) {
        List<Deck<FrenchCard>> copies = new ArrayList<>(decks.size());
        for (Deck<FrenchCard> d : decks)
            copies.add(d.copy());
        return copies;
    }

    @Override
    public void redeterminise(int playerId) {
        if (!isNotTerminal()) return;
        prizeDeck.shuffle(redeterminisationRnd);
        // The other players' bids this round are face down, so they go back to their hands and those players
        // are still to bid. The observer keeps their own bid.
        for (int p = 0; p < getNPlayers(); p++) {
            if (p != playerId && bids.get(p).getSize() > 0)
                returnToHand(p, bids.get(p).draw());
        }
        // All players bid at once, so the observer holds the turn if they are still to bid
        if (bids.get(playerId).getSize() == 0)
            setTurnOwner(playerId);
        else
            setTurnOwner(getPlayersStillToBid().get(0));
    }

    /**
     * Puts a bid card back in its place in the player's hand.
     */
    private void returnToHand(int player, FrenchCard card) {
        // Hands are in ascending order of value; on top of the hand the card would show what had been bid
        GoofspielParameters params = (GoofspielParameters) gameParameters;
        Deck<FrenchCard> hand = hands.get(player);
        int index = 0;
        while (index < hand.getSize() && params.cardValue(hand.get(index)) < params.cardValue(card))
            index++;
        hand.add(card, index);
    }

    /**
     * The total value of the prizes the player has won.
     */
    @Override
    public double getGameScore(int playerId) {
        GoofspielParameters params = (GoofspielParameters) gameParameters;
        int total = 0;
        for (FrenchCard c : wonPrizes.get(playerId))
            total += params.cardValue(c);
        return total;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        GoofspielParameters params = (GoofspielParameters) gameParameters;
        int maxScore = 0;
        for (FrenchCard c : params.suitCards(GoofspielParameters.PRIZE_SUIT))
            maxScore += params.cardValue(c);
        return getGameScore(playerId) / maxScore;
    }

    public Deck<FrenchCard> getHand(int player) {
        return hands.get(player);
    }

    public Deck<FrenchCard> getBid(int player) {
        return bids.get(player);
    }

    public Deck<FrenchCard> getPlayedBids(int player) {
        return playedBids.get(player);
    }

    public Deck<FrenchCard> getPrizeDeck() {
        return prizeDeck;
    }

    public Deck<FrenchCard> getPrizesOnOffer() {
        return prizesOnOffer;
    }

    public Deck<FrenchCard> getWonPrizes(int player) {
        return wonPrizes.get(player);
    }

    public Deck<FrenchCard> getDiscardedPrizes() {
        return discardedPrizes;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoofspielGameState that)) return false;
        return hands.equals(that.hands) &&
                bids.equals(that.bids) &&
                playedBids.equals(that.playedBids) &&
                prizeDeck.equals(that.prizeDeck) &&
                prizesOnOffer.equals(that.prizesOnOffer) &&
                wonPrizes.equals(that.wonPrizes) &&
                discardedPrizes.equals(that.discardedPrizes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hands, bids, playedBids, prizeDeck, prizesOnOffer, wonPrizes, discardedPrizes);
    }
}

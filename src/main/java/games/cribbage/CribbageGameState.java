package games.cribbage;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Game state for two-player six-card Cribbage. Data-only: all initialisation and rule logic lives in
 * {@link CribbageForwardModel}.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>drawDeck - the face-down undealt cards, HIDDEN_TO_ALL</li>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER. Cards leave the hand as they are played</li>
 *     <li>playedCards - one Deck per player, VISIBLE_TO_ALL: the cards that player has played this round. They
 *     are the player's hand again for the show</li>
 *     <li>crib - the four discarded cards that will be scored by the dealer. Each card is visible only to the player
 *     who discarded it</li>
 *     <li>starter - the card turned up after the discard, VISIBLE_TO_ALL; null until then</li>
 *     <li>playSequence - the cards in the current count, in the order played. This is a record of the play and
 *     not a container: each card in it is also in its player's playedCards</li>
 *     <li>scores - points pegged so far. These accumulate from the play and the show, so are not derivable
 *     from the components</li>
 * </ul>
 * <p>The dealer is not stored directly. In even numbered rounds it is player 0; in odd rounds player 1.</p>
 */
public class CribbageGameState extends AbstractGameState {

    public enum CribbageGamePhase implements IGamePhase {
        Discard,  // players each put nCardsToCrib cards into the crib, non-dealer first
        Play      // the play (pegging); the show is scored automatically when it ends
    }

    Deck<FrenchCard> drawDeck;
    List<Deck<FrenchCard>> playerHands;
    List<Deck<FrenchCard>> playedCards;
    PartialObservableDeck<FrenchCard> crib;
    FrenchCard starter;
    List<FrenchCard> playSequence;
    int[] scores;

    public CribbageGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Cribbage;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.addAll(playedCards);
        components.add(drawDeck);
        components.add(crib);
        if (starter != null) components.add(starter);
        return components;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getPlayerHand(int playerId) {
        return playerHands.get(playerId);
    }

    public Deck<FrenchCard> getPlayedCards(int playerId) {
        return playedCards.get(playerId);
    }

    public PartialObservableDeck<FrenchCard> getCrib() {
        return crib;
    }

    public FrenchCard getStarter() {
        return starter;
    }

    public List<FrenchCard> getPlaySequence() {
        return playSequence;
    }

    public int getScore(int playerId) {
        return scores[playerId];
    }

    /**
     * True if CribbageParameters.targetScore is set (above 0) and a player has reached it. The game then ends at
     * once; scores only rise and one player scores at a time, so that player has the higher score.
     */
    public boolean targetReached() {
        int target = ((CribbageParameters) gameParameters).targetScore;
        return target > 0 && Arrays.stream(scores).anyMatch(s -> s >= target);
    }

    /**
     * The dealer owns the crib. Player 0 deals in even numbered rounds, player 1 in odd ones.
     */
    public int getDealer() {
        return getRoundCounter() % getNPlayers();
    }

    public int getNonDealer() {
        return (getDealer() + 1) % getNPlayers();
    }

    /**
     * The running total of the current count.
     */
    public int getRunningTotal() {
        return playSequence.stream().mapToInt(CribbageUtils::pipValue).sum();
    }

    /**
     * True if the card can be played without taking the running total over CribbageParameters.maxCount.
     */
    public boolean canPlay(FrenchCard card) {
        return getRunningTotal() + CribbageUtils.pipValue(card) <= ((CribbageParameters) gameParameters).maxCount;
    }

    /**
     * True if the player has a card in hand that can be played in the current count.
     */
    public boolean canPlay(int playerId) {
        return playerHands.get(playerId).stream().anyMatch(this::canPlay);
    }

    @Override
    protected CribbageGameState _copy(int playerId) {
        CribbageGameState copy = new CribbageGameState(gameParameters, getNPlayers());
        copy.drawDeck = drawDeck.copy();
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.playedCards = new ArrayList<>();
        for (Deck<FrenchCard> played : playedCards)
            copy.playedCards.add(played.copy());
        copy.crib = crib.copy();
        copy.starter = starter;
        copy.playSequence = new ArrayList<>(playSequence);  // FrenchCard is immutable
        copy.scores = scores.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // The other player's hand, their crib cards and the draw deck are unknown: shuffle them together,
            // keeping deck sizes. The crib cards this player discarded stay put.
            List<Deck<FrenchCard>> decks = new ArrayList<>(copy.playerHands);
            decks.add(copy.crib);
            decks.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    /**
     * The player's score as a fraction of CribbageParameters.targetScore, capped at 1.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            int target = ((CribbageParameters) gameParameters).targetScore;
            return target > 0 ? Math.min(1.0, (double) scores[playerId] / target) : 0.0;
        } else {
            return getPlayerResults()[playerId].value;
        }
    }

    @Override
    public double getGameScore(int playerId) {
        return scores[playerId];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CribbageGameState that)) return false;
        return Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(playedCards, that.playedCards) &&
                Objects.equals(crib, that.crib) &&
                Objects.equals(starter, that.starter) &&
                Objects.equals(playSequence, that.playSequence) &&
                Arrays.equals(scores, that.scores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), drawDeck, playerHands, playedCards, crib, starter, playSequence)
                + 31 * Arrays.hashCode(scores);
    }
}

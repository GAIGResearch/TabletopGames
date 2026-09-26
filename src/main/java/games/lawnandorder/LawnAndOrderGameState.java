package games.lawnandorder;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Components tracked:</p>
 * <ul>
 *     <li>drawDeck - the Lawn cards not yet dealt or drawn, HIDDEN_TO_ALL</li>
 *     <li>hands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>chosenCards - the card each player has placed face down this turn, VISIBLE_TO_OWNER (empty until they
 *     choose)</li>
 *     <li>lawns - the cards each player has played this round, VISIBLE_TO_ALL</li>
 *     <li>discardDeck - the lawns (seen by all) and hands (seen by their owner) of players who received a Cease &amp;
 *     Desist this round</li>
 *     <li>agenda - the HOA Agenda, HIDDEN_TO_ALL</li>
 *     <li>insiderTips - the Rule cards dealt out of the agenda this round; tip i lies between players i and i+1</li>
 *     <li>revealedRules - the Rule cards revealed from the agenda this round, VISIBLE_TO_ALL</li>
 *     <li>citations - the Citations each player holds this round</li>
 *     <li>status - whether each player is active, has passed or has received a Cease &amp; Desist this round</li>
 *     <li>decisions - each active player's Continue or Pass choice, NONE until they make it</li>
 *     <li>goodwill - whether each player holds a Goodwill card this round</li>
 *     <li>trackScores - each player's score on the track of each LawnCard.Category, over the whole game</li>
 * </ul>
 */
public class LawnAndOrderGameState extends AbstractGameState {

    public enum Phase implements IGamePhase {PLAY_OBJECT, CONTINUE_OR_PASS}

    public enum PlayerStatus {ACTIVE, PASSED, CEASE_AND_DESIST}

    public enum Decision {NONE, CONTINUE, PASS}

    Deck<LawnCard> drawDeck;
    List<Deck<LawnCard>> hands;
    List<Deck<LawnCard>> chosenCards;
    List<Deck<LawnCard>> lawns;
    PartialObservableDeck<LawnCard> discardDeck;
    Deck<RuleCard> agenda;
    PartialObservableDeck<RuleCard> insiderTips;
    Deck<RuleCard> revealedRules;
    int[] citations;
    PlayerStatus[] status;
    Decision[] decisions;
    boolean[] goodwill;
    int[][] trackScores;

    public LawnAndOrderGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.LawnAndOrder;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(drawDeck);
        components.addAll(hands);
        components.addAll(chosenCards);
        components.addAll(lawns);
        components.add(discardDeck);
        components.add(agenda);
        components.add(insiderTips);
        components.add(revealedRules);
        return components;
    }

    public Deck<LawnCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<LawnCard> getHand(int player) {
        return hands.get(player);
    }

    public Deck<LawnCard> getChosenCard(int player) {
        return chosenCards.get(player);
    }

    public Deck<LawnCard> getLawn(int player) {
        return lawns.get(player);
    }

    public PartialObservableDeck<LawnCard> getDiscardDeck() {
        return discardDeck;
    }

    public Deck<RuleCard> getAgenda() {
        return agenda;
    }

    public PartialObservableDeck<RuleCard> getInsiderTips() {
        return insiderTips;
    }

    public Deck<RuleCard> getRevealedRules() {
        return revealedRules;
    }

    public int getCitations(int player) {
        return citations[player];
    }

    public PlayerStatus getStatus(int player) {
        return status[player];
    }

    public Decision getDecision(int player) {
        return decisions[player];
    }

    public boolean hasGoodwill(int player) {
        return goodwill[player];
    }

    public int getTrackScore(int player, LawnCard.Category category) {
        return trackScores[player][category.ordinal()];
    }

    public void setDecision(int player, Decision decision) {
        decisions[player] = decision;
    }

    /**
     * The most Citations the player can hold without receiving a Cease &amp; Desist.
     */
    public int getCitationLimit(int player) {
        LawnAndOrderParameters params = (LawnAndOrderParameters) gameParameters;
        int limit = lawns.get(player).getSize();
        if (goodwill[player])
            limit += params.goodwillBonus;
        if (isZeroTolerance())
            limit -= params.zeroToleranceReduction;
        return limit;
    }

    /**
     * Whether the player has reached the target score on every track.
     */
    public boolean hasReachedTarget(int player) {
        int target = ((LawnAndOrderParameters) gameParameters).targetScore;
        for (int score : trackScores[player])
            if (score < target)
                return false;
        return true;
    }

    /**
     * Whether a Zero Tolerance Policy has been revealed this round.
     */
    public boolean isZeroTolerance() {
        for (RuleCard r : revealedRules)
            if (r.special == RuleCard.Special.ZERO_TOLERANCE)
                return true;
        return false;
    }

    public boolean isCondemned(LawnCard.Attribute attribute) {
        for (RuleCard r : revealedRules)
            if (r.condemned == attribute)
                return true;
        return false;
    }

    /**
     * The active players who have not yet made this turn's choice: a card to play, or whether to continue.
     */
    public List<Integer> getPlayersStillToChoose() {
        List<Integer> players = new ArrayList<>();
        for (int p = 0; p < getNPlayers(); p++) {
            if (status[p] != PlayerStatus.ACTIVE) continue;
            boolean chosen = getGamePhase() == Phase.PLAY_OBJECT ? chosenCards.get(p).getSize() > 0
                    : decisions[p] != Decision.NONE;
            if (!chosen)
                players.add(p);
        }
        return players;
    }

    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        if (!isNotTerminal())
            return super.getCurrentSimultaneousPlayers();
        List<Integer> players = getPlayersStillToChoose();
        if (players.isEmpty())
            throw new AssertionError("Every active player has chosen but the turn has not been resolved");
        return players;
    }

    @Override
    protected LawnAndOrderGameState _copy(int playerId) {
        LawnAndOrderGameState copy = new LawnAndOrderGameState(gameParameters, getNPlayers());
        copy.drawDeck = drawDeck.copy();
        copy.hands = copyDecks(hands);
        copy.chosenCards = copyDecks(chosenCards);
        copy.lawns = copyDecks(lawns);
        copy.discardDeck = discardDeck.copy();
        copy.agenda = agenda.copy();
        copy.insiderTips = insiderTips.copy();
        copy.revealedRules = revealedRules.copy();
        copy.citations = citations.clone();
        copy.status = status.clone();
        copy.decisions = decisions.clone();
        copy.goodwill = goodwill.clone();
        copy.trackScores = new int[trackScores.length][];
        for (int p = 0; p < trackScores.length; p++)
            copy.trackScores[p] = trackScores[p].clone();
        return copy;
    }

    private static <T extends Component> List<Deck<T>> copyDecks(List<Deck<T>> decks) {
        List<Deck<T>> copies = new ArrayList<>(decks.size());
        for (Deck<T> d : decks)
            copies.add(d.copy());
        return copies;
    }

    @Override
    public void redeterminise(int playerId) {
        if (!isNotTerminal()) return;
        // The other players' choices this turn are face down, so in the copy they are still to choose, and a chosen
        // card goes back to its hand. The observer keeps their own choice.
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId) continue;
            if (chosenCards.get(p).getSize() > 0)
                hands.get(p).add(chosenCards.get(p).draw());
            decisions[p] = Decision.NONE;
        }
        // Hidden from the observer: the other hands, the draw deck, the busted hands of other players, the agenda and
        // the Insider Tips they do not sit beside
        List<Deck<LawnCard>> lawnDecks = new ArrayList<>(hands);
        lawnDecks.add(drawDeck);
        lawnDecks.add(discardDeck);
        DeterminisationUtilities.reshuffle(playerId, lawnDecks, c -> true, redeterminisationRnd);
        DeterminisationUtilities.reshuffle(playerId, List.of(agenda, insiderTips), c -> true, redeterminisationRnd);
        // All players choose at once, so the observer holds the turn if they are still to choose
        List<Integer> toChoose = getCurrentSimultaneousPlayers();
        setTurnOwner(toChoose.contains(playerId) ? playerId : toChoose.get(0));
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // progress towards the target on each track
        LawnAndOrderParameters params = (LawnAndOrderParameters) gameParameters;
        double progress = 0;
        for (int score : trackScores[playerId])
            progress += Math.min(score, params.targetScore);
        return progress / (params.targetScore * trackScores[playerId].length);
    }

    /**
     * The player's combined total over the three tracks.
     */
    @Override
    public double getGameScore(int playerId) {
        return Arrays.stream(trackScores[playerId]).sum();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LawnAndOrderGameState that)) return false;
        return Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(hands, that.hands) &&
                Objects.equals(chosenCards, that.chosenCards) &&
                Objects.equals(lawns, that.lawns) &&
                Objects.equals(discardDeck, that.discardDeck) &&
                Objects.equals(agenda, that.agenda) &&
                Objects.equals(insiderTips, that.insiderTips) &&
                Objects.equals(revealedRules, that.revealedRules) &&
                Arrays.equals(citations, that.citations) &&
                Arrays.equals(status, that.status) &&
                Arrays.equals(decisions, that.decisions) &&
                Arrays.equals(goodwill, that.goodwill) &&
                Arrays.deepEquals(trackScores, that.trackScores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), drawDeck, hands, chosenCards, lawns, discardDeck, agenda, insiderTips,
                revealedRules)
                + 31 * Arrays.hashCode(citations) + 37 * Arrays.hashCode(status) + 41 * Arrays.hashCode(decisions)
                + 43 * Arrays.hashCode(goodwill) + 47 * Arrays.deepHashCode(trackScores);
    }
}

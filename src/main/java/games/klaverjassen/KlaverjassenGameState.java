package games.klaverjassen;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.tricktaking.ITrickTakingState;
import games.tricktaking.KnownVoids;
import games.tricktaking.Trick;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Four players in two fixed partnerships: players 0 and 2 are team 0, players 1 and 3 team 1.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>currentTrick - the trick in progress, VISIBLE_TO_ALL, ordered by a {@link KlaverjassenCardOrder}</li>
 *     <li>discardPile - cards from completed tricks this hand, VISIBLE_TO_ALL</li>
 *     <li>trumpSuit - the trump suit of this hand, or null before it is chosen</li>
 *     <li>handPoints - each team's card points this hand, including the last trick bonus</li>
 *     <li>handRoem - each team's roem (bonus points for combinations in tricks, and the pit) this hand</li>
 *     <li>tricksWon - the tricks each team has won this hand</li>
 *     <li>teamScores - the points each team has scored for completed hands</li>
 *     <li>knownVoids - the suits each player is publicly known not to hold this hand</li>
 * </ul>
 */
public class KlaverjassenGameState extends AbstractGameState implements ITrickTakingState {

    List<Deck<FrenchCard>> playerHands;
    Trick currentTrick;
    Deck<FrenchCard> discardPile;
    FrenchCard.Suite trumpSuit;
    int[] handPoints;
    int[] handRoem;
    int[] tricksWon;
    int[] teamScores;
    KnownVoids knownVoids;

    public KlaverjassenGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        nTeams = 2;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Klaverjassen;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(currentTrick);
        components.add(discardPile);
        return components;
    }

    @Override
    public int getTeam(int player) {
        return player % 2;
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    @Override
    public Deck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    @Override
    public Trick getCurrentTrick() {
        return currentTrick;
    }

    @Override
    public KnownVoids getKnownVoids() {
        return knownVoids;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    public int getDealer() {
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    /**
     * The player who chooses trumps this hand, and then leads the first trick.
     */
    public int getTrumpChooser() {
        // the player on the dealer's left
        return (getDealer() + 1) % getNPlayers();
    }

    public FrenchCard.Suite getTrumpSuit() {
        return trumpSuit;
    }

    /**
     * Also starts the first trick, led by the trump chooser.
     */
    public void setTrumpSuit(FrenchCard.Suite suit) {
        trumpSuit = suit;
        currentTrick = new Trick("CurrentTrick", getNPlayers(), getTrumpChooser(), new KlaverjassenCardOrder(suit));
    }

    public int getHandPoints(int team) {
        return handPoints[team];
    }

    public int getHandRoem(int team) {
        return handRoem[team];
    }

    public int getTricksWon(int team) {
        return tricksWon[team];
    }

    public int getTeamScore(int team) {
        return teamScores[team];
    }

    @Override
    protected KlaverjassenGameState _copy(int playerId) {
        KlaverjassenGameState copy = new KlaverjassenGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.currentTrick = currentTrick.copy();
        copy.discardPile = discardPile.copy();
        copy.trumpSuit = trumpSuit;
        copy.handPoints = handPoints.clone();
        copy.handRoem = handRoem.clone();
        copy.tricksWon = tricksWon.clone();
        copy.teamScores = teamScores.clone();
        copy.knownVoids = knownVoids.copy();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other players' hands are shuffled together, never giving a player a card of a suit they are known
            // to be void in
            DeterminisationUtilities.reshuffle(playerId, copy.playerHands, c -> true, redeterminisationRnd,
                    knownVoids::permits);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // the team's share of all the points scored so far, including this hand's (0.5 if none have been scored)
        int team = getTeam(playerId);
        double own = teamScores[team] + handPoints[team] + handRoem[team];
        double other = teamScores[1 - team] + handPoints[1 - team] + handRoem[1 - team];
        return own + other == 0 ? 0.5 : own / (own + other);
    }

    /**
     * The points scored by the player's team.
     */
    @Override
    public double getGameScore(int playerId) {
        return teamScores[getTeam(playerId)];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KlaverjassenGameState that)) return false;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(discardPile, that.discardPile) &&
                trumpSuit == that.trumpSuit &&
                Arrays.equals(handPoints, that.handPoints) &&
                Arrays.equals(handRoem, that.handRoem) &&
                Arrays.equals(tricksWon, that.tricksWon) &&
                Arrays.equals(teamScores, that.teamScores) &&
                Objects.equals(knownVoids, that.knownVoids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, currentTrick, discardPile,
                trumpSuit == null ? -1 : trumpSuit.ordinal(), knownVoids)
                + 31 * Arrays.hashCode(handPoints) + 961 * Arrays.hashCode(handRoem)
                + 29791 * Arrays.hashCode(tricksWon) + 923521 * Arrays.hashCode(teamScores);
    }
}

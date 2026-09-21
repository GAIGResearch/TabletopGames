package games.whist;

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
 * <p>Game state for Whist. Data-only: all initialisation and rule logic lives in {@link WhistForwardModel}.</p>
 *
 * <p>Four players in two fixed partnerships: players 0 and 2 are team 0, players 1 and 3 team 1.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>currentTrick - the trick in progress, VISIBLE_TO_ALL; its leader leads (or led) it</li>
 *     <li>discardPile - cards from completed tricks this deal, VISIBLE_TO_ALL</li>
 *     <li>trumpSuit - the trump suit of this deal, or null if there are no trumps</li>
 *     <li>trumpCard - the dealer's turned-up last card, or null if trumps were not set by a turned-up card. It is
 *     public knowledge that the dealer holds it until they play it</li>
 *     <li>tricksTaken - for each player, the tricks they have won this deal</li>
 *     <li>teamPoints - for each team, the points scored in completed deals</li>
 *     <li>knownVoids - the suits each player is publicly known not to hold this deal</li>
 * </ul>
 */
public class WhistGameState extends AbstractGameState implements ITrickTakingState {

    List<Deck<FrenchCard>> playerHands;
    Trick currentTrick;
    Deck<FrenchCard> discardPile;
    FrenchCard.Suite trumpSuit;
    FrenchCard trumpCard;
    int[] tricksTaken;
    int[] teamPoints;
    KnownVoids knownVoids;

    public WhistGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        nTeams = 2;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Whist;
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

    /**
     * The player who dealt this deal: the last player deals first, then the deal passes to the left.
     */
    public int getDealer() {
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    /**
     * The trump suit of this deal, or null if there are no trumps.
     */
    public FrenchCard.Suite getTrumpSuit() {
        return trumpSuit;
    }

    /**
     * The dealer's turned-up card, or null if trumps were not set by turning up a card.
     */
    public FrenchCard getTrumpCard() {
        return trumpCard;
    }

    public int getTricksTaken(int player) {
        return tricksTaken[player];
    }

    /**
     * The tricks won by the team this deal.
     */
    public int getTeamTricks(int team) {
        int tricks = 0;
        for (int p = 0; p < getNPlayers(); p++) {
            if (getTeam(p) == team)
                tricks += tricksTaken[p];
        }
        return tricks;
    }

    public int getTeamPoints(int team) {
        return teamPoints[team];
    }

    @Override
    protected WhistGameState _copy(int playerId) {
        WhistGameState copy = new WhistGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.currentTrick = currentTrick.copy();
        copy.discardPile = discardPile.copy();
        copy.trumpSuit = trumpSuit;
        copy.trumpCard = trumpCard;
        copy.tricksTaken = tricksTaken.clone();
        copy.teamPoints = teamPoints.clone();
        copy.knownVoids = knownVoids.copy();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // Other players' hands are unknown: shuffle them together, keeping hand sizes, never giving a player a
            // card of a suit they are known to be void in, and leaving the turned-up trump card with the dealer.
            Deck<FrenchCard> dealerHand = copy.playerHands.get(getDealer());
            boolean trumpCardHeld = playerId != getDealer() && trumpCard != null && dealerHand.contains(trumpCard);
            if (trumpCardHeld)
                dealerHand.remove(trumpCard);
            DeterminisationUtilities.reshuffle(playerId, copy.playerHands, c -> true, redeterminisationRnd,
                    knownVoids::permits);
            if (trumpCardHeld)
                dealerHand.add(trumpCard);
        }
        return copy;
    }

    /**
     * The final result once the game is over; otherwise the team's points as a fraction of the most it could have
     * scored in the deals played so far (7 per deal).
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        int dealsPlayed = getRoundCounter();
        return dealsPlayed == 0 ? 0.0 : teamPoints[getTeam(playerId)] / (7.0 * dealsPlayed);
    }

    /**
     * The points scored by the player's team.
     */
    @Override
    public double getGameScore(int playerId) {
        return teamPoints[getTeam(playerId)];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WhistGameState that)) return false;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(discardPile, that.discardPile) &&
                trumpSuit == that.trumpSuit &&
                Objects.equals(trumpCard, that.trumpCard) &&
                Arrays.equals(tricksTaken, that.tricksTaken) &&
                Arrays.equals(teamPoints, that.teamPoints) &&
                Objects.equals(knownVoids, that.knownVoids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, currentTrick, discardPile,
                trumpSuit == null ? -1 : trumpSuit.ordinal(), trumpCard, knownVoids)
                + 31 * Arrays.hashCode(tricksTaken) + 961 * Arrays.hashCode(teamPoints);
    }
}

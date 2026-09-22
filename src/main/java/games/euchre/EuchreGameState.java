package games.euchre;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.tricktaking.CardOrder;
import games.tricktaking.ITrickTakingState;
import games.tricktaking.KnownVoids;
import games.tricktaking.Trick;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>Game state for Euchre. Data-only: all initialisation and rule logic lives in {@link EuchreForwardModel}.</p>
 *
 * <p>Four players in two fixed partnerships: players 0 and 2 are team 0, players 1 and 3 team 1.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>kitty - the cards not dealt, face down, HIDDEN_TO_ALL.</li>
 *     <li>upCard - the card turned up after the deal. Determines starting suit for trump bidding.</li>
 *     <li>dealerDiscard - the card the dealer put into the kitty after taking the up-card, or null</li>
 *     <li>currentTrick - the trick in progress, VISIBLE_TO_ALL</li>
 *     <li>discardPile - cards from completed tricks this deal, VISIBLE_TO_ALL</li>
 *     <li>trumpSuit - the trump suit of this deal, or null while it is being chosen</li>
 *     <li>maker - the player who chose trumps, or -1 while they are being chosen</li>
 *     <li>alone - whether the maker is playing without their partner</li>
 *     <li>passes - the passes made so far while choosing trumps this deal</li>
 *     <li>tricksTaken - the tricks each player has won this deal</li>
 *     <li>teamPoints - the points each team has scored for completed deals</li>
 *     <li>knownVoids - the suits each player is publicly known not to hold this deal</li>
 * </ul>
 */
public class EuchreGameState extends AbstractGameState implements ITrickTakingState {

    List<Deck<FrenchCard>> playerHands;
    Deck<FrenchCard> kitty;
    FrenchCard upCard;
    FrenchCard dealerDiscard;
    Trick currentTrick;
    Deck<FrenchCard> discardPile;
    FrenchCard.Suite trumpSuit;
    int maker;
    boolean alone;
    int passes;
    int[] tricksTaken;
    int[] teamPoints;
    KnownVoids knownVoids;

    public EuchreGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        nTeams = 2;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Euchre;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(kitty);
        components.add(currentTrick);
        components.add(discardPile);
        return components;
    }

    @Override
    public int getTeam(int player) {
        return player % 2;
    }

    public int getPartner(int player) {
        return (player + 2) % getNPlayers();
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    @Override
    public Deck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public Deck<FrenchCard> getKitty() {
        return kitty;
    }

    public FrenchCard getUpCard() {
        return upCard;
    }

    public FrenchCard getDealerDiscard() {
        return dealerDiscard;
    }

    @Override
    public Trick getCurrentTrick() {
        return currentTrick;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    @Override
    public KnownVoids getKnownVoids() {
        return knownVoids;
    }

    /**
     * The player who dealt this deal: the last player deals first, then the deal passes to the left.
     */
    public int getDealer() {
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    /**
     * The trump suit of this deal, or null while it is being chosen.
     */
    public FrenchCard.Suite getTrumpSuit() {
        return trumpSuit;
    }

    /**
     * How the cards belong to suits and rank in this deal's tricks: {@link EuchreCardOrder} for the trump suit once
     * trumps are chosen (the bowers), {@link CardOrder#STANDARD} while they are being chosen.
     */
    public CardOrder getCardOrder() {
        return trumpSuit == null ? CardOrder.STANDARD : new EuchreCardOrder(trumpSuit);
    }

    /**
     * The player who chose trumps, or -1 while they are being chosen.
     */
    public int getMaker() {
        return maker;
    }

    public boolean isAlone() {
        return alone;
    }

    /**
     * The partner of a maker going alone takes no part in the play; -1 if nobody is sitting out.
     */
    public int getSittingOut() {
        return alone ? getPartner(maker) : -1;
    }

    public int getPasses() {
        return passes;
    }

    /**
     * Whether trumps are still being chosen, and players decide whether to take the up-card's suit as trumps (false
     * once all four players have passed).
     */
    public boolean isFirstBiddingRound() {
        return trumpSuit == null && passes < getNPlayers();
    }

    public void countPass() {
        passes++;
    }

    public void setTrumps(FrenchCard.Suite suit, int maker, boolean alone) {
        this.trumpSuit = suit;
        this.maker = maker;
        this.alone = alone;
    }

    public void setDealerDiscard(FrenchCard card) {
        this.dealerDiscard = card;
    }

    public int getTricksTaken(int player) {
        return tricksTaken[player];
    }

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
    protected EuchreGameState _copy(int playerId) {
        EuchreGameState copy = new EuchreGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.kitty = kitty.copy();
        copy.upCard = upCard;
        copy.dealerDiscard = dealerDiscard;
        copy.currentTrick = currentTrick.copy();
        copy.discardPile = discardPile.copy();
        copy.trumpSuit = trumpSuit;
        copy.maker = maker;
        copy.alone = alone;
        copy.passes = passes;
        copy.tricksTaken = tricksTaken.clone();
        copy.teamPoints = teamPoints.clone();
        copy.knownVoids = knownVoids.copy();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // Other players' hands and the kitty are unknown, except for the up-card (seen by all, and then either
            // turned down in the kitty or taken by the dealer) and, for the dealer, the card they discarded.
            // Those stay where they are; the rest are shuffled together, keeping deck sizes and known voids.
            // If the dealer discarded the up-card itself, the other players cannot tell whether the dealer still holds
            // it, so for them it is shuffled with the rest.
            Set<FrenchCard> known = new HashSet<>();
            if (playerId == getDealer()) {
                known.add(upCard);
                if (dealerDiscard != null)
                    known.add(dealerDiscard);
            } else if (!upCard.equals(dealerDiscard)) {
                known.add(upCard);
            }
            List<Deck<FrenchCard>> decks = new ArrayList<>(copy.playerHands);
            decks.add(copy.kitty);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> !known.contains(c), redeterminisationRnd,
                    knownVoids.permits(getCardOrder()));
        }
        return copy;
    }

    /**
     * The final result once the game is over; otherwise the team's points as a fraction of the target score.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        int target = ((EuchreParameters) gameParameters).targetScore;
        return Math.min(1.0, teamPoints[getTeam(playerId)] / (double) target);
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
        if (!(o instanceof EuchreGameState that)) return false;
        return maker == that.maker && alone == that.alone && passes == that.passes &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(kitty, that.kitty) &&
                Objects.equals(upCard, that.upCard) &&
                Objects.equals(dealerDiscard, that.dealerDiscard) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(discardPile, that.discardPile) &&
                trumpSuit == that.trumpSuit &&
                Arrays.equals(tricksTaken, that.tricksTaken) &&
                Arrays.equals(teamPoints, that.teamPoints) &&
                Objects.equals(knownVoids, that.knownVoids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, kitty, upCard, dealerDiscard, currentTrick, discardPile,
                trumpSuit == null ? -1 : trumpSuit.ordinal(), maker, alone, passes, knownVoids)
                + 31 * Arrays.hashCode(tricksTaken) + 961 * Arrays.hashCode(teamPoints);
    }
}

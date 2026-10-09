package games.sueca;

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
 *     <li>currentTrick - the trick in progress, VISIBLE_TO_ALL, ordered by {@link SuecaUtils#CARD_ORDER}</li>
 *     <li>teamPiles - one Deck per team of the cards in the tricks it has won this deal, VISIBLE_TO_ALL</li>
 *     <li>trumpCard - the dealer's card turned face up to set trumps this deal; its suit is trumps</li>
 *     <li>knownVoids - the suits each player is publicly known not to hold this deal</li>
 *     <li>teamGames - the games each team has scored in the rubber (SuecaParameters.playRubber)</li>
 *     <li>extraGames - the extra games the next deal is worth, after deals tied at 60 points each</li>
 * </ul>
 */
public class SuecaGameState extends AbstractGameState implements ITrickTakingState<FrenchCard, FrenchCard.Suite> {

    List<Deck<FrenchCard>> playerHands;
    Trick<FrenchCard, FrenchCard.Suite> currentTrick;
    List<Deck<FrenchCard>> teamPiles;
    FrenchCard trumpCard;
    KnownVoids<FrenchCard.Suite> knownVoids;
    int[] teamGames;
    int extraGames;

    public SuecaGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        nTeams = 2;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Sueca;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(currentTrick);
        components.addAll(teamPiles);
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
    public Trick<FrenchCard, FrenchCard.Suite> getCurrentTrick() {
        return currentTrick;
    }

    @Override
    public KnownVoids<FrenchCard.Suite> getKnownVoids() {
        return knownVoids;
    }

    public Deck<FrenchCard> getTeamPile(int team) {
        return teamPiles.get(team);
    }

    public int getDealer() {
        // the last player deals first, so player 0 leads; the deal then passes to the next player each deal
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    /**
     * The player who leads the first trick of the deal.
     */
    public int getFirstLeader() {
        return (getDealer() + 1) % getNPlayers();
    }

    public FrenchCard getTrumpCard() {
        return trumpCard;
    }

    /**
     * Whether the dealer still holds the trump card.
     */
    public boolean isTrumpCardHeld() {
        return playerHands.get(getDealer()).contains(trumpCard);
    }

    public FrenchCard.Suite getTrumpSuit() {
        return trumpCard == null ? null : trumpCard.suite;
    }

    /**
     * The card points the team has won this deal.
     */
    public int getCardPoints(int team) {
        SuecaParameters params = (SuecaParameters) gameParameters;
        return teamPiles.get(team).getComponents().stream().mapToInt(params::cardPoints).sum();
    }

    /**
     * The tricks the team has won this deal.
     */
    public int getTricksWon(int team) {
        return teamPiles.get(team).getSize() / getNPlayers();
    }

    public int getTeamGames(int team) {
        return teamGames[team];
    }

    public int getExtraGames() {
        return extraGames;
    }

    @Override
    protected SuecaGameState _copy(int playerId) {
        SuecaGameState copy = new SuecaGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.currentTrick = currentTrick.copy();
        copy.teamPiles = new ArrayList<>();
        for (Deck<FrenchCard> pile : teamPiles)
            copy.teamPiles.add(pile.copy());
        copy.trumpCard = trumpCard;
        copy.knownVoids = knownVoids.copy();
        copy.teamGames = teamGames.clone();
        copy.extraGames = extraGames;

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other players' hands are shuffled together, never giving a player a card of a suit they are known
            // to be void in. The dealer keeps the trump card, which everyone saw, until they play it
            Deck<FrenchCard> dealerHand = copy.playerHands.get(getDealer());
            boolean keepTrumpCard = playerId != getDealer() && dealerHand.contains(trumpCard);
            if (keepTrumpCard)
                dealerHand.remove(trumpCard);
            DeterminisationUtilities.reshuffle(playerId, copy.playerHands, c -> true, redeterminisationRnd,
                    knownVoids.permits(SuecaUtils.CARD_ORDER));
            if (keepTrumpCard)
                dealerHand.add(trumpCard);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        SuecaParameters params = (SuecaParameters) gameParameters;
        // in the rubber, the team's share of the games needed to win it
        if (params.playRubber)
            return Math.min(1.0, (double) teamGames[getTeam(playerId)] / params.targetGames);
        // in a single deal, the share of the pack's 120 card points the player's team has won
        return getCardPoints(getTeam(playerId)) / 120.0;
    }

    /**
     * The card points the player's team has won this deal, or with SuecaParameters.playRubber, the games it has
     * scored in the rubber.
     */
    @Override
    public double getGameScore(int playerId) {
        int team = getTeam(playerId);
        return ((SuecaParameters) gameParameters).playRubber ? teamGames[team] : getCardPoints(team);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuecaGameState that)) return false;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(teamPiles, that.teamPiles) &&
                Objects.equals(trumpCard, that.trumpCard) &&
                Objects.equals(knownVoids, that.knownVoids) &&
                Arrays.equals(teamGames, that.teamGames) &&
                extraGames == that.extraGames;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, currentTrick, teamPiles, trumpCard, knownVoids, extraGames)
                + 31 * Arrays.hashCode(teamGames);
    }
}

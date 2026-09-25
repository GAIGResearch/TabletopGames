package games.pitch;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
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
 * Pitch: four players in two partnerships, players 0 and 2 (team 0) against players 1 and 3 (team 1).
 */
public class PitchGameState extends AbstractGameState implements ITrickTakingState {

    public enum Phase implements IGamePhase {
        BIDDING,
        PLAYING
    }

    List<Deck<FrenchCard>> playerHands;
    // the cards not dealt this deal (face down, never used)
    Deck<FrenchCard> undealtDeck;
    // the trick being played; before the first trick, an empty one led by the first bidder
    Trick currentTrick;
    // the cards of all the tricks each team has won this deal
    List<Deck<FrenchCard>> teamTricks;
    // each player's bid this deal: -1 not yet bid, 0 passed, otherwise the bid
    int[] playerBids;
    // the player holding the highest bid (-1 while nobody has bid)
    int pitcher = -1;
    // null until the first card of the deal is led
    FrenchCard.Suite trumpSuit;
    int[] teamScores;
    // the suits each player is publicly known to hold none of this deal
    KnownVoids knownVoids;

    public PitchGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        nTeams = 2;
        playerHands = new ArrayList<>();
        teamTricks = new ArrayList<>();
        playerBids = new int[nPlayers];
        Arrays.fill(playerBids, -1);
        teamScores = new int[2];
        knownVoids = new KnownVoids(nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Pitch;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerHands);
        components.addAll(teamTricks);
        if (currentTrick != null) components.add(currentTrick);
        if (undealtDeck != null) components.add(undealtDeck);
        return components;
    }

    @Override
    protected PitchGameState _copy(int playerId) {
        PitchGameState copy = new PitchGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.undealtDeck = undealtDeck == null ? null : undealtDeck.copy();
        copy.currentTrick = currentTrick == null ? null : currentTrick.copy();
        copy.teamTricks = new ArrayList<>();
        for (Deck<FrenchCard> tricks : teamTricks)
            copy.teamTricks.add(tricks.copy());
        copy.playerBids = playerBids.clone();
        copy.pitcher = pitcher;
        copy.trumpSuit = trumpSuit;
        copy.teamScores = teamScores.clone();
        copy.knownVoids = knownVoids.copy();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other hands and the undealt cards are hidden from playerId; shuffle them together
            List<Deck<FrenchCard>> hidden = new ArrayList<>();
            for (int p = 0; p < getNPlayers(); p++)
                if (p != playerId) hidden.add(copy.playerHands.get(p));
            hidden.add(copy.undealtDeck);
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd,
                    ((PitchParameters) gameParameters).rememberVoids ? copy.knownVoids::permits : null);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        int lead = teamScores[getTeam(playerId)] - teamScores[1 - getTeam(playerId)];
        return 0.5 + 0.5 * Math.tanh(lead / 10.0);
    }

    @Override
    public double getGameScore(int playerId) {
        return teamScores[getTeam(playerId)];
    }

    @Override
    public int getTeam(int player) {
        return player % 2;
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    public Deck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    public Deck<FrenchCard> getUndealtDeck() {
        return undealtDeck;
    }

    @Override
    public Trick getCurrentTrick() {
        return currentTrick;
    }

    public Deck<FrenchCard> getTeamTricks(int team) {
        return teamTricks.get(team);
    }

    public int getTricksWon(int team) {
        return teamTricks.get(team).getSize() / getNPlayers();
    }

    public int getPlayerBid(int player) {
        return playerBids[player];
    }

    public void setPlayerBid(int player, int bid) {
        playerBids[player] = bid;
    }

    public void setPitcher(int player) {
        pitcher = player;
    }

    public void setTrumpSuit(FrenchCard.Suite suit) {
        trumpSuit = suit;
    }

    public int getPitcher() {
        return pitcher;
    }

    /**
     * @return the highest bid this deal, or 0 if nobody has bid
     */
    public int getHighestBid() {
        return pitcher < 0 ? 0 : playerBids[pitcher];
    }

    public FrenchCard.Suite getTrumpSuit() {
        return trumpSuit;
    }

    public int getTeamScore(int team) {
        return teamScores[team];
    }

    /**
     * Player 3 deals first, so player 0 bids first; the deal then passes to the left.
     */
    public int getDealer() {
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    @Override
    public KnownVoids getKnownVoids() {
        return knownVoids;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof PitchGameState that)) return false;
        return pitcher == that.pitcher &&
                trumpSuit == that.trumpSuit &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(undealtDeck, that.undealtDeck) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(teamTricks, that.teamTricks) &&
                Arrays.equals(playerBids, that.playerBids) &&
                Arrays.equals(teamScores, that.teamScores) &&
                Objects.equals(knownVoids, that.knownVoids);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), pitcher, trumpSuit == null ? -1 : trumpSuit.ordinal(),
                playerHands, undealtDeck, currentTrick, teamTricks, knownVoids);
        result = 31 * result + Arrays.hashCode(playerBids);
        result = 31 * result + Arrays.hashCode(teamScores);
        return result;
    }
}

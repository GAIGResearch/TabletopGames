package games.spades;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.tricktaking.ITrickTakingState;
import games.tricktaking.KnownVoids;
import games.tricktaking.Trick;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SpadesGameState extends AbstractGameState implements IPrintable, ITrickTakingState {

    List<Deck<FrenchCard>> playerHands;
    public Trick currentTrick;
    public List<List<Deck<FrenchCard>>> tricksWon;
    public int[] playerBids;
    public int[] tricksTaken;
    public int[] teamScores;
    public int[] teamSandbags;
    public boolean[] playerBlindNil;
    public boolean spadesBroken = false;
    /**
     * For each player, the suits that they are publicly known to be void in; i.e. the suits that
     * were led in a trick this round to which they did not follow suit.
     */
    public KnownVoids knownVoids;

    public enum Phase implements IGamePhase {
        BIDDING,
        PLAYING
    }

    public SpadesGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);

        this.nTeams = 2;

        playerHands = new ArrayList<>();
        tricksWon = new ArrayList<>();
        playerBids = new int[nPlayers];
        Arrays.fill(playerBids, -1); // -1 means not bid yet
        tricksTaken = new int[nPlayers];
        teamScores = new int[2];
        teamSandbags = new int[2];
        playerBlindNil = new boolean[nPlayers];
        knownVoids = new KnownVoids(nPlayers);
        currentTrick = new Trick("CurrentTrick", nPlayers, 0);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Spades;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();

        for (Deck<FrenchCard> hand : playerHands) {
            components.add(hand);
            components.addAll(hand.getComponents());
        }

        for (List<Deck<FrenchCard>> playerTricks : tricksWon) {
            for (Deck<FrenchCard> trick : playerTricks) {
                components.add(trick);
                components.addAll(trick.getComponents());
            }
        }

        components.add(currentTrick);
        components.addAll(currentTrick.getComponents());

        return components;
    }

    @Override
    protected SpadesGameState _copy(int playerId) {
        SpadesGameState copy = new SpadesGameState(gameParameters, getNPlayers());

        copy.currentTrick = currentTrick.copy();

        copy.tricksWon = new ArrayList<>();
        for (List<Deck<FrenchCard>> playerTricks : tricksWon) {
            List<Deck<FrenchCard>> copyTricks = new ArrayList<>(playerTricks);
            copy.tricksWon.add(copyTricks);
        }

        copy.playerBids = Arrays.copyOf(playerBids, playerBids.length);
        copy.tricksTaken = Arrays.copyOf(tricksTaken, tricksTaken.length);
        copy.teamScores = Arrays.copyOf(teamScores, teamScores.length);
        copy.teamSandbags = Arrays.copyOf(teamSandbags, teamSandbags.length);
        copy.playerBlindNil = Arrays.copyOf(playerBlindNil, playerBlindNil.length);
        copy.spadesBroken = spadesBroken;

        copy.knownVoids = knownVoids.copy();

        copy.playerHands = new ArrayList<>();
        for (int i = 0; i < playerHands.size(); i++) {
            Deck<FrenchCard> originalHand = playerHands.get(i);
            Deck<FrenchCard> copiedHand = originalHand.copy();
            copy.playerHands.add(copiedHand);
        }
        // Now redeterminise
        if (playerId != -1) {
            // we reshuffle all other player hands
            List<Deck<FrenchCard>> otherPlayerDecks = new ArrayList<>();
            for (int p = 0; p < playerHands.size(); p++) {
                if (p != playerId)
                    otherPlayerDecks.add(copy.playerHands.get(p));
            }
            // a player who has failed to follow suit is known to hold no cards of that suit,
            // so we must not deal them any (none are recorded if SpadesParameters.rememberVoids is off)
            DeterminisationUtilities.reshuffle(playerId, otherPlayerDecks, x -> true, redeterminisationRnd,
                    copy.knownVoids::permits);
        }

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            SpadesHeuristic heuristic = new SpadesHeuristic();
            return heuristic.evaluateState(this, playerId);
        } else {
            return getPlayerResults()[playerId].value;
        }
    }

    @Override
    public double getGameScore(int playerId) {
        int team = getTeam(playerId);
        return teamScores[team];
    }

    public int getTeam(int playerId) {
        return playerId % 2;
    }

    public int getNTeams() {
        return 2;
    }

    public List<Deck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    @Override
    public Deck<FrenchCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    /**
     * The suits that each player is publicly known to be void in, from having failed to
     * follow suit earlier in the current round. This is information available to all players.
     */
    @Override
    public KnownVoids getKnownVoids() {
        return knownVoids;
    }

    public int getPlayerBid(int playerId) {
        return playerBids[playerId];
    }

    public void setPlayerBid(int playerId, int bid) {
        playerBids[playerId] = bid;
    }

    public boolean allPlayersBid() {
        for (int bid : playerBids) {
            if (bid == -1) return false;
        }
        return true;
    }

    public int getTricksTaken(int playerId) {
        return tricksTaken[playerId];
    }

    public void incrementTricksTaken(int playerId) {
        tricksTaken[playerId]++;
    }

    public int getTeamScore(int team) {
        return teamScores[team];
    }

    public void setTeamScore(int team, int score) {
        teamScores[team] = score;
    }

    public int getTeamSandbags(int team) {
        return teamSandbags[team];
    }

    public void addTeamSandbags(int team, int sandbags) {
        teamSandbags[team] += sandbags;
    }

    public boolean isSpadesBroken() {
        return spadesBroken;
    }

    public void setSpadesBroken(boolean broken) {
        this.spadesBroken = broken;
    }

    @Override
    public Trick getCurrentTrick() {
        return currentTrick;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpadesGameState that)) return false;
        if (!super.equals(o)) return false;

        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(tricksWon, that.tricksWon) &&
                Arrays.equals(playerBids, that.playerBids) &&
                Arrays.equals(tricksTaken, that.tricksTaken) &&
                Arrays.equals(teamScores, that.teamScores) &&
                Arrays.equals(teamSandbags, that.teamSandbags) &&
                Arrays.equals(playerBlindNil, that.playerBlindNil) &&
                spadesBroken == that.spadesBroken &&
                Objects.equals(knownVoids, that.knownVoids);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerHands, currentTrick, tricksWon, spadesBroken, knownVoids);
        result = 31 * result + Arrays.hashCode(playerBids);
        result = 31 * result + Arrays.hashCode(tricksTaken);
        result = 31 * result + Arrays.hashCode(teamScores);
        result = 31 * result + Arrays.hashCode(teamSandbags) + 31 * 31 * Arrays.hashCode(playerBlindNil);
        return result;
    }

    @Override
    public void printToConsole() {
        System.out.println("=== SPADES GAME STATE ===");
        System.out.println("Round: " + getRoundCounter() + ", Turn: " + getTurnCounter());
        System.out.println("Phase: " + gamePhase + ", Current Player: " + getCurrentPlayer());
        System.out.println("Spades Broken: " + spadesBroken);

        // Current trick information
        if (currentTrick.getSize() > 0) {
            System.out.println("\nCurrent Trick (Lead Suit: " + currentTrick.getLeadSuit() + "):");
            for (int i = 0; i < currentTrick.getSize(); i++) {
                System.out.println("  Player " + currentTrick.playerOf(i) + ": " + currentTrick.get(i));
            }
        }

        // Player information
        System.out.println("\nPLAYERS:");
        for (int i = 0; i < getNPlayers(); i++) {
            String marker = (i == getCurrentPlayer()) ? ">>> " : "    ";
            System.out.print(marker + "Player " + i + " (Team " + getTeam(i) + ")");

            // Bid information
            if (playerBids[i] != -1) {
                String bidText = (playerBids[i] == 0) ? "Nil" : String.valueOf(playerBids[i]);
                System.out.print(" - Bid: " + bidText);
            } else {
                System.out.print(" - Bid: Not set");
            }

            // Tricks taken
            System.out.print(", Tricks: " + tricksTaken[i]);

            // Hand size
            System.out.println(", Cards: " + playerHands.get(i).getSize());

            // Show actual cards for current player or if game is over
            if (i == getCurrentPlayer() || !isNotTerminal()) {
                System.out.println(marker + "Hand: " + playerHands.get(i).toString());
            }
        }

        // Team scores
        System.out.println("\nTEAM SCORES:");
        System.out.println("Team 0 (Players 0 & 2): " + teamScores[0] + " points, " + teamSandbags[0] + " sandbags");
        System.out.println("Team 1 (Players 1 & 3): " + teamScores[1] + " points, " + teamSandbags[1] + " sandbags");

        // Game status
        if (!isNotTerminal()) {
            System.out.println("\nGAME OVER!");
            for (int i = 0; i < getNPlayers(); i++) {
                System.out.println("Player " + i + " result: " + getPlayerResults()[i]);
            }
        }

        System.out.println("========================");
    }
} 
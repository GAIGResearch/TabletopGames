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
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpadesGameState extends AbstractGameState implements IPrintable {

    List<Deck<FrenchCard>> playerHands;
    public List<Map.Entry<Integer, FrenchCard>> currentTrick = new ArrayList<>();
    public List<List<Deck<FrenchCard>>> tricksWon;
    public int[] playerBids;
    public int[] tricksTaken;
    public int[] teamScores;
    public int[] teamSandbags;
    public boolean[] playerBlindNil;
    public int leadPlayer;
    public Phase gamePhase = Phase.BIDDING;
    public FrenchCard.Suite leadSuit;
    public boolean spadesBroken = false;
    
    public enum Phase implements IGamePhase {
        BIDDING,
        PLAYING
    }
    
    public SpadesGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);

        this.nTeams = 2;

        playerHands = new ArrayList<>();
        tricksWon = new ArrayList<>();
        playerBids = new int[4];
        Arrays.fill(playerBids, -1); // -1 means not bid yet
        tricksTaken = new int[4];
        teamScores = new int[2];
        teamSandbags = new int[2];
        playerBlindNil = new boolean[4];
        leadPlayer = 0;
        
        for (int i = 0; i < 4; i++) {
            Deck<FrenchCard> hand = new Deck<>("Player" + i + "Hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            hand.setOwnerId(i);
            playerHands.add(hand);
            tricksWon.add(new ArrayList<>());
        }
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
        
        for (Map.Entry<Integer, FrenchCard> entry : currentTrick) {
            components.add(entry.getValue());
        }
        
        return components;
    }
    
    @Override
    protected SpadesGameState _copy(int playerId) {
        SpadesGameState copy = new SpadesGameState(gameParameters, getNPlayers());

        copy.currentTrick = new ArrayList<>();
        for (Map.Entry<Integer, FrenchCard> entry : currentTrick) {
            copy.currentTrick.add(new HashMap.SimpleEntry<>(entry.getKey(), entry.getValue().copy()));
        }

        copy.tricksWon = new ArrayList<>();
        for (List<Deck<FrenchCard>> playerTricks : tricksWon) {
            List<Deck<FrenchCard>> copyTricks = new ArrayList<>();
            for (Deck<FrenchCard> trick : playerTricks) {
                copyTricks.add(trick.copy());
            }
            copy.tricksWon.add(copyTricks);
        }

        copy.playerBids = Arrays.copyOf(playerBids, playerBids.length);
        copy.tricksTaken = Arrays.copyOf(tricksTaken, tricksTaken.length);
        copy.teamScores = Arrays.copyOf(teamScores, teamScores.length);
        copy.teamSandbags = Arrays.copyOf(teamSandbags, teamSandbags.length);
        copy.playerBlindNil = Arrays.copyOf(playerBlindNil, playerBlindNil.length);
        copy.leadPlayer = leadPlayer;
        copy.gamePhase = gamePhase;
        copy.leadSuit = leadSuit;
        copy.spadesBroken = spadesBroken;

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
            DeterminisationUtilities.reshuffle(playerId, otherPlayerDecks, x -> true, redeterminisationRnd);
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
    
    public Phase getSpadesGamePhase() {
        return gamePhase;
    }
    
    public void setSpadesGamePhase(Phase phase) {
        this.gamePhase = phase;
    }
    
    public boolean isSpadesBroken() {
        return spadesBroken;
    }
    
    public void setSpadesBroken(boolean broken) {
        this.spadesBroken = broken;
    }
    
    public FrenchCard.Suite getLeadSuit() {
        return leadSuit;
    }
    
    public void setLeadSuit(FrenchCard.Suite suit) {
        this.leadSuit = suit;
    }
    
    public int getLeadPlayer() {
        return leadPlayer;
    }
    
    public void setLeadPlayer(int playerId) {
        this.leadPlayer = playerId;
    }
    
    public List<Map.Entry<Integer, FrenchCard>> getCurrentTrick() {
        return currentTrick;
    }
    
    public void clearCurrentTrick() {
        currentTrick.clear();
        leadSuit = null;
    }
    
    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpadesGameState)) return false;
        if (!super.equals(o)) return false;
        
        SpadesGameState that = (SpadesGameState) o;
        return Objects.equals(playerHands, that.playerHands) &&
               Objects.equals(currentTrick, that.currentTrick) &&
               Objects.equals(tricksWon, that.tricksWon) &&
               Arrays.equals(playerBids, that.playerBids) &&
               Arrays.equals(tricksTaken, that.tricksTaken) &&
               Arrays.equals(teamScores, that.teamScores) &&
               Arrays.equals(teamSandbags, that.teamSandbags) &&
               leadPlayer == that.leadPlayer &&
               gamePhase == that.gamePhase &&
               leadSuit == that.leadSuit &&
               spadesBroken == that.spadesBroken;
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerHands, currentTrick, tricksWon, 
                                  leadPlayer, gamePhase, leadSuit, spadesBroken);
        result = 31 * result + Arrays.hashCode(playerBids);
        result = 31 * result + Arrays.hashCode(tricksTaken);
        result = 31 * result + Arrays.hashCode(teamScores);
        result = 31 * result + Arrays.hashCode(teamSandbags);
        return result;
    }
    
    @Override
    public void printToConsole() {
        System.out.println("=== SPADES GAME STATE ===");
        System.out.println("Round: " + getRoundCounter() + ", Turn: " + getTurnCounter());
        System.out.println("Phase: " + gamePhase + ", Current Player: " + getCurrentPlayer());
        System.out.println("Spades Broken: " + spadesBroken);
        
        // Current trick information
        if (!currentTrick.isEmpty()) {
            System.out.println("\nCurrent Trick (Lead Suit: " + leadSuit + "):");
            for (Map.Entry<Integer, FrenchCard> entry : currentTrick) {
                System.out.println("  Player " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // Player information
        System.out.println("\nPLAYERS:");
        for (int i = 0; i < 4; i++) {
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
            for (int i = 0; i < 4; i++) {
                System.out.println("Player " + i + " result: " + getPlayerResults()[i]);
            }
        }
        
        System.out.println("========================");
    }
} 
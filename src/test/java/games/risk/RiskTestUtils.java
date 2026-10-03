package games.risk;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

class RiskTestUtils {

    static final List<RiskTerritory> AUSTRALIA = List.of(WorldMap.INDONESIA, WorldMap.NEW_GUINEA,
            WorldMap.WESTERN_AUSTRALIA, WorldMap.EASTERN_AUSTRALIA);

    private RiskTestUtils() {
    }

    /** A game with random players, and default parameters when params is null. */
    static Game newGame(int nPlayers, long seed, RiskParameters params) {
        Game game = GameType.Risk.createGameInstance(nPlayers, seed, params == null ? new RiskParameters() : params);
        List<AbstractPlayer> players = IntStream.range(0, nPlayers)
                .mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList();
        game.reset(players);
        return game;
    }

    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /** A directly instantiated state, set up with the given seed. */
    static RiskGameState newState(int nPlayers, long seed, RiskParameters params) {
        RiskParameters p = params == null ? new RiskParameters() : params;
        p.setRandomSeed(seed);
        RiskGameState state = new RiskGameState(p, nPlayers);
        new RiskForwardModel().setup(state);
        return state;
    }

    /** Gives every territory to the player, with 1 army each. */
    static void fillBoard(RiskGameState state, int player) {
        for (RiskTerritory t : WorldMap.ALL)
            give(state, player, 1, t);
    }

    /** Gives the territories to the player, each with exactly the given number of armies. */
    static void give(RiskGameState state, int player, int armies, RiskTerritory... territories) {
        for (RiskTerritory t : territories) {
            state.setOwner(t, player);
            state.addArmies(t, armies - state.getArmies(t));
        }
    }

    static void give(RiskGameState state, int player, int armies, Collection<RiskTerritory> territories) {
        give(state, player, armies, territories.toArray(new RiskTerritory[0]));
    }

    /**
     * Starts the player's turn in the given phase, with armiesToPlace armies to place. Nobody has armies left from the
     * initial placement.
     */
    static void startPlay(RiskGameState state, int player, RiskGamePhase phase, int armiesToPlace) {
        for (int p = 0; p < state.getNPlayers(); p++)
            state.setArmiesToPlace(p, 0);
        state.setArmiesToPlace(player, armiesToPlace);
        state.setTurnOwner(player);
        state.setGamePhase(phase);
    }

    /** Marks the player as already out of the game, in the given place. */
    static void eliminate(RiskGameState state, int player, int finalPlace) {
        state.setFinalPlace(player, finalPlace);
        state.setPlayerResult(core.CoreConstants.GameResult.LOSE_GAME, player);
    }

    static Set<AbstractAction> actionSet(RiskForwardModel fm, RiskGameState state) {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<AbstractAction> set = new HashSet<>(actions);
        assertTrue("duplicate actions: " + actions, set.size() == actions.size());
        return set;
    }

    /** The RISK card of the territory (value-equal to the one in the deck). */
    static RiskCard card(RiskTerritory t) {
        return new RiskCard(t);
    }

    /** A wild card (all wild cards are equal). */
    static RiskCard wild() {
        return new RiskCard();
    }

    /** Moves the given cards from the draw deck to the player's hand; fails if one is not in the draw deck. */
    static void giveCards(RiskGameState state, int player, RiskCard... cards) {
        for (RiskCard c : cards) {
            state.getDrawDeck().remove(c);
            state.getHand(player).add(c);
        }
    }

    /** Moves the given cards from the draw deck to the discard pile (as if traded in earlier). */
    static void discardCards(RiskGameState state, RiskCard... cards) {
        for (RiskCard c : cards) {
            state.getDrawDeck().remove(c);
            state.getDiscardPile().add(c);
        }
    }

    /** Moves the whole draw deck to the discard pile, leaving the draw deck empty. */
    static void discardWholeDeck(RiskGameState state) {
        while (state.getDrawDeck().getSize() > 0)
            state.getDiscardPile().add(state.getDrawDeck().draw());
    }

    /** Every card in the game: draw deck, then hands in player order, then the discard pile. */
    static List<RiskCard> allCards(RiskGameState state) {
        List<RiskCard> all = new ArrayList<>(state.getDrawDeck().getComponents());
        for (int p = 0; p < state.getNPlayers(); p++)
            all.addAll(state.getHand(p).getComponents());
        all.addAll(state.getDiscardPile().getComponents());
        return all;
    }

    /** All territories of the continent, in index order. */
    static List<RiskTerritory> continent(RiskContinent c) {
        return WorldMap.MAP.territories(c);
    }

    /** Default parameters with secretMission on. */
    static RiskParameters missionParams() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("secretMission", true);
        return params;
    }

    /** As eliminate(state, player, finalPlace), recording the player who eliminated them. */
    static void eliminate(RiskGameState state, int player, int finalPlace, int by) {
        eliminate(state, player, finalPlace);
        state.eliminatedBy[player] = by;
    }

    /** Gives the player the mission card, keeping all 14 mission cards in the game. */
    static void giveMission(RiskGameState state, int player, RiskMission mission) {
        RiskMission old = state.getMission(player);
        if (mission.equals(old)) return;
        // the card is taken from the player holding it, or else from the unused missions
        int holder = -1;
        for (int q = 0; q < state.getNPlayers(); q++)
            if (mission.equals(state.getMission(q))) holder = q;
        if (holder >= 0)
            state.missions.get(holder).clear();
        else if (state.getUnusedMissions().contains(mission))
            state.getUnusedMissions().remove(mission);
        state.setMission(player, mission);
        // setMission put the player's old card with the unused missions; a previous holder gets it instead
        if (holder >= 0 && old != null) {
            state.getUnusedMissions().remove(old);
            state.missions.get(holder).add(old);
        }
    }

    /** Gives each player p the mission perPlayer[p]. */
    static void giveMissions(RiskGameState state, RiskMission... perPlayer) {
        for (int p = 0; p < perPlayer.length; p++)
            giveMission(state, p, perPlayer[p]);
    }

    /** Every mission card in the game: each player's (in player order), then the unused ones. */
    static List<RiskMission> allMissionCards(RiskGameState state) {
        List<RiskMission> all = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++)
            all.addAll(state.missions.get(p).getComponents());
        all.addAll(state.getUnusedMissions().getComponents());
        return all;
    }

    /** Whether the two lists hold the same cards, ignoring order (each card counted). */
    static boolean sameCards(List<RiskMission> a, List<RiskMission> b) {
        List<String> x = new ArrayList<>(a.stream().map(RiskMission::toString).toList());
        List<String> y = new ArrayList<>(b.stream().map(RiskMission::toString).toList());
        Collections.sort(x);
        Collections.sort(y);
        return x.equals(y) && new HashSet<>(a).equals(new HashSet<>(b));
    }
}

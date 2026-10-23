package games.risk;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;
import org.junit.Test;

import java.util.*;

import static core.CoreConstants.GameResult.*;
import static games.risk.RiskTestUtils.newGame;
import static org.junit.Assert.*;

/**
 * Random-player games played to the end, checking invariants at every decision and the results against the rules.
 */
public class RiskGameplayTest {

    private static final int STEP_CAP = 200000;

    /** Plays the game at random to the end; returns the players seen starting a REINFORCE step. */
    private static Set<Integer> playOut(Game game, long seed) {
        return playOut(game, seed, new HashMap<>());
    }

    /** As playOut, counting the actions taken by class (simple name) in taken. */
    private static Set<Integer> playOut(Game game, long seed, Map<String, Integer> taken) {
        RiskGameState state = (RiskGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        Random rnd = new Random(seed);
        Set<Integer> reinforced = new HashSet<>();
        int steps = 0;
        while (state.isNotTerminal() && steps++ < STEP_CAP) {
            checkInvariants(state);
            if (state.getGamePhase() == RiskGamePhase.REINFORCE)
                reinforced.add(state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no actions in " + state.getGamePhase(), actions.isEmpty());
            checkCardActions(state, actions);
            checkDiceActions(state, actions);
            checkExpertActions(state, actions);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            taken.merge(action.getClass().getSimpleName(), 1, Integer::sum);
            step(state, fm, action, taken);
        }
        assertFalse("game did not end within " + STEP_CAP + " actions", state.isNotTerminal());
        checkResults(state);
        return reinforced;
    }

    private static void checkInvariants(RiskGameState state) {
        RiskGamePhase phase = (RiskGamePhase) state.getGamePhase();
        // a captured territory waits for its armies while the move-in choice is pending
        RiskTerritory awaitingMove = state.currentActionInProgress() instanceof MoveArmiesChoice c
                && phase == RiskGamePhase.ATTACK ? c.to : null;
        for (RiskTerritory t : WorldMap.ALL) {
            int owner = state.getOwner(t);
            if (owner < 0) {
                assertEquals("unclaimed " + t + " outside CLAIM", RiskGamePhase.CLAIM, phase);
                assertEquals(0, state.getArmies(t));
            } else if (!t.equals(awaitingMove)) {
                assertTrue(t + " has " + state.getArmies(t) + " armies", state.getArmies(t) >= 1);
                assertFalse(t + " held by an eliminated player", state.isEliminated(owner));
            }
        }
        assertFalse("eliminated player " + state.getCurrentPlayer() + " to act",
                state.isEliminated(state.getCurrentPlayer()));
        int limit = ((RiskParameters) state.getGameParameters()).maxArmiesPerTerritory;
        if (limit > 0)
            for (RiskTerritory t : WorldMap.ALL)
                assertTrue(t + " has " + state.getArmies(t) + " armies, above " + limit, state.getArmies(t) <= limit);
        checkCards(state);
        checkMissions(state);
    }

    /** The mission cards are conserved, and no mission win is missed. */
    private static void checkMissions(RiskGameState state) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int n = state.getNPlayers();
        // without secretMission nobody holds a mission
        if (!params.secretMission) {
            for (int p = 0; p < n; p++)
                assertNull(state.getMission(p));
            return;
        }
        // all 14 mission cards are in the game, and each player still in holds exactly one
        assertTrue("missions not conserved: " + RiskTestUtils.allMissionCards(state),
                RiskTestUtils.sameCards(WorldMissions.ALL_MISSIONS, RiskTestUtils.allMissionCards(state)));
        for (int p = 0; p < n; p++)
            if (!state.isEliminated(p))
                assertEquals("missions of player " + p, 1, state.missions.get(p).getSize());
        // once no follow-on choice is pending the forward model has checked for a win, so while the game goes on no
        // player still in has completed their mission
        if (state.isActionInProgress()) return;
        for (int p = 0; p < n; p++)
            if (!state.isEliminated(p))
                assertFalse("player " + p + " has completed " + state.getMission(p) + " but the game goes on",
                        missionDone(state, state.getMission(p), p));
    }

    /** Whether the holder has completed the mission, worked out from the rules independently of RiskMission. */
    private static boolean missionDone(RiskGameState state, RiskMission m, int holder) {
        RiskMap map = state.getMap();
        switch (m.kind) {
            case CONQUER -> {
                for (RiskContinent c : m.continents)
                    if (!holdsAll(state, holder, map.territories(c))) return false;
                if (!m.anotherContinent) return true;
                for (RiskContinent c : map.continents())
                    if (!m.continents.contains(c) && holdsAll(state, holder, map.territories(c))) return true;
                return false;
            }
            case OCCUPY -> {
                return held(state, holder, m.minArmies) >= m.nTerritories;
            }
            default -> {
                // a destroy mission whose target is the holder, not in the game, or eliminated by someone else
                // becomes the backup mission of holding the map's backupTerritories territories
                int k = m.target;
                boolean backup = k == holder || k >= state.getNPlayers()
                        || (state.isEliminated(k) && state.getEliminatedBy(k) != holder);
                if (backup) return held(state, holder, 1) >= map.backupTerritories();
                return state.isEliminated(k) && state.getEliminatedBy(k) == holder;
            }
        }
    }

    private static boolean holdsAll(RiskGameState state, int player, List<RiskTerritory> territories) {
        for (RiskTerritory t : territories)
            if (state.getOwner(t) != player) return false;
        return true;
    }

    /** The territories the player holds with at least minArmies armies. */
    private static int held(RiskGameState state, int player, int minArmies) {
        int n = 0;
        for (RiskTerritory t : WorldMap.ALL)
            if (state.getOwner(t) == player && state.getArmies(t) >= minArmies) n++;
        return n;
    }

    /**
     * Takes the action, checks its effect on the armies to place and the missions, and counts in taken the effects of
     * the expert rules.
     */
    private static void step(RiskGameState state, AbstractForwardModel fm, AbstractAction action,
                             Map<String, Integer> taken) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int n = state.getNPlayers();
        int actor = state.getCurrentPlayer();
        int turnOwner = state.getTurnOwner();
        int setsBefore = state.getNSetsTraded();
        int[] before = new int[n];
        for (int p = 0; p < n; p++) before[p] = state.getArmiesToPlace(p);

        RiskMission[] missionsBefore = new RiskMission[n];
        for (int p = 0; p < n; p++) missionsBefore[p] = state.getMission(p);

        fm.next(state, action);

        // a destroy card whose target someone else eliminated is replaced by the target's card
        for (int p = 0; p < n; p++)
            if (!state.isEliminated(p) && !Objects.equals(missionsBefore[p], state.getMission(p))) {
                assertEquals("player " + p + " changed mission from " + missionsBefore[p],
                        RiskMission.Kind.DESTROY, missionsBefore[p].kind);
                int k = missionsBefore[p].target;
                assertTrue(state.isEliminated(k));
                assertNotEquals(p, state.getEliminatedBy(k));
                assertTrue(state.getUnusedMissions().contains(missionsBefore[p]));
                taken.merge("missionTaken", 1, Integer::sum);
            }

        int lost = 0;
        for (int p = 0; p < n; p++) {
            int expected = before[p];
            if (p == actor && action instanceof PlaceArmy pa) expected -= pa.n;
            if (p == actor && action instanceof ClaimTerritory) expected--;
            if (p == actor && action instanceof TradeCards) {
                // the trade adds the value of the set, unless maxArmiesPerTerritory leaves no room for any of them
                assertEquals(setsBefore + 1, state.getNSetsTraded());
                expected += expectedTradeValue(params, setsBefore + 1);
                int after = state.getArmiesToPlace(p);
                assertTrue("trade of set " + (setsBefore + 1) + ": " + before[p] + " -> " + after,
                        after == expected || (params.maxArmiesPerTerritory > 0 && after == 0));
            }
            if (state.getArmiesToPlace(p) < expected) lost++;
        }
        // a new turn that goes straight to ATTACK: the reinforcements found no room
        if (state.isNotTerminal() && state.getTurnOwner() != turnOwner
                && state.getGamePhase() == RiskGamePhase.ATTACK && !state.isActionInProgress())
            lost++;
        // armies to place may vanish only with maxArmiesPerTerritory
        if (lost > 0) {
            assertTrue("armies lost without maxArmiesPerTerritory after " + action, params.maxArmiesPerTerritory > 0);
            taken.merge("lost", lost, Integer::sum);
        }
        if (action instanceof Fortify f && !state.getMap().adjacent(f.from, f.to))
            taken.merge("pathFortify", 1, Integer::sum);
        // a step after which some territory holds exactly the limit
        if (params.maxArmiesPerTerritory > 0)
            for (RiskTerritory t : WorldMap.ALL)
                if (state.getArmies(t) == params.maxArmiesPerTerritory) {
                    taken.merge("atLimit", 1, Integer::sum);
                    break;
                }
    }

    /** The value of the k-th set, worked out from the rules independently of RiskParameters.tradeValue. */
    private static int expectedTradeValue(RiskParameters params, int k) {
        int[] values = params.tradeValues;
        if (params.linearTradeValues) return values[0] + k - 1;
        if (k <= values.length) return values[k - 1];
        return values[values.length - 1] + (k - values.length) * params.tradeValueIncrement;
    }

    /** Checks the PlaceArmy, MoveArmies and Fortify actions offered against the expert rules. */
    private static void checkExpertActions(RiskGameState state, List<AbstractAction> actions) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int limit = params.maxArmiesPerTerritory;
        int player = state.getCurrentPlayer();
        RiskGamePhase phase = (RiskGamePhase) state.getGamePhase();
        boolean inProgress = state.isActionInProgress();

        // PlaceArmy exactly on the player's territories with room, and on some whenever an initial or REINFORCE
        // placement is due
        Set<RiskTerritory> placeTargets = new HashSet<>();
        for (AbstractAction a : actions)
            if (a instanceof PlaceArmy pa) placeTargets.add(pa.territory);
        if (!placeTargets.isEmpty()) {
            Set<RiskTerritory> withRoom = new HashSet<>();
            for (RiskTerritory t : state.getTerritories(player))
                if (hasRoom(state, t, limit)) withRoom.add(t);
            assertEquals("PlaceArmy targets", withRoom, placeTargets);
        }
        if (!inProgress && (phase == RiskGamePhase.PLACE_INITIAL || (phase == RiskGamePhase.REINFORCE
                && state.getHand(player).getSize() < params.handLimit)))
            assertFalse(phase + " decision with nowhere to place " + state.getArmiesToPlace(player),
                    placeTargets.isEmpty());

        // a move-in or fortify move offers numbers from min to min(armies(from) - 1, room): all of them, or with more
        // than maxMoveChoices, that many including both ends, evenly spaced (gaps differing by at most 1)
        if (inProgress && state.currentActionInProgress() instanceof MoveArmiesChoice c) {
            int max = state.getArmies(c.from) - 1;
            if (limit > 0) max = Math.min(max, limit - state.getArmies(c.to));
            String move = "move " + c.from + " -> " + c.to;
            List<Integer> offered = new ArrayList<>();
            for (AbstractAction a : actions) {
                MoveArmies m = (MoveArmies) a;
                assertEquals(move, c.from, m.from);
                assertEquals(move, c.to, m.to);
                offered.add(m.n);
            }
            Collections.sort(offered);
            assertEquals(move, Math.min(params.maxMoveChoices, max - c.min + 1), new HashSet<>(offered).size());
            assertEquals(move, c.min, (int) offered.get(0));
            assertEquals(move, max, (int) offered.get(offered.size() - 1));
            int minGap = Integer.MAX_VALUE, maxGap = 0;
            for (int i = 1; i < offered.size(); i++) {
                minGap = Math.min(minGap, offered.get(i) - offered.get(i - 1));
                maxGap = Math.max(maxGap, offered.get(i) - offered.get(i - 1));
            }
            assertTrue(move + " " + offered, offered.size() < 2 || maxGap - minGap <= 1);
        }

        // FORTIFY offers a Fortify from each territory with 2+ armies to each territory with room in its fortifyReach,
        // plus EndTurn
        if (!inProgress && phase == RiskGamePhase.FORTIFY) {
            Set<AbstractAction> expected = new HashSet<>();
            for (RiskTerritory from : state.getTerritories(player)) {
                if (state.getArmies(from) < 2) continue;
                for (RiskTerritory to : fortifyReach(state, player, from, params.fortifyAlongPath))
                    if (hasRoom(state, to, limit))
                        expected.add(new Fortify(from, to));
            }
            expected.add(new EndTurn());
            assertEquals("FORTIFY actions", expected, new HashSet<>(actions));
        }
    }

    private static boolean hasRoom(RiskGameState state, RiskTerritory t, int limit) {
        return limit == 0 || state.getArmies(t) < limit;
    }

    /** The player's other territories adjacent to from, or (alongPath) reachable from it through held territories. */
    private static Set<RiskTerritory> fortifyReach(RiskGameState state, int player, RiskTerritory from,
                                                   boolean alongPath) {
        Set<RiskTerritory> reached = new HashSet<>();
        Deque<RiskTerritory> open = new ArrayDeque<>(List.of(from));
        while (!open.isEmpty()) {
            RiskTerritory t = open.pop();
            for (RiskTerritory next : state.getMap().neighbours(t))
                if (state.getOwner(next) == player && !next.equals(from) && reached.add(next) && alongPath)
                    open.push(next);
        }
        return reached;
    }

    /** Each of the 42 territory cards and nWildCards wild cards is in one place, and eliminated players hold none. */
    private static void checkCards(RiskGameState state) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        List<RiskCard> all = RiskTestUtils.allCards(state);
        assertEquals("cards in the game", 42 + params.nWildCards, all.size());
        Set<RiskTerritory> territories = new HashSet<>();
        for (RiskCard c : all)
            if (!c.isWild())
                assertTrue("duplicate card " + c, territories.add(c.territory));
        assertEquals(42, territories.size());
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.isEliminated(p))
                assertEquals("eliminated player " + p + " holds cards", 0, state.getHand(p).getSize());
    }

    /** Checks the hand sizes against the forced trades. */
    private static void checkCardActions(RiskGameState state, List<AbstractAction> actions) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int hand = state.getHand(state.getCurrentPlayer()).getSize();
        if (state.isActionInProgress()) return;
        // in REINFORCE a hand of handLimit or more offers only trades, even when maxArmiesPerTerritory leaves no room
        // for the reinforcements
        if (state.getGamePhase() == RiskGamePhase.REINFORCE && hand >= params.handLimit)
            for (AbstractAction a : actions)
                assertTrue(a + " offered with " + hand + " cards", a instanceof TradeCards);
        // an elimination trade brings a larger hand down below handLimit before attacking resumes
        if (state.getGamePhase() == RiskGamePhase.ATTACK)
            assertTrue(hand + " cards while attacking", hand < params.eliminationTradeLimit);
    }

    /** Checks the Blitz and DefendWith actions offered. */
    private static void checkDiceActions(RiskGameState state, List<AbstractAction> actions) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        if (!params.allowBlitz)
            for (AbstractAction a : actions)
                assertFalse(a + " offered without allowBlitz", a instanceof Blitz);
        // DefendWith only while a DefenderDice is pending: a decision in ATTACK for the owner of to, not the turn
        // owner, offering 1 .. min(maxDefendDice, armies(to)) dice
        if (state.isActionInProgress() && state.currentActionInProgress() instanceof DefenderDice d) {
            assertTrue("DefenderDice without defenderChoosesDice", params.defenderChoosesDice);
            assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
            assertEquals(state.getOwner(d.to), state.getCurrentPlayer());
            assertNotEquals(state.getTurnOwner(), state.getCurrentPlayer());
            assertEquals(state.getTurnOwner(), state.getOwner(d.from));
            Set<AbstractAction> expected = new HashSet<>();
            for (int k = 1; k <= Math.min(params.maxDefendDice, state.getArmies(d.to)); k++)
                expected.add(new DefendWith(d.from, d.to, d.nAttackDice, k));
            assertEquals(expected, new HashSet<>(actions));
        } else {
            for (AbstractAction a : actions)
                assertFalse(a + " offered with no DefenderDice pending", a instanceof DefendWith);
        }
    }

    private static void checkResults(RiskGameState state) {
        int n = state.getNPlayers();
        GameResult[] results = state.getPlayerResults();
        int dominator = -1;
        for (int p = 0; p < n; p++)
            if (state.getNTerritories(p) == 42) dominator = p;
        // eliminated players: places nPlayers, nPlayers - 1, ... in order of going out
        Set<Integer> places = new HashSet<>();
        List<Integer> alive = new ArrayList<>();
        for (int p = 0; p < n; p++) {
            if (state.isEliminated(p)) {
                assertEquals(LOSE_GAME, results[p]);
                assertEquals(0, state.getNTerritories(p));
                places.add(state.getFinalPlace(p));
                assertEquals(state.getFinalPlace(p), state.getOrdinalPosition(p));
            } else alive.add(p);
        }
        Set<Integer> expectedPlaces = new HashSet<>();
        for (int k = 0; k < places.size(); k++) expectedPlaces.add(n - k);
        assertEquals(expectedPlaces, places);

        Comparator<Integer> byTerritoriesThenArmies = Comparator.comparingInt((Integer p) -> state.getNTerritories(p))
                .thenComparingInt(state::getTotalArmies);
        int missionWinner = state.getMissionWinner();
        if (dominator >= 0) {
            for (int p = 0; p < n; p++)
                assertEquals("player " + p, p == dominator ? WIN_GAME : LOSE_GAME, results[p]);
        } else if (missionWinner >= 0) {
            // a Secret Mission win: the winner's mission is complete; they are 1st, the others still in follow by
            // territories then armies (2, 3, ...), everyone else loses
            assertTrue(((RiskParameters) state.getGameParameters()).secretMission);
            assertFalse(state.isEliminated(missionWinner));
            assertTrue("winner's mission " + state.getMission(missionWinner) + " not complete",
                    missionDone(state, state.getMission(missionWinner), missionWinner));
            for (int p = 0; p < n; p++)
                assertEquals("player " + p, p == missionWinner ? WIN_GAME : LOSE_GAME, results[p]);
            assertEquals(1, state.getOrdinalPosition(missionWinner));
            for (int p : alive) {
                if (p == missionWinner) continue;
                int expected = 2;
                for (int q : alive)
                    if (q != p && q != missionWinner && byTerritoriesThenArmies.compare(q, p) > 0) expected++;
                assertEquals("position of player " + p, expected, state.getOrdinalPosition(p));
            }
        } else {
            assertEquals(-1, missionWinner);
            assertEquals(state.getGameParameters().getMaxRounds(), state.getRoundCounter());
            // the best by territories, then armies, wins (shared top place: a draw)
            Comparator<Integer> rank = Comparator.comparingInt((Integer p) -> state.getNTerritories(p))
                    .thenComparingInt(state::getTotalArmies);
            int best = alive.stream().max(rank).orElseThrow();
            List<Integer> top = alive.stream().filter(p -> rank.compare(p, best) == 0).toList();
            for (int p : alive)
                assertEquals("player " + p, top.contains(p) ? (top.size() == 1 ? WIN_GAME : DRAW_GAME) : LOSE_GAME, results[p]);
        }
    }

    private static RiskParameters params(int maxRounds, boolean randomDeal) {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxRounds", maxRounds);
        params.setParameterValue("randomTerritoryDeal", randomDeal);
        return params;
    }

    @Test
    public void randomGamesRunToTheEndForEveryPlayerCount() {
        for (int n = 3; n <= 6; n++)
            for (long seed = 1; seed <= 2; seed++)
                playOut(newGame(n, seed, params(30, false)), seed);
    }

    @Test
    public void randomGamesWithARandomTerritoryDealRunToTheEnd() {
        for (int n = 3; n <= 6; n++)
            playOut(newGame(n, 10 + n, params(30, true)), 10 + n);
    }

    @Test
    public void aRandomGameWithTheDefaultRoundCapRunsToTheEnd() {
        playOut(newGame(4, 99, null), 99);
    }

    @Test
    public void cardsAreEarnedAndTradedInRandomGames() {
        // 30 rounds of random play: someone captures and ends attacking, so cards reach hands and sets are traded
        int traded = 0;
        for (long seed = 1; seed <= 2; seed++) {
            Game game = newGame(4, seed, params(30, false));
            playOut(game, seed);
            traded += ((RiskGameState) game.getGameState()).getNSetsTraded();
        }
        assertTrue("no set traded in 2 random games", traded > 0);
    }

    @Test
    public void cardsAreConservedWithOtherWildCardCounts() {
        RiskParameters params = params(30, true);
        params.setParameterValue("nWildCards", 0);
        playOut(newGame(3, 21, params), 21);
        params = params(30, true);
        params.setParameterValue("nWildCards", 4);
        playOut(newGame(5, 22, params), 22);
    }

    @Test
    public void randomGamesWithTheDefenderChoosingDiceRunToTheEnd() {
        Map<String, Integer> taken = new HashMap<>();
        for (int n = 3; n <= 6; n++) {
            RiskParameters params = params(30, n % 2 == 0);
            params.setParameterValue("defenderChoosesDice", true);
            playOut(newGame(n, 30 + n, params), 30 + n, taken);
        }
        // not vacuous: defences chosen and blitzes made (allowBlitz is on by default)
        assertTrue("no DefendWith taken: " + taken, taken.getOrDefault("DefendWith", 0) > 0);
        assertTrue("no Blitz taken: " + taken, taken.getOrDefault("Blitz", 0) > 0);
    }

    @Test
    public void randomGamesWithoutBlitzRunToTheEnd() {
        Map<String, Integer> taken = new HashMap<>();
        for (int n = 3; n <= 6; n += 3) {
            RiskParameters params = params(30, false);
            params.setParameterValue("allowBlitz", false);
            playOut(newGame(n, 40 + n, params), 40 + n, taken);
            params = params(30, true);
            params.setParameterValue("allowBlitz", false);
            params.setParameterValue("defenderChoosesDice", true);
            playOut(newGame(n, 50 + n, params), 50 + n, taken);
        }
        assertEquals(0, (int) taken.getOrDefault("Blitz", 0));
        assertTrue("no Attack taken: " + taken, taken.getOrDefault("Attack", 0) > 0);
        assertTrue("no DefendWith taken: " + taken, taken.getOrDefault("DefendWith", 0) > 0);
    }

    private static RiskParameters expertParams(int maxRounds, boolean randomDeal) {
        RiskParameters params = params(maxRounds, randomDeal);
        params.setParameterValue("linearTradeValues", true);
        params.setParameterValue("fortifyAlongPath", true);
        params.setParameterValue("maxArmiesPerTerritory", 12);
        return params;
    }

    @Test
    public void randomGamesWithAllTheExpertRulesRunToTheEnd() {
        Map<String, Integer> taken = new HashMap<>();
        for (int n = 3; n <= 6; n++)
            playOut(newGame(n, 60 + n, expertParams(50, n % 2 == 0)), 60 + n, taken);
        // not vacuous: fortifies beyond a neighbour, sets traded (at linear values), territories filled to 12
        assertTrue("no fortify along a path: " + taken, taken.getOrDefault("pathFortify", 0) > 0);
        assertTrue("no trade: " + taken, taken.getOrDefault("TradeCards", 0) > 0);
        assertTrue("no territory at the limit: " + taken, taken.getOrDefault("atLimit", 0) > 0);
    }

    @Test
    public void randomGamesWithASmallLimitLoseArmies() {
        Map<String, Integer> taken = new HashMap<>();
        for (int n = 3; n <= 6; n++) {
            RiskParameters params = params(30, n % 2 == 1);
            params.setParameterValue("maxArmiesPerTerritory", 4);
            playOut(newGame(n, 70 + n, params), 70 + n, taken);
        }
        assertTrue("no armies lost with a limit of 4: " + taken, taken.getOrDefault("lost", 0) > 0);
    }

    @Test
    public void withMoreStartingArmiesThanRoomEveryTerritoryIsFilledAndTheRestLost() {
        // 3 players claim 14 territories each; limit 4 gives room for 14 x 4 = 56 armies each, but 60 are counted
        // out: every territory ends the placement at 4 (42 x 4 = 168 of the 180) and 4 per player are lost
        RiskParameters params = params(30, false);
        params.setParameterValue("maxArmiesPerTerritory", 4);
        params.setParameterValue("startArmies3", 60);
        Game game = newGame(3, 81, params);
        RiskGameState state = (RiskGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        Random rnd = new Random(81);
        Map<String, Integer> taken = new HashMap<>();
        int steps = 0;
        while (state.getGamePhase() == RiskGamePhase.CLAIM || state.getGamePhase() == RiskGamePhase.PLACE_INITIAL) {
            assertTrue("initial placement did not end", steps++ < 1000);
            checkInvariants(state);
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            checkExpertActions(state, actions);
            step(state, fm, actions.get(rnd.nextInt(actions.size())), taken);
        }
        for (RiskTerritory t : WorldMap.ALL)
            assertEquals(t.name(), 4, state.getArmies(t));
        for (int p = 0; p < 3; p++)
            assertEquals(56, state.getTotalArmies(p)); // 14 x 4
        assertTrue("no loss counted: " + taken, taken.getOrDefault("lost", 0) > 0);
        // the first player's turn: every territory full, so the reinforcements (at least 14 / 3 = 4) are all lost
        assertEquals(state.getFirstPlayer(), state.getCurrentPlayer());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getArmiesToPlace(state.getFirstPlayer()));
        playOut(game, 81);
    }

    @Test
    public void randomSecretMissionGamesRunToTheEndWithMissionWins() {
        // secretMission, randomTerritoryDeal set false (forced on by secretMission); checkMissions at every
        // decision, the mission-win branch of checkResults at the end
        Map<String, Integer> taken = new HashMap<>();
        int missionWins = 0;
        for (int n = 3; n <= 6; n++)
            for (long seed = 1; seed <= 3; seed++) {
                RiskParameters params = params(50, false);
                params.setParameterValue("secretMission", true);
                Game game = newGame(n, 90 + 10 * n + seed, params);
                RiskGameState state = (RiskGameState) game.getGameState();
                assertEquals("the territories are dealt", RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
                playOut(game, seed, taken);
                if (state.getMissionWinner() >= 0) missionWins++;
            }
        // not vacuous: some games are won by a mission
        assertTrue("no mission win in 12 games: " + taken, missionWins > 0);
    }

    @Test
    public void initialPlacementDoesNotCountTowardsTheRoundCap() {
        // with maxRounds 1 every player still gets a turn of play after claiming and placing
        Game game = newGame(3, 5, params(1, false));
        Set<Integer> reinforced = playOut(game, 5);
        assertEquals(Set.of(0, 1, 2), reinforced);
        assertEquals(1, game.getGameState().getRoundCounter());
    }
}

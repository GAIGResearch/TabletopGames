package games.ventlife.ai;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.ventlife.VentlifeForwardModel;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.Hex;
import games.ventlife.components.HexCell;
import games.ventlife.components.Terrain;
import games.ventlife.components.VentTile;
import games.ventlife.actions.PlaceCreature;
import games.ventlife.actions.PlaceTile;
import games.ventlife.actions.StopPlacing;
import players.IAnyTimePlayer;

import java.util.*;

/**
 * Chooses each creature placement or move by one-step look-ahead on a state heuristic (by default VentlifeHeuristic). A tile placement is chosen
 * by playing out the rest of the turn: each tile is valued one step ahead, then the best tileLookahead of them (as
 * many as the budget allows) are followed by greedy choices to the end of the turn, and valued there - or, with
 * opponentReplies, after the other players' next turns too, each played greedily (see opponentTurn).
 */
public class VentlifeGreedyPlayer extends AbstractPlayer implements IAnyTimePlayer {

    private static final VentlifeForwardModel fm = new VentlifeForwardModel();
    // the share of the budget for valuing the tiles one step ahead, before playing out the best of them
    private static final double oneStepShare = 0.7;
    // the number of tiles played out in the last tile decision (for diagnostics)
    private int lastPlayOuts;

    public VentlifeGreedyPlayer(VentlifeGreedyParams params) {
        super(params, "VentlifeGreedy");
    }

    public VentlifeGreedyPlayer() {
        this(new VentlifeGreedyParams());
    }

    @Override
    public VentlifeGreedyParams getParameters() {
        return (VentlifeGreedyParams) parameters;
    }

    private double value(VentlifeGameState state, int player) {
        return getParameters().heuristic.evaluateState(state, player);
    }

    private static boolean isTileDecision(AbstractGameState state) {
        return state.getGamePhase() == VentlifeGameState.Phase.PLACE_TILE && !state.isActionInProgress();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        long start = System.nanoTime();
        if (actions.size() == 1)
            return actions.get(0);
        VentlifeGameState state = (VentlifeGameState) gs;
        if (!isTileDecision(state)) {
            if (getParameters().creatureLookahead > 1 && getParameters().opponentReplies
                    && actions.stream().allMatch(a -> a instanceof PlaceCreature))
                return creatureSearch(state, actions, start);
            return greedyChoice(state, actions);
        }
        int player = state.getCurrentPlayer();
        int n = actions.size();
        long budgetNs = getParameters().budget * 1_000_000L;
        // value each tile one step ahead, in random order so that running out of time drops a random subset
        double[] oneStep = new double[n];
        Arrays.fill(oneStep, Double.NEGATIVE_INFINITY);
        List<Integer> shuffled = new ArrayList<>();
        for (int i = 0; i < n; i++) shuffled.add(i);
        Collections.shuffle(shuffled, rnd);
        long firstDeadline = start + (long) (budgetNs * oneStepShare);
        VentlifeTileEval tileEval = getParameters().heuristic instanceof VentlifeHeuristic h && h.fishSeed == 0
                ? new VentlifeTileEval(state, player, h) : null;
        boolean[] valued = new boolean[n];
        for (int i : shuffled) {
            if (System.nanoTime() > firstDeadline && i != shuffled.get(0))
                break;
            PlaceTile t = (PlaceTile) actions.get(i);
            oneStep[i] = (tileEval != null ? tileEval.tileValue(player, t.tile, t.smoker, t.orientation)
                    : value(VentlifeTileSim.afterTile(state, t), player)) + rnd.nextDouble() * 1e-6;
            valued[i] = true;
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> -oneStep[i]));
        int k = getParameters().tileLookahead;
        if (k <= 0)
            return actions.get(order[0]);
        // play out the rest of the turn after the best tiles
        // no play-out is started that would not be expected to finish within 95% of the budget
        long deadline = start + (long) (budgetNs * 0.95);
        long playOutStart = System.nanoTime();
        int best = order[0];
        double bestValue = Double.NEGATIVE_INFINITY;
        lastPlayOuts = 0;
        // the plans: each of the best tiles, followed by each of its best creature placements
        int m = Math.max(1, getParameters().creatureAlternatives);
        outer:
        for (int j = 0; j < Math.min(k, n); j++) {
            int i = order[j];
            if (!valued[i]) break;
            // the simulated state has no forward-model bookkeeping, so the play-out starts afresh
            VentlifeGameState afterTile = (VentlifeGameState) state.copy();
            fm.next(afterTile, actions.get(i).copy());
            for (VentlifeGameState s : creatureBranches(afterTile, m, null)) {
                long now = System.nanoTime();
                if (lastPlayOuts > 0 && now + (now - playOutStart) / lastPlayOuts > deadline)
                    break outer;
                lastPlayOuts++;
                playOutTurn(s, player);
                if (getParameters().opponentReplies) {
                    for (int o = 1; o < state.getNPlayers() && s.isNotTerminal(); o++)
                        opponentTurn(s);
                    // further rounds, in which every player (this one too) plays a reply turn
                    for (int r = 1; r < getParameters().replyRounds; r++)
                        for (int o = 0; o < state.getNPlayers() && s.isNotTerminal(); o++)
                            opponentTurn(s);
                }
                double v = value(s, player);
                if (v > bestValue) {
                    bestValue = v;
                    best = i;
                }
            }
        }
        return actions.get(best);
    }

    /**
     * A first creature placement chosen by play-outs: the best creatureLookahead placements by one-step value are each
     * played out to the end of the turn and through the other players' replies, as for tiles.
     */
    private AbstractAction creatureSearch(VentlifeGameState state, List<AbstractAction> actions, long start) {
        int player = state.getCurrentPlayer();
        VentlifeGameState base = (VentlifeGameState) state.copy();
        List<AbstractAction> placements = new ArrayList<>();
        List<VentlifeGameState> branches = creatureBranches(base, getParameters().creatureLookahead, placements);
        long deadline = start + (long) (getParameters().budget * 1_000_000L * 0.95);
        long playOutStart = System.nanoTime();
        double bestValue = Double.NEGATIVE_INFINITY;
        AbstractAction best = null;
        int done = 0;
        for (int b = 0; b < branches.size(); b++) {
            VentlifeGameState s = branches.get(b);
            long now = System.nanoTime();
            if (done > 0 && now + (now - playOutStart) / done > deadline)
                break;
            done++;
            AbstractAction placed = placements.get(b);
            playOutTurn(s, player);
            for (int o = 1; o < state.getNPlayers() && s.isNotTerminal(); o++)
                opponentTurn(s);
            for (int r = 1; r < getParameters().replyRounds; r++)
                for (int o = 0; o < state.getNPlayers() && s.isNotTerminal(); o++)
                    opponentTurn(s);
            double v = value(s, player);
            if (v > bestValue) {
                bestValue = v;
                best = placed;
            }
        }
        return best != null && actions.contains(best) ? best : greedyChoice(state, actions);
    }

    /**
     * After a tile: covering decisions are made greedily until the creature step, and then the state is branched on
     * each of the m best first creature placements (by one-step value), which are added to placements (if not null).
     * One branch if there is no creature step.
     */
    private List<VentlifeGameState> creatureBranches(VentlifeGameState s, int m, List<AbstractAction> placements) {
        while (s.isNotTerminal() && s.getGamePhase() != VentlifeGameState.Phase.PLACE_CREATURES && !isTileDecision(s)) {
            List<AbstractAction> options = fm.computeAvailableActions(s);
            fm.next(s, greedyChoice(s, options).copy());
        }
        if (m <= 1 || s.getGamePhase() != VentlifeGameState.Phase.PLACE_CREATURES || !s.isNotTerminal())
            return List.of(s);
        List<AbstractAction> options = fm.computeAvailableActions(s);
        int decider = s.getCurrentPlayer();
        double[] values = new double[options.size()];
        VentlifeFastEval fast = getParameters().heuristic instanceof VentlifeHeuristic h && h.fishSeed == 0
                ? new VentlifeFastEval(s, decider, h) : null;
        for (int i = 0; i < values.length; i++) {
            AbstractAction a = options.get(i);
            if (fast != null && a instanceof PlaceCreature pc)
                values[i] = fast.placementValue(decider, pc.species, pc.hex, pc.count);
            else {
                VentlifeGameState c = (VentlifeGameState) s.copy();
                fm.next(c, a.copy());
                values[i] = value(c, decider);
            }
            values[i] += rnd.nextDouble() * 1e-6;
        }
        Integer[] order = new Integer[values.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> -values[i]));
        List<VentlifeGameState> retValue = new ArrayList<>();
        for (int j = 0; j < Math.min(m, order.length); j++) {
            VentlifeGameState branch = (VentlifeGameState) s.copy();
            fm.next(branch, options.get(order[j]).copy());
            retValue.add(branch);
            if (placements != null) placements.add(options.get(order[j]));
        }
        return retValue;
    }

    /**
     * A greedy turn for the player about to place a tile: every plateau placement (those that can cover creatures) and
     * a random sample of seafloor placements are valued one step ahead, the best is taken, and the rest of the turn is
     * played greedily.
     */
    private void opponentTurn(VentlifeGameState s) {
        int opp = s.getCurrentPlayer();
        List<PlaceTile> options = replyCandidates(s, opp);
        if (options.isEmpty()) {
            fm.next(s, greedyChoice(s, fm.computeAvailableActions(s)).copy());
            playOutTurn(s, opp);
            return;
        }
        VentlifeTileEval eval = getParameters().heuristic instanceof VentlifeHeuristic h && h.fishSeed == 0
                ? new VentlifeTileEval(s, opp, h) : null;
        PlaceTile best = options.get(0);
        double bestValue = Double.NEGATIVE_INFINITY;
        for (PlaceTile t : options) {
            double v = (eval != null ? eval.tileValue(opp, t.tile, t.smoker, t.orientation)
                    : value(VentlifeTileSim.afterTile(s, t), opp)) + rnd.nextDouble() * 1e-6;
            if (v > bestValue) {
                bestValue = v;
                best = t;
            }
        }
        fm.next(s, best.copy());
        playOutTurn(s, opp);
    }

    /**
     * The tile placements an opponent considers in a reply: every plateau placement, and up to replySeafloorSample
     * random seafloor placements. Found directly, rather than by listing every legal placement.
     */
    List<PlaceTile> replyCandidates(VentlifeGameState s, int player) {
        List<PlaceTile> retValue = new ArrayList<>();
        if (s.getHand(player).getSize() == 0)
            return retValue;
        VentTile tile = s.getHand(player).peek();
        List<Hex> frontier = new ArrayList<>();
        Set<Hex> seen = new HashSet<>();
        for (Map.Entry<Hex, HexCell> e : s.getField().entrySet()) {
            Hex h = e.getKey();
            if (e.getValue().terrain() == Terrain.BLACK_SMOKER)
                for (int o = 0; o < 6; o++)
                    if (VentlifeUtils.isPlateauPlacement(s, h, o))
                        retValue.add(new PlaceTile(tile, h, o));
            for (Hex nb : h.neighbours())
                if (s.getCell(nb) == null && seen.add(nb))
                    frontier.add(nb);
        }
        // a seafloor tile is legal when its three positions are empty and one of them touches the field
        Set<PlaceTile> seafloor = new HashSet<>();
        int wanted = getParameters().replySeafloorSample;
        for (int attempt = 0; attempt < 4 * wanted && seafloor.size() < wanted && !frontier.isEmpty(); attempt++) {
            Hex f = frontier.get(rnd.nextInt(frontier.size()));
            int o = rnd.nextInt(6), at = rnd.nextInt(3);
            // f as the Smoker, the left terrain (direction o + 1 from the Smoker) or the right (direction o)
            Hex smoker = at == 0 ? f : f.neighbour(((at == 1 ? o + 1 : o) + 3) % 6);
            List<Hex> hexes = VentlifeUtils.tileHexes(smoker, o);
            if (s.getCell(hexes.get(0)) == null && s.getCell(hexes.get(1)) == null && s.getCell(hexes.get(2)) == null)
                seafloor.add(new PlaceTile(tile, smoker, o));
        }
        retValue.addAll(seafloor);
        return retValue;
    }

    /**
     * Greedy one-step choices, for whoever is to decide, until the next tile placement.
     */
    private void playOutTurn(VentlifeGameState s, int player) {
        while (s.isNotTerminal() && !isTileDecision(s)) {
            List<AbstractAction> options = fm.computeAvailableActions(s);
            fm.next(s, greedyChoice(s, options).copy());
        }
    }

    /**
     * The action with the best one-step value for the player to decide. Creature placements are valued without
     * copying the state (VentlifeFastEval) when the heuristic allows it.
     */
    private AbstractAction greedyChoice(VentlifeGameState s, List<AbstractAction> options) {
        if (options.size() == 1)
            return options.get(0);
        int decider = s.getCurrentPlayer();
        AbstractAction bestAction = options.get(0);
        double bestValue = Double.NEGATIVE_INFINITY;
        if (getParameters().heuristic instanceof VentlifeHeuristic h && h.fishSeed == 0
                && options.stream().allMatch(a -> a instanceof PlaceCreature || a instanceof StopPlacing)) {
            VentlifeFastEval fast = new VentlifeFastEval(s, decider, h);
            for (AbstractAction a : options) {
                double v = (a instanceof PlaceCreature pc ? fast.placementValue(decider, pc.species, pc.hex, pc.count)
                        : fast.value(decider)) + rnd.nextDouble() * 1e-6;
                if (v > bestValue) {
                    bestValue = v;
                    bestAction = a;
                }
            }
            return bestAction;
        }
        for (AbstractAction a : options) {
            VentlifeGameState c = (VentlifeGameState) s.copy();
            fm.next(c, a.copy());
            double v = value(c, decider) + rnd.nextDouble() * 1e-6;
            if (v > bestValue) {
                bestValue = v;
                bestAction = a;
            }
        }
        return bestAction;
    }

    public int getLastPlayOuts() {
        return lastPlayOuts;
    }

    @Override
    public VentlifeGreedyPlayer copy() {
        VentlifeGreedyPlayer retValue = new VentlifeGreedyPlayer((VentlifeGreedyParams) getParameters().copy());
        retValue.setName(toString());
        return retValue;
    }

    @Override
    public void setBudget(int budget) {
        parameters.setParameterValue("budget", budget);
    }

    @Override
    public int getBudget() {
        return parameters.budget;
    }
}

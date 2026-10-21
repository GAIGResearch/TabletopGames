package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.*;

import java.util.*;

/**
 * Values creature placements in one state without copying it: the VentlifeHeuristic value (exposure included, but
 * not fishSeed) for the placing player of the state after each placement, computed from the local changes it makes.
 * The changes a placement can make are to the placed creature's own score, the shoal it joins, the majorities on its
 * plateau and the scores of Octopuses next to it, and to the exposure of those creatures.
 */
public class VentlifeFastEval {

    private final VentlifeGameState state;
    private final VentlifeParameters params;
    private final Map<Hex, Integer> exposed;
    private final double[] adjusted;
    private final double[] expWeight;
    private final double[] riskWeight;
    private final int tilesLeft;
    private final int riskModel;
    private final double oppMaxWeight, oppMeanWeight, moveFactor, crabValue;

    public VentlifeFastEval(VentlifeGameState state, int perspective, VentlifeHeuristic h) {
        this.state = state;
        this.params = state.getParams();
        int n = state.getNPlayers();
        oppMaxWeight = h.oppMaxWeight;
        oppMeanWeight = h.oppMeanWeight;
        moveFactor = h.exposureMoveFactor;
        crabValue = h.exposureCrabValue;
        double scale = 1.0;
        if (h.endScaling) {
            int remaining = state.getDrawDeck().getSize();
            for (int p = 0; p < n; p++)
                remaining += state.getHand(p).getSize();
            scale = Math.min(1.0, (double) remaining / n);
        }
        double oppExp = h.oppExposure < 0 ? h.exposure : h.oppExposure;
        expWeight = new double[n];
        for (int p = 0; p < n; p++)
            expWeight[p] = scale * (p == perspective ? h.exposure : oppExp);
        double oppR = h.oppRisk < 0 ? h.risk : h.oppRisk;
        riskWeight = new double[n];
        for (int p = 0; p < n; p++)
            riskWeight[p] = p == perspective ? h.risk : oppR;
        tilesLeft = CoverageModel.tilesLeft(state);
        riskModel = h.riskModel;
        int[] scores = VentlifeScorer.scores(state);
        adjusted = new double[n];
        boolean anyExposure = false, anyRisk = false;
        for (int p = 0; p < n; p++) {
            adjusted[p] = scores[p];
            anyExposure |= expWeight[p] != 0;
            anyRisk |= riskWeight[p] != 0;
        }
        exposed = anyExposure || anyRisk ? VentlifeScorer.exposedPositions(state) : Map.of();
        if (anyExposure) {
            double[] ev = VentlifeScorer.exposedValue(state, exposed, moveFactor, crabValue);
            for (int p = 0; p < n; p++)
                adjusted[p] -= expWeight[p] * ev[p];
        }
        if (anyRisk) {
            double[] rv = VentlifeScorer.riskValue(state, exposed, moveFactor, crabValue, riskModel);
            for (int p = 0; p < n; p++)
                adjusted[p] -= riskWeight[p] * rv[p];
        }
    }

    /**
     * The heuristic value for player of the state as it is.
     */
    public double value(int player) {
        return VentlifeHeuristic.value(player, adjusted, oppMaxWeight, oppMeanWeight);
    }

    private boolean isExposed(Hex hex, Species species) {
        Integer bits = exposed.get(hex);
        if (bits == null) return false;
        return species != Species.TUBE_WORM || (bits & 1) != 0;
    }

    /**
     * The heuristic value, for the placing player, after player places count creatures of the species on the hex
     * (which must be a legal placement).
     */
    public double placementValue(int player, Species species, Hex hex, int count) {
        double[] d = adjusted.clone();
        Map<Hex, Creature> creatures = state.getCreatures();
        boolean hexExposed = isExposed(hex, species);
        if (riskWeight[player] != 0) {
            double loss = VentlifeScorer.lossIfCovered(state, hex, new Creature(species, player, count), moveFactor, crabValue);
            if (loss != 0)
                d[player] -= riskWeight[player] * loss * risk(hex, species);
        }
        switch (species) {
            case EELPOUT_FISH -> {
                // the shoals next to the hex merge with the new Fish
                Set<Hex> seen = new HashSet<>();
                int merged = 1;
                double before = 0;
                boolean anyNeighbour = false;
                for (Hex nb : hex.neighbours()) {
                    Creature f = creatures.get(nb);
                    if (f == null || f.species() != Species.EELPOUT_FISH || f.owner() != player) continue;
                    anyNeighbour = true;
                    // a neighbouring Fish with no other Fish beside it becomes worth losing
                    if (ownFishNeighbours(nb, player) == 0) {
                        if (expWeight[player] != 0 && isExposed(nb, Species.EELPOUT_FISH))
                            d[player] -= expWeight[player] * moveFactor * params.shoalPoints;
                        if (riskWeight[player] != 0)
                            d[player] -= riskWeight[player] * moveFactor * params.shoalPoints * risk(nb, Species.EELPOUT_FISH);
                    }
                    if (seen.contains(nb)) continue;
                    int size = shoal(nb, player, seen);
                    merged += size;
                    before += params.shoalScore(size);
                }
                d[player] += params.shoalScore(merged) - before;
                if (hexExposed && anyNeighbour)
                    d[player] -= expWeight[player] * moveFactor * params.shoalPoints;
            }
            case YETI_CRAB -> {
                Set<Hex> plateau = plateauOf(hex);
                if (plateau.size() >= 2) {
                    int[] crabs = VentlifeUtils.crabsOn(state, plateau);
                    int[] before = majorityScores(crabs, plateau.size());
                    crabs[player]++;
                    int[] after = majorityScores(crabs, plateau.size());
                    for (int p = 0; p < d.length; p++)
                        d[p] += after[p] - before[p];
                }
                if (hexExposed)
                    d[player] -= expWeight[player] * moveFactor * crabValue;
            }
            default -> {
                int sc = VentlifeScorer.creatureScore(state, hex, new Creature(species, player, count));
                d[player] += sc;
                if (hexExposed) {
                    double loss = (species == Species.TUBE_WORM || species == Species.VENT_SPONGE) ? sc : moveFactor * sc;
                    d[player] -= expWeight[player] * loss;
                }
            }
        }
        // Octopuses next to the hex score a species new to them
        if (species != Species.OCTOPUS && state.getSpeciesInPlay().contains(Species.OCTOPUS)) {
            for (Hex nb : hex.neighbours()) {
                Creature o = creatures.get(nb);
                if (o == null || o.species() != Species.OCTOPUS) continue;
                boolean already = false;
                for (Hex nn : nb.neighbours()) {
                    Creature c = creatures.get(nn);
                    if (c != null && c.species() == species) {
                        already = true;
                        break;
                    }
                }
                if (already) continue;
                d[o.owner()] += params.octopusPointsPerSpecies;
                if (isExposed(nb, Species.OCTOPUS))
                    d[o.owner()] -= expWeight[o.owner()] * moveFactor * params.octopusPointsPerSpecies;
                if (riskWeight[o.owner()] != 0)
                    d[o.owner()] -= riskWeight[o.owner()] * moveFactor * params.octopusPointsPerSpecies * risk(nb, Species.OCTOPUS);
            }
        }
        return VentlifeHeuristic.value(player, d, oppMaxWeight, oppMeanWeight);
    }

    private double risk(Hex hex, Species species) {
        return CoverageModel.probability(state, hex, species, exposed, tilesLeft, riskModel);
    }

    private int ownFishNeighbours(Hex hex, int player) {
        int retValue = 0;
        for (Hex nb : hex.neighbours()) {
            Creature f = state.getCreature(nb);
            if (f != null && f.species() == Species.EELPOUT_FISH && f.owner() == player) retValue++;
        }
        return retValue;
    }

    private int shoal(Hex start, int player, Set<Hex> seen) {
        int size = 0;
        ArrayDeque<Hex> open = new ArrayDeque<>();
        open.push(start);
        seen.add(start);
        while (!open.isEmpty()) {
            Hex h = open.pop();
            size++;
            for (Hex nb : h.neighbours()) {
                Creature f = state.getCreature(nb);
                if (f != null && f.species() == Species.EELPOUT_FISH && f.owner() == player && seen.add(nb))
                    open.push(nb);
            }
        }
        return size;
    }

    private Set<Hex> plateauOf(Hex start) {
        int level = state.getLevel(start);
        Set<Hex> retValue = new HashSet<>();
        if (level < 2) return retValue;
        ArrayDeque<Hex> open = new ArrayDeque<>();
        open.push(start);
        retValue.add(start);
        while (!open.isEmpty()) {
            Hex h = open.pop();
            for (Hex nb : h.neighbours())
                if (state.getLevel(nb) == level && retValue.add(nb))
                    open.push(nb);
        }
        return retValue;
    }

    private static int[] majorityScores(int[] crabs, int size) {
        int[] retValue = new int[crabs.length];
        int most = 0;
        for (int c : crabs) most = Math.max(most, c);
        if (most == 0) return retValue;
        int nMost = 0, second = 0;
        for (int c : crabs) {
            if (c == most) nMost++;
            else second = Math.max(second, c);
        }
        for (int p = 0; p < crabs.length; p++) {
            if (crabs[p] == 0) continue;
            if (crabs[p] == most) retValue[p] = size;
            else if (nMost == 1 && crabs[p] == second) retValue[p] = size / 2;
        }
        return retValue;
    }
}

package games.ventlife.ai;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.*;
import players.IAnyTimePlayer;
import players.PlayerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Generates data on which creatures get covered. Plays games between the given agents and, at the start of every
 * tile placement, records features of every creature on the field and whether a later tile covers it before the
 * end of the game (a Tube Worm climbing onto a Smoker placed on its own is not covered).
 * <p>
 * Usage: CoverageData output.csv games budget agent1.json agent2.json ... (each game seats four agents drawn at
 * random from the list)
 */
public class CoverageData {

    public static final String[] FEATURES = {"exposed", "level", "edge", "sameLevelNbrs", "emptyNbrs", "higherNbrs",
            "lowerNbrs", "smokersNear", "tilesLeft", "ownerMovesNext"};

    private record Sample(int tick, Hex hex, Species species, int owner, double[] features, double loss) {
    }

    public static double[] features(VentlifeGameState state, Hex hex, Species species, Map<Hex, Integer> exposed,
                                    int tilesLeft) {
        double[] f = new double[FEATURES.length];
        Integer bits = exposed.get(hex);
        f[0] = bits != null && (species != Species.TUBE_WORM || (bits & 1) != 0) ? 1 : 0;
        int level = state.getLevel(hex);
        f[1] = level;
        int same = 0, empty = 0, higher = 0, lower = 0, smokers = 0;
        HexCell own = state.getCell(hex);
        if (own.terrain() == Terrain.BLACK_SMOKER) smokers++;
        for (Hex nb : hex.neighbours()) {
            HexCell c = state.getCell(nb);
            if (c == null) {
                empty++;
                continue;
            }
            if (c.level() == level) {
                same++;
                if (c.terrain() == Terrain.BLACK_SMOKER) smokers++;
            } else if (c.level() > level) higher++;
            else lower++;
        }
        f[2] = empty > 0 ? 1 : 0;
        f[3] = same;
        f[4] = empty;
        f[5] = higher;
        f[6] = lower;
        f[7] = smokers;
        f[8] = tilesLeft;
        return f;
    }

    public static void main(String[] args) throws IOException {
        String out = args[0];
        int games = Integer.parseInt(args[1]);
        int budget = Integer.parseInt(args[2]);
        List<String> agents = Arrays.asList(args).subList(3, args.length);
        Random rnd = new Random(games * 31L + budget);
        Game game = GameType.Ventlife.createGameInstance(4, 1);
        try (PrintWriter w = new PrintWriter(new FileWriter(out))) {
            w.println("game,species," + String.join(",", FEATURES) + ",loss,covered");
            for (int g = 0; g < games; g++) {
                List<AbstractPlayer> players = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    AbstractPlayer p = PlayerFactory.createPlayer(agents.get(rnd.nextInt(agents.size())));
                    if (p instanceof IAnyTimePlayer a) a.setBudget(budget);
                    players.add(p);
                }
                game.reset(players, rnd.nextLong());
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                List<Sample> samples = new ArrayList<>();
                // the tick and the covered positions of each tile placed, in order
                List<int[]> tileTicks = new ArrayList<>();
                List<List<Hex>> tileHexes = new ArrayList<>();
                while (state.isNotTerminal()) {
                    int p = state.getCurrentPlayer();
                    boolean tileDecision = state.getGamePhase() == VentlifeGameState.Phase.PLACE_TILE
                            && !state.isActionInProgress();
                    if (tileDecision) {
                        int tilesLeft = state.getDrawDeck().getSize();
                        for (int q = 0; q < 4; q++) tilesLeft += state.getHand(q).getSize();
                        Map<Hex, Integer> exposed = VentlifeScorer.exposedPositions(state);
                        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
                            Creature c = e.getValue();
                            double[] f = features(state, e.getKey(), c.species(), exposed, tilesLeft);
                            f[9] = c.owner() == p ? 1 : 0;
                            double loss = switch (c.species()) {
                                case YETI_CRAB, EELPOUT_FISH -> 0;
                                default -> VentlifeScorer.creatureScore(state, e.getKey(), c);
                            };
                            samples.add(new Sample(state.getGameTick(), e.getKey(), c.species(), c.owner(), f, loss));
                        }
                    }
                    AbstractGameState obs = state.copy(p);
                    List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(obs);
                    AbstractAction a = players.get(p).getAction(obs, actions);
                    if (a instanceof PlaceTile t) {
                        tileTicks.add(new int[]{state.getGameTick()});
                        tileHexes.add(VentlifeUtils.tileHexes(t.smoker, t.orientation));
                        // a Worm on the Smoker this tile extends climbs with it
                        Creature onSmoker = state.getCreature(t.smoker);
                        if (onSmoker != null && onSmoker.species() == Species.TUBE_WORM)
                            tileHexes.set(tileHexes.size() - 1, List.of(tileHexes.getLast().get(1), tileHexes.getLast().get(2)));
                    }
                    game.getForwardModel().next(state, a);
                }
                for (Sample s : samples) {
                    boolean covered = false;
                    for (int i = 0; i < tileTicks.size(); i++)
                        if (tileTicks.get(i)[0] >= s.tick && tileHexes.get(i).contains(s.hex)) {
                            covered = true;
                            break;
                        }
                    StringBuilder sb = new StringBuilder();
                    sb.append(g).append(',').append(s.species.name());
                    for (double v : s.features) sb.append(',').append(v);
                    sb.append(',').append(s.loss).append(',').append(covered ? 1 : 0);
                    w.println(sb);
                }
            }
        }
    }
}

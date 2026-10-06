package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.AbstractPlayer;
import games.GameType;
import players.PlayerConstants;
import players.PlayerFactory;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * The agents a browser player can play against: a few built in (random, one-step look-ahead, and MCTS at several
 * thinking times), and any agent JSON files in the server's agent directory or in the game's own
 * {@code data/<game>/agents} directory (as used by FrontendSimple).
 */
class OpponentCatalog {

    record Opponent(String id, String label, Supplier<AbstractPlayer> factory) {
        AbstractPlayer create() {
            return factory.get();
        }
    }

    private static final List<Opponent> builtIn = List.of(
            new Opponent("random", "Random", RandomPlayer::new),
            new Opponent("osla", "One-step look-ahead", OSLAPlayer::new),
            mcts(100, "MCTS, 0.1 s per decision"),
            mcts(1000, "MCTS, 1 s per decision"),
            mcts(5000, "MCTS, 5 s per decision"));

    static final String DEFAULT = "mcts-1000";

    private final String agentDir;

    /**
     * @param agentDir a directory of agent JSON files to offer for every game; may be empty
     */
    OpponentCatalog(String agentDir) {
        this.agentDir = agentDir;
    }

    List<Opponent> forGame(GameType game) {
        List<Opponent> result = new ArrayList<>(builtIn);
        if (agentDir != null && !agentDir.isBlank()) addAgentFiles(new File(agentDir), result);
        addAgentFiles(new File("data/" + game.name() + "/agents"), result);
        return result;
    }

    Opponent find(GameType game, String id) {
        for (Opponent o : forGame(game))
            if (o.id().equals(id)) return o;
        throw new IllegalArgumentException("Unknown opponent: " + id);
    }

    JsonArray describe(GameType game) {
        JsonArray result = new JsonArray();
        for (Opponent o : forGame(game)) {
            JsonObject j = new JsonObject();
            j.addProperty("id", o.id());
            j.addProperty("label", o.label());
            result.add(j);
        }
        return result;
    }

    private static Opponent mcts(int ms, String label) {
        return new Opponent("mcts-" + ms, label, () -> {
            MCTSParams params = new MCTSParams();
            params.setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
            params.setParameterValue("budget", ms);
            return new MCTSPlayer(params, "MCTS " + ms + "ms");
        });
    }

    private static void addAgentFiles(File dir, List<Opponent> into) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length() - ".json".length());
            String id = "agent:" + name;
            if (into.stream().anyMatch(o -> o.id().equals(id))) continue;
            into.add(new Opponent(id, name + " (agent file)", () -> {
                AbstractPlayer player = PlayerFactory.createPlayer(f.getPath());
                player.setName(name);
                return player;
            }));
        }
    }
}

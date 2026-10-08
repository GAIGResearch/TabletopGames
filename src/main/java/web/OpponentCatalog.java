package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.AbstractPlayer;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utilities.JSONUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The agents a browser player can play against: the agent JSON files in the server's agent directory, and any in the
 * game's {@code data/<game>/agents} directory (the game's name in lower case). An agent's id is its file name without ".json".
 * <p>
 * The id is also the agent's name, on the start page and in the game. Besides what
 * {@link utilities.JSONUtils#loadClassFromJSON} reads, an agent file may have a {@code label}, a description shown
 * after the name on the start page, and {@code "default": true} to be the opponent chosen when a setup names none
 * (else the first agent is).
 */
class OpponentCatalog {

    record Opponent(String id, String description, boolean isDefault, File file) {
        AbstractPlayer create() {
            JSONObject json = agentJson(file);
            Object loaded = JSONUtils.loadClassFromJSON(json);
            if (loaded instanceof TunableParameters<?> params) loaded = params.instantiate();
            if (!(loaded instanceof AbstractPlayer player))
                throw new IllegalArgumentException(file + " does not define an AbstractPlayer or TunableParameters class");
            player.setName(id);
            return player;
        }
    }

    static final String DEFAULT_DIR = "json/players/webserver";

    private final File agentDir;

    /**
     * @param agentDir the directory of agent JSON files to offer for every game
     */
    OpponentCatalog(String agentDir) {
        this.agentDir = new File(agentDir);
        if (!this.agentDir.isDirectory())
            System.out.println("Warning: no agent directory at " + this.agentDir.getAbsolutePath() + "; only game-specific agents will be offered");
    }

    List<Opponent> forGame(GameType game) {
        List<Opponent> result = new ArrayList<>();
        addAgentFiles(agentDir, result);
        addAgentFiles(new File("data/" + game.name().toLowerCase() + "/agents"), result);
        return result;
    }

    Opponent find(GameType game, String id) {
        for (Opponent o : forGame(game))
            if (o.id().equals(id)) return o;
        throw new IllegalArgumentException("Unknown opponent: " + id);
    }

    /**
     * The opponent for a seat the setup does not fill.
     */
    String defaultFor(GameType game) {
        List<Opponent> all = forGame(game);
        if (all.isEmpty()) throw new IllegalArgumentException("There are no agents to play " + game.name() + " against");
        return all.stream().filter(Opponent::isDefault).findFirst().orElse(all.get(0)).id();
    }

    JsonArray describe(GameType game) {
        JsonArray result = new JsonArray();
        String defaultId = forGame(game).isEmpty() ? null : defaultFor(game);
        for (Opponent o : forGame(game)) {
            JsonObject j = new JsonObject();
            j.addProperty("id", o.id());
            if (!o.description().isEmpty()) j.addProperty("description", o.description());
            if (o.id().equals(defaultId)) j.addProperty("default", true);
            result.add(j);
        }
        return result;
    }

    /**
     * The file's JSON, less the keys only the catalog reads.
     */
    private static JSONObject agentJson(File file) {
        JSONObject json = readJson(file);
        json.remove("label");
        json.remove("default");
        return json;
    }

    // not JSONUtils.loadJSONFile, which shares one parser and requests are read on several threads
    private static JSONObject readJson(File file) {
        try (FileReader reader = new FileReader(file)) {
            return (JSONObject) new JSONParser().parse(reader);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read agent file " + file + ": " + e.getMessage(), e);
        }
    }

    private static void addAgentFiles(File dir, List<Opponent> into) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - ".json".length());
            // a file in the server's agent directory takes precedence over one of the same name in the game's
            if (into.stream().anyMatch(o -> o.id().equals(id))) continue;
            JSONObject json;
            try {
                json = readJson(f);
            } catch (Exception e) {
                System.out.println("Skipping agent file " + f + ": " + e.getMessage());
                continue;
            }
            Object label = json.get("label");
            into.add(new Opponent(id, label instanceof String s ? s.trim() : "",
                    Boolean.TRUE.equals(json.get("default")), f));
        }
    }
}

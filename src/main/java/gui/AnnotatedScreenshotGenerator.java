package gui;

import games.GameType;
import utilities.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces a set of annotated screenshots for a directory of saved game states from an Advisers run.
 * <p>
 * The input directory (e.g. {@code .../Player_A_10}) is expected to contain:
 * <ul>
 *   <li>{@code SavedStates/<gameType>/G<GameID>/P<PlayerID>_Tick<Tick>.json} - the saved states to render;</li>
 *   <li>{@code <prefix>Adviser.txt} - one tab-separated row per saved state, linked by (PlayerID, GameID, Tick);</li>
 *   <li>{@code ACTION_CHOSEN.csv} - comma-separated action log covering all ticks (not just saved states).</li>
 * </ul>
 * For each saved state this writes, alongside the JSON file, a {@code P<PlayerID>_Tick<Tick>.png} render
 * (via {@link StateRenderer#renderToImage}) annotated with the agent's and adviser's chosen actions taken
 * from the Adviser file. For each game it also writes, into that game's {@code G<GameID>} directory, a copy
 * of the Adviser file and a copy of the ACTION_CHOSEN file filtered to that game's rows.
 * <p>
 * Arguments (key=value):
 * <ul>
 *   <li>{@code input}     - the root data directory (required).</li>
 *   <li>{@code game}      - {@link GameType} name (default {@code XIIScripta}).</li>
 *   <li>{@code fullPanel} - {@code true} to render the whole game panel; default {@code false} renders the board only.</li>
 *   <li>{@code annotate}  - {@code true} (default) to draw the agent/adviser actions onto the PNG.</li>
 * </ul>
 */
public class AnnotatedScreenshotGenerator {

    // P<PlayerID>_Tick<Tick>.json
    private static final Pattern STATE_FILE = Pattern.compile("P(\\d+)_Tick(\\d+)\\.json", Pattern.CASE_INSENSITIVE);
    // G<GameID> directory
    private static final Pattern GAME_DIR = Pattern.compile("G(\\d+)");

    // Column indices in <prefix>Adviser.txt (tab-separated)
    private static final int ADV_PLAYER_ID = 0, ADV_AGENT_ACTION = 2, ADV_AGENT_VALUE = 3,
            ADV_ADVISER_ACTION = 4, ADV_ADVISER_VALUE = 5, ADV_GAME_ID = 6, ADV_TICK = 9;
    // Column index of GameID in ACTION_CHOSEN.csv
    private static final int AC_GAME_ID = 7;

    public static void main(String[] args) {
        String input = Utils.getArg(args, "input", "");
        String game = Utils.getArg(args, "game", "XIIScripta");
        boolean fullPanel = Utils.getArg(args, "fullPanel", false);
        boolean annotate = Utils.getArg(args, "annotate", true);

        if (input.isEmpty()) {
            System.out.println("Usage: input=<root-dir> [game=XIIScripta] [fullPanel=false] [annotate=true]");
            return;
        }

        GameType gameType = GameType.valueOf(game);
        File root = new File(input);
        if (!root.isDirectory())
            throw new AssertionError("Input directory does not exist: " + input);

        File savedStatesRoot = new File(new File(root, "SavedStates"), game);
        if (!savedStatesRoot.isDirectory())
            throw new AssertionError("No SavedStates/" + game + " directory under " + input);

        // 1. Parse the Adviser file, keyed by (PlayerID, GameID, Tick).
        File adviserFile = findAdviserFile(root);
        Map<String, String[]> adviserRows = new HashMap<>();
        String adviserHeader = null;
        List<String[]> adviserAll = new ArrayList<>();
        if (adviserFile != null) {
            List<String> lines = readLines(adviserFile);
            if (!lines.isEmpty()) {
                adviserHeader = lines.get(0);
                for (int i = 1; i < lines.size(); i++) {
                    if (lines.get(i).isBlank()) continue;
                    String[] f = lines.get(i).split("\t", -1);
                    adviserAll.add(f);
                    adviserRows.put(key(f[ADV_PLAYER_ID], f[ADV_GAME_ID], f[ADV_TICK]), f);
                }
            }
        } else {
            System.out.println("Warning: no *Adviser.txt found in " + input + " - PNGs will not be annotated.");
        }

        // 2. Walk the saved states, render (+annotate) each one, and collect the games seen.
        File[] gameDirs = savedStatesRoot.listFiles(File::isDirectory);
        if (gameDirs == null) gameDirs = new File[0];
        Map<String, File> gameDirById = new HashMap<>();
        int rendered = 0, total = 0;
        for (File gameDir : gameDirs) {
            Matcher gm = GAME_DIR.matcher(gameDir.getName());
            if (!gm.matches()) continue;
            String gameId = gm.group(1);
            gameDirById.put(gameId, gameDir);

            File[] jsons = gameDir.listFiles((d, n) -> STATE_FILE.matcher(n).matches());
            if (jsons == null) continue;
            for (File json : jsons) {
                total++;
                Matcher sm = STATE_FILE.matcher(json.getName());
                if (!sm.matches()) continue;
                String playerId = sm.group(1);
                String tick = sm.group(2);
                try {
                    BufferedImage img = StateRenderer.renderToImage(gameType, json, fullPanel);
                    String[] adv = adviserRows.get(key(playerId, gameId, tick));
                    if (annotate && adv != null) img = annotate(img, adv);
                    File png = new File(gameDir, stripExtension(json.getName()) + ".png");
                    ImageIO.write(img, "png", png);
                    rendered++;
                } catch (Exception e) {
                    System.err.println("Failed to render " + json.getName() + " : " + e.getMessage());
                }
            }
        }
        System.out.printf("Rendered %d of %d saved states.%n", rendered, total);

        // 3a. Per-game copy of the Adviser file, filtered to that game's rows.
        if (adviserFile != null && adviserHeader != null) {
            for (Map.Entry<String, File> e : gameDirById.entrySet()) {
                String gameId = e.getKey();
                List<String> out = new ArrayList<>();
                out.add(adviserHeader);
                for (String[] f : adviserAll)
                    if (f[ADV_GAME_ID].equals(gameId)) out.add(String.join("\t", f));
                writeLines(new File(e.getValue(), adviserFile.getName()), out);
            }
        }

        // 3b. Per-game copy of ACTION_CHOSEN.csv, filtered to that game's rows in a single streaming pass.
        File actionChosen = new File(root, "ACTION_CHOSEN.csv");
        if (actionChosen.isFile()) {
            filterActionChosen(actionChosen, gameDirById);
        } else {
            System.out.println("Warning: no ACTION_CHOSEN.csv found in " + input);
        }
    }

    /** Streams ACTION_CHOSEN.csv once, writing each relevant row to the matching game directory's copy. */
    private static void filterActionChosen(File actionChosen, Map<String, File> gameDirById) {
        Map<String, BufferedWriter> writers = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(actionChosen.toPath(), StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) return;
            for (Map.Entry<String, File> e : gameDirById.entrySet()) {
                BufferedWriter w = Files.newBufferedWriter(new File(e.getValue(), actionChosen.getName()).toPath(),
                        StandardCharsets.UTF_8);
                w.write(header);
                w.newLine();
                writers.put(e.getKey(), w);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                if (f.length <= AC_GAME_ID) continue;
                BufferedWriter w = writers.get(f[AC_GAME_ID]);
                if (w != null) {
                    w.write(line);
                    w.newLine();
                }
            }
        } catch (IOException ex) {
            throw new AssertionError("Failed reading " + actionChosen + " : " + ex.getMessage());
        } finally {
            for (BufferedWriter w : writers.values()) {
                try { w.close(); } catch (IOException ignored) { }
            }
        }
    }

    /** Adds the agent's and adviser's chosen actions in a strip below the rendered board. */
    private static BufferedImage annotate(BufferedImage board, String[] adv) {
        String agentLine = "Agent:   " + adv[ADV_AGENT_ACTION] + "   (" + adv[ADV_AGENT_VALUE] + ")";
        String adviserLine = "Adviser: " + adv[ADV_ADVISER_ACTION] + "   (" + adv[ADV_ADVISER_VALUE] + ")";
        return StateRenderer.withTextBelow(board,
                List.of(agentLine, adviserLine),
                List.of(new Color(0x10, 0x10, 0x10), new Color(0x00, 0x66, 0x00)));
    }

    private static File findAdviserFile(File root) {
        File[] matches = root.listFiles((d, n) -> n.endsWith("Adviser.txt"));
        return (matches != null && matches.length > 0) ? matches[0] : null;
    }

    private static String key(String playerId, String gameId, String tick) {
        return playerId + "|" + gameId + "|" + tick;
    }

    private static List<String> readLines(File f) {
        try {
            return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Failed reading " + f + " : " + e.getMessage());
        }
    }

    private static void writeLines(File f, List<String> lines) {
        try {
            Files.write(f.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Failed writing " + f + " : " + e.getMessage());
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(0, dot);
    }
}

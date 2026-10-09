package web;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GUIMessages;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One browser player's game: the {@link Game} on its own thread, with a {@link HumanGUIPlayer} in the browser player's
 * seat, and the game's Swing GUI in a frame no one sees. A {@link FrameStreamer} sends what the frame shows to the
 * browser, and an {@link InputForwarder} turns the browser's mouse and key events into events on the frame, so the GUI
 * works exactly as it does on the desktop.
 * <p>
 * Besides the frames, the session tells the browser when the game starts (with its seed and players), whose turn it
 * is, and the results when it ends.
 * <p>
 * The session starts on the browser's first resize message, which gives the size to lay the GUI out at.
 */
public class GameSession {

    private static final AtomicInteger ids = new AtomicInteger();
    // to find the session a GUI's message (see GUIMessages) comes from
    private static final Map<Window, GameSession> byFrame = new ConcurrentHashMap<>();

    private final int id = ids.incrementAndGet();
    private final SessionConfig config;
    private final OpponentCatalog opponents;
    private final Sender out;
    private final boolean showFrames;

    private Game game;
    private JFrame frame;
    private AbstractGUIManager gui;
    private Timer guiUpdater;
    private Thread gameThread;
    private FrameStreamer streamer;
    private InputForwarder input;
    private ChromeReader chrome;
    private volatile boolean started, stopped;
    // the size of the browser's space for the GUI, and its device pixel ratio (Swing thread only)
    private Dimension space;
    private double dpr;
    // image pixels per frame pixel (see fitFrame)
    private volatile double imageScale;
    private volatile long lastActivity = System.currentTimeMillis();
    private final long startedAt = System.currentTimeMillis();
    private long seed;
    // the last turn reported to the browser, and whether the results have been sent (Swing thread only)
    private String reportedTurn;
    private boolean reportedResults;

    /**
     * @param showFrames place the Swing frame on screen (for debugging); otherwise it is off screen
     */
    public GameSession(SessionConfig config, OpponentCatalog opponents, Sender out, boolean showFrames) {
        this.config = config;
        this.opponents = opponents;
        this.out = out;
        this.showFrames = showFrames;
    }

    /**
     * Swing settings for a JVM whose GUIs are all streamed.
     *
     * @param lookAndFeel "flat" for FlatLaf's light look and feel, or "default" for Swing's own
     */
    public static void configureSwing(String lookAndFeel) {
        if (lookAndFeel.equalsIgnoreCase("flat"))
            FlatLightLaf.setup();
        else if (!lookAndFeel.equalsIgnoreCase("default"))
            throw new IllegalArgumentException("Unknown look and feel (flat or default): " + lookAndFeel);
        // Swing moves a tooltip on to a real screen, and so out of the frame; the InputForwarder sends their text to
        // the browser instead
        ToolTipManager.sharedInstance().setEnabled(false);
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        // a message a GUI would show in a dialog goes to the browser of the session the GUI belongs to
        GUIMessages.setHandler((parent, title, message) -> {
            GameSession session = byFrame.get(SwingUtilities.getWindowAncestor(parent));
            if (session != null) session.sendMessage(title, message);
            else System.out.println("GUI message with no session: " + message);
        });
    }

    public synchronized void onMessage(String text) {
        if (stopped) return;
        lastActivity = System.currentTimeMillis();
        JsonObject msg = JsonParser.parseString(text).getAsJsonObject();
        String type = msg.get("type").getAsString();
        if (type.equals("resize")) {
            int w = msg.get("w").getAsInt(), h = msg.get("h").getAsInt();
            double dpr = msg.get("dpr").getAsDouble();
            if (started) {
                resize(w, h, dpr);
                return;
            }
            try {
                start(w, h, dpr);
            } catch (RuntimeException e) {
                e.printStackTrace();
                // nothing was left running to stop
                started = false;
                sendError("The game could not be started: " + e.getMessage());
                out.close();
            }
        } else if (!started) {
            return;
        } else if (type.equals("action")) {
            int i = msg.get("i").getAsInt();
            String label = msg.get("label").getAsString();
            SwingUtilities.invokeLater(() -> chrome.choose(i, label));
        } else if (type.equals("log")) {
            out.sendText(gameLog().toString());
        } else if (type.equals("actionHover")) {
            int i = msg.get("i").getAsInt();
            boolean enter = msg.get("enter").getAsBoolean();
            SwingUtilities.invokeLater(() -> chrome.hover(i, enter));
        } else {
            input.handle(type, msg);
        }
    }

    /**
     * When the browser player last sent anything (System.currentTimeMillis()).
     */
    public long lastActivity() {
        return lastActivity;
    }

    private void start(int w, int h, double dpr) {
        started = true;
        GameType gameType = config.game();
        long seed = config.seed() == -1 ? System.currentTimeMillis() : config.seed();
        AbstractParameters params = gameType.createParameters(seed);
        GameCatalog.apply(params, config.params());

        ActionController ac = new ActionController();
        List<AbstractPlayer> players = new ArrayList<>();
        for (int i = 0; i < config.nPlayers(); i++) {
            AbstractPlayer player = i == config.seat() ? new HumanGUIPlayer(ac) : opponents.find(gameType, config.opponents().get(i)).create();
            // the GUIs show each player's name; an agent's is its file name
            if (i == config.seat()) player.setName("Human");
            players.add(player);
        }
        game = gameType.createGameInstance(config.nPlayers(), seed, params);
        game.reset(players);
        game.setTurnPause(config.turnPause());
        if (config.insight()) game.addListener(new AIInsight(game, out, config.seat()));
        this.seed = seed;

        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("TAG web session " + id);
                frame.setUndecorated(true);
                frame.setFocusableWindowState(false);
                GamePanel panel = new GamePanel();
                frame.setContentPane(panel);
                gui = gameType.createGUIManager(panel, game, ac);
                frame.setSize(w, h);
                space = new Dimension(w, h);
                this.dpr = dpr;
                // Off screen, unless debugging. Its events come from the browser, so where it is does not matter.
                frame.setLocation(showFrames ? 0 : -10_000, 0);
                frame.setVisible(true);
                chrome = new ChromeReader(gui, out);
                fitFrame();
                byFrame.put(frame, this);
                sendStarted(seed, players);
                guiUpdater = new Timer((int) game.getCoreParameters().frameSleepMS, e -> {
                    game.updateGUI(gui, frame);
                    // a GUI can grow as the game goes on
                    fitFrame();
                    chrome.update();
                    reportProgress();
                });
                guiUpdater.start();
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not create the GUI for " + gameType, e);
        }

        input = new InputForwarder(frame, out);
        streamer = new FrameStreamer(frame, out, imageScale);
        streamer.start();

        gameThread = new Thread(this::runGame, "web-game-" + id);
        gameThread.setDaemon(true);
        gameThread.start();
        System.out.printf("Session %d: %s, %d players, browser in seat %d, seed %d%n", id, gameType.name(), config.nPlayers(), config.seat(), seed);
    }

    private void runGame() {
        try {
            game.run();
        } catch (Throwable t) {
            if (!stopped) {
                t.printStackTrace();
                sendError("The game stopped with an error: " + t);
            }
        }
    }

    private void sendStarted(long seed, List<AbstractPlayer> players) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "started");
        msg.addProperty("game", config.game().name());
        msg.addProperty("seed", seed);
        msg.addProperty("seat", config.seat());
        msg.add("players", playerNames(players));
        msg.addProperty("actionsInPage", chrome.hasActions());
        msg.addProperty("infoInPage", chrome.hasInfo());
        out.sendText(msg.toString());
    }

    private void sendMessage(String title, String message) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "message");
        msg.addProperty("title", title);
        msg.addProperty("text", message);
        out.sendText(msg.toString());
    }

    private JsonArray playerNames(List<AbstractPlayer> players) {
        JsonArray names = new JsonArray();
        for (int i = 0; i < players.size(); i++)
            names.add(i == config.seat() ? "You" : players.get(i).toString());
        return names;
    }

    /**
     * Tells the browser whose turn it is when that changes, and the results once the game is over.
     */
    private void reportProgress() {
        AbstractGameState state = game.getGameState();
        if (!state.isNotTerminal()) {
            if (reportedResults) return;
            reportedResults = true;
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "gameOver");
            JsonArray results = new JsonArray();
            for (int i = 0; i < state.getNPlayers(); i++) {
                JsonObject r = new JsonObject();
                r.addProperty("player", i);
                r.addProperty("position", state.getOrdinalPosition(i));
                r.addProperty("score", state.getGameScore(i));
                r.addProperty("result", String.valueOf(state.getPlayerResults()[i]));
                results.add(r);
            }
            msg.add("results", results);
            out.sendText(msg.toString());
            return;
        }
        // the player being asked for an action, which with simultaneous moves need not be the current player
        int player = game.getPlayerToMove();
        boolean yourTurn = game.getPlayers().get(player) instanceof HumanGUIPlayer;
        String turn = yourTurn + ":" + player;
        if (turn.equals(reportedTurn)) return;
        reportedTurn = turn;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "turn");
        msg.addProperty("yourTurn", yourTurn);
        msg.addProperty("player", player);
        out.sendText(msg.toString());
    }

    /**
     * A record of the game so far, for a bug report or to set the game up again. The actions are described as the
     * browser player saw them.
     */
    private JsonObject gameLog() {
        JsonObject log = new JsonObject();
        log.addProperty("type", "log");
        log.addProperty("game", config.game().name());
        log.addProperty("seed", seed);
        log.addProperty("started", java.time.Instant.ofEpochMilli(startedAt).toString());
        log.addProperty("browserSeat", config.seat());
        JsonArray players = new JsonArray();
        for (int i = 0; i < config.nPlayers(); i++) {
            JsonObject p = new JsonObject();
            p.addProperty("seat", i);
            p.addProperty("agent", config.opponents().get(i));
            p.addProperty("name", game.getPlayers().get(i).toString());
            players.add(p);
        }
        log.add("players", players);
        JsonObject changed = new JsonObject();
        config.params().forEach(changed::addProperty);
        log.add("changedParameters", changed);
        JsonObject all = new JsonObject();
        if (game.getGameState().getGameParameters() instanceof evaluation.optimisation.TunableParameters<?> tp)
            tp.getParameterNames().forEach(n -> all.addProperty(n, String.valueOf(tp.getParameterValue(n))));
        log.add("parameters", all);

        AbstractGameState state = game.getGameState();
        JsonArray actions = new JsonArray();
        Set<Integer> perspective = Set.of(config.seat());
        for (var entry : history(state)) {
            JsonObject a = new JsonObject();
            a.addProperty("player", entry.a);
            a.addProperty("action", entry.b.getString(state, perspective));
            actions.add(a);
        }
        log.add("actions", actions);
        log.addProperty("gameStatus", String.valueOf(state.getGameStatus()));
        if (!state.isNotTerminal()) {
            JsonArray results = new JsonArray();
            for (int i = 0; i < state.getNPlayers(); i++) {
                JsonObject r = new JsonObject();
                r.addProperty("player", i);
                r.addProperty("position", state.getOrdinalPosition(i));
                r.addProperty("score", state.getGameScore(i));
                results.add(r);
            }
            log.add("results", results);
        }
        return log;
    }

    private static List<utilities.Pair<Integer, core.actions.AbstractAction>> history(AbstractGameState state) {
        // the game thread adds to the history, so copying it may meet a change; it is then copied again
        for (int attempt = 0; ; attempt++) {
            try {
                return state.getHistory();
            } catch (java.util.ConcurrentModificationException e) {
                if (attempt == 5) throw e;
            }
        }
    }

    private void sendError(String message) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "error");
        msg.addProperty("message", message);
        out.sendText(msg.toString());
    }

    private void resize(int w, int h, double dpr) {
        SwingUtilities.invokeLater(() -> {
            space = new Dimension(w, h);
            this.dpr = dpr;
            fitFrame();
        });
    }

    /**
     * Sizes the frame to the browser's space or, where the GUI needs more, to a larger frame of the same proportions,
     * which the browser scales down to fit. Swing thread only.
     */
    private void fitFrame() {
        Container content = frame.getContentPane();
        LayoutManager layout = content.getLayout();
        // Many GUIs are laid out at a fixed size. The layout's own preferred size is used, not the size the GUI may have
        // set on its panel, as that counts the panels the ChromeReader hides.
        Dimension needed = layout != null ? layout.preferredLayoutSize(content) : content.getPreferredSize();
        // scaled down by s, a frame of the space's proportions fills the space exactly
        double s = Math.min(1, Math.min((double) space.width / needed.width, (double) space.height / needed.height));
        Dimension size = new Dimension((int) Math.ceil(space.width / s), (int) Math.ceil(space.height / s));
        if (!size.equals(frame.getSize())) {
            frame.setSize(size);
            frame.validate();
        }
        // the browser shows the frame scaled by s, so an image at dpr * s is as sharp as it can show
        imageScale = dpr * s;
        if (streamer != null) streamer.setScale(imageScale);
    }

    /**
     * Tells the browser the game was ended for want of activity, and closes the connection (which stops the session).
     */
    public void endIdle(int minutes) {
        sendError("This game was ended after " + minutes + (minutes == 1 ? " minute" : " minutes") + " without activity.");
        out.close();
    }

    /**
     * Ends the game and disposes of the frame.
     */
    public synchronized void stop() {
        if (stopped) return;
        stopped = true;
        if (!started) return;
        game.setStopped(true);
        // the game thread may be waiting for the browser player's action
        gameThread.interrupt();
        streamer.stop();
        SwingUtilities.invokeLater(() -> {
            guiUpdater.stop();
            byFrame.remove(frame);
            frame.dispose();
        });
        System.out.printf("Session %d: stopped%n", id);
    }
}

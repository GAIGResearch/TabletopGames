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
import java.util.ArrayList;
import java.awt.*;
import java.util.List;
import java.util.Map;
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
    // the image is drawn at dpr * the scale the browser shows the frame at (see fitFrame)
    private volatile double imageScale;
    private volatile long lastActivity = System.currentTimeMillis();
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
     * Swing settings for a JVM whose GUIs are all streamed. Swing's own tooltips are switched off, as Swing moves them
     * on to a real screen and so out of the frame; the InputForwarder sends their text to the browser instead. Messages
     * a GUI would show in a dialog are sent to its browser.
     *
     * @param lookAndFeel "flat" for FlatLaf's light look and feel, or "default" for Swing's own
     */
    public static void configureSwing(String lookAndFeel) {
        if (lookAndFeel.equalsIgnoreCase("flat"))
            FlatLightLaf.setup();
        else if (!lookAndFeel.equalsIgnoreCase("default"))
            throw new IllegalArgumentException("Unknown look and feel (flat or default): " + lookAndFeel);
        ToolTipManager.sharedInstance().setEnabled(false);
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
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
                sendError("The game could not be started: " + e.getMessage());
                out.close();
            }
        } else if (!started) {
            return;
        } else if (type.equals("action")) {
            int i = msg.get("i").getAsInt();
            String label = msg.get("label").getAsString();
            SwingUtilities.invokeLater(() -> chrome.choose(i, label));
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
        for (int i = 0; i < config.nPlayers(); i++)
            players.add(i == config.seat() ? new HumanGUIPlayer(ac) : opponents.find(gameType, config.opponents().get(i)).create());
        game = gameType.createGameInstance(config.nPlayers(), seed, params);
        game.reset(players);
        game.setTurnPause(config.turnPause());

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
     * Tells the browser whose turn it is when that changes, and the results once the game is over. Runs on the Swing
     * thread, after each GUI update.
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
        boolean yourTurn = game.isHumanToMove();
        int player = state.getCurrentPlayer();
        String turn = yourTurn + ":" + player;
        if (turn.equals(reportedTurn)) return;
        reportedTurn = turn;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "turn");
        msg.addProperty("yourTurn", yourTurn);
        msg.addProperty("player", player);
        out.sendText(msg.toString());
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
     * Sizes the frame to the browser's space or, where the GUI's layout needs more (many GUIs are laid out at a fixed
     * size), to a larger frame of the same proportions, which the browser scales down to fit. The layout's own preferred size is used, not the
     * size the GUI may have set on its panel, as that counts the panels the ChromeReader hides. A GUI can grow as the
     * game goes on, so this runs after every update. Swing thread only.
     */
    private void fitFrame() {
        Container content = frame.getContentPane();
        LayoutManager layout = content.getLayout();
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
     * Ends the session because the browser player has been away too long: tells the browser, then closes the
     * connection, which stops the session.
     */
    public void endIdle(int minutes) {
        sendError("This game was ended after " + minutes + (minutes == 1 ? " minute" : " minutes") + " without activity.");
        out.close();
    }

    /**
     * Ends the game (the game thread is interrupted while it waits for the browser player's action) and disposes of
     * the frame.
     */
    public synchronized void stop() {
        if (stopped) return;
        stopped = true;
        if (!started) return;
        game.setStopped(true);
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

package web;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GUIMessages;
import gui.GamePanel;
import gui.IMovePlanner;
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
import java.util.function.Predicate;

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
 * <p>
 * If the GUI offers a planner (see {@link IMovePlanner}), the browser player plans the decisions it covers on a copy
 * of the game (a {@link MovePlan}): the GUI shows the planned state and offers the plan's options, and a choice adds
 * to the plan rather than going to the game, until the plan is sent (see {@link BrowserPlayer}).
 * <p>
 * The session outlives its connection: if the browser's connection is lost (a phone asleep, a network change), the game
 * carries on, and the browser may reconnect to it with the session's key (see {@link #attach}).
 */
public class GameSession {

    private static final AtomicInteger ids = new AtomicInteger();
    // to find the session a GUI's message (see GUIMessages) comes from
    private static final Map<Window, GameSession> byFrame = new ConcurrentHashMap<>();

    private final int id = ids.incrementAndGet();
    private final SessionConfig config;
    private final OpponentCatalog opponents;
    private final RelaySender out;
    private final boolean showFrames;
    // the secret with which the browser may reconnect to this session
    private final String key = newKey();
    // when the browser's connection was lost (0 while it has one)
    private volatile long detachedAt;

    private Game game;
    private JFrame frame;
    private AbstractGUIManager gui;
    private Timer guiUpdater;
    private Thread gameThread;
    private FrameStreamer streamer;
    private InputForwarder input;
    private ChromeReader chrome;
    private BrowserPlayer browser;
    private DivertingController ac;
    // the plan being made, and the plan last sent to the page (Swing thread only)
    private MovePlan plan;
    private String sentPlan;
    private volatile boolean started, stopped;
    // for updateDue: used on the Swing thread, except updateNow, which the browser's input sets
    private int lastGameTick = -1;
    private boolean lastHumanToMove;
    private long lastChange, lastUpdate;
    private volatile boolean updateNow = true;
    // the size of the browser's space for the GUI, and its device pixel ratio (Swing thread only)
    private Dimension space;
    private double dpr;
    // the zoom the page asked for, or 0 to fit the GUI to the space (Swing thread only)
    private double zoom;
    // image pixels per frame pixel (see fitFrame)
    private volatile double imageScale;
    // the scale at which the browser shows the frame (see fitFrame)
    private volatile double displayScale = 1;
    private volatile long lastActivity = System.currentTimeMillis();
    private final long startedAt = System.currentTimeMillis();
    private long seed;
    // the last turn reported to the browser, and whether the results have been sent (Swing thread only)
    private String reportedTurn;
    private boolean reportedResults;

    /**
     * @param connection the browser's connection, to which the session's key is sent at once (see {@link #attach})
     * @param showFrames place the Swing frame on screen (for debugging); otherwise it is off screen
     */
    public GameSession(SessionConfig config, OpponentCatalog opponents, Sender connection, boolean showFrames) {
        this.config = config;
        this.opponents = opponents;
        this.out = new RelaySender(connection);
        this.showFrames = showFrames;
        sendKey();
    }

    private static String newKey() {
        byte[] bytes = new byte[18];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String key() {
        return key;
    }

    private void sendKey() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "session");
        msg.addProperty("key", key);
        out.sendText(msg.toString());
    }

    /**
     * Carries on the session over a new connection from the browser (one that has reconnected, or the same game
     * opened in another window), sending it all the page needs. Returns the connection it replaces, or null.
     */
    public synchronized Sender attach(Sender connection) {
        if (streamer != null) streamer.resendAll();
        Sender previous = out.attach(connection);
        detachedAt = 0;
        lastActivity = System.currentTimeMillis();
        updateNow = true;
        sendKey();
        if (started && !stopped)
            SwingUtilities.invokeLater(() -> {
                sendStarted(seed, game.getPlayers());
                chrome.resend();
                sentPlan = null;
                reportedTurn = null;
                reportedResults = false;
            });
        return previous;
    }

    /**
     * Stops sending to the connection, which has been lost. The session carries on until the browser reconnects (see
     * {@link #attach}) or it is stopped. Returns whether the connection was the session's current one.
     */
    public synchronized boolean detach(Sender connection) {
        if (!out.detach(connection)) return false;
        detachedAt = System.currentTimeMillis();
        return true;
    }

    public long detachedAt() {
        return detachedAt;
    }

    /**
     * Whether there is a game for a browser to come back to.
     */
    public boolean isResumable() {
        return started && !stopped && game.getGameState().isNotTerminal();
    }

    public boolean isStopped() {
        return stopped;
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
        // so that a frame is streamed only when something in it has changed
        DirtyTracker.install();
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
        // Anything but a mouse move may change what the GUI shows (a click may change its tab, say), so the GUI is
        // updated. A move's effects are the views' own, and they repaint themselves.
        if (!(type.equals("mouse") && msg.get("kind").getAsString().equals("move"))) updateNow = true;
        if (type.equals("resize")) {
            int w = msg.get("w").getAsInt(), h = msg.get("h").getAsInt();
            double dpr = msg.get("dpr").getAsDouble();
            // the page's zoom, or none (0) to fit the GUI to the space
            double zoom = msg.has("zoom") && !msg.get("zoom").isJsonNull() ? msg.get("zoom").getAsDouble() : 0;
            zoom = zoom <= 0 ? 0 : Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
            if (started) {
                resize(w, h, dpr, zoom);
                return;
            }
            try {
                start(w, h, dpr, zoom);
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
            streamer.nudge();
        } else if (type.equals("region")) {
            int v = msg.get("v").getAsInt(), r = msg.get("r").getAsInt(), o = msg.get("o").getAsInt();
            SwingUtilities.invokeLater(() -> chrome.chooseRegion(v, r, o));
            streamer.nudge();
        } else if (type.equals("plan")) {
            String op = msg.get("op").getAsString();
            int i = msg.has("i") ? msg.get("i").getAsInt() : -1;
            SwingUtilities.invokeLater(() -> planOp(op, i));
            streamer.nudge();
        } else if (type.equals("log")) {
            out.sendText(gameLog().toString());
        } else if (type.equals("actionHover")) {
            int i = msg.get("i").getAsInt();
            boolean enter = msg.get("enter").getAsBoolean();
            SwingUtilities.invokeLater(() -> chrome.hover(i, enter));
            streamer.nudge();
        } else if (type.equals("quit")) {
            // the player has left this game for another, so it will not be resumed
            stop();
            out.close();
        } else {
            input.handle(type, msg);
            streamer.nudge();
        }
    }

    /**
     * When the browser player last sent anything (System.currentTimeMillis()).
     */
    public long lastActivity() {
        return lastActivity;
    }

    private void start(int w, int h, double dpr, double zoom) {
        started = true;
        GameType gameType = config.game();
        long seed = config.seed() == -1 ? System.currentTimeMillis() : config.seed();
        AbstractParameters params = gameType.createParameters(seed);
        GameCatalog.apply(params, config.params());

        ac = new DivertingController();
        browser = new BrowserPlayer(ac, text -> sendMessage("Plan stopped", text));
        List<AbstractPlayer> players = new ArrayList<>();
        for (int i = 0; i < config.nPlayers(); i++) {
            AbstractPlayer player = i == config.seat() ? browser : opponents.find(gameType, config.opponents().get(i)).create();
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
                this.zoom = zoom;
                // Off screen, unless debugging. Its events come from the browser, so where it is does not matter.
                frame.setLocation(showFrames ? 0 : -10_000, 0);
                frame.setVisible(true);
                chrome = new ChromeReader(gui, game, frame.getRootPane(), gameType, out);
                fitFrame();
                byFrame.put(frame, this);
                sendStarted(seed, players);
                // a choice made while planning adds to the plan
                ac.divert = this::addToPlan;
                guiUpdater = new Timer((int) game.getCoreParameters().frameSleepMS, e -> tick());
                guiUpdater.start();
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not create the GUI for " + gameType, e);
        }

        input = new InputForwarder(frame, out);
        streamer = new FrameStreamer(frame, out, imageScale, displayScale);
        streamer.start();

        gameThread = new Thread(this::runGame, "web-game-" + id);
        gameThread.setDaemon(true);
        gameThread.start();
        System.out.printf("Session %d: %s, %d players, browser in seat %d, seed %d%n", id, gameType.name(), config.nPlayers(), config.seat(), seed);
    }

    /**
     * Updates the GUI, if due, and sends the page what has changed. Swing thread only.
     */
    private void tick() {
        // with no browser to see it, the GUI waits until one reconnects (see attach)
        if (detachedAt > 0 || !updateDue()) return;
        updatePlan();
        // As game.updateGUI does, but without repainting the frame: the frame streamer paints it, and a repaint would
        // paint it a second time, for a screen no one sees.
        if (plan != null) {
            gui.offerInstead(plan.options());
            gui.update(browser, plan.preview(), true);
        } else {
            gui.update(game.getPlayers().get(game.getPlayerToMove()), game.getGameState(), game.isHumanToMove());
        }
        // a GUI can grow as the game goes on
        fitFrame();
        chrome.update();
        sendPlan();
        reportProgress();
    }

    /**
     * Starts a plan when the game waits for a decision the GUI's planner covers, and drops one whose decision has
     * passed.
     */
    private void updatePlan() {
        IMovePlanner planner = gui.getPlanner();
        if (planner == null) return;
        AbstractGameState waiting = browser.waitingOn();
        if (plan != null && plan.decision() != waiting) endPlan();
        if (plan == null && waiting != null && !browser.isSending() && planner.plans(waiting, config.seat()))
            plan = new MovePlan(planner, waiting, config.seat(), gui::actionLabel);
    }

    private void endPlan() {
        plan = null;
        gui.offerInstead(null);
    }

    /**
     * Adds a choice to the plan, if one is being made. Returns whether it was taken for the plan (and so not passed
     * to the game).
     */
    private boolean addToPlan(AbstractAction action) {
        if (plan == null) return false;
        plan.add(action);
        refresh();
        return true;
    }

    /**
     * A change to the plan from the page: "remove" step i, "clear" all of it, or "send" it to the game.
     */
    private void planOp(String op, int i) {
        if (plan == null) return;
        switch (op) {
            case "remove" -> plan.remove(i);
            case "clear" -> plan.clear();
            case "send" -> {
                MovePlan sent = plan;
                endPlan();
                browser.send(gui.getPlanner(), sent.decision(), sent.steps());
            }
            default -> {
                return;
            }
        }
        refresh();
    }

    /**
     * Updates the GUI as soon as the Swing thread is free (not at once: a choice is made from within a button's or
     * a view's handler, which may still be changing the GUI).
     */
    private void refresh() {
        updateNow = true;
        SwingUtilities.invokeLater(this::tick);
    }

    /**
     * Sends the plan to the page when it has changed (no steps when there is no plan).
     */
    private void sendPlan() {
        JsonObject msg;
        if (plan != null) {
            msg = plan.toJson();
        } else {
            msg = new JsonObject();
            msg.addProperty("type", "plan");
        }
        String text = msg.toString();
        if (text.equals(sentPlan)) return;
        sentPlan = text;
        out.sendText(text);
    }

    /**
     * Whether the GUI should be updated on this tick of the GUI timer.
     */
    private boolean updateDue() {
        // Updating a GUI repaints much of it, which is most of a session's work, so it is done only while the game is
        // moving: when the game state has moved on, and for a while after, as the game thread may still be finishing
        // a move when the tick changes; after input from the browser; and every few seconds regardless.
        long now = System.currentTimeMillis();
        int tick = game.getGameState().getGameTick();
        boolean human = game.isHumanToMove();
        if (tick != lastGameTick || human != lastHumanToMove) {
            lastGameTick = tick;
            lastHumanToMove = human;
            lastChange = now;
        }
        if (!updateNow && now - lastChange > UPDATE_SETTLE_MS && now - lastUpdate < FrameStreamer.REFRESH_MS)
            return false;
        updateNow = false;
        lastUpdate = now;
        return true;
    }

    static final long UPDATE_SETTLE_MS = 1000;

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

    private void resize(int w, int h, double dpr, double zoom) {
        SwingUtilities.invokeLater(() -> {
            space = new Dimension(w, h);
            this.dpr = dpr;
            this.zoom = zoom;
            fitFrame();
        });
    }

    // the zooms the page may ask for, and the most a GUI is enlarged to fit its space
    static final double MIN_ZOOM = 0.25, MAX_ZOOM = 4, MAX_FIT = 3;
    // a GUI whose layout asks for less than this (in either direction) gives no real size of its own
    static final int MIN_LAYOUT = 100;
    // the most pixels in an image; beyond this a zoomed frame is drawn less sharply, rather than sent larger
    static final double MAX_IMAGE_PIXELS = 12_000_000;

    /**
     * Sizes the frame, and sets the scales at which it is drawn and shown. Swing thread only.
     */
    private void fitFrame() {
        Container content = frame.getContentPane();
        LayoutManager layout = content.getLayout();
        // Many GUIs are laid out at a fixed size. The layout's own preferred size is used, not the size the GUI may have
        // set on its panel, as that counts the panels the ChromeReader hides.
        Dimension needed = layout != null ? layout.preferredLayoutSize(content) : content.getPreferredSize();
        needed = new Dimension(Math.max(1, needed.width), Math.max(1, needed.height));
        // The browser shows the frame scaled by s: the zoom the page asked for or, with none, the scale at which the
        // GUI just fits the space (enlarged, up to MAX_FIT, if it needs less). The page centres a frame smaller than
        // its space, and scrolls one larger.
        double fit = Math.min(MAX_FIT, Math.min((double) space.width / needed.width, (double) space.height / needed.height));
        double s = zoom > 0 ? zoom : fit;
        // The frame is laid out at the GUI's own size, not larger to fill the space: many views draw from their top
        // left corner, and would be left against one side of a larger frame.
        Dimension size = needed;
        if (needed.width < MIN_LAYOUT || needed.height < MIN_LAYOUT) {
            // a GUI that gives no real size of its own is laid out to the space, as it would be in a window
            s = zoom > 0 ? zoom : 1;
            size = new Dimension((int) Math.floor(space.width / s), (int) Math.floor(space.height / s));
        }
        if (!size.equals(frame.getSize())) {
            frame.setSize(size);
            frame.validate();
        }
        // an image at dpr * s is as sharp as the browser can show, unless that is too many pixels
        double image = dpr * s;
        double pixels = image * image * size.width * size.height;
        if (pixels > MAX_IMAGE_PIXELS) image *= Math.sqrt(MAX_IMAGE_PIXELS / pixels);
        imageScale = image;
        displayScale = s;
        if (streamer != null) streamer.setScale(imageScale, s);
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
    /**
     * The browser player's action controller, whose choices may be diverted (to a plan) rather than go to the game.
     */
    private static class DivertingController extends ActionController {

        // given each choice first, and returns whether it took it
        volatile Predicate<AbstractAction> divert;

        @Override
        public void addAction(AbstractAction candidate) {
            Predicate<AbstractAction> d = divert;
            if (d != null && candidate != null && d.test(candidate)) return;
            super.addAction(candidate);
        }
    }

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

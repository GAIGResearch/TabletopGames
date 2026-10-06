package web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.PlayerFactory;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One browser player's game: the {@link Game} on its own thread, with a {@link HumanGUIPlayer} in the browser player's
 * seat, and the game's Swing GUI in a frame no one sees. A {@link FrameStreamer} sends what the frame shows to the
 * browser, and an {@link InputForwarder} turns the browser's mouse and key events into events on the frame, so the GUI
 * works exactly as it does on the desktop.
 * <p>
 * The session starts on the browser's first resize message, which gives the size to lay the GUI out at.
 */
public class GameSession {

    private static final AtomicInteger ids = new AtomicInteger();

    private final int id = ids.incrementAndGet();
    private final SessionConfig config;
    private final Sender out;

    private Game game;
    private JFrame frame;
    private AbstractGUIManager gui;
    private Timer guiUpdater;
    private Thread gameThread;
    private FrameStreamer streamer;
    private InputForwarder input;
    private volatile boolean started, stopped;

    public GameSession(SessionConfig config, Sender out) {
        this.config = config;
        this.out = out;
    }

    /**
     * Swing settings for a JVM whose GUIs are all streamed. Swing's own tooltips are switched off, as Swing moves them
     * on to a real screen and so out of the frame; the InputForwarder sends their text to the browser instead.
     */
    public static void configureSwing() {
        ToolTipManager.sharedInstance().setEnabled(false);
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
    }

    public synchronized void onMessage(String text) {
        if (stopped) return;
        JsonObject msg = JsonParser.parseString(text).getAsJsonObject();
        String type = msg.get("type").getAsString();
        if (type.equals("resize")) {
            int w = msg.get("w").getAsInt(), h = msg.get("h").getAsInt();
            double dpr = msg.get("dpr").getAsDouble();
            if (!started) start(w, h, dpr);
            else resize(w, h, dpr);
        } else if (started) {
            input.handle(type, msg);
        }
    }

    private void start(int w, int h, double dpr) {
        started = true;
        GameType gameType = config.gameType();
        ActionController ac = new ActionController();
        List<AbstractPlayer> players = new ArrayList<>();
        for (int i = 0; i < config.nPlayers(); i++)
            players.add(i == config.seat() ? new HumanGUIPlayer(ac) : PlayerFactory.createPlayer(config.opponent()));
        long seed = config.seed() == -1 ? System.currentTimeMillis() : config.seed();
        game = gameType.createGameInstance(config.nPlayers(), seed);
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
                // Off screen, unless debugging. Its events come from the browser, so where it is does not matter.
                frame.setLocation(config.showFrames() ? 0 : -10_000, 0);
                frame.setVisible(true);
                frame.validate();
                guiUpdater = new Timer((int) game.getCoreParameters().frameSleepMS, e -> game.updateGUI(gui, frame));
                guiUpdater.start();
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not create the GUI for " + gameType, e);
        }

        input = new InputForwarder(frame, out);
        streamer = new FrameStreamer(frame, out, dpr);
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
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "error");
                msg.addProperty("message", "The game stopped with an error: " + t);
                out.sendText(msg.toString());
            }
        }
    }

    private void resize(int w, int h, double dpr) {
        SwingUtilities.invokeLater(() -> {
            frame.setSize(w, h);
            frame.validate();
        });
        streamer.setDpr(dpr);
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
            frame.dispose();
        });
        System.out.printf("Session %d: stopped%n", id);
    }
}

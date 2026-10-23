package web;

import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import utilities.Utils;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serves TAG games to a browser. The game, its AI players and its Swing GUI all run here; the GUI is drawn into a
 * frame no one sees, streamed to the browser as images, and driven by the mouse and key events the browser sends back.
 * Each WebSocket connection plays one {@link GameSession}, set up from the connection's query string.
 * <p>
 * Arguments (key=value):
 * <ul>
 *     <li>port (8080)</li>
 *     <li>games: the games offered, comma separated, or "all" for every game with a GUI (all)</li>
 *     <li>agents: the directory of agent JSON files to offer as opponents in every game, besides any in
 *     data/&lt;game&gt;/agents; see {@link OpponentCatalog} (json/players/webserver)</li>
 *     <li>maxSessions: the most games played at once (3)</li>
 *     <li>idleMinutes: a game with no input from its browser for this long is ended (30)</li>
 *     <li>resumeMinutes: how long a game waits for its browser to reconnect after losing its connection (10). A game
 *     waiting counts towards maxSessions, but the one waiting longest is ended to make room for a new one.</li>
 *     <li>token: a secret that every visitor must have, given once in the link ({@code /?token=...}) and then kept in a
 *     cookie; see {@link AccessToken} (the TAG_TOKEN environment variable, if set, which keeps the secret off the
 *     command line; otherwise none, and the server is open)</li>
 *     <li>lookAndFeel: "flat" (FlatLaf) or "default" (Swing's own) for the GUIs' widgets (flat)</li>
 *     <li>showFrames: true to place the Swing frames on screen, for debugging (false)</li>
 * </ul>
 */
public class WebServer {

    public static void main(String[] args) {
        // Java on Linux draws text without antialiasing unless asked. Grey-scale, not sub-pixel, as the browser may
        // scale the image.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        int port = Utils.getArg(args, "port", 8080);
        GameCatalog games = new GameCatalog(Utils.getArg(args, "games", "all"));
        OpponentCatalog opponents = new OpponentCatalog(Utils.getArg(args, "agents", OpponentCatalog.DEFAULT_DIR));
        int maxSessions = Utils.getArg(args, "maxSessions", 3);
        int idleMinutes = Utils.getArg(args, "idleMinutes", 30);
        boolean showFrames = Utils.getArg(args, "showFrames", false);
        String lookAndFeel = Utils.getArg(args, "lookAndFeel", "flat");
        String token = Utils.getArg(args, "token", System.getenv().getOrDefault("TAG_TOKEN", ""));
        AccessToken access = new AccessToken(token);

        int resumeMinutes = Utils.getArg(args, "resumeMinutes", 10);

        GameSession.configureSwing(lookAndFeel);
        // the sessions by key, and the connections by WebSocket id
        Map<String, GameSession> sessions = new ConcurrentHashMap<>();
        Map<String, Connection> connections = new ConcurrentHashMap<>();

        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.staticFiles.add("/web", Location.CLASSPATH);
        });

        app.before(access::check);

        app.get("/api/games", ctx -> ctx.contentType("application/json").result(games.describe().toString()));
        app.get("/api/opponents", ctx -> {
            try {
                String game = ctx.queryParam("game");
                ctx.contentType("application/json").result(opponents.describe(game == null ? games.games().get(0) : games.find(game)).toString());
            } catch (IllegalArgumentException e) {
                ctx.status(400).result(e.getMessage());
            }
        });

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                ctx.enableAutomaticPings();
                WsSender sender = new WsSender(ctx);
                try {
                    if (!access.allows(ctx))
                        throw new IllegalStateException("This game server is private: open it with the link you were sent.");
                    String resume = ctx.queryParam("resume");
                    if (resume != null) {
                        GameSession session = sessions.get(resume);
                        if (session == null || !session.isResumable()) {
                            sendError(sender, "This game is no longer on the server.", "gone");
                            return;
                        }
                        connections.put(ctx.sessionId(), new Connection(session, sender));
                        Sender previous = session.attach(sender);
                        // The game was open in another window, which is told so, rather than left to reconnect and
                        // take the game back.
                        if (previous != null) sendError(previous, "This game was opened in another window.", "moved");
                        return;
                    }
                    if (sessions.size() >= maxSessions) {
                        // make room by ending the game whose player has been gone longest, if any has gone
                        sessions.values().stream().filter(s -> s.detachedAt() > 0)
                                .min(Comparator.comparingLong(GameSession::detachedAt))
                                .ifPresent(s -> end(sessions, s));
                    }
                    if (sessions.size() >= maxSessions)
                        throw new IllegalStateException("The server is busy (" + maxSessions + " games in play). Try again later.");
                    SessionConfig config = SessionConfig.fromQuery(ctx.queryParamMap(), games, opponents);
                    GameSession session = new GameSession(config, opponents, sender, showFrames);
                    sessions.put(session.key(), session);
                    connections.put(ctx.sessionId(), new Connection(session, sender));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    sendError(sender, e.getMessage(), null);
                } catch (Throwable t) {
                    // anything else is a fault in the server or a game, not in the setup
                    System.out.println("Could not start a game for " + ctx.queryString());
                    t.printStackTrace();
                    sendError(sender, "The game could not be started: " + t, null);
                }
            });
            ws.onMessage(ctx -> {
                Connection c = connections.get(ctx.sessionId());
                if (c != null) c.session().onMessage(ctx.message());
            });
            ws.onClose(ctx -> disconnected(sessions, connections.remove(ctx.sessionId())));
            ws.onError(ctx -> {
                if (ctx.error() != null) {
                    System.out.println("WebSocket error in session " + ctx.sessionId());
                    ctx.error().printStackTrace();
                }
                disconnected(sessions, connections.remove(ctx.sessionId()));
            });
        });

        ScheduledExecutorService idleCheck = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "web-idle-check");
            t.setDaemon(true);
            return t;
        });
        idleCheck.scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();
            for (GameSession s : sessions.values()) {
                if (s.isStopped()) {
                    end(sessions, s);
                } else if (s.detachedAt() > 0 && s.detachedAt() < now - resumeMinutes * 60_000L) {
                    System.out.println("Ending a game its player left " + resumeMinutes + " minutes ago");
                    end(sessions, s);
                } else if (s.lastActivity() < now - idleMinutes * 60_000L) {
                    s.endIdle(idleMinutes);
                    end(sessions, s);
                }
            }
        }, 10, 10, TimeUnit.SECONDS);

        app.start(port);
        System.out.printf("Serving %s on http://localhost:%d/%s%n",
                games.games().size() == 1 ? games.games().get(0).name() : games.games().size() + " games", port,
                access.isOpen() ? "" : "?token=" + token);
    }

    /**
     * A WebSocket, and the session it plays.
     */
    private record Connection(GameSession session, Sender sender) {
    }

    /**
     * Called when a WebSocket closes. Its session waits for the browser to reconnect if there is a game to come back
     * to, and otherwise ends.
     */
    private static void disconnected(Map<String, GameSession> sessions, Connection c) {
        if (c == null) return;
        if (c.session().detach(c.sender()) && !c.session().isResumable())
            end(sessions, c.session());
    }

    private static void end(Map<String, GameSession> sessions, GameSession session) {
        sessions.remove(session.key());
        session.stop();
    }

    /**
     * Sends an error and closes the connection.
     *
     * @param code for the page: "gone" for a game it cannot reconnect to, "moved" for one taken over by another window
     */
    private static void sendError(Sender sender, String message, String code) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "error");
        msg.addProperty("message", message);
        if (code != null) msg.addProperty("code", code);
        sender.sendText(msg.toString());
        sender.close();
    }
}

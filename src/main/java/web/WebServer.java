package web;

import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import utilities.Utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serves TAG games to a browser. The game, its AI players and its Swing GUI all run here; the GUI is drawn into a
 * frame no one sees, streamed to the browser as images, and driven by the mouse and key events the browser sends back.
 * <p>
 * The start page (index.html) sets up a game and opens the play page (play.html) with the setup in its query string
 * (see {@link SessionConfig}). The play page's WebSocket carries the same query; each connection gets its own
 * {@link GameSession}, which ends when the connection closes.
 * <p>
 * Arguments (key=value):
 * <ul>
 *     <li>port (8080)</li>
 *     <li>games: the games offered, comma separated, or "all" for every game with a GUI (all)</li>
 *     <li>agents: a directory of agent JSON files to offer as opponents, besides the built-in ones and any in
 *     data/&lt;game&gt;/agents (none)</li>
 *     <li>maxSessions: the most games played at once (3)</li>
 *     <li>idleMinutes: a game with no input from its browser for this long is ended (30)</li>
 *     <li>showFrames: true to place the Swing frames on screen, for debugging (false)</li>
 * </ul>
 */
public class WebServer {

    public static void main(String[] args) {
        int port = Utils.getArg(args, "port", 8080);
        GameCatalog games = new GameCatalog(Utils.getArg(args, "games", "all"));
        OpponentCatalog opponents = new OpponentCatalog(Utils.getArg(args, "agents", ""));
        int maxSessions = Utils.getArg(args, "maxSessions", 3);
        int idleMinutes = Utils.getArg(args, "idleMinutes", 30);
        boolean showFrames = Utils.getArg(args, "showFrames", false);

        GameSession.configureSwing();
        Map<String, GameSession> sessions = new ConcurrentHashMap<>();

        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.staticFiles.add("/web", Location.CLASSPATH);
        });

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
                    if (sessions.size() >= maxSessions)
                        throw new IllegalStateException("The server is busy (" + maxSessions + " games in play). Try again later.");
                    SessionConfig config = SessionConfig.fromQuery(ctx.queryParamMap(), games, opponents);
                    sessions.put(ctx.sessionId(), new GameSession(config, opponents, sender, showFrames));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    JsonObject msg = new JsonObject();
                    msg.addProperty("type", "error");
                    msg.addProperty("message", e.getMessage());
                    sender.sendText(msg.toString());
                    sender.close();
                }
            });
            ws.onMessage(ctx -> {
                GameSession session = sessions.get(ctx.sessionId());
                if (session != null) session.onMessage(ctx.message());
            });
            ws.onClose(ctx -> {
                GameSession session = sessions.remove(ctx.sessionId());
                if (session != null) session.stop();
            });
            ws.onError(ctx -> {
                GameSession session = sessions.remove(ctx.sessionId());
                if (session != null) session.stop();
            });
        });

        ScheduledExecutorService idleCheck = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "web-idle-check");
            t.setDaemon(true);
            return t;
        });
        idleCheck.scheduleWithFixedDelay(() -> {
            long cutoff = System.currentTimeMillis() - idleMinutes * 60_000L;
            sessions.values().stream().filter(s -> s.lastActivity() < cutoff).forEach(s -> s.endIdle(idleMinutes));
        }, 1, 1, TimeUnit.MINUTES);

        app.start(port);
        System.out.printf("Serving %s on http://localhost:%d%n",
                games.games().size() == 1 ? games.games().get(0).name() : games.games().size() + " games", port);
    }
}

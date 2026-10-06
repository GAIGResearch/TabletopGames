package web;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import utilities.Utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves TAG games to a browser. The game, its AI players and its Swing GUI all run here; the GUI is drawn into a
 * frame no one sees, streamed to the browser as images, and driven by the mouse and key events the browser sends back.
 * <p>
 * Each browser connection (one WebSocket) gets its own {@link GameSession}, which ends when the connection closes.
 * <p>
 * Arguments (key=value): port, game, nPlayers, seat (the browser player's seat), opponent (a PlayerFactory key or agent
 * JSON file, used for every other seat), seed (-1 for a new seed each game), turnPause (ms after each AI action), and
 * showFrames (true to place the Swing frames on screen, for debugging).
 */
public class WebServer {

    public static void main(String[] args) {
        int port = Utils.getArg(args, "port", 8080);
        SessionConfig config = new SessionConfig(
                Utils.getArg(args, "game", "LawnAndOrder"),
                Utils.getArg(args, "nPlayers", 3),
                Utils.getArg(args, "seat", 0),
                Utils.getArg(args, "opponent", "mcts"),
                Utils.getArg(args, "seed", -1L),
                Utils.getArg(args, "turnPause", 300),
                Utils.getArg(args, "showFrames", false));
        config.validate();

        GameSession.configureSwing();
        Map<String, GameSession> sessions = new ConcurrentHashMap<>();

        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.staticFiles.add("/web", Location.CLASSPATH);
        });
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                ctx.enableAutomaticPings();
                sessions.put(ctx.sessionId(), new GameSession(config, new WsSender(ctx)));
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
        app.start(port);
        System.out.printf("Serving %s on http://localhost:%d%n", config.game(), port);
    }
}

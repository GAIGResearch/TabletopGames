package web;

import io.javalin.http.Context;
import io.javalin.websocket.WsContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An optional shared secret for the whole server, so that only people sent the link
 * ({@code http://host:port/?token=SECRET}) can play. Every page, API call and WebSocket must carry the token, in the
 * query or in the cookie it sets. With no token configured, the server is open.
 */
class AccessToken {

    static final String COOKIE = "tag_access";
    private static final int COOKIE_DAYS = 30;

    private final byte[] token;

    /**
     * @param token the secret (letters, digits, '-' and '_'), or empty for an open server
     */
    AccessToken(String token) {
        if (token == null || token.isEmpty()) {
            this.token = null;
        } else {
            if (!token.matches("[A-Za-z0-9_-]{8,}"))
                throw new IllegalArgumentException("The token must be at least 8 letters, digits, '-' or '_'");
            this.token = token.getBytes(StandardCharsets.UTF_8);
        }
    }

    boolean isOpen() {
        return token == null;
    }

    /**
     * Lets an HTTP request through if it carries the token; otherwise answers 401 and stops it.
     */
    void check(Context ctx) {
        if (isOpen()) return;
        String fromQuery = ctx.queryParam("token");
        // a token in the query sets the cookie, which then carries it
        if (fromQuery != null && matches(fromQuery)) {
            ctx.header("Set-Cookie", COOKIE + "=" + fromQuery + "; Path=/; Max-Age=" + COOKIE_DAYS * 24 * 3600
                    + "; HttpOnly; SameSite=Lax" + (ctx.scheme().equals("https") ? "; Secure" : ""));
            // take the token out of the address bar, so it is not copied along with a game's link
            if (ctx.method().name().equals("GET") && !ctx.path().startsWith("/api/")) {
                ctx.redirect(ctx.path() + queryWithout(ctx.queryParamMap(), "token"));
                ctx.skipRemainingHandlers();
            }
            return;
        }
        if (matches(ctx.cookie(COOKIE))) return;
        ctx.status(401).contentType("text/html").result("""
                <!doctype html><html lang="en"><head><meta charset="utf-8"><title>Access link needed</title>
                <meta name="viewport" content="width=device-width, initial-scale=1"></head>
                <body style="font: 15px/1.5 system-ui, sans-serif; max-width: 520px; margin: 64px auto; padding: 0 16px">
                <h1 style="font-size: 22px">Access link needed</h1>
                <p>This game server is private. Open it with the link you were sent (it ends in <code>?token=...</code>).</p>
                </body></html>""");
        ctx.skipRemainingHandlers();
    }

    /**
     * Whether a WebSocket connection carries the token.
     */
    boolean allows(WsContext ctx) {
        return isOpen() || matches(ctx.cookie(COOKIE)) || matches(ctx.queryParam("token"));
    }

    private boolean matches(String candidate) {
        if (candidate == null) return false;
        // in constant time, so the time taken gives nothing of the token away
        return MessageDigest.isEqual(token, candidate.getBytes(StandardCharsets.UTF_8));
    }

    private static String queryWithout(Map<String, List<String>> query, String name) {
        String rest = query.entrySet().stream()
                .filter(e -> !e.getKey().equals(name))
                .flatMap(e -> e.getValue().stream().map(v -> encode(e.getKey()) + "=" + encode(v)))
                .collect(Collectors.joining("&"));
        return rest.isEmpty() ? "" : "?" + rest;
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

package web;

import games.GameType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a new {@link GameSession} plays: the game, the number of players, which seat the browser player takes, the
 * agent in every other seat, the seed and any game parameters changed from their defaults.
 * <p>
 * It is read from the query string of the play page (which the browser repeats on its WebSocket URL), so a game set up
 * on the start page can be bookmarked or shared as a link:
 * <pre>game=LawnAndOrder&amp;players=3&amp;seat=0&amp;opponents=you,mcts-1000,random&amp;seed=42&amp;pause=300&amp;p.handSize=6</pre>
 *
 * @param opponents an {@link OpponentCatalog} id for every seat, in seat order; the browser player's entry is ignored
 * @param seed      the game seed, or -1 for a new seed for each game
 * @param turnPause ms the game waits after each action, so the browser player can see what the AIs did
 * @param params    game parameters (by name) to change from their defaults, as strings
 */
public record SessionConfig(GameType game, int nPlayers, int seat, List<String> opponents, long seed, int turnPause,
                            Map<String, String> params) {

    static final int DEFAULT_TURN_PAUSE = 300;

    /**
     * @throws IllegalArgumentException if the query names an unknown game, opponent or parameter, or is otherwise
     *                                  not a game that can be played
     */
    static SessionConfig fromQuery(Map<String, List<String>> query, GameCatalog games, OpponentCatalog opponentCatalog) {
        GameType game = query.containsKey("game") ? games.find(first(query, "game")) : games.games().get(0);
        int nPlayers = query.containsKey("players") ? integer(query, "players")
                : Math.max(game.getMinPlayers(), Math.min(3, game.getMaxPlayers()));
        if (nPlayers < game.getMinPlayers() || nPlayers > game.getMaxPlayers())
            throw new IllegalArgumentException(game.name() + " takes " + game.getMinPlayers() + " to " + game.getMaxPlayers() + " players, not " + nPlayers);
        int seat = query.containsKey("seat") ? integer(query, "seat") : 0;
        if (seat < 0 || seat >= nPlayers)
            throw new IllegalArgumentException("The seat must be between 0 and " + (nPlayers - 1));

        List<String> opponents = new ArrayList<>();
        String[] given = query.containsKey("opponents") ? first(query, "opponents").split(",") : new String[0];
        for (int i = 0; i < nPlayers; i++) {
            String id = i < given.length && !given[i].isBlank() ? given[i].trim() : OpponentCatalog.DEFAULT;
            if (i == seat) id = "you";
            else opponentCatalog.find(game, id);   // throws if unknown
            opponents.add(id);
        }

        long seed = query.containsKey("seed") && !first(query, "seed").isBlank() ? longValue(query, "seed") : -1;
        int turnPause = query.containsKey("pause") ? integer(query, "pause") : DEFAULT_TURN_PAUSE;
        if (turnPause < 0 || turnPause > 10_000)
            throw new IllegalArgumentException("The pause must be between 0 and 10000 ms");

        Map<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : query.entrySet())
            if (e.getKey().startsWith("p.") && !e.getValue().isEmpty())
                params.put(e.getKey().substring(2), e.getValue().get(0));
        // check the parameters now, so a bad link fails before a game is started
        GameCatalog.apply(game.createParameters(0), params);

        return new SessionConfig(game, nPlayers, seat, opponents, seed, turnPause, params);
    }

    private static String first(Map<String, List<String>> query, String name) {
        List<String> values = query.get(name);
        return values == null || values.isEmpty() ? "" : values.get(0);
    }

    private static int integer(Map<String, List<String>> query, String name) {
        try {
            return Integer.parseInt(first(query, name).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a whole number for " + name + ": " + first(query, name));
        }
    }

    private static long longValue(Map<String, List<String>> query, String name) {
        try {
            return Long.parseLong(first(query, name).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a whole number for " + name + ": " + first(query, name));
        }
    }
}

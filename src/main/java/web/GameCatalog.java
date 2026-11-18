package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The games the server offers, and their parameters as a form can show them.
 */
class GameCatalog {

    private final List<GameType> games;

    /**
     * @param spec a comma-separated list of game names, or "all" (or empty) for every game with a GUI except
     *             GameTemplate, sorted by name
     */
    GameCatalog(String spec) {
        if (spec == null || spec.isBlank() || spec.equalsIgnoreCase("all")) {
            games = Arrays.stream(GameType.values())
                    .filter(g -> g.getGuiManagerClass() != null && g != GameType.GameTemplate)
                    .sorted(Comparator.comparing(GameType::name))
                    .toList();
        } else {
            games = new ArrayList<>();
            for (String name : spec.split(",")) {
                GameType game = GameType.valueOf(name.trim());
                if (game.getGuiManagerClass() == null)
                    throw new IllegalArgumentException(game.name() + " has no GUI, so it cannot be played in a browser");
                games.add(game);
            }
        }
        if (games.isEmpty()) throw new IllegalArgumentException("No games to offer");
    }

    List<GameType> games() {
        return games;
    }

    GameType find(String name) {
        for (GameType game : games)
            if (game.name().equals(name)) return game;
        throw new IllegalArgumentException("Unknown game: " + name);
    }

    JsonArray describe() {
        JsonArray result = new JsonArray();
        for (GameType game : games) {
            JsonObject g = new JsonObject();
            g.addProperty("name", game.name());
            g.addProperty("minPlayers", game.getMinPlayers());
            g.addProperty("maxPlayers", game.getMaxPlayers());
            g.add("params", describeParameters(game));
            result.add(g);
        }
        return result;
    }

    /**
     * The game's parameters that a form can set, each with its default and any possible values the game lists.
     */
    static JsonArray describeParameters(GameType game) {
        JsonArray result = new JsonArray();
        // only a TunableParameters class lists its parameters
        if (!(game.createParameters(0) instanceof TunableParameters<?> params)) return result;
        for (String name : params.getParameterNames()) {
            Class<?> type = typeOf(params, name);
            String kind = kind(type);
            // only numbers, booleans, strings and enums
            if (kind == null) continue;
            JsonObject p = new JsonObject();
            p.addProperty("name", name);
            p.addProperty("type", kind);
            p.addProperty("default", String.valueOf(params.getDefaultParameterValue(name)));
            JsonArray values = new JsonArray();
            List<Object> possible = type.isEnum() ? Arrays.asList(type.getEnumConstants()) : params.getPossibleValues(name);
            // a parameter declared without a list of settings lists only its default; that is free entry, not a choice
            if (possible.size() > 1)
                for (Object v : possible) values.add(String.valueOf(v));
            p.add("values", values);
            result.add(p);
        }
        return result;
    }

    /**
     * Sets the parameters named in the overrides, each given as the string a form sends.
     *
     * @throws IllegalArgumentException for an unknown parameter or a value that does not fit its type
     */
    static void apply(AbstractParameters parameters, Map<String, String> overrides) {
        if (overrides.isEmpty()) return;
        if (!(parameters instanceof TunableParameters<?> params))
            throw new IllegalArgumentException("This game has no parameters that can be set");
        for (Map.Entry<String, String> e : overrides.entrySet()) {
            String name = e.getKey();
            if (!params.getParameterNames().contains(name))
                throw new IllegalArgumentException("Unknown parameter: " + name);
            Class<?> type = typeOf(params, name);
            if (kind(type) == null)
                throw new IllegalArgumentException("Parameter " + name + " cannot be set from the browser");
            params.setParameterValue(name, convert(type, e.getValue(), params.getPossibleValues(name), name));
        }
    }

    private static Class<?> typeOf(TunableParameters<?> params, String name) {
        Class<?> type = params.getParameterTypes().get(name);
        if (type == null) {
            Object value = params.getDefaultParameterValue(name);
            type = value == null ? Object.class : value.getClass();
        }
        return type;
    }

    /**
     * How a form shows a parameter of this type: "int", "double", "boolean", "string" or "enum"; null if it cannot.
     */
    private static String kind(Class<?> type) {
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class) return "int";
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) return "double";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == String.class) return "string";
        if (type.isEnum()) return "enum";
        return null;
    }

    private static Object convert(Class<?> type, String text, List<Object> possible, String name) {
        // a listed value is used as it is, so that it has exactly the type the game expects
        for (Object v : possible)
            if (Objects.equals(String.valueOf(v), text)) return v;
        try {
            if (type == Integer.class || type == int.class) return Integer.valueOf(text.trim());
            if (type == Long.class || type == long.class) return Long.valueOf(text.trim());
            if (type == Double.class || type == double.class) return Double.valueOf(text.trim());
            if (type == Float.class || type == float.class) return Float.valueOf(text.trim());
            if (type == Boolean.class || type == boolean.class) {
                if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) return Boolean.valueOf(text);
                throw new IllegalArgumentException();
            }
            if (type.isEnum()) {
                for (Object c : type.getEnumConstants())
                    if (c.toString().equals(text)) return c;
                throw new IllegalArgumentException();
            }
            return text;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Bad value for " + name + ": " + text);
        }
    }
}

package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sends the browser what an AI player weighed up for each decision it makes: its most visited actions, with the share
 * of its search each took and their value to it, from {@link AbstractPlayer#getDecisionStats()}.
 * <p>
 * This shows what the AI knows, including its own hidden information (in a game of simultaneous choices, the move it
 * is about to make).
 */
class AIInsight implements IGameListener {

    static final int CANDIDATES = 8;

    private final Sender out;
    private final int browserSeat;
    private Game game;

    AIInsight(Game game, Sender out, int browserSeat) {
        this.game = game;
        this.out = out;
        this.browserSeat = browserSeat;
    }

    @Override
    public void onEvent(Event event) {
        if (event.type != Event.GameEvent.ACTION_CHOSEN || event.playerID == browserSeat || event.action == null)
            return;
        AbstractPlayer player = game.getPlayers().get(event.playerID);
        Map<AbstractAction, Map<String, Object>> stats = player.getDecisionStats();
        // players that do not search (random, say) report nothing
        if (stats == null || stats.isEmpty()) return;

        AbstractGameState state = event.state != null ? event.state : game.getGameState();
        List<Map.Entry<AbstractAction, Map<String, Object>>> ranked = new ArrayList<>(stats.entrySet());
        ranked.sort((a, b) -> Integer.compare(visits(b.getValue()), visits(a.getValue())));

        JsonArray candidates = new JsonArray();
        for (Map.Entry<AbstractAction, Map<String, Object>> e : ranked.subList(0, Math.min(CANDIDATES, ranked.size()))) {
            JsonObject c = new JsonObject();
            c.addProperty("label", e.getKey().getString(state));
            c.addProperty("visits", visits(e.getValue()));
            if (e.getValue().get("visitProportion") instanceof Number share)
                c.addProperty("share", share.doubleValue());
            if (e.getValue().get("nodeValue") instanceof double[] values && event.playerID < values.length)
                c.addProperty("value", values[event.playerID]);
            c.addProperty("chosen", e.getKey().equals(event.action));
            candidates.add(c);
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "insight");
        msg.addProperty("player", event.playerID);
        msg.addProperty("name", player.toString());
        msg.addProperty("chosen", event.action.getString(state));
        msg.addProperty("considered", stats.size());
        msg.add("candidates", candidates);
        out.sendText(msg.toString());
    }

    private static int visits(Map<String, Object> stats) {
        return stats.get("visits") instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public void report() {
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }
}

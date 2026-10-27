package games.backgammon.metrics;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.backgammon.BGGameState;
import games.backgammon.actions.LoadDice;
import games.backgammon.actions.MovePiece;
import games.backgammon.actions.RollDice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static evaluation.metrics.Event.GameEvent.ACTION_CHOSEN;

public class BGActionMetrics implements IMetricsCollection {

    /**
     * Records the actions taken during the game
     * This is 'Reduced' compared to the Actions metric in that it does not have separate columns for each player
     * Instead of having separate columns for 'Player-2', 'Player-3' etc, it has a single column 'Player' with
     * the player ID taking the action. (And similarly for PlayerName)
     * This simplifies data analysis in some circumstances (in others the Actions metric is more useful)
     */
    public static class BGActions extends AbstractMetric {
        public BGActions() {
            super();
        }

        public BGActions(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            BGGameState state = (BGGameState) e.state;
            AbstractAction action = e.action;
            String type = switch(action) {
                case RollDice ignored -> "RollDice";
                case LoadDice ignored -> "LoadDice";
                case MovePiece mp when (mp.from == -1) -> "BearOn";
                case MovePiece mp when (mp.to == -1) -> "BearOff";
                case MovePiece ignored -> "Move";
                case DoNothing ignored -> "DoNothing";
                default -> "Unknown";
            };
            if (action instanceof MovePiece move) {
                // get number of pieces on from/to points
                int fromCount = move.from > -1 ? state.getPiecesOnPoint(e.playerID, move.from) : -1;
                int toCountOwn = move.to > -1 ? state.getPiecesOnPoint(e.playerID, move.to) : -1;
                int toCountOpp = move.to > -1 ? state.getPiecesOnPoint(1 - e.playerID, move.to) : 0;
                if (toCountOpp == 1) {
                    type = "Blot";
                }
                records.put("From", fromCount);
                records.put("To", toCountOwn + toCountOpp);
            }
            records.put("Type", type);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Type", String.class);
            columns.put("From", Integer.class);
            columns.put("To", Integer.class);
            return columns;
        }
    }


}

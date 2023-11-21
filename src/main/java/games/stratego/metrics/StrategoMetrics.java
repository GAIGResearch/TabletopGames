package games.stratego.metrics;

import core.actions.LogEvent;
import core.components.BoardNode;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.stratego.StrategoForwardModel;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.actions.AttackMove;
import games.stratego.actions.NormalMove;
import games.stratego.components.Piece;
import utilities.Distance;
import utilities.Vector2D;

import java.util.*;

@SuppressWarnings("unused")
public class StrategoMetrics implements IMetricsCollection {
    public enum StrategoEvent implements IGameEvent {
        EndCondition,
        BattleOutcome;
        @Override
        public Set<IGameEvent> getValues() {
            return new HashSet<>(Arrays.asList(StrategoEvent.values()));
        }
    }

    public static class GameEndReason extends AbstractMetric {
        // How many times did games end with flag capture vs player running out of options vs max number of game ticks?

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("GameEnd", String.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            String[] text = ((LogEvent)e.action).text.split(":");
            StrategoForwardModel.EndCondition condition = StrategoForwardModel.EndCondition.valueOf(text[0]);
            records.put("GameEnd", condition.toString());
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(StrategoMetrics.StrategoEvent.EndCondition);
        }
    }

    // How many battles were won / lost / tied by each player type
    // In how many battles was each player the attacker vs the defender?
    // How many times did the spy kill the marshall
    public static class BattleOutcome extends AbstractMetric {
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String name : playerNames) {
                columns.put(name + "_wins", Integer.class);
                columns.put(name + "_ties", Integer.class);
                columns.put(name + "_attacker", Integer.class);
                columns.put(name + "_defender", Integer.class);
                columns.put(name + "_spykill", Integer.class);
            }
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            String[] text = ((LogEvent)e.action).text.split(":");
            String[] players = text[0].split(" vs ");
            String[] pieces = text[1].split(" vs ");
            int winner = Integer.parseInt(text[2].trim());
            String attackerName = listener.getGame().getPlayers().get(Integer.parseInt(players[0].trim())).toString();
            String defenderName = listener.getGame().getPlayers().get(Integer.parseInt(players[1].trim())).toString();

            for (String s : players) {
                int i = Integer.parseInt(s.trim());
                String player = listener.getGame().getPlayers().get(i).toString();
                if (winner == i) {
                    records.put(player + "_wins", 1);
                    records.put(player + "_ties", 0);
                } else if (winner != -1) {
                    records.put(player + "_wins", 0);
                    records.put(player + "_ties", 0);
                } else {
                    records.put(player + "_ties", 1);
                    records.put(player + "_wins", 0);
                }
            }

            records.put(attackerName + "_attacker", 1);
            records.put(attackerName + "_defender", 0);
            records.put(defenderName + "_defender", 1);
            records.put(defenderName + "_attacker", 0);

            if (Piece.PieceType.valueOf(pieces[0].trim()) == Piece.PieceType.SPY) {
                if (Piece.PieceType.valueOf(pieces[1].trim()) == Piece.PieceType.MARSHAL) {
                    records.put(attackerName + "_spykill", 1);
                } else {
                    records.put(attackerName + "_spykill", 0);
                }
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(StrategoEvent.BattleOutcome);
        }
    }


    // How many times did the scout move more than 1 square
    // Which piece type moves, which attacks
    public static class PieceMoves extends AbstractMetric {
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String name : playerNames) {
                columns.put(name + "_move", String.class);
                columns.put(name + "_moveRank", Integer.class);
                columns.put(name + "_attack", String.class);
                columns.put(name + "_attackRank", Integer.class);
                columns.put(name + "_scoutMove", Integer.class);
            }
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.action instanceof NormalMove) {
                NormalMove move = (NormalMove) e.action;
                Piece movedPiece = move.getPiece((StrategoGameState) e.state);
                String player = listener.getGame().getPlayers().get(movedPiece.getOwnerId()).toString();
                records.put(player + "_move", movedPiece.getPieceType().name());
                records.put(player + "_moveRank", movedPiece.getPieceRank());

                if (movedPiece.getPieceType() == Piece.PieceType.SCOUT) {
                    Vector2D from = movedPiece.getPiecePosition().copy();
                    Vector2D to = move.destinationCoordinate;
                    if (to == null) {
                        to = from.add(move.displacement.copy());
                    }
                    double nMoved = Distance.manhattan_distance(from, to);
                    if (nMoved > ((StrategoParams)e.state.getGameParameters()).moveSpeed) {
                        records.put(player + "_scoutMove", 1);
                    }
                }

                return true;
            } else if (e.action instanceof AttackMove) {
                AttackMove move = (AttackMove) e.action;
                Piece movedPiece = move.getPiece((StrategoGameState) e.state);
                String player = listener.getGame().getPlayers().get(movedPiece.getOwnerId()).toString();
                records.put(player + "_attack", movedPiece.getPieceType().name());
                records.put(player + "_attackRank", movedPiece.getPieceRank());

                return true;
            }

            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    // Sum of piece ranks at the end of the game
    public static class PieceStatsEnd extends AbstractMetric {
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String name : playerNames) {
                columns.put(name + "_rankSum", Integer.class);
                columns.put(name + "_nPieces", Integer.class);
                columns.put(name + "_rankSumOpp", Integer.class);
                columns.put(name + "_nPiecesOpp", Integer.class);
            }
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            StrategoGameState gs = ((StrategoGameState)e.state);

            int[] sum = new int[e.state.getNPlayers()];
            int[] n = new int[e.state.getNPlayers()];
            for (BoardNode bn : gs.getGridBoard().getComponents()) {
                if (bn == null) continue;
                Piece p = (Piece) bn;
                sum[p.getOwnerId()] += p.getPieceRank();
                n[p.getOwnerId()] ++;
            }

            for (int i = 0; i < listener.getGame().getPlayers().size(); i++) {
                String player = listener.getGame().getPlayers().get(i).toString();
                records.put(player + "_rankSum", sum[i]);
                records.put(player + "_nPieces", n[i]);
                records.put(player + "_rankSumOpp", sum[(i+1)%e.state.getNPlayers()]);
                records.put(player + "_nPiecesOpp", n[(i+1)%e.state.getNPlayers()]);
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }
}

package games.loveletter.stats;

import core.CoreConstants;
import core.Game;
import core.actions.LogEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;

@SuppressWarnings("unused")
public class LoveLetterMetrics implements IMetricsCollection {

    public static class ActionsPlayed extends AbstractMetric
    {
        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < game.getPlayers().size(); i++) {
                columns.put("Player-" + i, String.class);
            }
            columns.put("Aggregate", String.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PlayCard pc = (PlayCard) e.action;
            for (int i = 0; i < listener.getGame().getPlayers().size(); i++) {
                if(i == e.state.getCurrentPlayer())
                    records.put("Player-" + e.state.getCurrentPlayer(), pc.getCardType().toString());
                else records.put("Player-" + i, null);
            }
            records.put("Aggregate", pc.getCardType().toString());
            return true;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class WinCause extends AbstractMetric
    {
        int killer = -1;
        int victim = -1;
        Set<Integer> eliminatedPlayers = new HashSet<>();

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.GAME_EVENT) {
                String[] text = ((LogEvent)e.action).text.split(":")[1].split(",");
                killer = Integer.parseInt(text[0].trim());
                victim = Integer.parseInt(text[1].trim());
                eliminatedPlayers.add(victim);
                return false;
            } else {
                PlayCard action = (PlayCard) e.state.getHistory().get(e.state.getHistory().size() - 1);  // Last action played
                if (killer != -1) {
                    // An elimination happened as a result of the last action played
                    records.put("WinCause", killer == victim ? action.getCardType().name() + ".opp" : action.getCardType().name());
                } else {
                    // A showdown ended the round
                    records.put("WinCause", getShowdownWin((LoveLetterGameState) e.state));
                }
                // Reset killer, victim
                killer = -1;
                victim = -1;
                eliminatedPlayers.clear();
                return true;
            }
        }

        private String getShowdownWin(LoveLetterGameState llgs)
        {
            // Showdown happens at the end of the round when there are no more cards in the discard pile,
            // between the players which have not yet been eliminated. We ignore eliminated players.
            // Cases:
            // 1. One player has a higher card than all others, they win (cardType.end)
            // 2. Two or more players have the same highest card, they tie. But tiebreak is points in discard pile, the highest wins (cardType.tiebreak).
            // 3. All players have the same card and same points in discard pile, they tie (cardType.tie).

            boolean sameCardInHand = false;
            LoveLetterCard.CardType cardType = null;
            for (int i = 0; i < llgs.getNPlayers(); ++i) {
                if (!eliminatedPlayers.contains(i)) {
                    if (cardType == null) {
                        cardType = llgs.getPlayerHandCards().get(i).peek().cardType;
                    } else if (cardType == llgs.getPlayerHandCards().get(i).peek().cardType) {
                        sameCardInHand = true;
                    }
                }
            }
            for (int i = 0; i < llgs.getNPlayers(); ++i) {
                if (llgs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_ROUND)
                    if (sameCardInHand) return llgs.getPlayerHandCards().get(i).get(0).cardType + ".tiebreak";
                    else return llgs.getPlayerHandCards().get(i).get(0).cardType + ".end";
                else if (llgs.getPlayerResults()[i] == CoreConstants.GameResult.DRAW_ROUND) {
                    return llgs.getPlayerHandCards().get(i).get(0).cardType + ".tie";
                }
            }
            return "";
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ROUND_OVER, Event.GameEvent.GAME_EVENT));
        }
        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            return Collections.singletonMap("WinCause", String.class);
        }
    }

    public static class RoundLength extends AbstractMetric
    {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            int nCards = 0;
            LoveLetterGameState llgs = (LoveLetterGameState) e.state;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                nCards += llgs.getPlayerDiscardCards().get(i).getSize();
            }
            records.put("# actions", nCards);
            records.put("# actions (avg)", nCards / (double) e.state.getNPlayers());
            return true;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            return new HashMap<String, Class<?>>() {{
                put("# actions", Integer.class);
                put("# actions (avg)", Double.class);
            }};
        }
    }

    public static class EliminatingCards extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            // This is spawned whenever a player is eliminated
            String[] text = ((LogEvent)e.action).text.split(":")[1].split(",");
            int killer = Integer.parseInt(text[0].trim());
            int killed = Integer.parseInt(text[1].trim());
            LoveLetterCard.CardType cardUsed = LoveLetterCard.CardType.valueOf(text[2].trim());
            records.put("EliminatingCard", cardUsed + (killed == killer? ".self" : ""));
            return true;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_EVENT);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            return Collections.singletonMap("EliminatingCard", String.class);
        }
    }
}

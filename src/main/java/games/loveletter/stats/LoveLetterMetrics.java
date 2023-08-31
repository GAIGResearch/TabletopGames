package games.loveletter.stats;

import core.AbstractPlayer;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.LogEvent;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PlayCard;
import games.loveletter.actions.PrinceAction;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;

@SuppressWarnings("unused")
public class LoveLetterMetrics implements IMetricsCollection {

    public static class CardStatsActionChosen extends AbstractMetric {
        Set<String> playerNames;

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            this.playerNames = playerNames;
            for (String name: playerNames) {
                columns.put(name + "-KingTradedCard", String.class);
                columns.put(name + "-CountessPlayNotForced", Integer.class);
                columns.put(name + "-PrinceSelfTarget", Integer.class);
            }
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.action instanceof PlayCard) {
                PlayCard pc = (PlayCard) e.action;
                String playerName = listener.getGame().getPlayers().get(e.playerID).toString();
                boolean record = false;

                if (pc.getCardType() == LoveLetterCard.CardType.King) {
                    // Check other card in hand before king trade
                    String otherCard = null;
                    LoveLetterGameState gs = (LoveLetterGameState) e.state;
                    for (LoveLetterCard card: gs.getPlayerHandCards().get(e.playerID).getComponents()) {
                        if (card.cardType != LoveLetterCard.CardType.King) {
                            otherCard = card.cardType.name();
                            break;
                        }
                    }
                    records.put(playerName + "-KingTradedCard", otherCard);
                    record = true;

                } else if (pc.getCardType() == LoveLetterCard.CardType.Countess) {
                    // Check countess play not forced, if before play countess, player has prince or king
                    boolean forced = false;
                    LoveLetterGameState gs = (LoveLetterGameState) e.state;
                    for (LoveLetterCard card : gs.getPlayerHandCards().get(e.playerID).getComponents()) {
                        if (card.cardType == LoveLetterCard.CardType.Prince || card.cardType == LoveLetterCard.CardType.King) {
                            forced = true;
                            break;
                        }
                    }
                    if (!forced) {
                        records.put(playerName + "-CountessPlayNotForced", 1);
                    } else {
                        records.put(playerName + "-CountessPlayNotForced", 0);
                    }
                    record = true;

                } else if (pc instanceof PrinceAction) {
                    // Check prince self target
                    if (pc.getTargetPlayer() == e.playerID) {
                        records.put(playerName + "-PrinceSelfTarget", 1);
                    } else {
                        records.put(playerName + "-PrinceSelfTarget", 0);
                    }
                    record = true;
                }
                return record;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(Event.GameEvent.ACTION_CHOSEN);
            }};
        }
    }

    public static class CardStatsGameEvent extends AbstractMetric {
        Set<String> playerNames;
        LoveLetterCard.CardType cardPlayed = null;
        boolean successfulPlay = false;

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            this.playerNames = playerNames;
            for (String name: playerNames) {
                columns.put(name + "-GuardSuccess", Integer.class);
                columns.put(name + "-BaronSuccess", Integer.class);
            }
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.GAME_EVENT) {
                String[] text = ((LogEvent)e.action).text.split(":")[1].split(",");
                int killer = Integer.parseInt(text[0].trim());
                int killed = Integer.parseInt(text[1].trim());
                int activePlayer = Integer.parseInt(text[3].trim());

                cardPlayed = LoveLetterCard.CardType.valueOf(text[2].trim());
                successfulPlay = activePlayer != killed;

                return false;
            } else {
                // On action taken, we check if game event triggered before and set successful use of card
                if (e.action instanceof PlayCard) {
                    LoveLetterCard.CardType cardType = ((PlayCard) e.action).getCardType();
                    if (cardType == LoveLetterCard.CardType.Guard || cardType == LoveLetterCard.CardType.Baron) {
                        String playerName = listener.getGame().getPlayers().get(e.playerID).toString();
                        if (cardPlayed != null && cardPlayed == cardType) {
                            records.put(playerName + "-" + cardType.name() + "Success", successfulPlay ? 1 : 0);
                        } else {
                            records.put(playerName + "-" + cardType.name() + "Success", 0);
                        }

                        // Reset successful use of card
                        cardPlayed = null;
                        successfulPlay = false;
                        return true;
                    }
                }

                // Reset successful use of card
                cardPlayed = null;
                successfulPlay = false;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(Event.GameEvent.GAME_EVENT);
                add(Event.GameEvent.ACTION_TAKEN);
            }};
        }
    }

    public static class CardsPlayed extends AbstractMetric {
        List<String> playerNames;
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            this.playerNames = new ArrayList<>(playerNames);
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, String.class);
            }
            for (int i = 0; i < playerNames.size(); i++) {
                columns.put(this.playerNames.get(i) + "-" + i, String.class);
            }
            columns.put("Aggregate", String.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.action instanceof PlayCard) {
                PlayCard pc = (PlayCard) e.action;
                for (int i = 0; i < listener.getGame().getPlayers().size(); i++) {
                    if (i == e.state.getCurrentPlayer())
                        records.put("Player-" + e.state.getCurrentPlayer(), pc.getCardType().toString());
                    else records.put("Player-" + i, null);
                    for (int j = 0; j < playerNames.size(); j++) {
                        for (AbstractPlayer player : listener.getGame().getPlayers()) {
                            if (player.toString().equals(playerNames.get(j))) {
                                records.put(playerNames.get(j) + "-" + j, pc.getCardType().toString());
                            } else {
                                records.put(playerNames.get(j) + "-" + j, null);
                            }
                        }
                    }
                }
                records.put("Aggregate", pc.getCardType().toString());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
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
                AbstractAction action = e.state.getHistory().get(e.state.getHistory().size() - 1);  // Last action played
                if (action instanceof PlayCard) {
                    PlayCard pc = (PlayCard) action;
                    if (killer != -1) {
                        // An elimination happened as a result of the last action played
                        records.put("WinCause", killer == victim ? pc.getCardType().name() + ".opp" : pc.getCardType().name());
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
                return false;
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
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ROUND_OVER, Event.GameEvent.GAME_EVENT));
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
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
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
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
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_EVENT);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return Collections.singletonMap("EliminatingCard", String.class);
        }
    }
}

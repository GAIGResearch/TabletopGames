package games.loveletter.stats;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.LogEvent;
import core.components.Deck;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.IMetricsCollection;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;

@SuppressWarnings("unused")
public class LoveLetterMetrics implements IMetricsCollection {

    public static class ActionsPlayed extends AbstractMetric
    {
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            Deck<LoveLetterCard> played = ((LoveLetterGameState)e.state).getPlayerDiscardCards().get(e.playerID);
            StringBuilder ss = new StringBuilder();
            for (LoveLetterCard card : played.getComponents()) {
                ss.append(card.cardType).append(",");
            }
            if (ss.toString().equals("")) return ss.toString();
            ss.append("]");
            return ss.toString().replace(",]", "");
        }
        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_TAKEN);
        }
    }

    public static class ActionsPlayedWin extends AbstractMetric
    {
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            StringBuilder ss = new StringBuilder();
            if (e.state.getPlayerResults()[e.playerID] == CoreConstants.GameResult.WIN_ROUND) {
                Deck<LoveLetterCard> played = ((LoveLetterGameState)e.state).getPlayerDiscardCards().get(e.playerID);
                for (LoveLetterCard card : played.getComponents()) {
                    ss.append(card.cardType).append(",");
                }
                if (ss.toString().equals("")) return ss.toString();
                ss.append("]");
            }
            return ss.toString().replace(",]", "");
        }
        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class DiscardedCards extends AbstractMetric
    {
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            int nCards = 0;
            LoveLetterGameState llgs = (LoveLetterGameState) e.state;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                nCards += llgs.getPlayerDiscardCards().get(i).getSize();
            }
            return nCards;
        }
        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class EliminatingCards extends AbstractMetric {

        @Override
        public Object run(MetricsGameListener listener, Event e) {
            // This is spawned whenever a player is eliminated
            String[] text = ((LogEvent)e.action).text.split(":")[1].split(",");
            int killer = Integer.parseInt(text[0].trim());
            int killed = Integer.parseInt(text[1].trim());
            LoveLetterCard.CardType cardUsed = LoveLetterCard.CardType.valueOf(text[2].trim());
            return cardUsed + (killed == killer? ".self" : "");
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_EVENT);
        }
    }

    public static class WinningCards extends AbstractMetric {

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Object run(MetricsGameListener listener, Event e) {
            PlayCard action = (PlayCard) e.state.getHistory().get(e.state.getHistory().size()-1);  // Last action played
            return processAction((LoveLetterGameState) e.state, action);
        }

        private String processAction(LoveLetterGameState llgs, PlayCard action) {
            int currentPlayerID = llgs.getCurrentPlayer();
            boolean drawPileEmpty = llgs.getDrawPile().getSize() == 0;
            int whoPlayedIt = action.getPlayerID();

            if (action.getCardType() == LoveLetterCard.CardType.Princess) {
                return "Princess.opp";
            } else if (action instanceof GuardAction) {
                int opponentID = action.getTargetPlayer();
                if (opponentID != -1) {
                    boolean wonByCard = llgs.getPlayerHandCards().get(opponentID).getSize() == 0;
                    if (wonByCard) {
                        return "Guard";
                    } else {
                        return getShowdownWin(llgs, whoPlayedIt, action);
                    }
                } else {
                    return getShowdownWin(llgs, whoPlayedIt, action);
                }
            } else if(action instanceof BaronAction) {
                int opponentID = action.getTargetPlayer();
                if (opponentID != -1) {
                    boolean wonByCard = llgs.getPlayerHandCards().get(opponentID).getSize() == 0;
                    if (wonByCard) {
                        return "Baron";
                    } else {
                        boolean lostByCard = llgs.getPlayerHandCards().get(whoPlayedIt).getSize() == 0;
                        if (lostByCard) {
                            return "Baron.opp";
                        } else {
                            return getShowdownWin(llgs, whoPlayedIt, action);
                        }
                    }
                } else {
                    return getShowdownWin(llgs, whoPlayedIt, action);
                }
            } else if (action instanceof PrinceAction) {
                if (llgs.getPlayerResults()[currentPlayerID] == CoreConstants.GameResult.WIN_GAME) { //made the opponent discard princess.
                    return "Prince";
                } else {
                    return "Prince.opp";
                }
            } else if (drawPileEmpty) {
                return getShowdownWin(llgs, whoPlayedIt, action);
            } else return action.getCardType().name();
        }

        private String getShowdownWin(LoveLetterGameState llgs, int whoPlayedLast, AbstractAction action)
        {
            for (int i = 0; i < llgs.getNPlayers(); ++i) {
                int numCardsPlayerHand = llgs.getPlayerHandCards().get(i).getSize();
                if (llgs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_ROUND)
                    return llgs.getPlayerHandCards().get(i).get(0).cardType + ".end";
                else if (llgs.getPlayerResults()[i] == CoreConstants.GameResult.DRAW_ROUND) {
                    return llgs.getPlayerHandCards().get(i).get(0).cardType + ".tie";
                }
            }
            return "";
        }
    }
}

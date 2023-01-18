package games.loveletter.stats;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import evaluation.metrics.IMetricsCollection;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.BaronAction;
import games.loveletter.actions.GuardAction;
import games.loveletter.actions.PrinceAction;
import games.loveletter.actions.PrincessAction;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;

@SuppressWarnings("unused")
public class LoveLetterMetrics implements IMetricsCollection {

    public static class ActionsPlayed extends AbstractMetric
    {
        @Override
        public Object run(GameListener listener, Event e) {
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
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_TAKEN);
        }
    }

    public static class ActionsPlayedWin extends AbstractMetric
    {
        @Override
        public Object run(GameListener listener, Event e) {
            StringBuilder ss = new StringBuilder();
            if (e.state.getPlayerResults()[e.playerID] == CoreConstants.GameResult.WIN) {
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
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_TAKEN);
        }
    }

    public static class DiscardedCards extends AbstractMetric
    {
        @Override
        public Object run(GameListener listener, Event e) {
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
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    public static class WinningCards extends AbstractMetric
    {
        String winningCards;
        String losingCards;

        public WinningCards() {
            super();
            winningCards = null;
            losingCards = null;
        }

        @Override
        public Set<Event.GameEvent> getEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_TAKEN, Event.GameEvent.GAME_OVER));
        }
        @Override
        public Object run(GameListener listener, Event e) {
            if(e.type == Event.GameEvent.ACTION_TAKEN){
                processAction(e.state, e.action);
            }else if(e.type == Event.GameEvent.GAME_OVER) {
                CoreConstants.GameResult[] results = e.state.getPlayerResults();
                String winStr = null, loseStr = null;
                for (int i = 0; i < results.length; i++) {
                    if(results[i] == CoreConstants.GameResult.WIN)
                        winStr = "Win: " + processCards(winningCards, i);
                    else if (results[i] == CoreConstants.GameResult.LOSE)
                        loseStr = "Lose: " + processCards(losingCards, i);
                }
                return winStr + " " + loseStr;
            }
            return null;
        }

        private String processCards(String token, int playerID)
        {
            StringBuilder ss = new StringBuilder();

            if(token != null)
                ss.append(token).append(",");
            else
                ss.append("NA,");

            return ss.toString();
        }

        private void processAction(AbstractGameState state, AbstractAction action) {

            if((state.getGameStatus() == CoreConstants.GameResult.GAME_END))
            {
                winningCards = "NA";
                losingCards = "NA";

                LoveLetterGameState llgs = ((LoveLetterGameState) state);
                int currentPlayerID = llgs.getCurrentPlayer();
                boolean drawPileEmpty = llgs.getDrawPile().getSize() == 0;
                String lastAction = llgs.getHistoryAsText().get(llgs.getHistoryAsText().size()-1);
                String[] tokens = lastAction.split(" ");
                int whoPlayedIt = Integer.parseInt(tokens[1]);

                boolean playAndWin = llgs.getPlayerResults()[whoPlayedIt] == CoreConstants.GameResult.WIN;

                if(action instanceof PrincessAction)
                {
                    losingCards = ("Princess");
                    winningCards  = ("Princess (opp)");

                }else if(drawPileEmpty)
                {
                    if(action instanceof GuardAction) {
                        int opponentID = ((GuardAction) action).getOpponentID();
                        boolean wonByCard = llgs.getPlayerHandCards().get(opponentID).getSize() == 0;
                        if (wonByCard) {
                            winningCards  = "Guard";
                            losingCards = "Guard (opp)";
                        }else{
                            getShowdownWin(llgs, whoPlayedIt, action);
                        }
                    }else if(action instanceof BaronAction) {
                        int opponentID = ((BaronAction) action).getOpponentID();
                        boolean wonByCard = llgs.getPlayerHandCards().get(opponentID).getSize() == 0;
                        if (wonByCard) {
                            winningCards = ("Baron");
                            losingCards  = ("Baron (opp)");
                        }else{
                            boolean lostByCard = llgs.getPlayerHandCards().get(whoPlayedIt).getSize() == 0;
                            if(lostByCard)
                            {
                                losingCards = ("Baron");
                                winningCards  = ("Baron (opp)");
                            }else
                            {
                                getShowdownWin(llgs, whoPlayedIt, action);
                            }
                        }
                    }else {
                        getShowdownWin(llgs, whoPlayedIt, action);
                    }
                }else if(action instanceof BaronAction)
                {
                    if(playAndWin)
                    {
                        winningCards  = ("Baron");
                        losingCards  = ("Baron (opp)");
                    }else
                    {
                        losingCards  = ("Baron");
                        winningCards  = ("Baron (opp)");
                    }

                }else if(action instanceof GuardAction) {
                    winningCards  = ("Guard");
                    losingCards  = ("Guard (opp)");
                }else if(action instanceof PrinceAction)
                {
                    if(llgs.getPlayerResults()[currentPlayerID] == CoreConstants.GameResult.WIN) { //made the opponent discard princess.
                        winningCards = ("Prince");
                        losingCards = "Prince (opp)";
                    }else{
                        losingCards =  "Prince";
                        winningCards = "Prince (opp)";
                    }
                }else{
                    winningCards  = (llgs.getHistoryAsText().get(llgs.getHistoryAsText().size()-1));
                }
            }
        }

        private void getShowdownWin(LoveLetterGameState llgs, int whoPlayedLast, AbstractAction action)
        {
            for(int i = 0; i < llgs.getPlayerResults().length; ++i)
            {
                int numCardsPlayerHand = llgs.getPlayerHandCards().get(i).getSize();
                if(llgs.getPlayerResults()[i] == CoreConstants.GameResult.WIN && numCardsPlayerHand>0)
                    winningCards = (llgs.getPlayerHandCards().get(i).get(0).cardType + " (end)");
                else if (llgs.getPlayerResults()[i] == CoreConstants.GameResult.LOSE  && numCardsPlayerHand>0)
                    losingCards = (llgs.getPlayerHandCards().get(i).get(0).cardType + " (end)");
                else{
                    //This is a draw in the showdown.
                    winningCards = "Tie-breaker";
                    losingCards = "Tie-breaker";
                    return;
                }
            }

        }
    }
}

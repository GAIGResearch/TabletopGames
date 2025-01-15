package games.monopolydeal;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.monopolydeal.actions.actioncards.*;
import games.monopolydeal.actions.boardmanagement.AddMoney;
import games.monopolydeal.actions.informationcontainer.PayCardFrom;
import games.monopolydeal.cards.CardType;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.*;

public class MonopolyDealMetrics implements IMetricsCollection {
    public static class GameDuration extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("Rounds", e.state.getRoundCounter());
            return true;
        }
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Rounds", Integer.class);
            return columns;
        }
    }

    public static class ActionsTaken extends AbstractMetric{

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (ACTION_CHOSEN == e.type) {
                records.put("ActionsTaken", e.action.toString());
                records.put("PlayerID", e.playerID);
                records.put("TurnOwner", e.state.getTurnOwner());
            }
            else if(GAME_OVER == e.type){
                if(e.state.getWinners().isEmpty()){return true;}
                records.put("Winner",e.state.getWinners().iterator().next());
                records.put("FirstPlayer",e.state.getFirstPlayer());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(ACTION_CHOSEN, GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("ActionsTaken", String.class);
            columns.put("PlayerID", Integer.class);
            columns.put("Winner", Integer.class);
            columns.put("TurnOwner", Integer.class);
            columns.put("FirstPlayer", Integer.class);
            return columns;
        }
    }
    public static class CardsUsed extends AbstractMetric {
        public CardsUsed() {
            super();
            counters = new int[11][5];
        }

        public CardsUsed(Event.GameEvent... args) {
            super(args);
            counters = new int[11][5];
        }
        // Counters
        int[][] counters;
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (ACTION_CHOSEN.equals(e.type)) {
                if (e.action instanceof PassGoAction)
                    counters[0][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof SlyDealAction)
                    counters[1][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof ForcedDealAction)
                    counters[2][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof DebtCollectorAction)
                    counters[3][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof ItsMyBirthdayAction)
                    counters[4][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof MulticolorRentAction)
                    counters[5][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof PropertyRentAction)
                    counters[6][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof DealBreakerAction)
                    counters[7][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof JustSayNoAction)
                    counters[8][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof AddMoney){
                    AddMoney action = (AddMoney) e.action;
                    if(action.cardType == CardType.DealBreaker)
                        counters[9][e.state.getCurrentPlayer()]++;
                    else if(action.cardType == CardType.JustSayNo)
                        counters[10][e.state.getCurrentPlayer()]++;
                }
                return true;
            } else if (GAME_OVER.equals(e.type)) {
                if(e.state.getWinners().isEmpty()){
                    for(int i=0;i<11;i++)
                        for(int j=0;j<5;j++)
                            counters[i][j]=0;
                    return true;
                }

                int winner = e.state.getWinners().iterator().next();
                records.put("PassGoByWinner", counters[0][winner]);
                records.put("SlyDealByWinner", counters[1][winner]);
                records.put("ForcedDealByWinner", counters[2][winner]);
                records.put("DebtCollectorByWinner", counters[3][winner]);
                records.put("ItsMyBirthdayByWinner", counters[4][winner]);
                records.put("MulticolorRentByWinner", counters[5][winner]);
                records.put("PropertyRentByWinner", counters[6][winner]);
                records.put("DealBreakerByWinner", counters[7][winner]);
                records.put("JustSayNoByWinner", counters[8][winner]);
                records.put("DealBreakerAddedToBankByWinner", counters[9][winner]);
                records.put("JustSayNoAddedToBankByWinner", counters[10][winner]);
                records.put("PassGoUsed", Arrays.stream(counters[0]).sum());
                records.put("SlyDealUsed", Arrays.stream(counters[1]).sum());
                records.put("ForcedDealUsed", Arrays.stream(counters[2]).sum());
                records.put("DebtCollectorUsed", Arrays.stream(counters[3]).sum());
                records.put("ItsMyBirthdayUsed", Arrays.stream(counters[4]).sum());
                records.put("MulticolorRentUsed", Arrays.stream(counters[5]).sum());
                records.put("PropertyRentUsed", Arrays.stream(counters[6]).sum());
                records.put("DealBreakerUsed", Arrays.stream(counters[7]).sum());
                records.put("JustSayNoUsed", Arrays.stream(counters[8]).sum());
                records.put("DealBreakerAddedToBank", Arrays.stream(counters[9]).sum());
                records.put("JustSayNoAddedToBank", Arrays.stream(counters[10]).sum());

                for(int i=0;i<11;i++)
                    for(int j=0;j<5;j++)
                        counters[i][j]=0;

                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(ACTION_CHOSEN, GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("PassGoByWinner", Integer.class);
            columns.put("SlyDealByWinner", Integer.class);
            columns.put("ForcedDealByWinner", Integer.class);
            columns.put("DebtCollectorByWinner", Integer.class);
            columns.put("ItsMyBirthdayByWinner", Integer.class);
            columns.put("MulticolorRentByWinner", Integer.class);
            columns.put("PropertyRentByWinner", Integer.class);
            columns.put("DealBreakerByWinner", Integer.class);
            columns.put("JustSayNoByWinner", Integer.class);
            columns.put("DealBreakerAddedToBankByWinner", Integer.class);
            columns.put("JustSayNoAddedToBankByWinner", Integer.class);
            columns.put("PassGoUsed", Integer.class);
            columns.put("SlyDealUsed", Integer.class);
            columns.put("ForcedDealUsed", Integer.class);
            columns.put("DebtCollectorUsed", Integer.class);
            columns.put("ItsMyBirthdayUsed", Integer.class);
            columns.put("MulticolorRentUsed", Integer.class);
            columns.put("PropertyRentUsed", Integer.class);
            columns.put("DealBreakerUsed", Integer.class);
            columns.put("JustSayNoUsed", Integer.class);
            columns.put("DealBreakerAddedToBank", Integer.class);
            columns.put("JustSayNoAddedToBank", Integer.class);
            return columns;
        }

    }

    public static class FinalHeuristicScore extends AbstractMetric {
        public FinalHeuristicScore() {
            super();
        }

        public FinalHeuristicScore(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("Player-" + i, e.state.getHeuristicScore(i));
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Double.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }

    public static class Interaction extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.action instanceof IActionCard) {
                IActionCard action = (IActionCard) e.action;
                records.put("ActionTargetPlayer", action.getTarget((MonopolyDealGameState) e.state));
                return true;
            }
            else if (e.action instanceof PayCardFrom) {
                records.put("PayProperty", ((PayCardFrom) e.action).cardType.isProperty ? 1 : 0);
                records.put("PayMoney", !((PayCardFrom) e.action).cardType.isProperty && !((PayCardFrom) e.action).cardType.isAction ? 1 : 0);
                records.put("PayAction", ((PayCardFrom) e.action).cardType.isAction ? 1 : 0);
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("ActionTargetPlayer", Integer.class);
            columns.put("PayProperty", Integer.class);
            columns.put("PayMoney", Integer.class);
            columns.put("PayAction", Integer.class);
            return columns;
        }
    }

    public static class Money extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("Money", ((MonopolyDealGameState)e.state).getBankValue(e.playerID));
            records.put("PlayerIdx", e.playerID);
            records.put("PlayerName", listener.getGame().getPlayers().get(e.playerID).toString());
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(TURN_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Money", Integer.class);
            columns.put("PlayerIdx", Integer.class);
            columns.put("PlayerName", String.class);
            return columns;
        }
    }
}

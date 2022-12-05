package games.terraformingmars.stats;

import core.interfaces.IGameMetric;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TMPlayerListener extends GameListener {

    IStatisticLogger[] loggerArray;

    public TMPlayerListener(IStatisticLogger[] loggerArray, IStatisticLogger aggregate) {
        super(aggregate, new AbstractMetric[]{});
        this.loggerArray = loggerArray;
    }

    @Override
    public void onEvent(Event event) {
        if(event.type == Event.GameEvent.GAME_OVER) {
            for (int i = 0; i < event.state.getNPlayers(); i++) {
                final int player = i;
                Map<String, Object> data = Arrays.stream(TMPlayerAttributes.values())
                        .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
                loggerArray[i].record(data);
                loggers.get(event.type).record(data);
            }
        }
    }

    @Override
    public void allGamesFinished() {
        for (IStatisticLogger log : loggerArray) {
            log.processDataAndFinish();
        }
        super.allGamesFinished();
    }


    public enum TMPlayerAttributes implements IGameMetric {
        //    GAME_ID((l, e) -> ((TMGameState)e.state).getGameID()),
//    GENERATION((l, e) -> ((TMGameState)e.state).getGeneration()),
        RESULT((l, e) -> e.state.getPlayerResults()[e.playerID].value),
        GP_OCEAN_CONTRIBUTION((l, e) -> {
            int count = 0;
            TMGameState s = ((TMGameState)e.state);
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, e.playerID)) count++;
            }
            return count*1.0 / increases.size();
        }),  // Pairs are (generation,player)
        GP_TEMPERATURE_CONTRIBUTION((l, e) -> {
            int count = 0;
            TMGameState s = ((TMGameState)e.state);
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, e.playerID)) count++;
            }
            return count*1.0 / increases.size();
        }),
        GP_OXYGEN_CONTRIBUTION((l, e) -> {
            int count = 0;
            TMGameState s = ((TMGameState)e.state);
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, e.playerID)) count++;
            }
            return count*1.0 / increases.size();
        }),
        CORP_CARD((l, e) -> ((TMGameState)e.state).getPlayerCorporations()[e.playerID].getComponentName()),
        CORP_CARD_WIN((l, e) -> {
            TMGameState s = ((TMGameState)e.state);
            if (s.getPlayerResults()[e.playerID] == Utils.GameResult.WIN) return s.getPlayerCorporations()[e.playerID].getComponentName();
            else return "";
        }),
        N_CARDS_PLAYED((l, e) -> ((TMGameState)e.state).getPlayedCards()[e.playerID].getSize()),
        N_CARDS_PLAYED_ACTIVE((l, e) -> {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
                if (card.cardType == TMTypes.CardType.Active) c++;
            }
            return c;
        }),
        N_CARDS_PLAYED_AUTOMATED((l, e) -> {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
                if (card.cardType == TMTypes.CardType.Automated) c++;
            }
            return c;
        }),
        N_CARDS_PLAYED_EVENT((l, e) -> {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
                if (card.cardType == TMTypes.CardType.Event) c++;
            }
            return c;
        }),
        CARDS_PLAYED((l, e) -> {
            String ss = "";
            TMGameState s = ((TMGameState)e.state);
            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
                ss += card.getComponentName() + ",";
            }
            if (ss.equals("")) return ss;
            ss += "]";
            return ss.replace(",]", "");
        }),
        CARDS_PLAYED_WIN((l, e) -> {
            TMGameState s = ((TMGameState)e.state);
            if (s.getPlayerResults()[e.playerID] == Utils.GameResult.WIN) {
                String ss = "";
                for (TMCard card : s.getPlayedCards()[e.playerID].getComponents()) {
                    ss += card.getComponentName() + ",";
                }
                if (ss.equals("")) return ss;
                ss += "]";
                return ss.replace(",]", "");
            }
            return "";
        }),
        N_POINTS_TOTAL((l, e) -> ((TMGameState)e.state).countPoints(e.playerID)),
        N_POINTS_TR((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.TR).getValue()),
        N_POINTS_MILESTONES((l, e) -> ((TMGameState)e.state).countPointsMilestones(e.playerID)),
        N_POINTS_AWARDS((l, e) -> ((TMGameState)e.state).countPointsAwards(e.playerID)),
        N_POINTS_BOARD((l, e) -> ((TMGameState)e.state).countPointsBoard(e.playerID)),
        N_POINTS_CARDS((l, e) -> ((TMGameState)e.state).countPointsCards(e.playerID)),
        N_MILESTONES((l, e) -> {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for(Milestone m: s.getMilestones()) {
                if (m.isClaimed() && m.claimed == e.playerID) c++;
            }
            return c;
        }),
        MILESTONES((l, e) -> {
            String ms = "";
            TMGameState s = ((TMGameState)e.state);
            for(Milestone m: s.getMilestones()) {
                if (m.isClaimed() && m.claimed == e.playerID) {
                    ms += m.getComponentName() + ",";
                }
            }
            ms += "]";
            if (ms.equals("]")) return "";
            return ms.replace(",]", "");
        }),
        N_AWARDS_WON((l, e) -> {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed()) {
                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
                    if (winners.a.contains(e.playerID) || winners.b.contains(e.playerID)) {
                        c++;
                    }
                }
            }
            return c;
        }),
        N_AWARDS_FUNDED((l, e) -> {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed() && aa.claimed == e.playerID) {
                    c++;
                }
            }
            return c;
        }),
        N_AWARDS_WON_AND_FUNDED((l, e) -> {
            int c = 0;
            int p = e.playerID;
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed() && aa.claimed == p) {
                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
                    if (winners.a.contains(p) || winners.b.contains(p)) {
                        c++;
                    }
                }
            }
            return c;
        }),
        AWARDS_WON((l, e) -> {
            String ss = "";
            int p = e.playerID;
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed()) {
                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
                    if (winners.a.contains(p) || winners.b.contains(p)) {
                        ss += aa.getComponentName() + ",";
                    }
                }
            }
            ss += "]";
            if (ss.equals("]")) return "";
            return ss.replace(",]", "");
        }),
        AWARDS_FUNDED((l, e) -> {
            String ss = "";
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed() && aa.claimed == e.playerID) {
                    ss += aa.getComponentName() + ",";
                }
            }
            ss += "]";
            if (ss.equals("]")) return "";
            return ss.replace(",]", "");
        }),
        MAP_COVERAGE((l, e) -> {
            int tilesPlaced = 0;
            int nTiles = 0;
            TMGameState s = ((TMGameState)e.state);
            for (int i = 0; i < s.getBoard().getHeight(); i++) {
                for (int j = 0; j < s.getBoard().getWidth(); j++) {
                    if (s.getBoard().getElement(j, i) != null) {
                        nTiles ++;
                        if (s.getBoard().getElement(j, i).getTilePlaced() != null && s.getBoard().getElement(j, i).getOwnerId() == e.playerID) {
                            tilesPlaced++;
                        }
                    }
                }
            }
            return tilesPlaced*1.0 / nTiles;
        }),
        MEGACREDIT_PROD((l, e) -> ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(TMTypes.Resource.MegaCredit).getValue() ),
        STEEL_PROD((l, e) -> ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(TMTypes.Resource.Steel).getValue() ),
        TITANIUM_PROD((l, e) -> ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(TMTypes.Resource.Titanium).getValue() ),
        PLANT_PROD((l, e) -> ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(TMTypes.Resource.Plant).getValue() ),
        ENERGY_PROD((l, e) -> ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(TMTypes.Resource.Energy).getValue() ),
        HEAT_PROD((l, e) -> ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(TMTypes.Resource.Heat).getValue() ),

        MEGACREDIT((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.MegaCredit).getValue() ),
        STEEL((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.Steel).getValue() ),
        TITANIUM((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.Titanium).getValue() ),
        PLANT((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.Plant).getValue() ),
//        ENERGY((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.Energy).getValue() ),
        HEAT((l, e) -> ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.Heat).getValue() ),

        CARDS((l, e) -> ((TMGameState)e.state).getPlayerHands()[e.playerID].getSize()),
        ;

        private final BiFunction<TMPlayerListener, Event, Object> lambda_sp;

        TMPlayerAttributes(BiFunction<TMPlayerListener, Event, Object> lambda) {
            this.lambda_sp = lambda;
        }

        public Object get(GameListener listener, Event event) {
            return lambda_sp.apply((TMPlayerListener) listener, event);
        }

        @Override
        public boolean listens(Event.GameEvent eventType) {
            return eventType == Event.GameEvent.GAME_OVER;
        }

        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }

    }

}

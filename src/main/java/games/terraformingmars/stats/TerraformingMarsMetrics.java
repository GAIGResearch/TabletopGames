package games.terraformingmars.stats;
import core.CoreConstants;
import core.Game;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.*;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import org.jetbrains.annotations.NotNull;
import utilities.Group;
import utilities.Pair;
import utilities.TimeStamp;

import java.util.*;

//@SuppressWarnings("unused")
//public class TerraformingMarsMetrics implements IMetricsCollection {
//
//
//    public static class ParameterComplete extends AbstractParameterizedMetric {
//        public ParameterComplete(){super();}
//
//        @Override
//        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
//            TMTypes.GlobalParameter parameter = (TMTypes.GlobalParameter) getParameterValue("parameter");
//            TMGameState tmgs = (TMGameState) e.state;
//            if (tmgs.getGlobalParameters().containsKey(parameter)) {
//                ArrayList<Pair<Integer, Integer>> increases = tmgs.getGlobalParameters().get(parameter).getIncreases();
//                records.put("Parameter Value", increases.get(increases.size() - 1).a);
//            }else
//                records.put("Parameter Value", 0);
//
//            return true;
//        }
//
//        public ParameterComplete(Object arg){super(arg);}
//
//        @Override
//        public Map<String, Class<?>> getColumns(Game game) {
//            Map<String, Class<?>> columns = new HashMap<>();
//            columns.put("Parameter Value", Integer.class);
//            return columns;
//        }
//
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("parameter", Arrays.asList(TMTypes.GlobalParameter.values()), TMTypes.GlobalParameter.Oxygen));
//        }
//    }


//
//    public static class ParameterIncrease extends AbstractParameterizedMetric {
//        public ParameterIncrease(){super();}
//        public ParameterIncrease(Object arg){super(arg);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMTypes.GlobalParameter parameter = (TMTypes.GlobalParameter) getParameterValue("parameter");
//            TMGameState tmgs = (TMGameState) e.state;
//            if (tmgs.getGlobalParameters().containsKey(parameter)) {
//                return (tmgs.getGlobalParameters().get(parameter).getIncreasesString());
//            }
//            return "";
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("parameter", Arrays.asList(TMTypes.GlobalParameter.values()), TMTypes.GlobalParameter.Oxygen));
//        }
//    }
//
//    public static class Generation extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState) e.state).getGeneration();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class Result extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return Arrays.toString(e.state.getPlayerResults());
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class AveragePoints extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int c = 0;
//            TMGameState tmgs = (TMGameState) e.state;
//            for (int i = 0; i < tmgs.getNPlayers(); i++) {
//                c += tmgs.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue();
//                c += tmgs.countPointsMilestones(i);
//                c += tmgs.countPointsAwards(i);
//                c += tmgs.countPointsBoard(i);
//                c += tmgs.countPointsCards(i);
//            }
//            return c / tmgs.getNPlayers();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class CardsPlayed extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int nCards = 0;
//            TMGameState tmgs = (TMGameState) e.state;
//            for (int i = 0; i < tmgs.getNPlayers(); i++) {
//                nCards += tmgs.getPlayedCards()[i].getSize();
//            }
//            return nCards;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//
//    public static class CorporationCards extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState tmgs = (TMGameState) e.state;
//            StringBuilder ss = new StringBuilder("[");
//            for(TMCard c: tmgs.getPlayerCorporations()) {
//                ss.append(c.getComponentName()).append(",");
//            }
//            ss.append("]");
//            return ss.toString().replace(",]", "]");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class PointDifference extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int c = 0;
//            TMGameState tmgs = (TMGameState) e.state;
//            for (int i = 0; i < tmgs.getNPlayers() - 1; i++) {
//                int s1 = tmgs.countPoints(i);
//                int s2 = tmgs.countPoints(i + 1);
//                c += Math.abs(s1 - s2);
//            }
//            return c / (tmgs.getNPlayers() - 1);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class Milestones extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder("[");
//            TMGameState tmgs = (TMGameState) e.state;
//            for(Milestone m: tmgs.getMilestones()) {
//                if (m.isClaimed()) ss.append(m.getComponentName()).append("-").append(m.claimed).append(",");
//            }
//            ss.append("]");
//            return ss.toString().replace(",]", "]");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class Expansions extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder("[");
//            TMGameState tmgs = (TMGameState) e.state;
//            for(TMTypes.Expansion exp: ((TMGameParameters)tmgs.getGameParameters()).getExpansions()) {
//                ss.append(exp.name()).append(",");
//            }
//            ss.append("]");
//            return ss.toString().replace(",]", "]");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class Map extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            String ss = "Tharsis";
//            TMGameState tmgs = (TMGameState) e.state;
//            HashSet<TMTypes.Expansion> exps = ((TMGameParameters)tmgs.getGameParameters()).getExpansions();
//            if (exps.contains(TMTypes.Expansion.Hellas)) ss = "Hellas";
//            else if (exps.contains(TMTypes.Expansion.Elysium)) ss = "Elysium";
//            return ss;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class Awards extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState tmgs = (TMGameState) e.state;
//            StringBuilder ss = new StringBuilder("[");
//            for(Award aa: tmgs.getAwards()) {
//                if (aa.isClaimed()) {
//                    Pair<HashSet<Integer>, HashSet<Integer>> winners = tmgs.awardWinner(aa);
//                    String wins = "(" + winners.a.toString().replace(" ", "") + "," + winners.b.toString().replace(" ", "") + ")";
//                    ss.append(aa.getComponentName()).append("-").append(aa.claimed).append("-").append(wins).append(",");
//                }
//            }
//            ss.append("]");
//            return ss.toString().replace(",]", "]");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class MapCoverage extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int tilesPlaced = 0;
//            int nTiles = 0;
//            TMGameState tmgs = (TMGameState) e.state;
//            for (int i = 0; i < tmgs.getBoard().getHeight(); i++) {
//                for (int j = 0; j < tmgs.getBoard().getWidth(); j++) {
//                    if (tmgs.getBoard().getElement(j, i) != null) {
//                        nTiles++;
//                        if (tmgs.getBoard().getElement(j, i).getTilePlaced() != null) {
//                            tilesPlaced++;
//                        }
//                    }
//                }
//            }
//            return tilesPlaced * 1.0 / nTiles;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class MapTiles extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder();
//            TMGameState tmgs = (TMGameState) e.state;
//            for (int i = 0; i < tmgs.getBoard().getHeight(); i++) {
//                for (int j = 0; j < tmgs.getBoard().getWidth(); j++) {
//                    if (tmgs.getBoard().getElement(j, i) != null) {
//                        if (tmgs.getBoard().getElement(j, i).getTilePlaced() != null) {
//                            ss.append("(").append(j).append("-").append(i).append("),");
//                        }
//                    }
//                }
//            }
//            for (TMMapTile map : tmgs.getExtraTiles()) {
//                ss.append(map.getComponentName()).append(",");
//            }
//            ss.append("]");
//            return ss.toString().replace(",]", "");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class PlayerResourceProduction extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder();
//            TMGameState tmgs = (TMGameState) e.state;
//            for (TMTypes.Resource r: tmgs.getPlayerProduction()[e.playerID].keySet()) {
//                ss.append(r.name()).append(":").append(tmgs.getPlayerProduction()[e.playerID].get(r).getValue()).append(",");
//            }
//            return ss.toString();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class TRPointsProgress extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState) e.state);
//            int x = e.state.getRoundCounter();
//            return new TimeStamp(x, s.getPlayerResources()[e.playerID].get(TMTypes.Resource.TR).getValue());
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class AwardsPointProgress extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState) e.state);
//            int x = e.state.getRoundCounter();
//            return new TimeStamp(x, s.countPointsAwards(e.playerID));
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class MilestonesPointsProgress extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState) e.state);
//            int x = e.state.getRoundCounter();
//            return new TimeStamp(x, s.countPointsMilestones(e.playerID));
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class CardsPointsProgress extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState) e.state);
//            int x = e.state.getRoundCounter();
//            return new TimeStamp(x, s.countPointsCards(e.playerID));
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class BoardPointsProgress extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState) e.state);
//            int x = e.state.getRoundCounter();
//            return new TimeStamp(x, s.countPointsBoard(e.playerID));
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerCardsPlayed extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).getPlayedCards()[e.playerID].getSize();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerParameterContribution extends AbstractParameterizedMetric {
//        public PlayerParameterContribution(){super();}
//        public PlayerParameterContribution(Object arg){super(arg);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMTypes.GlobalParameter parameter = (TMTypes.GlobalParameter) getParameterValue("parameter");
//
//            int count = 0;
//            TMGameState s = ((TMGameState)e.state);
//            if (s.getGlobalParameters().containsKey(parameter)) {
//                ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(parameter).getIncreases();
//                for (Pair<Integer, Integer> pair : increases) {
//                    if (Objects.equals(pair.b, e.playerID)) count++;
//                }
//                return count * 1.0 / increases.size();
//            }
//            return 0;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("parameter", Arrays.asList(TMTypes.GlobalParameter.values()), TMTypes.GlobalParameter.Oxygen));
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerResult extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return e.state.getPlayerResults()[e.playerID].value;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerCorporationCard extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).getPlayerCorporations()[e.playerID].getComponentName();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class PlayerCorporationCardWin extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState)e.state);
//            if (s.getPlayerResults()[e.playerID] == CoreConstants.GameResult.WIN_GAME) return s.getPlayerCorporations()[e.playerID].getComponentName();
//            else return "";
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerPlayedCardsPerType extends AbstractParameterizedMetric {
//        public PlayerPlayedCardsPerType(){super();}
//        public PlayerPlayedCardsPerType(Object arg){super(arg);}
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMTypes.CardType type = (TMTypes.CardType) getParameterValue("type");
//            int c = 0;
//            TMGameState s = ((TMGameState)e.state);
//            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
//                if (card.cardType == type) c++;
//            }
//            return c;
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("type", Arrays.asList(TMTypes.CardType.values()), TMTypes.CardType.Automated));
//        }
//    }
//
//    public static class PlayerAllCardsPlayed extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder();
//            TMGameState s = ((TMGameState)e.state);
//            return getCards(e, ss, s);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerCardsPlayedWin extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState)e.state);
//            if (s.getPlayerResults()[e.playerID] == CoreConstants.GameResult.WIN_GAME) {
//                StringBuilder ss = new StringBuilder();
//                return getCards(e, ss, s);
//            }
//            return "";
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    @NotNull
//    private static Object getCards(Event e, StringBuilder ss, TMGameState s) {
//        for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
//            ss.append(card.getComponentName()).append(",");
//        }
//        if (ss.toString().equals("")) return ss.toString();
//        ss.append("]");
//        return ss.toString().replace(",]", "");
//    }
//
//    public static class PlayerPointsTotal extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).countPoints(e.playerID);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerPointsTR extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.TR).getValue();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerPointsMilestones extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).countPointsMilestones(e.playerID);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerPointsAwards extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).countPointsAwards(e.playerID);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerPointsBoard extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).countPointsBoard(e.playerID);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerPointsCards extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((TMGameState)e.state).countPointsCards(e.playerID);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerMilestones extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ms = new StringBuilder();
//            TMGameState s = ((TMGameState)e.state);
//            for(Milestone m: s.getMilestones()) {
//                if (m.isClaimed() && m.claimed == e.playerID) {
//                    ms.append(m.getComponentName()).append(",");
//                }
//            }
//            ms.append("]");
//            if (ms.toString().equals("]")) return "";
//            return ms.toString().replace(",]", "");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerNumMilestones extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int c = 0;
//            TMGameState s = ((TMGameState)e.state);
//            for(Milestone m: s.getMilestones()) {
//                if (m.isClaimed() && m.claimed == e.playerID) c++;
//            }
//            return c;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class PlayerNumAwardsFunded extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int c = 0;
//            TMGameState s = ((TMGameState)e.state);
//            for(Award aa: s.getAwards()) {
//                if (aa.isClaimed() && aa.claimed == e.playerID) {
//                    c++;
//                }
//            }
//            return c;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerNumAwardsWon extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int c = 0;
//            TMGameState s = ((TMGameState)e.state);
//            for(Award aa: s.getAwards()) {
//                if (aa.isClaimed()) {
//                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
//                    if (winners.a.contains(e.playerID) || winners.b.contains(e.playerID)) {
//                        c++;
//                    }
//                }
//            }
//            return c;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerAwardsFundedAndWon extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int c = 0;
//            int p = e.playerID;
//            TMGameState s = ((TMGameState)e.state);
//            for(Award aa: s.getAwards()) {
//                if (aa.isClaimed() && aa.claimed == p) {
//                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
//                    if (winners.a.contains(p) || winners.b.contains(p)) {
//                        c++;
//                    }
//                }
//            }
//            return c;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerAwardsWon extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder();
//            int p = e.playerID;
//            TMGameState s = ((TMGameState)e.state);
//            for(Award aa: s.getAwards()) {
//                if (aa.isClaimed()) {
//                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
//                    if (winners.a.contains(p) || winners.b.contains(p)) {
//                        ss.append(aa.getComponentName()).append(",");
//                    }
//                }
//            }
//            ss.append("]");
//            if (ss.toString().equals("]")) return "";
//            return ss.toString().replace(",]", "");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerAwardsFunded extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            StringBuilder ss = new StringBuilder();
//            TMGameState s = ((TMGameState)e.state);
//            for(Award aa: s.getAwards()) {
//                if (aa.isClaimed() && aa.claimed == e.playerID) {
//                    ss.append(aa.getComponentName()).append(",");
//                }
//            }
//            ss.append("]");
//            if (ss.toString().equals("]")) return "";
//            return ss.toString().replace(",]", "");
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerMapCoverage extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int tilesPlaced = 0;
//            int nTiles = 0;
//            TMGameState s = ((TMGameState)e.state);
//            for (int i = 0; i < s.getBoard().getHeight(); i++) {
//                for (int j = 0; j < s.getBoard().getWidth(); j++) {
//                    if (s.getBoard().getElement(j, i) != null) {
//                        nTiles ++;
//                        if (s.getBoard().getElement(j, i).getTilePlaced() != null && s.getBoard().getElement(j, i).getOwnerId() == e.playerID) {
//                            tilesPlaced++;
//                        }
//                    }
//                }
//            }
//            return tilesPlaced*1.0 / nTiles;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//
//    public static class PlayerProduction extends AbstractParameterizedMetric {
//        public PlayerProduction(){super();}
//        public PlayerProduction(Object arg){super(arg);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMTypes.Resource type = (TMTypes.Resource) getParameterValue("type");
//            return ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(type).getValue();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("type", Arrays.asList(TMTypes.Resource.getPlayerBoardResources()), TMTypes.Resource.MegaCredit));
//        }
//    }
//
//    public static class PlayerResource extends AbstractParameterizedMetric {
//        public PlayerResource(){super();}
//        public PlayerResource(Object arg){super(arg);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMTypes.Resource type = (TMTypes.Resource) getParameterValue("type");
//            return ((TMGameState)e.state).getPlayerResources()[e.playerID].get(type).getValue();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("type", Arrays.asList(TMTypes.Resource.getPlayerBoardResources()), TMTypes.Resource.MegaCredit));
//        }
//    }
//
//    public static class PlayerHandSize extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            TMGameState s = ((TMGameState) e.state);
//            int x = e.state.getRoundCounter();
//            return new TimeStamp(x, ((TMGameState)e.state).getPlayerHands()[e.playerID].getSize());
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//}

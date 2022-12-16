package games.terraformingmars.stats;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import evaluation.metrics.IMetricsCollection;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import utilities.Pair;
import utilities.Utils;

import java.util.*;


public class TerraformingMarsMetrics implements IMetricsCollection {
    public static class ParameterComplete extends AbstractMetric {

        public ParameterComplete(){ this("Oxygen"); }
        private TMTypes.GlobalParameter parameter;
        public ParameterComplete(String param) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.parameter = TMTypes.GlobalParameter.valueOf(param);
        }
        public String name() {return getClass().getSimpleName() + " (" + parameter + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            TMGameState tmgs = (TMGameState) e.state;
            ArrayList<Pair<Integer, Integer>> increases = tmgs.getGlobalParameters().get(parameter).getIncreases();
            return increases.get(increases.size() - 1).a;
        }

        public Object[] getAllowedParameters() { return TMTypes.GlobalParameter.values(); }
    }


    public static class ParameterIncrease extends AbstractMetric {

        public ParameterIncrease(){this("Oxygen"); }
        private TMTypes.GlobalParameter parameter;
        public ParameterIncrease(String param) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.parameter = TMTypes.GlobalParameter.valueOf(param);
        }
        public String name() {return getClass().getSimpleName() + " (" + parameter + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            TMGameState tmgs = (TMGameState) e.state;
            return (tmgs.getGlobalParameters().get(parameter).getIncreasesString());
        }
        public Object[] getAllowedParameters() { return TMTypes.GlobalParameter.values(); }
    }

    public static class Generation extends AbstractMetric {

        public Generation() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState) e.state).getGeneration();
        }
    }

    public static class Result extends AbstractMetric {

        public Result() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return Arrays.toString(e.state.getPlayerResults());
        }
    }

    public static class AveragePoints extends AbstractMetric {

        public AveragePoints() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            int c = 0;
            TMGameState tmgs = (TMGameState) e.state;
            for (int i = 0; i < tmgs.getNPlayers(); i++) {
                c += tmgs.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue();
                c += tmgs.countPointsMilestones(i);
                c += tmgs.countPointsAwards(i);
                c += tmgs.countPointsBoard(i);
                c += tmgs.countPointsCards(i);
            }
            return c / tmgs.getNPlayers();
        }
    }

    public static class CardsPlayed extends AbstractMetric {

        public CardsPlayed() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            int nCards = 0;
            TMGameState tmgs = (TMGameState) e.state;
            for (int i = 0; i < tmgs.getNPlayers(); i++) {
                nCards += tmgs.getPlayedCards()[i].getSize();
            }
            return nCards;
        }
    }


    public static class CorporationCards extends AbstractMetric {

        public CorporationCards() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            TMGameState tmgs = (TMGameState) e.state;
            StringBuilder ss = new StringBuilder("[");
            for(TMCard c: tmgs.getPlayerCorporations()) {
                ss.append(c.getComponentName()).append(",");
            }
            ss.append("]");
            return ss.toString().replace(",]", "]");
        }
    }

    public static class PointDifference extends AbstractMetric {

        public PointDifference() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            int c = 0;
            TMGameState tmgs = (TMGameState) e.state;
            for (int i = 0; i < tmgs.getNPlayers() - 1; i++) {
                int s1 = tmgs.countPoints(i);
                int s2 = tmgs.countPoints(i + 1);
                c += Math.abs(s1 - s2);
            }
            return c / (tmgs.getNPlayers() - 1);
        }
    }

    public static class Milestones extends AbstractMetric {

        public Milestones() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            StringBuilder ss = new StringBuilder("[");
            TMGameState tmgs = (TMGameState) e.state;
            for(Milestone m: tmgs.getMilestones()) {
                if (m.isClaimed()) ss.append(m.getComponentName()).append("-").append(m.claimed).append(",");
            }
            ss.append("]");
            return ss.toString().replace(",]", "]");
        }
    }

    public static class Expansions extends AbstractMetric {

        public Expansions() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            StringBuilder ss = new StringBuilder("[");
            TMGameState tmgs = (TMGameState) e.state;
            for(TMTypes.Expansion exp: ((TMGameParameters)tmgs.getGameParameters()).getExpansions()) {
                ss.append(exp.name()).append(",");
            }
            ss.append("]");
            return ss.toString().replace(",]", "]");
        }
    }

    public static class Map extends AbstractMetric {

        public Map() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            String ss = "Tharsis";
            TMGameState tmgs = (TMGameState) e.state;
            HashSet<TMTypes.Expansion> exps = ((TMGameParameters)tmgs.getGameParameters()).getExpansions();
            if (exps.contains(TMTypes.Expansion.Hellas)) ss = "Hellas";
            else if (exps.contains(TMTypes.Expansion.Elysium)) ss = "Elysium";
            return ss;
        }
    }

    public static class Awards extends AbstractMetric {

        public Awards() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            TMGameState tmgs = (TMGameState) e.state;
            StringBuilder ss = new StringBuilder("[");
            for(Award aa: tmgs.getAwards()) {
                if (aa.isClaimed()) {
                    Pair<HashSet<Integer>, HashSet<Integer>> winners = tmgs.awardWinner(aa);
                    String wins = "(" + winners.a.toString().replace(" ", "") + "," + winners.b.toString().replace(" ", "") + ")";
                    ss.append(aa.getComponentName()).append("-").append(aa.claimed).append("-").append(wins).append(",");
                }
            }
            ss.append("]");
            return ss.toString().replace(",]", "]");
        }
    }

    public static class MapCoverage extends AbstractMetric {

        public MapCoverage() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            int tilesPlaced = 0;
            int nTiles = 0;
            TMGameState tmgs = (TMGameState) e.state;
            for (int i = 0; i < tmgs.getBoard().getHeight(); i++) {
                for (int j = 0; j < tmgs.getBoard().getWidth(); j++) {
                    if (tmgs.getBoard().getElement(j, i) != null) {
                        nTiles++;
                        if (tmgs.getBoard().getElement(j, i).getTilePlaced() != null) {
                            tilesPlaced++;
                        }
                    }
                }
            }
            return tilesPlaced * 1.0 / nTiles;
        }
    }

    public static class MapTiles extends AbstractMetric {

        public MapTiles() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            String ss = "";
            TMGameState tmgs = (TMGameState) e.state;
            for (int i = 0; i < tmgs.getBoard().getHeight(); i++) {
                for (int j = 0; j < tmgs.getBoard().getWidth(); j++) {
                    if (tmgs.getBoard().getElement(j, i) != null) {
                        if (tmgs.getBoard().getElement(j, i).getTilePlaced() != null) {
                            ss += "(" + j + "-" + i + "),";
                        }
                    }
                }
            }
            for (TMMapTile map : tmgs.getExtraTiles()) {
                ss += map.getComponentName() + ",";
            }
            ss += "]";
            return ss.replace(",]", "");
        }
    }

    public static class PlayerResourceProduction extends AbstractMetric {

        public PlayerResourceProduction() {addEventType(Event.GameEvent.GAME_OVER); this.recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            StringBuilder ss = new StringBuilder();
            TMGameState tmgs = (TMGameState) e.state;
            for (TMTypes.Resource r: tmgs.getPlayerProduction()[e.playerID].keySet()) {
                ss.append(r.name()).append(":").append(tmgs.getPlayerProduction()[e.playerID].get(r).getValue()).append(",");
            }
            return ss.toString();
        }
    }

    public static class PointsProgress extends AbstractMetric {

        public PointsProgress() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            TMGameState s = ((TMGameState) e.state);
            StringBuilder ss = new StringBuilder("[");
            for (int i = 0; i < s.getNPlayers(); i++) {
                ss.append("(").append(s.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue())
                        .append(",").append(s.countPointsMilestones(i)).append(",").append(s.countPointsAwards(i))
                        .append(",").append(s.countPointsBoard(i)).append(",").append(s.countPointsCards(i)).append("),");
            }
            ss.append("]");
            return ss.toString().replace(",]", "]");
        }
    }

    /// PLAYER


    public static class PlayerCardsPlayed extends AbstractMetric {

        public PlayerCardsPlayed() {addEventType(Event.GameEvent.GAME_OVER); recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).getPlayedCards()[e.playerID].getSize();
        }
    }

    public static class PlayerParameterContribution extends AbstractMetric {

        public PlayerParameterContribution() {this("Oxygen"); }

        private TMTypes.GlobalParameter parameter;
        public PlayerParameterContribution(String param) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.parameter = TMTypes.GlobalParameter.valueOf(param);
            recordPerPlayer = true;
        }
        public String name() {return getClass().getSimpleName() + " (" + parameter + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            int count = 0;
            TMGameState s = ((TMGameState)e.state);
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(parameter).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, e.playerID)) count++;
            }
            return count*1.0 / increases.size();
        }
        public Object[] getAllowedParameters() { return TMTypes.GlobalParameter.values(); }
    }

    public static class PlayerResult extends AbstractMetric {

        public PlayerResult() {addEventType(Event.GameEvent.GAME_OVER); recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getPlayerResults()[e.playerID].value;
        }
    }

    public static class PlayerCorporationCard extends AbstractMetric {

        public PlayerCorporationCard() {addEventType(Event.GameEvent.GAME_OVER); recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).getPlayerCorporations()[e.playerID].getComponentName();
        }
    }


    public static class PlayerCorporationCardWin extends AbstractMetric {

        public PlayerCorporationCardWin() {addEventType(Event.GameEvent.GAME_OVER); recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            TMGameState s = ((TMGameState)e.state);
            if (s.getPlayerResults()[e.playerID] == Utils.GameResult.WIN) return s.getPlayerCorporations()[e.playerID].getComponentName();
            else return "";
        }
    }

    public static class PlayerPlayedCardsPerType extends AbstractMetric {
        public PlayerPlayedCardsPerType(){this("Automated"); }
        private TMTypes.CardType type;
        public PlayerPlayedCardsPerType(String cardType) {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
            type = TMTypes.CardType.valueOf(cardType);
        }

        public String name() {return getClass().getSimpleName() + " (" + type + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
                if (card.cardType == type) c++;
            }
            return c;
        }
        public Object[] getAllowedParameters() { return TMTypes.CardType.values(); }
    }

    public static class PlayerAllCardsPlayed extends AbstractMetric {

        public PlayerAllCardsPlayed() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            String ss = "";
            TMGameState s = ((TMGameState)e.state);
            for (TMCard card: s.getPlayedCards()[e.playerID].getComponents()) {
                ss += card.getComponentName() + ",";
            }
            if (ss.equals("")) return ss;
            ss += "]";
            return ss.replace(",]", "");
        }
    }


    public static class PlayerCardsPlayedWin extends AbstractMetric {

        public PlayerCardsPlayedWin() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
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
        }
    }



    public static class PlayerPointsTotal extends AbstractMetric {

        public PlayerPointsTotal() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).countPoints(e.playerID);
        }
    }

    public static class PlayerPointsTR extends AbstractMetric {

        public PlayerPointsTR() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).getPlayerResources()[e.playerID].get(TMTypes.Resource.TR).getValue();
        }
    }

    public static class PlayerPointsMilestones extends AbstractMetric {

        public PlayerPointsMilestones() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).countPointsMilestones(e.playerID);
        }
    }

    public static class PlayerPointsBoard extends AbstractMetric {

        public PlayerPointsBoard() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).countPointsBoard(e.playerID);
        }
    }

    public static class PlayerPointsCards extends AbstractMetric {

        public PlayerPointsCards() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).countPointsCards(e.playerID);
        }
    }


    public static class PlayerMilestones extends AbstractMetric {

        public PlayerMilestones() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
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
        }
    }

    public static class PlayerNumMilestones extends AbstractMetric {

        public PlayerNumMilestones() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for(Milestone m: s.getMilestones()) {
                if (m.isClaimed() && m.claimed == e.playerID) c++;
            }
            return c;
        }
    }


    public static class PlayerNumAwardsFunded extends AbstractMetric {

        public PlayerNumAwardsFunded() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            int c = 0;
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed() && aa.claimed == e.playerID) {
                    c++;
                }
            }
            return c;
        }
    }

    public static class PlayerNumAwardsWon extends AbstractMetric {

        public PlayerNumAwardsWon() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
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
        }
    }

    public static class PlayerAwardsFundedAndWon extends AbstractMetric {

        public PlayerAwardsFundedAndWon() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
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
        }
    }

    public static class PlayerAwardsWon extends AbstractMetric {

        public PlayerAwardsWon() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            StringBuilder ss = new StringBuilder();
            int p = e.playerID;
            TMGameState s = ((TMGameState)e.state);
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed()) {
                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
                    if (winners.a.contains(p) || winners.b.contains(p)) {
                        ss.append(aa.getComponentName()).append(",");
                    }
                }
            }
            ss.append("]");
            if (ss.toString().equals("]")) return "";
            return ss.toString().replace(",]", "");
        }
    }

    public static class PlayerAwardsFunded extends AbstractMetric {

        public PlayerAwardsFunded() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
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
        }
    }
    public static class PlayerMapCoverage extends AbstractMetric {

        public PlayerMapCoverage() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
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
        }
    }


    public static class PlayerProduction extends AbstractMetric {
        public PlayerProduction(){this("MegaCredit");}
        private TMTypes.Resource type;
        public PlayerProduction(String res) {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
            type = TMTypes.Resource.valueOf(res);
        }

        public String name() {return getClass().getSimpleName() + " (" + type + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).getPlayerProduction()[e.playerID].get(type).getValue();
        }
        public Object[] getAllowedParameters() { return TMTypes.Resource.values(); }
    }

    public static class PlayerResource extends AbstractMetric {

        public PlayerResource(){this("MegaCredit");}

        private TMTypes.Resource type;
        public PlayerResource(String res) {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
            type = TMTypes.Resource.valueOf(res);
        }

        public String name() {return getClass().getSimpleName() + " (" + type + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).getPlayerResources()[e.playerID].get(type).getValue();
        }
        public Object[] getAllowedParameters() { return TMTypes.Resource.values(); }
    }


    public static class PlayerHandSize extends AbstractMetric {

        public PlayerHandSize() {
            addEventType(Event.GameEvent.GAME_OVER);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            return ((TMGameState)e.state).getPlayerHands()[e.playerID].getSize();
        }
    }
}

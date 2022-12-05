package games.terraformingmars.stats;

import core.interfaces.IGameMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import utilities.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;

public enum TMGameAttributes implements IGameMetric {
    //        GAME_ID((l, e) -> s.getGameID()),
    GENERATION((l, e) -> ((TMGameState) e.state).getGeneration()),
    //        N_PLAYERS((l, e) -> s.getNPlayers()),
    RESULT((l, e) -> Arrays.toString(e.state.getPlayerResults())),
    GP_OCEAN_COMPLETE_GEN((l, e) -> {
        TMGameState tmgs = (TMGameState) e.state;
        ArrayList<Pair<Integer, Integer>> increases = tmgs.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreases();
        return increases.get(increases.size() - 1).a;
    }),
    GP_TEMPERATURE_COMPLETE_GEN((l, e) -> {
        TMGameState tmgs = (TMGameState) e.state;
        ArrayList<Pair<Integer, Integer>> increases = tmgs.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreases();
        return increases.get(increases.size() - 1).a;
    }),
    GP_OXYGEN_COMPLETE_GEN((l, e) -> {
        TMGameState tmgs = (TMGameState) e.state;
        ArrayList<Pair<Integer, Integer>> increases = tmgs.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreases();
        return increases.get(increases.size() - 1).a;
    }),
    //        GP_OCEAN((l, e) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreasesString()),  // Pairs are (generation,player)
//        GP_TEMPERATURE((l, e) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreasesString()),
//        GP_OXYGEN((l, e) -> e.state.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreasesString()),
//        CORP_CARDS((l, e) -> {
//            String ss = "[";
//            for(TMCard c: s.getPlayerCorporations()) {
//                ss += c.getComponentName() + ",";
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
    N_CARDS_PLAYED((l, e) -> {
        int nCards = 0;
        TMGameState tmgs = (TMGameState) e.state;
        for (int i = 0; i < tmgs.getNPlayers(); i++) {
            nCards += tmgs.getPlayedCards()[i].getSize();
        }
        return nCards;
    }),
    AVG_N_POINTS((l, e) -> {
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
    }),
    POINT_DIFF((l, e) -> {
        int c = 0;
        TMGameState tmgs = (TMGameState) e.state;
        for (int i = 0; i < tmgs.getNPlayers() - 1; i++) {
            int s1 = tmgs.countPoints(i);
            int s2 = tmgs.countPoints(i + 1);
            c += Math.abs(s1 - s2);
        }
        return c / (tmgs.getNPlayers() - 1);
    }),
    //        MILESTONES((l, e) -> {
//            String ss = "[";
//            for(Milestone m: s.getMilestones()) {
//                if (m.isClaimed()) ss += m.getComponentName() + "-" + m.claimed + ",";
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
//        EXPANSIONS((l, e) -> {
//            String ss = "[";
//            for(TMTypes.Expansion e: ((TMGameParameters)s.getGameParameters()).getExpansions()) {
//                ss += e.name() + ",";
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
//        MAP((l, e) -> {
//            String ss = "Tharsis";
//            HashSet<TMTypes.Expansion> exps = ((TMGameParameters)s.getGameParameters()).getExpansions();
//            if (exps.contains(TMTypes.Expansion.Hellas)) ss = "Hellas";
//            else if (exps.contains(TMTypes.Expansion.Elysium)) ss = "Elysium";
//            return ss;
//        }),
//        AWARDS((l, e) -> {
//            String ss = "[";
//            for(Award aa: s.getAwards()) {
//                if (aa.isClaimed()) {
//                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
//                    String wins = "(" + winners.a.toString().replace(" ", "") + "," + winners.b.toString().replace(" ", "") + ")";
//                    ss += aa.getComponentName() + "-" + aa.claimed + "-" + wins + ",";
//                }
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
    MAP_COVERAGE((l, e) -> {
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
    }),
    MAP_TILES((l, e) -> {
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
    });
//        RESOURCE_PROD((l, e) -> {
//            String ss = "[";
//            for (int i = 0; i < s.getNPlayers(); i++) {
//                ss += "{";
//                for (TMTypes.Resource r: s.getPlayerProduction()[i].keySet()) {
//                    ss += r.name() + ":" + s.getPlayerProduction()[i].get(r).getValue() + ",";
//                }
//                ss += "},";
//                ss = ss.replace(",}", "}");
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        });

    private final BiFunction<GameListener, Event, Object> lambda_sa;

    TMGameAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda_sa = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda_sa.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == Event.GameEvent.GAME_OVER;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }

}

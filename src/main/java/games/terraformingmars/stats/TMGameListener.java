package games.terraformingmars.stats;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import utilities.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TMGameListener implements IGameListener {

    IStatisticLogger logger;
    public TMGameListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            Map<String, Object> data = Arrays.stream(TMGameAttributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, null)));
            logger.record(data);
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    public enum TMGameAttributes implements IGameAttribute {
//        GAME_ID((s, a) -> s.getGameID()),
        GENERATION((s, a) -> s.getGeneration()),
//        N_PLAYERS((s, a) -> s.getNPlayers()),
        RESULT((s, a) -> Arrays.toString(s.getPlayerResults())),
        GP_OCEAN_COMPLETE_GEN((s, a) -> {
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreases();
            return increases.get(increases.size()-1).a;}),
        GP_TEMPERATURE_COMPLETE_GEN((s, a) -> {
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreases();
            return increases.get(increases.size()-1).a;}),
        GP_OXYGEN_COMPLETE_GEN((s, a) -> {
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreases();
            return increases.get(increases.size()-1).a;}),
//        GP_OCEAN((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreasesString()),  // Pairs are (generation,player)
//        GP_TEMPERATURE((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreasesString()),
//        GP_OXYGEN((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreasesString()),
//        CORP_CARDS((s,a) -> {
//            String ss = "[";
//            for(TMCard c: s.getPlayerCorporations()) {
//                ss += c.getComponentName() + ",";
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
        N_CARDS_PLAYED((s,a) -> {
            int nCards = 0;
            for(int i = 0; i < s.getNPlayers(); i++) {
                nCards += s.getPlayedCards()[i].getSize();
            }
            return nCards;
        }),
        AVG_N_POINTS((s,a) -> {
            int c = 0;
            for(int i = 0; i < s.getNPlayers(); i++) {
                c += s.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue();
                c += s.countPointsMilestones(i);
                c += s.countPointsAwards(i);
                c += s.countPointsBoard(i);
                c += s.countPointsCards(i);
            }
            return c / s.getNPlayers();
        }),
        POINT_DIFF((s,a) -> {
            int c = 0;
            for(int i = 0; i < s.getNPlayers()-1; i++) {
                int s1 = s.countPoints(i);
                int s2 = s.countPoints(i+1);
                c += Math.abs(s1-s2);
            }
            return c / (s.getNPlayers()-1);
        }),
//        MILESTONES((s,a) -> {
//            String ss = "[";
//            for(Milestone m: s.getMilestones()) {
//                if (m.isClaimed()) ss += m.getComponentName() + "-" + m.claimed + ",";
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
//        EXPANSIONS((s,a) -> {
//            String ss = "[";
//            for(TMTypes.Expansion e: ((TMGameParameters)s.getGameParameters()).getExpansions()) {
//                ss += e.name() + ",";
//            }
//            ss += "]";
//            return ss.replace(",]", "]");
//        }),
//        MAP((s,a) -> {
//            String ss = "Tharsis";
//            HashSet<TMTypes.Expansion> exps = ((TMGameParameters)s.getGameParameters()).getExpansions();
//            if (exps.contains(TMTypes.Expansion.Hellas)) ss = "Hellas";
//            else if (exps.contains(TMTypes.Expansion.Elysium)) ss = "Elysium";
//            return ss;
//        }),
//        AWARDS((s,a) -> {
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
        MAP_COVERAGE((s,a) -> {
            int tilesPlaced = 0;
            int nTiles = 0;
            for (int i = 0; i < s.getBoard().getHeight(); i++) {
                for (int j = 0; j < s.getBoard().getWidth(); j++) {
                    if (s.getBoard().getElement(j, i) != null) {
                        nTiles ++;
                        if (s.getBoard().getElement(j, i).getTilePlaced() != null) {
                            tilesPlaced++;
                        }
                    }
                }
            }
            return tilesPlaced*1.0 / nTiles;
        }),
        MAP_TILES((s,a) -> {
            String ss = "";
            for (int i = 0; i < s.getBoard().getHeight(); i++) {
                for (int j = 0; j < s.getBoard().getWidth(); j++) {
                    if (s.getBoard().getElement(j, i) != null) {
                        if (s.getBoard().getElement(j, i).getTilePlaced() != null) {
                            ss += "(" + j + "-" + i + "),";
                        }
                    }
                }
            }
            for (TMMapTile map: s.getExtraTiles()) {
                ss += map.getComponentName() + ",";
            }
            ss += "]";
            return ss.replace(",]","");
        });
//        RESOURCE_PROD((s,a) -> {
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

        private final BiFunction<TMGameState, TMAction, Object> lambda_sa;

        TMGameAttributes(BiFunction<TMGameState, TMAction, Object> lambda) {
            this.lambda_sa = lambda;
        }

        @Override
        public Object get(AbstractGameState state, AbstractAction action) {
            return lambda_sa.apply((TMGameState) state, (TMAction) action);
        }

    }

}

package games.terraformingmars.stats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiFunction;

public enum TMGameAttributes implements IGameAttribute {
    GAME_ID((s, a) -> s.getGameID()),
    GENERATION((s, a) -> s.getGeneration()),
    N_PLAYERS((s, a) -> s.getNPlayers()),
    RESULT((s, a) -> Arrays.toString(s.getPlayerResults())),
//    PLAYER((s, a) -> s.getCurrentPlayer()),
//    ACTION_TYPE((s, a) -> a == null ? "NONE" : a.getClass().getSimpleName()),
//    ACTION_DESCRIPTION((s, a) ->  a == null ? "NONE" : a.getString(s)),
    GP_OCEAN((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreasesString()),  // Pairs are (generation,player)
    GP_TEMPERATURE((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreasesString()),
    GP_OXYGEN((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreasesString()),
    CORP_CARDS((s,a) -> {
        String ss = "[";
        for(TMCard c: s.getPlayerCorporations()) {
            ss += c.getComponentName() + ",";
        }
        ss += "]";
        return ss.replace(",]", "]");
    }),
    N_CARDS_PLAYED((s,a) -> {
        String ss = "[";
        for(int i = 0; i < s.getNPlayers(); i++) {
            ss += s.getPlayedCards()[i].getSize() + ",";
        }
        ss += "]";
        return ss.replace(",]", "]");
    }),
    N_POINTS((s,a) -> {
        String ss = "[";
        for(int i = 0; i < s.getNPlayers(); i++) {
//            ss += s.countPoints(i) + ",";
            ss += "(" + s.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue() + "," +
                    s.countPointsMilestones(i) + "," +
                    s.countPointsAwards(i) + "," +
                    s.countPointsBoard(i) + "," +
                    s.countPointsCards(i) + "),";
        }
        ss += "]";
        return ss.replace(",]", "]");
    }),
//    N_CARDS_PLAYED_0((s, a) -> s.getPlayedCards()[0].getSize()),
//    N_CARDS_PLAYED_1((s, a) -> s.getPlayedCards()[1].getSize()),
//    N_POINTS_0((s, a) -> s.countPoints(0)),
//    N_POINTS_1((s, a) -> s.countPoints(1)),
    MILESTONES((s,a) -> {
        String ss = "[";
        for(Milestone m: s.getMilestones()) {
            if (m.isClaimed()) ss += m.getComponentName() + "-" + m.claimed + ",";
        }
        ss += "]";
        return ss.replace(",]", "]");
    }),
    EXPANSIONS((s,a) -> {
        String ss = "[";
        for(TMTypes.Expansion e: ((TMGameParameters)s.getGameParameters()).getExpansions()) {
            ss += e.name() + ",";
        }
        ss += "]";
        return ss.replace(",]", "]");
    }),
    MAP((s,a) -> {
        String ss = "Tharsis";
        HashSet<TMTypes.Expansion> exps = ((TMGameParameters)s.getGameParameters()).getExpansions();
        if (exps.contains(TMTypes.Expansion.Hellas)) ss = "Hellas";
        else if (exps.contains(TMTypes.Expansion.Elysium)) ss = "Elysium";
        return ss;
    }),
    AWARDS((s,a) -> {
        String ss = "[";
        for(Award aa: s.getAwards()) {
            if (aa.isClaimed()) {
                Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
                String wins = "(" + winners.a.toString().replace(" ", "") + "," + winners.b.toString().replace(" ", "") + ")";
                ss += aa.getComponentName() + "-" + aa.claimed + "-" + wins + ",";
            }
        }
        ss += "]";
        return ss.replace(",]", "]");
    }),
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
    RESOURCE_PROD((s,a) -> {
        String ss = "[";
        for (int i = 0; i < s.getNPlayers(); i++) {
            ss += "{";
            for (TMTypes.Resource r: s.getPlayerProduction()[i].keySet()) {
                ss += r.name() + ":" + s.getPlayerProduction()[i].get(r).getValue() + ",";
            }
            ss += "},";
            ss = ss.replace(",}", "}");
        }
        ss += "]";
        return ss.replace(",]", "]");
    });

    private final BiFunction<TMGameState, TMAction, Object> lambda;

    TMGameAttributes(BiFunction<TMGameState, TMAction, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda.apply((TMGameState) state, (TMAction) action);
    }

}

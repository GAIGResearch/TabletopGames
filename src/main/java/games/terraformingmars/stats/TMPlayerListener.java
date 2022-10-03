package games.terraformingmars.stats;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
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

public class TMPlayerListener implements IGameListener {

    IStatisticLogger[] logger;
    IStatisticLogger aggregate;

    public TMPlayerListener(IStatisticLogger[] logger, IStatisticLogger aggregate) {
        this.logger = logger;
        this.aggregate = aggregate;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            for (int i = 0; i < state.getNPlayers(); i++) {
                final int player = i;
                Map<String, Object> data = Arrays.stream(TMPlayerAttributes.values())
                        .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, player)));
                logger[i].record(data);
                aggregate.record(data);
            }
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
    }

    @Override
    public void allGamesFinished() {
        for (IStatisticLogger log : logger) {
            log.processDataAndFinish();
        }
        aggregate.processDataAndFinish();
    }


    public enum TMPlayerAttributes implements IGameAttribute {
        //    GAME_ID((s, p) -> s.getGameID()),
//    GENERATION((s, p) -> s.getGeneration()),
        RESULT((s, p) -> s.getPlayerResults()[p].value),
        GP_OCEAN_CONTRIBUTION((s, p) -> {
            int count = 0;
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, p)) count++;
            }
            return count*1.0 / increases.size();
        }),  // Pairs are (generation,player)
        GP_TEMPERATURE_CONTRIBUTION((s, p) -> {
            int count = 0;
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, p)) count++;
            }
            return count*1.0 / increases.size();
        }),
        GP_OXYGEN_CONTRIBUTION((s, p) -> {
            int count = 0;
            ArrayList<Pair<Integer, Integer>> increases = s.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen).getIncreases();
            for (Pair<Integer, Integer> pair: increases) {
                if (Objects.equals(pair.b, p)) count++;
            }
            return count*1.0 / increases.size();
        }),
        CORP_CARD((s, p) -> s.getPlayerCorporations()[p].getComponentName()),
        CORP_CARD_WIN((s, p) -> {
            if (s.getPlayerResults()[p] == Utils.GameResult.WIN) return s.getPlayerCorporations()[p].getComponentName();
            else return "";
        }),
        N_CARDS_PLAYED((s, p) -> s.getPlayedCards()[p].getSize()),
        N_CARDS_PLAYED_ACTIVE((s, p) -> {
            int c = 0;
            for (TMCard card: s.getPlayedCards()[p].getComponents()) {
                if (card.cardType == TMTypes.CardType.Active) c++;
            }
            return c;
        }),
        N_CARDS_PLAYED_AUTOMATED((s, p) -> {
            int c = 0;
            for (TMCard card: s.getPlayedCards()[p].getComponents()) {
                if (card.cardType == TMTypes.CardType.Automated) c++;
            }
            return c;
        }),
        N_CARDS_PLAYED_EVENT((s, p) -> {
            int c = 0;
            for (TMCard card: s.getPlayedCards()[p].getComponents()) {
                if (card.cardType == TMTypes.CardType.Event) c++;
            }
            return c;
        }),
        CARDS_PLAYED((s, p) -> {
            String ss = "";
            for (TMCard card: s.getPlayedCards()[p].getComponents()) {
                ss += card.getComponentName() + ",";
            }
            if (ss.equals("")) return ss;
            ss += "]";
            return ss.replace(",]", "");
        }),
        CARDS_PLAYED_WIN((s, p) -> {
            if (s.getPlayerResults()[p] == Utils.GameResult.WIN) {
                String ss = "";
                for (TMCard card : s.getPlayedCards()[p].getComponents()) {
                    ss += card.getComponentName() + ",";
                }
                if (ss.equals("")) return ss;
                ss += "]";
                return ss.replace(",]", "");
            }
            return "";
        }),
        N_POINTS_TOTAL((s, p) -> s.countPoints(p)),
        N_POINTS_TR((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.TR).getValue()),
        N_POINTS_MILESTONES((s, p) -> s.countPointsMilestones(p)),
        N_POINTS_AWARDS((s, p) -> s.countPointsAwards(p)),
        N_POINTS_BOARD((s, p) -> s.countPointsBoard(p)),
        N_POINTS_CARDS((s, p) -> s.countPointsCards(p)),
        N_MILESTONES((s, p) -> {
            int c = 0;
            for(Milestone m: s.getMilestones()) {
                if (m.isClaimed() && m.claimed == p) c++;
            }
            return c;
        }),
        MILESTONES((s, p) -> {
            String ms = "";
            for(Milestone m: s.getMilestones()) {
                if (m.isClaimed() && m.claimed == p) {
                    ms += m.getComponentName() + ",";
                }
            }
            ms += "]";
            if (ms.equals("]")) return "";
            return ms.replace(",]", "");
        }),
        N_AWARDS_WON((s, p) -> {
            int c = 0;
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed()) {
                    Pair<HashSet<Integer>, HashSet<Integer>> winners = s.awardWinner(aa);
                    if (winners.a.contains(p) || winners.b.contains(p)) {
                        c++;
                    }
                }
            }
            return c;
        }),
        N_AWARDS_FUNDED((s, p) -> {
            int c = 0;
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed() && aa.claimed == p) {
                    c++;
                }
            }
            return c;
        }),
        N_AWARDS_WON_AND_FUNDED((s, p) -> {
            int c = 0;
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
        AWARDS_WON((s, p) -> {
            String ss = "";
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
        AWARDS_FUNDED((s, p) -> {
            String ss = "";
            for(Award aa: s.getAwards()) {
                if (aa.isClaimed() && aa.claimed == p) {
                    ss += aa.getComponentName() + ",";
                }
            }
            ss += "]";
            if (ss.equals("]")) return "";
            return ss.replace(",]", "");
        }),
        MAP_COVERAGE((s, p) -> {
            int tilesPlaced = 0;
            int nTiles = 0;
            for (int i = 0; i < s.getBoard().getHeight(); i++) {
                for (int j = 0; j < s.getBoard().getWidth(); j++) {
                    if (s.getBoard().getElement(j, i) != null) {
                        nTiles ++;
                        if (s.getBoard().getElement(j, i).getTilePlaced() != null && s.getBoard().getElement(j, i).getOwnerId() == p) {
                            tilesPlaced++;
                        }
                    }
                }
            }
            return tilesPlaced*1.0 / nTiles;
        }),
        MEGACREDIT_PROD((s, p) -> s.getPlayerProduction()[p].get(TMTypes.Resource.MegaCredit).getValue() ),
        STEEL_PROD((s, p) -> s.getPlayerProduction()[p].get(TMTypes.Resource.Steel).getValue() ),
        TITANIUM_PROD((s, p) -> s.getPlayerProduction()[p].get(TMTypes.Resource.Titanium).getValue() ),
        PLANT_PROD((s, p) -> s.getPlayerProduction()[p].get(TMTypes.Resource.Plant).getValue() ),
        ENERGY_PROD((s, p) -> s.getPlayerProduction()[p].get(TMTypes.Resource.Energy).getValue() ),
        HEAT_PROD((s, p) -> s.getPlayerProduction()[p].get(TMTypes.Resource.Heat).getValue() ),

        MEGACREDIT((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.MegaCredit).getValue() ),
        STEEL((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.Steel).getValue() ),
        TITANIUM((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.Titanium).getValue() ),
        PLANT((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.Plant).getValue() ),
//        ENERGY((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.Energy).getValue() ),
        HEAT((s, p) -> s.getPlayerResources()[p].get(TMTypes.Resource.Heat).getValue() ),

        CARDS((s, p) -> s.getPlayerHands()[p].getSize()),
        ;

        private final BiFunction<TMGameState, Integer, Object> lambda_sp;

        TMPlayerAttributes(BiFunction<TMGameState, Integer, Object> lambda) {
            this.lambda_sp = lambda;
        }

        public Object get(AbstractGameState state, int player) {
            return lambda_sp.apply((TMGameState) state, player);
        }

    }

}

package games.wonders7.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameMetrics;
import evaluation.metrics.IMetricsCollection;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import games.wonders7.actions.*;
import games.wonders7.cards.Wonder7Card;
import games.wonders7.cards.Wonder7Board;

import java.util.*;


public class Wonders7Metrics implements IMetricsCollection {

    /**
     * Records the parameters chosen for the game that was played
     */
    public static class ChosenParams extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {

            Wonders7GameState state = (Wonders7GameState) e.state;
            Wonders7GameParameters params = (Wonders7GameParameters) state.getGameParameters();

            records.put("nWonderCardsPerPlayer", params.nWonderCardsPerPlayer);
            records.put("nCostNeighbourResource", params.nCostNeighbourResource);
            records.put("nCostDiscountedResource", params.nCostDiscountedResource);
            records.put("nCoinsDiscard", params.nCoinsDiscard);
            records.put("startingCoins", params.startingCoins);

            records.put("rawMaterialLow", params.rawMaterialLow);
            records.put("rawMaterialHigh", params.rawMaterialHigh);
            records.put("manufacturedMaterial", params.manufacturedMaterial);
            records.put("victoryLow", params.victoryLow);
            records.put("victoryMed", params.victoryMed);
            records.put("victoryHigh", params.victoryHigh);
            records.put("victoryVeryHigh", params.victoryVeryHigh);
            records.put("victoryPantheon", params.victoryPantheon);
            records.put("victoryPalace", params.victoryPalace);
            records.put("tavernMoney", params.tavernMoney);
            records.put("wildcardProduction", params.wildcardProduction);
            records.put("commercialMultiplierLow", params.commercialMultiplierLow);
            records.put("commercialMultiplierMed", params.commercialMultiplierMed);
            records.put("commercialMultiplierHigh", params.commercialMultiplierHigh);
            records.put("militaryLow", params.militaryLow);
            records.put("militaryMed", params.militaryMed);
            records.put("militaryHigh", params.militaryHigh);
            records.put("scienceCompass", params.scienceCompass);
            records.put("scienceTablet", params.scienceTablet);
            records.put("scienceCog", params.scienceCog);
            records.put("guildMultiplierLow", params.guildMultiplierLow);
            records.put("guildMultiplierMed", params.guildMultiplierMed);
            records.put("builderMultiplier", params.builderMultiplier);
            records.put("decoratorVictoryPoints", params.decoratorVictoryPoints);

            for (String name : Wonders7GameParameters.DEFAULT_WONDERS) {
                int isIncluded = params.wonders.contains(Wonder7Board.Wonder.valueOf(name)) ? 1 : 0;
                records.put(name, isIncluded);
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("nWonderCardsPerPlayer", Integer.class);
            columns.put("nCostNeighbourResource", Integer.class);
            columns.put("nCostDiscountedResource", Integer.class);
            columns.put("nCoinsDiscard", Integer.class);
            columns.put("startingCoins", Integer.class);

            columns.put("rawMaterialLow", Integer.class);
            columns.put("rawMaterialHigh", Integer.class);
            columns.put("manufacturedMaterial", Integer.class);
            columns.put("victoryLow", Integer.class);
            columns.put("victoryMed", Integer.class);
            columns.put("victoryHigh", Integer.class);
            columns.put("victoryVeryHigh", Integer.class);
            columns.put("victoryPantheon", Integer.class);
            columns.put("victoryPalace", Integer.class);
            columns.put("tavernMoney", Integer.class);
            columns.put("wildcardProduction", Integer.class);
            columns.put("commercialMultiplierLow", Integer.class);
            columns.put("commercialMultiplierMed", Integer.class);
            columns.put("commercialMultiplierHigh", Integer.class);
            columns.put("militaryLow", Integer.class);
            columns.put("militaryMed", Integer.class);
            columns.put("militaryHigh", Integer.class);
            columns.put("scienceCompass", Integer.class);
            columns.put("scienceTablet", Integer.class);
            columns.put("scienceCog", Integer.class);
            columns.put("guildMultiplierLow", Integer.class);
            columns.put("guildMultiplierMed", Integer.class);
            columns.put("builderMultiplier", Integer.class);
            columns.put("decoratorVictoryPoints", Integer.class);

            for (String name : Wonders7GameParameters.DEFAULT_WONDERS) {
                columns.put(name, Integer.class);
            }

            return columns;
        }
    }

    public static class GameSeeds extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState state = (Wonders7GameState) e.state;
            Wonders7GameParameters params = (Wonders7GameParameters) state.getGameParameters();
            records.put("CardSeed", params.cardShuffleSeed);
            records.put("WonderSeed", params.wonderShuffleSeed);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            columns.put("CardSeed", Integer.class);
            columns.put("WonderSeed", Integer.class);
            return columns;
        }
    }


    public static class Boards extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState state = (Wonders7GameState) e.state;
            for (int i = 0; i < state.getNPlayers(); i++) {
                records.put("Player " + i +  " Board", state.getPlayerWonderBoard(i).wonderType().name());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player " + i +  " Board", String.class);
            }
            return columns;
        }
    }

    public static class ActionsCount extends GameMetrics.ActionsCount {

        @Override
        protected void updateActionCount(AbstractAction a) {
            if (a instanceof ChooseCard c) {
                a = c.actionChosen;
            }
            if (actionsTracked.contains(a.getClass())) {
                String actionName = a.getClass().getSimpleName();
                actionsCount.put(actionName, actionsCount.getOrDefault(actionName, 0) + 1);
            }
        }

        @Override
        protected void initializeActionsTracked() {
            actionsTracked = Set.of(BuildFromDiscard.class, BuildStage.class, DiscardCard.class, PlayCard.class);
        }
    }

    public static class CardTypeCount extends AbstractMetric {

        HashMap<String, Integer> cardTypeCount = new HashMap<>();
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.ACTION_CHOSEN) {
                AbstractAction a = e.action;
                if (a instanceof ChooseCard c) {
                    a = c.actionChosen;
                }
                Wonders7GameParameters params = (Wonders7GameParameters) e.state.getGameParameters();
                if (a instanceof PlayCard p) {
                    Wonder7Card card = Wonder7Card.factory(p.cardType, params);
                    cardTypeCount.put(card.buildingType.name(), cardTypeCount.getOrDefault(card.buildingType.name(), 0) + 1);
                }
                return false;
            }
            if (e.type == Event.GameEvent.GAME_OVER) {
                for (Wonder7Card.Type cardType : Wonder7Card.Type.values()) {
                    records.put(cardType.name(), cardTypeCount.getOrDefault(cardType.name(), 0));
                }
                return true;
            }
            return false;
        }

        @Override
        public void notifyGameOver() {
            super.notifyGameOver();
            cardTypeCount.clear();
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (Wonder7Card.Type cardType : Wonder7Card.Type.values()) {
                columns.put(cardType.name(), Integer.class);
            }
            return columns;
        }
    }

    public static class ResourcesPerPlayer extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState wgs = (Wonders7GameState) e.state;
            for (int i=0; i < e.state.getNPlayers(); i++) {
                for (Wonders7Constants.Resource r : Wonders7Constants.Resource.values()) {
                    records.put("Player-" + i + "." + r.name(), wgs.getResource(i, r));
                }
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i=0; i<nPlayersPerGame; i++) {
                for (Wonders7Constants.Resource r : Wonders7Constants.Resource.values()) {
                    columns.put("Player-" + i + "." + r.name(), Integer.class);
                }
            }
            return columns;
        }
    }

    public static class Resources extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState wgs = (Wonders7GameState) e.state;
            for (Wonders7Constants.Resource r : Wonders7Constants.Resource.values()) {
                int resourceCount = 0;
                for (int i = 0; i < e.state.getNPlayers(); i++) {
                    resourceCount += wgs.getResource(i, r);
                }
                records.put(r.name(), resourceCount);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (Wonders7Constants.Resource r : Wonders7Constants.Resource.values()) {
                columns.put(r.name(), Integer.class);
            }
            return columns;
        }
    }

    public static class GameParams extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameParameters params = (Wonders7GameParameters) e.state.getGameParameters();
            records.put("nCostNeighbourResource", params.nCostNeighbourResource);
            records.put("nCostDiscountedResource", params.nCostDiscountedResource);
            records.put("nCoinsDiscard", params.nCoinsDiscard);
            records.put("startingCoins", params.startingCoins);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("nCostNeighbourResource", Integer.class);
            columns.put("nCostDiscountedResource", Integer.class);
            columns.put("nCoinsDiscard", Integer.class);
            columns.put("startingCoins", Integer.class);
            return columns;
        }
    }

}
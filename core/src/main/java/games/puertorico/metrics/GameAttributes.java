package games.puertorico.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

import java.util.*;


public class GameAttributes implements IMetricsCollection {

    public static class BuildingsConstructed extends AbstractMetric {

        PuertoRicoConstants.BuildingType[] buildings = PuertoRicoConstants.BuildingType.values();

        public BuildingsConstructed() {
            super();
        }

        public BuildingsConstructed(String[] args) {
            super(args);
            buildings = new PuertoRicoConstants.BuildingType[args.length];
            for (int i = 0; i < args.length; i++) {
                buildings[i] = PuertoRicoConstants.BuildingType.valueOf(args[i]);
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            for (PuertoRicoConstants.BuildingType building : buildings) {
                int[] allData = new int[state.getNPlayers()];
                for (int i = 0; i < state.getNPlayers(); i++) {
                    int value = (int) state.getPlayerBoard(i).getBuildings().stream().filter(b -> b.buildingType == building).count();
                    records.put("Player " + i + " " + building, value);
                    allData[i] = value;
                }
                records.put("Total " + building, Arrays.stream(allData).sum());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.GAME_OVER, Event.GameEvent.ROUND_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (PuertoRicoConstants.BuildingType building : buildings) {
                columns.put("Total " + building, Integer.class);
                for (int i = 0; i < nPlayersPerGame; i++) {
                    columns.put("Player " + i + " " + building, Integer.class);
                }
            }
            return columns;
        }
    }

    public static class SupplyCounts extends AbstractMetric {
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("VP Chips Left", Integer.class);
                put("Colonists Left", Integer.class);
                put("Colonists On Ship", Integer.class);
            }};
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            records.put("VP Chips Left", state.getVPSupply());
            records.put("Colonists Left", state.getColonistsInSupply());
            records.put("Colonists On Ship", state.getColonistsOnShip());
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.GAME_OVER, Event.GameEvent.ROUND_OVER));
        }
    }

    public static class PlayerBoardStats extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("Player " + i + " Town Size", state.getPlayerBoard(i).getTownSize());
                records.put("Player " + i + " Plantation Size", state.getPlayerBoard(i).getPlantationSize());
                records.put("Player " + i + " Doubloons", state.getPlayerBoard(i).getDoubloons());
                records.put("Player " + i + " Town Vacancies", state.getPlayerBoard(i).getTownVacancies());
                records.put("Player " + i + " Plantation Vacancies", state.getPlayerBoard(i).getPlantationVacancies());

            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.GAME_OVER, Event.GameEvent.ROUND_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player " + i + " Town Size", Integer.class);  // TODO: this is # buildings
                columns.put("Player " + i + " Plantation Size", Integer.class);  // TODO: this is # plantations
                columns.put("Player " + i + " Doubloons", Integer.class);
                columns.put("Player " + i + " Town Vacancies", Integer.class);
                columns.put("Player " + i + " Plantation Vacancies", Integer.class);
            }
            return columns;
        }
    }


    public static class PlantationType extends AbstractMetric {
        PuertoRicoConstants.Crop[] crops = PuertoRicoConstants.Crop.values();

        public PlantationType() {
            super();
        }

        public PlantationType(String[] args) {
            super(args);
            crops = new PuertoRicoConstants.Crop[args.length];
            for (int i = 0; i < args.length; i++) {
                crops[i] = PuertoRicoConstants.Crop.valueOf(args[i]);
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            for (PuertoRicoConstants.Crop crop : crops) {
                int sum = 0;
                for (int i = 0; i < state.getNPlayers(); i++) {
                    records.put("Player " + i + " " + crop.name(), state.getPlayerBoard(i).getPlantationsOf(crop));
                    sum += state.getPlayerBoard(i).getPlantationsOf(crop);
                }
                records.put(crop.name(), sum);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.GAME_OVER, Event.GameEvent.ROUND_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (PuertoRicoConstants.Crop crop : crops) {
                for (int i = 0; i < nPlayersPerGame; i++) {
                    columns.put("Player " + i + " " + crop.name(), Integer.class);
                }
                columns.put(crop.name(), Integer.class);
            }
            return columns;
        }
    }

    public static class CropsInStores extends AbstractMetric {

        List<PuertoRicoConstants.Crop> crops = PuertoRicoConstants.Crop.getTradeableCrops();

        public CropsInStores() {
            super();
        }

        public CropsInStores(String[] args) {
            super(args);
            crops = new ArrayList<>(args.length);
            for (String arg : args) {
                crops.add(PuertoRicoConstants.Crop.valueOf(arg));
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            for (PuertoRicoConstants.Crop crop : crops) {
                int sum = 0;
                for (int i = 0; i < e.state.getNPlayers(); i++) {
                    records.put("Player " + i + " " + crop.name(), state.getPlayerBoard(i).getStoresOf(crop));
                    sum += state.getPlayerBoard(i).getStoresOf(crop);
                }
                records.put(crop.name(), sum);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.GAME_OVER, Event.GameEvent.ROUND_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (PuertoRicoConstants.Crop crop : crops) {
                for (int i = 0; i < nPlayersPerGame; i++) {
                    columns.put("Player " + i + " " + crop.name(), Integer.class);
                }
                columns.put(crop.name(), Integer.class);
            }
            return columns;
        }
    }

    public static class CropsInSupply extends AbstractMetric {
        List<PuertoRicoConstants.Crop> crops = PuertoRicoConstants.Crop.getTradeableCrops();

        public CropsInSupply() {
            super();
        }

        public CropsInSupply(String[] args) {
            super(args);
            crops = new ArrayList<>();
            for (String arg : args) {
                crops.add(PuertoRicoConstants.Crop.valueOf(arg));
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            for (PuertoRicoConstants.Crop crop : crops) {
                records.put(crop.name(), state.getSupplyOf(crop));
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.GAME_OVER, Event.GameEvent.ROUND_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (PuertoRicoConstants.Crop crop : crops) {
                columns.put(crop.name(), Integer.class);
            }
            return columns;
        }
    }


    public static class MoneyOnRole extends AbstractMetric {
        PuertoRicoConstants.Role[] roles = PuertoRicoConstants.Role.values();

        public MoneyOnRole() {
            super();
        }

        public MoneyOnRole(String[] args) {
            super(args);
            roles = new PuertoRicoConstants.Role[args.length];
            for (int i = 0; i < args.length; i++) {
                roles[i] = PuertoRicoConstants.Role.valueOf(args[i]);
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            PuertoRicoGameState state = (PuertoRicoGameState) e.state;
            for (PuertoRicoConstants.Role role : roles) {
                records.put(role.name(), state.getMoneyOnRole(role));
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ROUND_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (PuertoRicoConstants.Role role : roles) {
                columns.put(role.name(), Integer.class);
            }
            return columns;
        }
    }
}
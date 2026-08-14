package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.spatial.SpatialConfig;
import games.descent2e.pcg_clean.spatial.RepairMode;

import java.nio.file.Path;

/** Named application settings and minimal command-line parsing for reproducible demonstrations. */
public record SpatialRunSettings(Path tilePath, int population, int generations, int offspring,
                                 int tournamentSize, int minimumCoordinate, int maximumCoordinate,
                                 int minimumPieces, double mutationRate, long seed, RepairMode repairMode) {
    public static SpatialRunSettings defaults() {
        return new SpatialRunSettings(Path.of("data/descent2e/tiles.json"),
                100, 2500, 100, 3, -24, 24, 10, 0.05, 20260813L, RepairMode.BASIC);
    }

    public static SpatialRunSettings parse(String[] args) {
        SpatialRunSettings value = defaults();
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (!option.startsWith("--")) value = value.withTilePath(Path.of(option));
            else {
                if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + option);
                String argument = args[++i];
                value = switch (option) {
                    case "--seed" -> value.withSeed(Long.parseLong(argument));
                    case "--population" -> value.withPopulation(Integer.parseInt(argument));
                    case "--generations" -> value.withGenerations(Integer.parseInt(argument));
                    case "--offspring" -> value.withOffspring(Integer.parseInt(argument));
                    case "--pieces" -> value.withMinimumPieces(Integer.parseInt(argument));
                    case "--mutation" -> value.withMutationRate(Double.parseDouble(argument));
                    case "--repair" -> value.withRepairMode(RepairMode.parse(argument));
                    default -> throw new IllegalArgumentException("Unknown option: " + option);
                };
            }
        }
        return value;
    }

    public SpatialConfig toConfig() {
        return new SpatialConfig(population, generations, offspring, tournamentSize,
                minimumCoordinate, maximumCoordinate, minimumPieces, mutationRate, seed, repairMode);
    }

    public long evaluations() { return population + (long) generations * offspring; }

    public String description() {
        return "seed=%d, population=%d, generations=%d, offspring=%d, pieces>=%d, mutation=%.3f, repair=%s, evaluations=%,d"
                .formatted(seed, population, generations, offspring, minimumPieces, mutationRate,
                        repairMode.name().toLowerCase(), evaluations());
    }

    private SpatialRunSettings withTilePath(Path v) { return copy(v, population, generations, offspring, minimumPieces, mutationRate, seed); }
    private SpatialRunSettings withSeed(long v) { return copy(tilePath, population, generations, offspring, minimumPieces, mutationRate, v); }
    private SpatialRunSettings withPopulation(int v) { return copy(tilePath, v, generations, offspring, minimumPieces, mutationRate, seed); }
    private SpatialRunSettings withGenerations(int v) { return copy(tilePath, population, v, offspring, minimumPieces, mutationRate, seed); }
    private SpatialRunSettings withOffspring(int v) { return copy(tilePath, population, generations, v, minimumPieces, mutationRate, seed); }
    private SpatialRunSettings withMinimumPieces(int v) { return copy(tilePath, population, generations, offspring, v, mutationRate, seed); }
    private SpatialRunSettings withMutationRate(double v) { return copy(tilePath, population, generations, offspring, minimumPieces, v, seed); }
    private SpatialRunSettings withRepairMode(RepairMode v) {
        return new SpatialRunSettings(tilePath, population, generations, offspring, tournamentSize,
                minimumCoordinate, maximumCoordinate, minimumPieces, mutationRate, seed, v);
    }

    private SpatialRunSettings copy(Path path, int pop, int gens, int children, int pieces, double mutation, long randomSeed) {
        return new SpatialRunSettings(path, pop, gens, children, tournamentSize,
                minimumCoordinate, maximumCoordinate, pieces, mutation, randomSeed, repairMode);
    }
}

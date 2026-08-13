package games.descent2e.pcg_clean.spatial;

public record SpatialConfig(int populationSize, int generations, int offspringPerGeneration,
                            int tournamentSize, int minCoordinate, int maxCoordinate,
                            int initialSelectedPieces, double geneMutationRate, long seed,
                            RepairMode repairMode) {
    /** Source-compatible constructor using the fast default repair. */
    public SpatialConfig(int populationSize, int generations, int offspringPerGeneration,
                         int tournamentSize, int minCoordinate, int maxCoordinate,
                         int initialSelectedPieces, double geneMutationRate, long seed) {
        this(populationSize, generations, offspringPerGeneration, tournamentSize, minCoordinate,
                maxCoordinate, initialSelectedPieces, geneMutationRate, seed, RepairMode.BASIC);
    }

    public SpatialConfig {
        if (repairMode == null) throw new IllegalArgumentException("Repair mode is required");
        if (populationSize < 2 || generations < 0 || offspringPerGeneration < 1 || tournamentSize < 2)
            throw new IllegalArgumentException("Invalid evolutionary configuration");
        if (minCoordinate >= maxCoordinate || initialSelectedPieces < 1)
            throw new IllegalArgumentException("Invalid spatial configuration");
        if (geneMutationRate <= 0 || geneMutationRate > 1)
            throw new IllegalArgumentException("Mutation rate must be in (0,1]");
    }
}

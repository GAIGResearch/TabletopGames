package games.descent2e.pcg_clean.evolution;

public record GeneratorConfig(int populationSize, int generations, int offspringPerGeneration,
                              int tileCount, int tournamentSize, long seed) {
    public GeneratorConfig {
        if (populationSize < 2 || generations < 0 || offspringPerGeneration < 1 || tileCount < 2 || tournamentSize < 2)
            throw new IllegalArgumentException("Invalid generator configuration");
    }
}

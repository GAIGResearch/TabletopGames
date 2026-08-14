package games.descent2e.pcg_clean.mapelites;

import games.descent2e.pcg_clean.spatial.*;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** Deterministic, bounded MAP-Elites search over spatial board chromosomes. */
public final class MapElitesEngine {
    private static final double REPAIR_PROBABILITY = 0.35;
    private static final double RANDOM_RESTART_PROBABILITY = 0.05;
    private static final double EXPLORATORY_CROSSOVER_PROBABILITY = 0.10;
    private static final int UI_UPDATE_INTERVAL = 25;

    private final SpatialConfig config;
    private final SpatialInitializer initializer;
    private final UniformPieceCrossover crossover;
    private final SpatialMutation mutation;
    private final SpatialRepairOperator repair;
    private final SpatialEvaluator evaluator;
    private final MapElitesArchive archive;
    private final MapElitesListener listener;
    private final RandomGenerator random;
    private long nextId;

    public MapElitesEngine(SpatialConfig config, SpatialInitializer initializer,
                           UniformPieceCrossover crossover, SpatialMutation mutation,
                           SpatialRepairOperator repair, SpatialEvaluator evaluator,
                           BehaviorDescriptor descriptor, MapElitesListener listener) {
        this.config = config;
        this.initializer = initializer;
        this.crossover = crossover;
        this.mutation = mutation;
        this.repair = repair;
        this.evaluator = evaluator;
        this.archive = new MapElitesArchive(descriptor);
        this.listener = listener;
        this.random = RandomGeneratorFactory.of("L64X128MixRandom").create(config.seed());
    }

    public MapElitesResult run() {
        for (int i = 0; i < config.populationSize(); i++)
            offer(repair.repair(initializer.create(random), random));
        int iterations = config.generations() * config.offspringPerGeneration();
        for (int i = 0; i < iterations; i++) {
            List<SpatialCandidate> parents = new ArrayList<>(archive.snapshot().elites().values());
            SpatialCandidate parent = parents.get(random.nextInt(parents.size()));
            SpatialChromosome child;
            double emitter = random.nextDouble();
            if (emitter < RANDOM_RESTART_PROBABILITY) {
                child = initializer.create(random);
            } else if (parents.size() > 1
                    && emitter < RANDOM_RESTART_PROBABILITY + EXPLORATORY_CROSSOVER_PROBABILITY) {
                SpatialCandidate second = parents.get(random.nextInt(parents.size()));
                child = crossover.cross(parent.chromosome(), second.chromosome(), random);
            } else child = parent.chromosome();
            child = mutation.mutate(child, random);
            if (random.nextDouble() < REPAIR_PROBABILITY) child = repair.repair(child, random);
            offer(child);
        }
        MapElitesSnapshot result = archive.snapshot();
        listener.archiveChanged(result);
        return new MapElitesResult(result);
    }

    private void offer(SpatialChromosome chromosome) {
        SpatialCandidate candidate = new SpatialCandidate(nextId++, chromosome, evaluator.evaluate(chromosome));
        boolean changed = archive.offer(candidate);
        MapElitesSnapshot snapshot = archive.snapshot();
        if (changed || snapshot.evaluations() % UI_UPDATE_INTERVAL == 0) listener.archiveChanged(snapshot);
    }
}

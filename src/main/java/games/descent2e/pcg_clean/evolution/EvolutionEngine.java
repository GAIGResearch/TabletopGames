package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.domain.BoardGenome;
import games.descent2e.pcg_clean.evaluation.BoardEvaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** Bounded, deterministic, mutation-based EA. The engine knows nothing about Descent constraints. */
public final class EvolutionEngine {
    private static final int VIEWING_ARCHIVE_CAPACITY = 200;
    private final GeneratorConfig config;
    private final GenomeFactory factory;
    private final MutationOperator mutation;
    private final BoardEvaluator evaluator;
    private final RandomGenerator random;
    private long nextId;
    private final Map<Long, Candidate> viewingArchive = new LinkedHashMap<>();

    public EvolutionEngine(GeneratorConfig config, GenomeFactory factory,
                           MutationOperator mutation, BoardEvaluator evaluator) {
        this.config = config;
        this.factory = factory;
        this.mutation = mutation;
        this.evaluator = evaluator;
        this.random = RandomGeneratorFactory.of("L64X128MixRandom").create(config.seed());
    }

    public EvolutionResult run() {
        List<Candidate> population = new ArrayList<>(config.populationSize());
        for (int i = 0; i < config.populationSize(); i++)
            population.add(evaluate(factory.create(config.tileCount(), random)));
        for (int generation = 0; generation < config.generations(); generation++) {
            for (int child = 0; child < config.offspringPerGeneration(); child++) {
                Candidate parent = tournament(population);
                population.add(evaluate(mutation.mutate(parent.genome(), random)));
            }
            population.sort(Comparator.naturalOrder());
            population = selectSurvivors(population);
        }
        population.sort(Comparator.naturalOrder());
        List<Candidate> archive = viewingArchive.values().stream().sorted().toList();
        return new EvolutionResult(population.get(0), population, archive, nextId);
    }

    private Candidate evaluate(BoardGenome genome) {
        Candidate candidate = new Candidate(nextId++, genome, evaluator.evaluate(genome));
        long fitnessKey = Math.round(candidate.evaluation().fitness() * 1_000_000.0);
        if (viewingArchive.size() < VIEWING_ARCHIVE_CAPACITY)
            viewingArchive.putIfAbsent(fitnessKey, candidate);
        return candidate;
    }

    /** Keeps the best copy of each genome before filling any spare slots with duplicates. */
    private List<Candidate> selectSurvivors(List<Candidate> ranked) {
        Map<BoardGenome, Candidate> unique = new LinkedHashMap<>();
        ranked.forEach(candidate -> unique.putIfAbsent(candidate.genome(), candidate));
        List<Candidate> survivors = new ArrayList<>(config.populationSize());
        unique.values().stream().limit(config.populationSize()).forEach(survivors::add);
        if (survivors.size() < config.populationSize()) {
            for (Candidate candidate : ranked) {
                if (survivors.size() == config.populationSize()) break;
                if (!survivors.contains(candidate)) survivors.add(candidate);
            }
        }
        return survivors;
    }

    private Candidate tournament(List<Candidate> population) {
        Candidate best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < config.tournamentSize(); i++) {
            Candidate challenger = population.get(random.nextInt(population.size()));
            if (challenger.compareTo(best) < 0) best = challenger;
        }
        return best;
    }
}

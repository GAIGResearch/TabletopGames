package games.descent2e.pcg_clean.spatial;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** NSGA-II style two-objective EA: minimise violations and maximise weighted quality. */
public final class SpatialEvolutionEngine {
    private static final int ARCHIVE_CAPACITY = 200;
    private static final double OFFSPRING_REPAIR_PROBABILITY = 0.20;

    private record ObjectiveKey(int violations, long fitness) {}
    private record Ranking(Map<SpatialCandidate, Integer> rank, Map<SpatialCandidate, Double> crowding,
                           List<List<SpatialCandidate>> fronts) {}

    private final SpatialConfig config;
    private final SpatialInitializer initializer;
    private final UniformPieceCrossover crossover;
    private final SpatialMutation mutation;
    private final SpatialRepairOperator repair;
    private final SpatialEvaluator evaluator;
    private final RandomGenerator random;
    private final Map<ObjectiveKey, SpatialCandidate> archive = new LinkedHashMap<>();
    private long nextId;

    public SpatialEvolutionEngine(SpatialConfig config, SpatialInitializer initializer,
                                  UniformPieceCrossover crossover, SpatialMutation mutation,
                                  SpatialRepairOperator repair, SpatialEvaluator evaluator) {
        this.config = config;
        this.initializer = initializer;
        this.crossover = crossover;
        this.mutation = mutation;
        this.repair = repair;
        this.evaluator = evaluator;
        this.random = RandomGeneratorFactory.of("L64X128MixRandom").create(config.seed());
    }

    public SpatialEvolutionResult run() {
        List<SpatialCandidate> population = new ArrayList<>();
        for (int i = 0; i < config.populationSize(); i++)
            population.add(evaluate(repair.repair(initializer.create(random), random)));
        List<GenerationStatistics> history = new ArrayList<>();
        history.add(statistics(0, population));
        for (int generation = 1; generation <= config.generations(); generation++) {
            Ranking ranking = rank(population);
            List<SpatialCandidate> combined = new ArrayList<>(population);
            for (int i = 0; i < config.offspringPerGeneration(); i++) {
                SpatialCandidate first = tournament(population, ranking);
                SpatialCandidate second = tournament(population, ranking);
                SpatialChromosome child = crossover.cross(first.chromosome(), second.chromosome(), random);
                child = mutation.mutate(child, random);
                if (random.nextDouble() < OFFSPRING_REPAIR_PROBABILITY) child = repair.repair(child, random);
                combined.add(evaluate(child));
            }
            population = select(combined);
            history.add(statistics(generation, population));
        }
        Ranking finalRanking = rank(population);
        return new SpatialEvolutionResult(finalRanking.fronts().get(0), population,
                archive.values().stream().sorted(this::objectiveOrder).toList(), history, nextId);
    }

    private SpatialCandidate evaluate(SpatialChromosome chromosome) {
        SpatialCandidate candidate = new SpatialCandidate(nextId++, chromosome, evaluator.evaluate(chromosome));
        ObjectiveKey key = new ObjectiveKey(candidate.evaluation().violationCount(),
                Math.round(candidate.evaluation().quality().fitness() * 1_000_000.0));
        updateArchive(key, candidate);
        return candidate;
    }

    private void updateArchive(ObjectiveKey key, SpatialCandidate candidate) {
        if (archive.containsKey(key)) return;
        if (archive.size() < ARCHIVE_CAPACITY) {
            archive.put(key, candidate);
            return;
        }
        Map.Entry<ObjectiveKey, SpatialCandidate> worst = archive.entrySet().stream()
                .max((a, b) -> objectiveOrder(a.getValue(), b.getValue())).orElseThrow();
        if (objectiveOrder(candidate, worst.getValue()) < 0) {
            archive.remove(worst.getKey());
            archive.put(key, candidate);
        }
    }

    private List<SpatialCandidate> select(List<SpatialCandidate> combined) {
        Ranking ranking = rank(combined);
        List<SpatialCandidate> survivors = new ArrayList<>(config.populationSize());
        for (List<SpatialCandidate> front : ranking.fronts()) {
            if (survivors.size() + front.size() <= config.populationSize()) survivors.addAll(front);
            else {
                front.stream().sorted(Comparator.comparingDouble(
                                (SpatialCandidate candidate) -> ranking.crowding().get(candidate)).reversed()
                                .thenComparingLong(SpatialCandidate::id))
                        .limit(config.populationSize() - survivors.size()).forEach(survivors::add);
                break;
            }
        }
        return survivors;
    }

    private SpatialCandidate tournament(List<SpatialCandidate> population, Ranking ranking) {
        SpatialCandidate best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < config.tournamentSize(); i++) {
            SpatialCandidate challenger = population.get(random.nextInt(population.size()));
            if (better(challenger, best, ranking)) best = challenger;
        }
        return best;
    }

    private boolean better(SpatialCandidate first, SpatialCandidate second, Ranking ranking) {
        int rankComparison = Integer.compare(ranking.rank().get(first), ranking.rank().get(second));
        if (rankComparison != 0) return rankComparison < 0;
        int crowdingComparison = Double.compare(ranking.crowding().get(first), ranking.crowding().get(second));
        return crowdingComparison != 0 ? crowdingComparison > 0 : first.id() < second.id();
    }

    private Ranking rank(List<SpatialCandidate> candidates) {
        Map<SpatialCandidate, Set<SpatialCandidate>> dominates = new LinkedHashMap<>();
        Map<SpatialCandidate, Integer> dominatedBy = new LinkedHashMap<>();
        candidates.forEach(candidate -> { dominates.put(candidate, new LinkedHashSet<>()); dominatedBy.put(candidate, 0); });
        for (int i = 0; i < candidates.size(); i++) for (int j = i + 1; j < candidates.size(); j++) {
            SpatialCandidate a = candidates.get(i);
            SpatialCandidate b = candidates.get(j);
            if (dominates(a, b)) { dominates.get(a).add(b); dominatedBy.merge(b, 1, Integer::sum); }
            else if (dominates(b, a)) { dominates.get(b).add(a); dominatedBy.merge(a, 1, Integer::sum); }
        }
        List<List<SpatialCandidate>> fronts = new ArrayList<>();
        List<SpatialCandidate> current = candidates.stream().filter(c -> dominatedBy.get(c) == 0).toList();
        Map<SpatialCandidate, Integer> ranks = new HashMap<>();
        int level = 0;
        while (!current.isEmpty()) {
            List<SpatialCandidate> front = new ArrayList<>(current);
            fronts.add(front);
            final int rankLevel = level++;
            front.forEach(candidate -> ranks.put(candidate, rankLevel));
            List<SpatialCandidate> next = new ArrayList<>();
            for (SpatialCandidate candidate : front) for (SpatialCandidate dominated : dominates.get(candidate)) {
                int remaining = dominatedBy.merge(dominated, -1, Integer::sum);
                if (remaining == 0) next.add(dominated);
            }
            current = next;
        }
        Map<SpatialCandidate, Double> crowding = new HashMap<>();
        fronts.forEach(front -> calculateCrowding(front, crowding));
        return new Ranking(ranks, crowding, fronts);
    }

    private boolean dominates(SpatialCandidate a, SpatialCandidate b) {
        int av = a.evaluation().violationCount();
        int bv = b.evaluation().violationCount();
        double af = a.evaluation().quality().fitness();
        double bf = b.evaluation().quality().fitness();
        return av <= bv && af >= bf && (av < bv || af > bf);
    }

    private void calculateCrowding(List<SpatialCandidate> front, Map<SpatialCandidate, Double> result) {
        front.forEach(candidate -> result.put(candidate, 0.0));
        addCrowding(front, result, Comparator.comparingInt(c -> c.evaluation().violationCount()),
                candidate -> candidate.evaluation().violationCount());
        addCrowding(front, result, Comparator.comparingDouble(c -> c.evaluation().quality().fitness()),
                candidate -> candidate.evaluation().quality().fitness());
    }

    private void addCrowding(List<SpatialCandidate> front, Map<SpatialCandidate, Double> result,
                             Comparator<SpatialCandidate> comparator,
                             java.util.function.ToDoubleFunction<SpatialCandidate> value) {
        if (front.isEmpty()) return;
        List<SpatialCandidate> sorted = front.stream().sorted(comparator.thenComparingLong(SpatialCandidate::id)).toList();
        result.put(sorted.get(0), Double.POSITIVE_INFINITY);
        result.put(sorted.get(sorted.size() - 1), Double.POSITIVE_INFINITY);
        double range = value.applyAsDouble(sorted.get(sorted.size() - 1)) - value.applyAsDouble(sorted.get(0));
        if (range == 0 || sorted.size() < 3) return;
        for (int i = 1; i < sorted.size() - 1; i++) {
            if (Double.isInfinite(result.get(sorted.get(i)))) continue;
            double distance = (value.applyAsDouble(sorted.get(i + 1)) - value.applyAsDouble(sorted.get(i - 1))) / range;
            result.merge(sorted.get(i), distance, Double::sum);
        }
    }

    private GenerationStatistics statistics(int generation, List<SpatialCandidate> population) {
        int minimum = population.stream().mapToInt(c -> c.evaluation().violationCount()).min().orElse(0);
        double averageViolations = population.stream().mapToInt(c -> c.evaluation().violationCount()).average().orElse(0);
        double bestFitness = population.stream().filter(c -> c.evaluation().violationCount() == minimum)
                .mapToDouble(c -> c.evaluation().quality().fitness()).max().orElse(0);
        double averageFitness = population.stream().mapToDouble(c -> c.evaluation().quality().fitness()).average().orElse(0);
        return new GenerationStatistics(generation, minimum, averageViolations, bestFitness, averageFitness);
    }

    private int objectiveOrder(SpatialCandidate first, SpatialCandidate second) {
        int violations = Integer.compare(first.evaluation().violationCount(), second.evaluation().violationCount());
        return violations != 0 ? violations : -Double.compare(first.evaluation().quality().fitness(), second.evaluation().quality().fitness());
    }
}

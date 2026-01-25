package games.root;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import core.interfaces.IStateHeuristic;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class EvolutionaryHeuristicGenerator {

    private final Random random = new Random();

    public enum crossoverTypes{
        UNIFORM,
        SINGLE_POINT,
        AVERAGE
    }
    private final crossoverTypes crossoverType;
    private final int tournamentSize;
    private final int numberOfWeights = 8;
    private final double minWeight = 0.0;
    private final double maxWeight = 6.0;
    private final int populationSize;
    private final String filePath;
    private int currentGenerationNumber;

    public EvolutionaryHeuristicGenerator(int populationSize, crossoverTypes crossoverType) {
        this.populationSize = populationSize;
        this.filePath = "data/root/default.json";
        this.currentGenerationNumber = getLastGenerationNumber();
        this.tournamentSize = 5;
        this.crossoverType = crossoverType;
        ensurePopulationExists();
    }

    public EvolutionaryHeuristicGenerator(int populationSize, crossoverTypes crossoverType, String path) {
        this.populationSize = populationSize;
        this.filePath = path;
        this.currentGenerationNumber = getLastGenerationNumber();
        this.tournamentSize = 5;
        this.crossoverType = crossoverType;
        ensurePopulationExists();
    }

    // Class to represent an individual
    private static class Individual {
        double[] weights;
        Double fitness;

        Individual(double[] weights) {
            this.weights = weights;
            this.fitness = null;
        }
    }

    // Class to represent a generation
    private static class Generation {
        int generationNumber;
        List<Individual> population;

        Generation(int generationNumber, List<Individual> population) {
            this.generationNumber = generationNumber;
            this.population = population;
        }
    }

    // Class to represent the entire evolutionary process
    private static class Generations {
        List<Generation> generations = new ArrayList<>();
    }

    // Ensure that a population exists for the current generation
    private void ensurePopulationExists() {
        Generations generations = loadEvolutionData();
        if (generations.generations.isEmpty() || !generationExists(currentGenerationNumber, generations)) {
            initializePopulation();
        }
    }

    // Initialize a new population for the current generation
    private void initializePopulation() {
        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            double[] weights = new double[numberOfWeights];
            for (int j = 0; j < numberOfWeights; j++) {
                weights[j] = minWeight + random.nextDouble() * (maxWeight - minWeight);
            }
            population.add(new Individual(weights));
        }

        Generations generations = loadEvolutionData();
        generations.generations.add(new Generation(currentGenerationNumber, population));
        saveEvolutionData(generations);
    }

    // Load the entire evolutionary data from the JSON file
    private Generations loadEvolutionData() {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            Type evolutionDataType = new TypeToken<Generations>() {}.getType();
            Generations data = gson.fromJson(reader, evolutionDataType);
            return data != null ? data : new Generations();
        } catch (IOException e) {
            e.printStackTrace();
            return new Generations();
        }
    }

    // Save the entire evolutionary data to the JSON file
    private void saveEvolutionData(Generations data) {
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();  // Enable pretty printing
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get the last generation
    private int getLastGenerationNumber() {
        Generations data = loadEvolutionData();
        if (data.generations.isEmpty()) {
            return 1;
        }
        return data.generations.get(data.generations.size() - 1).generationNumber;
    }

    private boolean generationExists(int generationNumber, Generations data) {
        return data.generations.stream().anyMatch(g -> g.generationNumber == generationNumber);
    }

    // Get the current generation from the saved json
    private Generation getCurrentGeneration() {
        Generations data = loadEvolutionData();
        return data.generations.stream().filter(g -> g.generationNumber == currentGenerationNumber).findFirst().orElse(null);
    }

    private Generation getLastGeneration() {
        Generations data = loadEvolutionData();
        return data.generations.stream().filter(gen -> gen.generationNumber == currentGenerationNumber-1).findFirst().orElse(null);
    }

    // get the candidate solution from the last generation
    public IStateHeuristic getBestHeuristic() {
        Generation generation = getLastGeneration();
        if (generation == null) {
            initializePopulation();
            generation = getCurrentGeneration();
        }
        List<Individual> highFitnessIndividuals = new ArrayList<>();
        for (Individual individual : generation.population) {
            if (individual.fitness != null && individual.fitness >= 30) {
                highFitnessIndividuals.add(individual);
            }
        }
        List<Individual> individualsToAverage = highFitnessIndividuals.isEmpty() ? generation.population : highFitnessIndividuals;
        double[] totalWeights = new double[numberOfWeights];
        for (Individual individual : individualsToAverage) {
            for (int i = 0; i < numberOfWeights; i++) {
                totalWeights[i] += individual.weights[i];
            }
        }
        for (int i = 0; i < numberOfWeights; i++) {
            totalWeights[i] /= individualsToAverage.size();
        }
        return createHeuristic(totalWeights);
    }

    // get the next heuristic function to be evaluated
    public IStateHeuristic getNextHeuristic() {
        Generation generation = getCurrentGeneration();
        if (generation == null) {
            initializePopulation();
            generation = getCurrentGeneration();
        }

        for (Individual individual : generation.population) {
            if (individual.fitness == null) {
                return createHeuristic(individual.weights);
            }
        }
        evolvePopulation(generation);
        return getNextHeuristic();
    }

    private void evolvePopulation(Generation currentGeneration) {
        currentGeneration.population.sort(Comparator.comparing(ind -> ind.fitness));

        // Elitism: Always keep the best performing individual
        Individual bestIndividual = currentGeneration.population.get(0);
        List<Individual> newPopulation = new ArrayList<>();
        newPopulation.add(bestIndividual);

        // Keep all winning individuals
        for(int i = 1; i < currentGeneration.population.size(); i++){
            if (currentGeneration.population.get(i).fitness >= 30){
                newPopulation.add(currentGeneration.population.get(i));
            }else{
                break;
            }
        }

        // Generate the rest of the new population using crossover and mutation
        while (newPopulation.size() < populationSize) {
            Individual parent1 = selectParent(currentGeneration.population);
            Individual parent2 = selectParent(currentGeneration.population);
            double[] childWeights = crossover(parent1.weights, parent2.weights);
            mutate(childWeights);
            newPopulation.add(new Individual(childWeights));
        }
        currentGenerationNumber++;
        Generations data = loadEvolutionData();
        data.generations.add(new Generation(currentGenerationNumber, newPopulation));
        saveEvolutionData(data);
    }

    private Individual selectParent(List<Individual> population) {
        int tournamentSize = this.tournamentSize;
        List<Individual> tournament = new ArrayList<>();

        for (int i = 0; i < tournamentSize; i++) {
            Individual randomIndividual = population.get(random.nextInt(population.size()));
            tournament.add(randomIndividual);
        }
        Individual bestIndividual = tournament.get(0);
        for (Individual individual : tournament) {
            if (individual.fitness > bestIndividual.fitness) {
                bestIndividual = individual;
            }
        }

        return bestIndividual;
    }

    private double[] crossover(double[] parent1, double[] parent2){
        return switch (crossoverType){
            case UNIFORM -> uniformCrossover(parent1,parent2);
            case AVERAGE -> averageCrossover(parent1,parent2);
            case SINGLE_POINT -> singlePointCrossover(parent1,parent2);
        };
    }

    // UNIFORM CROSSOVER
    private double[] uniformCrossover(double[] parent1, double[] parent2) {
        double[] child = new double[numberOfWeights];
        for (int i = 0; i < numberOfWeights; i++) {
            child[i] = random.nextBoolean() ? parent1[i] : parent2[i];
        }
        return child;
    }

    // AVERAGE CROSSOVER
    private double[] averageCrossover(double[] parent1, double[] parent2) {
        double[] child = new double[numberOfWeights];
        for (int i = 0; i < numberOfWeights; i++) {
            child[i] = parent1[i] + parent2[i] / 2;
        }
        return child;
    }
    //Single-Point crossover
    private double[] singlePointCrossover(double[] parent1, double[] parent2){
        double[] child = new double[numberOfWeights];
        int cPoint = random.nextInt(numberOfWeights);
        for (int i = 0; i < numberOfWeights; i++){
            if (i <= cPoint){
                child[i] = parent1[i];
            }else {
                child[i] = parent2[i];
            }
        }
        return child;
    }
    // Mutation method
    private void mutate(double[] individual) {
        for (int i = 0; i < numberOfWeights; i++) {
            if (random.nextDouble() < 0.1) {
                individual[i] += random.nextGaussian();
                individual[i] = Math.max(minWeight, Math.min(maxWeight, individual[i]));
            }
        }
    }

    // Create a RootHeuristic instance from an individual's weights
    public IStateHeuristic createHeuristic(double[] weights) {
        RootHeuristic heuristic = new RootHeuristic();
        heuristic.setParameterValue("ScorePlayerWeight", weights[0]);
        heuristic.setParameterValue("ScoreOpponentWeight", weights[1]);
        heuristic.setParameterValue("MapPresencePlayerWeight", weights[2]);
        heuristic.setParameterValue("MapPresenceOpponentWeight", weights[3]);
        heuristic.setParameterValue("HandQualityWeight", weights[4]);
        heuristic.setParameterValue("OpponentHandQualityWeight", weights[5]);
        heuristic.setParameterValue("FactionSpecificPlayerWeight", weights[6]);
        heuristic.setParameterValue("FactionSpecificOpponentWeight", weights[7]);
        return heuristic;
    }
    public void updateFitness(RootHeuristic heuristic, double fitnessScore) {
        Generations data = loadEvolutionData();  // Load the existing data
        Generation generation = data.generations.stream()
                .filter(g -> g.generationNumber == currentGenerationNumber)
                .findFirst()
                .orElse(null);

        if (generation != null) {
            for (Individual individual : generation.population) {
                if (individual.fitness == null && Arrays.equals(individual.weights, heuristic.getWeights())) {  // Assuming getWeights method exists in RootHeuristic
                    individual.fitness = fitnessScore;  // Update fitness
                    break;
                }
            }
            saveEvolutionData(data);  // Save the updated data
        }
    }
}

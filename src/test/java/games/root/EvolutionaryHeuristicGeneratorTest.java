package games.root;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import utilities.Json;

import java.io.File;

import static org.junit.Assert.*;

public class EvolutionaryHeuristicGeneratorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void populationAndFitnessSurviveAReload() throws Exception {
        File file = new File(tmp.getRoot(), "evolution.json");
        EvolutionaryHeuristicGenerator first = new EvolutionaryHeuristicGenerator(4,
                EvolutionaryHeuristicGenerator.crossoverTypes.UNIFORM, file.getPath());
        RootHeuristic evaluated = (RootHeuristic) first.getNextHeuristic();
        first.updateFitness(evaluated, 42.0);

        JsonNode saved = Json.mapper().readTree(file);
        JsonNode population = saved.get("generations").get(0).get("population");
        assertEquals(4, population.size());
        assertEquals(42.0, population.get(0).get("fitness").asDouble(), 0.0);
        assertFalse("an unevaluated individual has no fitness yet", population.get(1).has("fitness"));

        EvolutionaryHeuristicGenerator reloaded = new EvolutionaryHeuristicGenerator(4,
                EvolutionaryHeuristicGenerator.crossoverTypes.UNIFORM, file.getPath());
        RootHeuristic next = (RootHeuristic) reloaded.getNextHeuristic();
        assertArrayEquals(Json.mapper().treeToValue(population.get(1).get("weights"), double[].class),
                next.getWeights(), 0.0);
    }
}

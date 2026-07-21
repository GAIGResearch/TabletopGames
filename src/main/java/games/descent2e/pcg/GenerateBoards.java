package games.descent2e.pcg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.crypto.tink.subtle.Random;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameData;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import org.apache.hadoop.shaded.com.nimbusds.jose.shaded.json.JSONObject;
import org.apache.hadoop.shaded.com.nimbusds.jose.shaded.json.JSONStyle;
import utilities.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static core.CoreConstants.nodeHash;
import static games.descent2e.pcg.ControlVariables.*;
import static games.descent2e.pcg.FitnessFunction.*;

public class GenerateBoards {

    static int nowServing = 0;

    public static final List<String> positions = List.of("N-0", "E-0", "S-0", "W-0");
    // How many boards we generate using purely the starting Quests
    public static final int FIRSTLOOP = 4;

    // How many boards we generate using the feasible/infeasible pools
    public static final int GENERATIONLOOP = 0;
    public static final int OFFSPRING = 1;

    public static final int CHOOSE_INFEASIBLE = 3;

    public static final String path = "data/descent2e/";

    public static List<GraphBoard> originalBoards = new ArrayList<>();
    public static List<Quest> originalQuests = new ArrayList<>();

    public static List<GridBoard> tiles = new ArrayList<>();

    public static HashMap<String, HashMap<String, Monster>> monsters;
    public static HashMap<String, HashMap<String, Monster>> lieutenants;

    public static List<Pair<Quest, GraphBoard>> feasible = new ArrayList<>();
    public static List<Pair<Quest, GraphBoard>> infeasible = new ArrayList<>();
    public static List<HashMap<String, Float>> feasibleFitness = new ArrayList<>();
    public static List<HashMap<String, Float>> infeasibleFitness = new ArrayList<>();
    public static List<Boolean> feasibleList = new ArrayList<>();

    // MAP-Elites
    public static HashMap<Pair<Float, Float>, Pair<Integer, Float>> map_SizeVsGroups = new HashMap<>(); // Board Size vs Group Count

    public static void main(String[] args) throws IOException {

        DescentGameData data = new DescentGameData();
        data.load(path);
        originalBoards = data.getBoardConfigurations();
        originalQuests = data.getQuests();
        tiles = data.getTiles();
        monsters = data.getMonsters();
        lieutenants = data.getLieutenants();

        for (Quest q : originalQuests) {
            System.out.println(q.getBoards());
            HashMap<String, Float> scores = FitnessFunction.getFitness(q, Objects.requireNonNull(getBoardByName(q.getBoards().get(0))));
            System.out.println(scores.get("Fitness"));
            //feasibleFitness.add(fitness);
        }

        int originalSize = originalQuests.size();

        for (int i = 0; i < FIRSTLOOP; i++) {
            int x = Random.randInt(originalSize);
            int y = Random.randInt(originalSize);
            while (y == x) {
                y = Random.randInt(originalSize);
            }
            Quest one = originalQuests.get(x);
            Quest two = originalQuests.get(y);

            generateOffspring(one, two);
        }

        for (int i = 0; i < GENERATIONLOOP; i++) {
            System.out.println(FIRSTLOOP + (i * OFFSPRING));
            int choice = Random.randInt(10);

            // Failsafe - force Infeasible Parents
            // if, for whatever reason, we got this far and failed to spawn enough Feasible parents
            if (feasible.size() < 4)
                choice = 0;

            // 30% to pick parents from the Infeasible pool
            Pair<Quest, Quest> parents;
            String isFeasible = "feasible";

            if (choice < CHOOSE_INFEASIBLE) {
                parents = infeasibleParents();
                isFeasible = "infeasible";
            }
            else
                parents = feasibleParents();

            for (int j = 0; j < OFFSPRING; j++) {
                generateOffspring(parents.a, parents.b, isFeasible);
            }
        }

        exportPCGToJSON(true);
        exportPCGToJSON(false);
        exportMAPElitesToJSON();

        System.out.println("Complete!");
    }

    static void exportPCGToJSON(boolean isFeasible) throws IOException {

        String destination = "feasible.json";
        List<Pair<Quest, GraphBoard>> set = feasible;
        List<HashMap<String, Float>> fitness = feasibleFitness;
        if (!isFeasible) {
            destination = "infeasible.json";
            set = infeasible;
            fitness = infeasibleFitness;
        }

        ObjectMapper mapper = new ObjectMapper();
        Path boardOutput = Paths.get("data/descent2e/pcg/pcgboards_" + destination);
        Files.write(boardOutput,"[\n".getBytes());
        Path questOutput = Paths.get("data/descent2e/pcg/pcgquests_" + destination);
        Files.write(questOutput,"[\n".getBytes());
        Path fitnessOutput = Paths.get("data/descent2e/pcg/pcgscores_" + destination);
        Files.write(fitnessOutput,"[\n".getBytes());
        int counter = 0;
        int max = set.size();
        for (Pair<Quest, GraphBoard> pair : set) {
            counter++;
            Quest quest = pair.a;
            GraphBoard board = pair.b;

            StringBuilder outputQ = new StringBuilder("{\"");

            outputQ.append("id\":\"").append(quest.getName()).append("\"");
            outputQ.append(",\"act\":\"").append(quest.getAct()).append("\"");
            outputQ.append(",\"starting-gold\":\"").append(quest.getGold()).append("\"");
            outputQ.append(",\"starting-xp\":\"").append(quest.getStartingXP()).append("\"");
            outputQ.append(",\"traits\": [\"").append(String.join("\", \"", quest.getMonsterTraits())).append("\"]");
            outputQ.append(",\"monsters\": [");
            int monsterMax = quest.getMonsters().size();
            int monsterCounter = 0;
            for (String[] monster : quest.getMonsters()) {
                monsterCounter++;
                outputQ.append("[\"").append(monster[0]).append("\", \"").append(monster[1]).append("\"]");
                if (monsterCounter < monsterMax)
                    outputQ.append(",");
                else
                    outputQ.append("]");
            }

            outputQ.append(",\"tokens\": []");
            outputQ.append(",\"rules\": []");
            outputQ.append(",\"game-over\": [{" +
                    "\"id\": \"CountGameOver\"," +
                        "\"count\": {" +
                        "\"type\": \"NFiguresAlive\"," +
                        "\"figureNameContains\": \"Hero\"}," +
                    "\"target\": 0," +
                    "\"comparison-type\": \"Equal\"," +
                    "\"result-heroes\": \"LOSE_GAME\"," +
                    "\"result-overlord\": \"WIN_GAME\"" +
                    "},{" +
                    "\"id\": \"CountGameOver\"," +
                        "\"count\": {" +
                        "\"type\": \"NFiguresAlive\"," +
                        "\"figureNameContains\": \"Monster\"}," +
                    "\"target\": 0," +
                    "\"comparison-type\": \"Equal\"," +
                    "\"result-heroes\": \"WIN_GAME\"," +
                    "\"result-overlord\": \"LOSE_GAME\"" +
                    "}]");

            outputQ.append(",\"common-rewards\": [{" +
                    "\"rewardType\": \"Attribute\"," +
                    "\"attribute\": \"XP\"," +
                    "\"value\": 1.0" +
                    "}]");
            outputQ.append(",\"overlord-rewards\": [{" +
                    "\"rewardType\": \"Attribute\"," +
                    "\"attribute\": \"XP\"," +
                    "\"value\": 1.0," +
                    "\"mustWinToReceive\": true" +
                    "}]");
            outputQ.append(",\"boards\": [ \"").append(board.getComponentName()).append("\"]");
            outputQ.append(",\"starting-tile\": \"").append(quest.getStartingTile()).append("\"");

            outputQ.append("}");

            // --- BOARD ---

            StringBuilder outputB = new StringBuilder("{");

            outputB.append("\"type\": \"graph\",");
            outputB.append("\"verticesKey\": \"name\",");
            outputB.append("\"neighboursKey\": \"neighbours\",");
            outputB.append("\"maxNeighbours\": -1,");
            outputB.append("\"id\": \"").append(board.getComponentName()).append("\",");
            outputB.append("\"nodes\": [");
            int nodeCount = 0;
            for (BoardNode node : board.getBoardNodes()) {
                nodeCount++;

                String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();

                outputB.append("{ \"name\": [\"String\", \"").append(node.getComponentName()).append("\"],");
                outputB.append("\"orientation\": [\"Integer\", ").append(node.getProperty("orientation")).append("],");
                outputB.append("\"neighbours\": [\"String[]\", [");

                int neighbourCount = 0;
                for (String neighbour : neighbours) {
                    neighbourCount++;
                    outputB.append("\"").append(neighbour).append("\"");
                    if (neighbourCount < neighbours.length)
                        outputB.append(", ");
                    else
                        outputB.append("]],");
                }
                outputB.append("\"connections\": [\"String[]\", [");
                neighbourCount = 0;
                for (String connect : connections) {
                    neighbourCount++;
                    outputB.append("\"").append(connect).append("\"");
                    if (neighbourCount < connections.length)
                        outputB.append(", ");
                    else
                        outputB.append("]]}");
                }
                if (nodeCount < board.getBoardNodes().size())
                    outputB.append(",");
                else
                    outputB.append("]}");
            }




            // -- FINAL OUTPUTS ---

            Object q = mapper.readValue(outputQ.toString(), Object.class);
            String prettyQ = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(q);
            Object b = mapper.readValue(outputB.toString(), Object.class);
            String prettyB = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(b);


            if (counter < max) {
                prettyQ += ",";
                prettyB += ",";
            }
            else {
                prettyQ  += "\n]";
                prettyB += "\n]";
            }
            Files.writeString(questOutput,prettyQ + System.lineSeparator(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.writeString(boardOutput,prettyB + System.lineSeparator(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        // --- SCORES ---

        counter = 0;
        max = fitness.size();
        for (HashMap<String, Float> f : fitness) {
            counter++;
            StringBuilder outputS = new StringBuilder("{");

            outputS.append("\"ID\": ").append(f.get("ID").intValue());
            outputS.append(",\"Feasible\": ").append(isFeasible);
            outputS.append(",\"Fitness\": ").append(f.get("Fitness"));
            outputS.append(",\"Connectedness\": ").append(f.get("Connectedness").intValue());
            outputS.append(",\"Free Edges\": ").append(f.get("Free Edges").intValue());
            outputS.append(",\"Size\": ").append(f.get("Size").intValue());
            outputS.append(",\"Tile Count\": ").append(f.get("Tile Count").intValue());
            outputS.append(",\"Consistency\": ").append(f.get("Consistency"));
            outputS.append(",\"Geometry\": ").append(f.get("Geometry") > 0.0f);
            outputS.append(",\"Groups\": ").append(f.get("Groups").intValue());
            outputS.append(",\"Monster Count\": ").append(f.get("Monster Count"));
            outputS.append(",\"No Repeats\": ").append(f.get("Repeats") > 0.0f);
            outputS.append(",\"Total Health\": ").append(f.get("Total Health"));
            outputS.append(",\"Average Health\": ").append(f.get("Health"));
            outputS.append(",\"Legal Spawning\": ").append(f.get("Spawning") > 0.0f);
            outputS.append(",\"Complexity\": ").append(f.get("Complexity"));
            outputS.append(",\"Rules\": ").append(f.get("Rules"));
            outputS.append(",\"Act\": ").append(f.get("Act").intValue());
            outputS.append(",\"XP\": ").append(f.get("XP").intValue());
            outputS.append(",\"Gold\": ").append(f.get("Gold").intValue());
            outputS.append("}");

            Object s = mapper.readValue(outputS.toString(), Object.class);
            String prettyS = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s);

            if (counter < max)
                prettyS += ",";
            else
                prettyS += "\n]";

            Files.writeString(fitnessOutput,prettyS + System.lineSeparator(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    static void exportMAPElitesToJSON() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Path mapEliteOutput = Paths.get("data/descent2e/pcg/mapelites_size&groups.json");
        Files.write(mapEliteOutput,"[\n".getBytes());

        int counter = 0;
        int max = map_SizeVsGroups.size();
        for (Pair<Float, Float> key : map_SizeVsGroups.keySet()) {
            counter++;
            Pair<Integer, Float> result = map_SizeVsGroups.get(key);
            String output = "{\"";
            output += "size\":\"" + key.a + "\"";
            output += ",\"groups\":\"" + key.b + "\"";
            output += ",\"id\":\"" + result.a + "\"";
            output += ",\"fitness\":\"" + result.b + "\"}";
            Object o = mapper.readValue(output, Object.class);
            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            if (counter < max)
                pretty += ",";
            else
                pretty  += "]";
            Files.writeString(mapEliteOutput,pretty + System.lineSeparator(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    static Pair<Quest, Quest> feasibleParents() {
        int w, x, y, z;
        w = x = y = z = 0;
        boolean allDifferent = false;
        int n = feasible.size();

        while (!allDifferent) {
            w = Random.randInt(n);
            x = Random.randInt(n);
            y = Random.randInt(n);
            z = Random.randInt(n);
            allDifferent = w != x && w != y && w != z && x != y && x != z && y != z;
        }

        HashMap<String, Float> one = feasibleFitness.get(w);
        HashMap<String, Float> two = feasibleFitness.get(x);
        HashMap<String, Float> three = feasibleFitness.get(y);
        HashMap<String, Float> four = feasibleFitness.get(z);

        int size = one.size();

        java.util.Random r = new java.util.Random();
        float mean = 0;
        float variance = 0.2f;
        double noise = r.nextGaussian() * Math.sqrt(variance) + mean;
        double scoreOne = one.get("Fitness") + noise;
        Pair<Integer, Double> p1 = new Pair<>(w, scoreOne);

        noise = r.nextGaussian() * Math.sqrt(variance) + mean;
        double scoreTwo = two.get("Fitness") + noise;
        Pair<Integer, Double> p2 = new Pair<>(x, scoreTwo);

        noise = r.nextGaussian() * Math.sqrt(variance) + mean;
        double scoreThree = three.get("Fitness") + noise;
        Pair<Integer, Double> p3 = new Pair<>(y, scoreThree);

        noise = r.nextGaussian() * Math.sqrt(variance) + mean;
        double scoreFour = four.get("Fitness") + noise;
        Pair<Integer, Double> p4 = new Pair<>(z, scoreFour);

        List<Pair<Integer, Double>> results = new ArrayList<>();
        results.add(p1);
        if (p2.b > results.get(0).b)
            results.add(0, p2);
        else
            results.add(p2);

        if (p3.b > results.get(0).b)
            results.add(0, p3);
        else if (p3.b > results.get(1).b)
            results.add(1, p3);
        else
            results.add(p3);

        if (p4.b > results.get(0).b)
            results.add(0, p4);
        else if (p4.b > results.get(1).b)
            results.add(1, p4);
        else if (p4.b > results.get(2).b)
            results.add(2, p4);
        else
            results.add(p4);

        Quest first = feasible.get(results.get(0).a).a;
        Quest second = feasible.get(results.get(1).a).a;

        return new Pair<>(first, second);
    }

    static Pair<Quest, Quest> infeasibleParents() {
        List<Pair<Integer, Double>> results = new ArrayList<>();
        float decay = 1.5f;
        float sigma = 5f;
        for (int i = 0; i < infeasibleFitness.size(); i++) {
            double calc = getCalc(i, decay, sigma);

            Pair<Integer, Double> r = new Pair<>(i, calc);

            // Sort from highest to lowest
            if (results.isEmpty())
                results.add(r);
            else {
                boolean placed = false;
                for (int j = 0; j < results.size(); j++) {
                    if (calc >= results.get(j).b) {
                        results.add(j, r);
                        placed = true;
                        break;
                    }
                }
                if (!placed)
                    results.add(r);
            }
        }

        int n = results.size();
        double[] probability = new double[n];
        double total = 0.0;

        // Now, decay rank probabilities exponentially
        for (int i = 0; i < n; i++) {
            probability[i] = Math.exp(-(i+1));
            total += probability[i];
        }

        // Then normalise
        for (int i = 0; i < n; i++)
            probability[i] /= total;

        double roll = (double) mutate() / 100;
        double increment = 0.0;

        // And do a weighted random selection
        for (int i = 0; i < n-1; i++) {
            increment += probability[i];
            if (roll <= increment) {
                Quest one = infeasible.get(results.get(i).a).a;
                Quest two = infeasible.get(results.get(i+1).a).a;

                return new Pair<>(one, two);
            }
        }

        // Failsafe, return the last two in the pool
        Quest one = infeasible.get(results.get(n-2).a).a;
        Quest two = infeasible.get(results.get(n-1).a).a;
        return new Pair<>(one, two);
    }

    private static double getCalc(int i, float decay, float sigma) {
        HashMap<String, Float> score = infeasibleFitness.get(i);
        // Connectedness
        double connect = Math.pow(decay, 1 - score.get("Connectedness"));

        double geometry = score.get("Geometry");

        double repeats = score.get("Repeats");

        double spawning = score.get("Spawning");

        double consistency = Math.pow(decay, 1 - score.get("Consistency"));

        double size = Math.exp(-(Math.pow(score.get("Size") - IDEAL_SIZE, 2)) / Math.pow(2 * sigma, 2));

        double groups = Math.exp(-(Math.pow(score.get("Groups") - IDEAL_GROUP, 2)) / Math.pow(2 * sigma, 2));

        double health = Math.exp(-(Math.pow(score.get("Health") - IDEAL_HEALTH, 2)) / Math.pow(2 * sigma, 2));

        double complexity = Math.pow(decay, 1 - score.get("Complexity"));

        double rules = Math.pow(decay, 1 - score.get("Rules"));

        return Math.sqrt(connect + geometry + repeats + spawning + consistency + size + groups + health + complexity + rules);
    }

    static void generateOffspring(Quest one, Quest two) {
        nowServing++;
        System.out.println("Generating Offspring " + nowServing);

        Pair<Pair<Quest, GraphBoard>, Boolean> offspring = createOffspring(one, two, "null");

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);
    }

    static void generateOffspring(Quest one, Quest two, String isFeasible) {
        nowServing++;
        System.out.println("Generating Offspring " + nowServing);

        Pair<Pair<Quest, GraphBoard>, Boolean> offspring = createOffspring(one, two, isFeasible);

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);
    }

    static GridBoard getTileByName(String name) {
        if (name.contains("-"))
            name = name.split("-")[0];
        for (GridBoard tile : tiles) {
            if (tile.getComponentName().equals(name))
                return tile;
        }
        return null;
    }

    static int mutate() {
        return Random.randInt(100);
    }

    static Pair<Pair<Quest, GraphBoard>, Boolean> createOffspring(Quest parent1, Quest parent2, String type) {
        Quest newQuest;
        GraphBoard newBoard;
        Quest otherParent;
        GraphBoard otherBoard;

        // Deciding which Parent is the Base Quest
        int choice = Random.randInt(2);
        if (choice == 0) {
            newQuest = parent1.copy();
            otherParent = parent2.copy();
        }
        else {
            newQuest = parent2.copy();
            otherParent = parent1.copy();
        }

        // Now, decide which Parent is the Base Board
        choice = Random.randInt(2);
        if (choice == 0) {
            otherBoard = switch (type) {
                case "feasible" -> {
                    newBoard = Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0), true)).copy();
                    yield Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0), true)).copy();
                }
                case "infeasible" -> {
                    newBoard = Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0), false)).copy();
                    yield Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0), false)).copy();
                }
                default -> {
                    newBoard = Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0))).copy();
                    yield Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0))).copy();
                }
            };
        }
        else {
            otherBoard = switch (type) {
                case "feasible" -> {
                    newBoard = Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0), true)).copy();
                    yield Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0), true)).copy();
                }
                case "infeasible" -> {
                    newBoard = Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0), false)).copy();
                    yield Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0), false)).copy();
                }
                default -> {
                    newBoard = Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0))).copy();
                    yield Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0))).copy();
                }
            };
        }

        // -- BOARD MUTATIONS ---
        List<BoardNode> finalNodes = new ArrayList<>();
        boolean freeNodes = true;

        List<BoardNode> newNodes = newBoard.getComponents();
        List<BoardNode> oldNodes = otherBoard.getComponents();

        int attempt = 0;
        while (freeNodes) {
            attempt++;
            System.out.println("Attempt: " + attempt);
            freeNodes = false;

            finalNodes.clear();
            for (BoardNode node : newNodes)
                finalNodes.add(node.copy());

            // Construct Board Assembly with all the new tiles
            crossoverMutate(oldNodes, finalNodes);
            deletionMutate(finalNodes);
            rotateMutate(finalNodes);

            assembleBoard(finalNodes);

            for (BoardNode node : finalNodes) {
                String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();

                for (String neighbour : neighbours) {
                    if (neighbour.equals("null")) {
                        //freeNodes = true;
                        break;
                    }
                }

                if (freeNodes) break;

                for (String connection : connections) {
                    if (!positions.contains(connection)) {
                        //freeNodes = true;
                        break;
                    }
                }

                if (freeNodes) break;

                int expected = ((PropertyInt) Objects.requireNonNull(getTileByName(node.getComponentName())).getProperty(nodeHash)).value;

                if (expected != neighbours.length || expected != connections.length) {
                    //freeNodes = true;
                    break;
                }
            }
        }
        newBoard.setBoardNodes(finalNodes);

        // --- MONSTER MUTATIONS ---

        List<String[]> monsters = newQuest.getMonsters();
        List<String> monsterList = new ArrayList<>();
        for (String[] m : monsters)
            monsterList.add(m[0]);

        // 10% crossover chance
        List<String[]> parentMonsters = new ArrayList<>(otherParent.getMonsters());
        for (String[] m : parentMonsters) {
            if (Random.randInt(10) < 1) {
                // Make sure we don't add duplicate Monsters
                if (!monsterList.contains(m[0]) || m[0].contains("Open"))
                    monsters.add(m);
            }
        }

        // 10% deletion chance
        List<String[]> finalMonsters = new ArrayList<>(monsters);
        for (String[] m : monsters) {
            if (Random.randInt(10) < 1) {
                finalMonsters.remove(m);
            }
        }

        // Force a mutation if the group sizes are now too big or too small
        boolean forceMutate = (finalMonsters.size() < ControlVariables.GROUP_MIN) || (finalMonsters.size() > ControlVariables.GROUP_MAX);

        if (!forceMutate)
            if (mutate() < ControlVariables.MONSTER_MUTATE)
                forceMutate = true;

        while (forceMutate) {
            finalMonsters = mutateMonsters(finalMonsters);
            forceMutate = (finalMonsters.size() < ControlVariables.GROUP_MIN) || (finalMonsters.size() > ControlVariables.GROUP_MAX);
        }

        mutateAct(newQuest);
        mutateXP(newQuest);
        mutateTraits(newQuest);

        mutatePositions(finalNodes, newQuest, finalMonsters);

        newQuest.setMonsters(finalMonsters);



        String newBoardName = "pcg-" + nowServing;
        newBoard.setComponentName(newBoardName);
        newQuest.setName("PCG-" + nowServing);
        List<String> boards = newQuest.getBoards();
        boards.clear();
        boards.add(newBoardName);


        HashMap<String, Float> scores = FitnessFunction.getFitness(newQuest, newBoard);

        boolean feasible = scores.get("Feasible") > 0f;

        if (feasible)
            feasibleFitness.add(scores);
        else
            infeasibleFitness.add(scores);
        feasibleList.add(feasible);

        Pair<Float, Float> mapKey = new Pair<>(scores.get("Size"), scores.get("Groups"));
        Pair<Integer, Float> mapResult = new Pair<>(nowServing, scores.get("Fitness"));
        if (map_SizeVsGroups.containsKey(mapKey)) {
            Pair<Integer, Float> oldResult = map_SizeVsGroups.get(mapKey);
            if (oldResult.b < mapResult.b)
                map_SizeVsGroups.put(mapKey, mapResult);
        }
        else
            map_SizeVsGroups.put(mapKey, mapResult);

        Pair<Quest, GraphBoard> offspring = new Pair<>(newQuest, newBoard);

        return new Pair<>(offspring, feasible);
    }

    static boolean checkFeasible(HashMap<String, Float> scores) {

        // Connectedness Check
        if (scores.get("Connectedness") < 1f) {
            System.out.println("Connectedness Failure");
            return false;
        }

        // Free Edge Failure
        if (scores.get("Free Edges") > 0f) {
            System.out.println("Free Edge Failure");
            return false;
        }

        // Geometry Check
        if (scores.get("Geometry") < 1f) {
            System.out.println("Geometry Failure");
            return false;
        }

        // No Repeating Monsters Check
        if (scores.get("Spawning") < 1f) {
            System.out.println("Repeating Groups Failure");
            return false;
        }

        // Consistency Check
        if (scores.get("Consistency") < 1f) {
            System.out.println("Consistency Failure");
            return false;
        }

        // Board Size Check
        float size = scores.get("Size");
        if (size > ControlVariables.SIZE_MAX || size < ControlVariables.SIZE_MIN) {
            System.out.println("Size Failure");
            return false;
        }
        // Monster Group Check
        float groups = scores.get("Groups");
        if (groups > ControlVariables.GROUP_MAX || groups < ControlVariables.GROUP_MIN) {
            System.out.println("Group Count Failure");
            return false;
        }

        return true;
    }

    static String getRandomMonster(List<String> oldMonsters) {
        String newMonster = "null";
        String monType = "";
        String otherType = "";
        int type = mutate();
        List<String> toAdd = new ArrayList<>();
        List<String> other = new ArrayList<>();

        if (type < ControlVariables.SMALLOPENGROUP)
            return "OpenSmall:group";
        if (type < ControlVariables.OPENGROUP)
            return "Open:group";

        if (type >= (100 - ControlVariables.LIEUTENANT)) {
            toAdd.addAll(lieutenants.keySet());
            monType = ":lieutenant";
            other.addAll(monsters.keySet());
            otherType = ":group";
        }
        else {
            toAdd.addAll(monsters.keySet());
            monType = ":group";
            other.addAll(lieutenants.keySet());
            otherType = ":lieutenant";
        }

        Collections.shuffle(toAdd);
        boolean done = false;
        for (String m : toAdd) {
            if (!oldMonsters.contains(m+monType)) {
                newMonster = m;
                done = true;
                break;
            }
        }
        if (done)
            return newMonster + monType;
        for (String m : other) {
            if (!oldMonsters.contains(m + otherType)) {
                return m + otherType;
            }
        }
        return "Open:group";
    }

    static List<String[]> mutateMonsters(List<String[]> monsters) {
        List<String[]> newMonsters = new ArrayList<>(monsters);
        int mutate = mutate();

        int size = monsters.size();

        // Force a specific mutation if for whatever reason we're already over or under the limits
        boolean add = size < ControlVariables.GROUP_MIN;
        boolean remove = size > ControlVariables.GROUP_MAX;

        if (!add)
            if (size < ControlVariables.GROUP_MAX)
                if (mutate < ControlVariables.ADD_GROUP)
                    add = true;

        if (!remove)
            if (size > ControlVariables.GROUP_MIN)
                if (mutate >= (100 - ControlVariables.REMOVE_GROUP))
                    remove = true;

        // Add a new Monster to the groups
        if (add) {
            List<String> currentMonsters = new ArrayList<>();
            for (String[] m : monsters) {
                currentMonsters.add(m[0]);
            }
            String[] newMonster = new  String[2];
            newMonster[0] = getRandomMonster(currentMonsters);
            newMonster[1] = "null";
            newMonsters.add(newMonster);
        }
        // Remove a Monster from the groups
        else if (remove) {
            newMonsters.remove(Random.randInt(newMonsters.size()));
        }
        // Replace a Monster from the groups
        else {
            List<String> currentMonsters = new ArrayList<>();
            for (String[] m : monsters) {
                currentMonsters.add(m[0]);
            }
            String newMonster = getRandomMonster(currentMonsters);
            (newMonsters.get(Random.randInt(newMonsters.size())))[0] = newMonster;
        }

        return newMonsters;
    }

    static void mutatePositions(List<BoardNode> nodes, Quest quest, List<String[]> monsters) {
        List<String> tiles = new ArrayList<>();
        List<String> taken = new ArrayList<>();
        for (BoardNode node : nodes) {
            tiles.add(node.getComponentName());
        }
        Collections.shuffle(tiles);
        String heroStart = quest.getStartingTile();

        // First, check if the Heroes' original starting tile still exists or not
        // Then, roll Mutation chance (10%)
        boolean forceHeroMutate = !tiles.contains(heroStart) || ControlVariables.illegalHeroSpawns.contains(heroStart);
        if (!forceHeroMutate)
            forceHeroMutate = Random.randInt(10) < 1;

        if (forceHeroMutate) {
            heroStart = "null";
            for (String tile : tiles) {
                if (taken.contains(tile)) continue;
                boolean legal = true;
                for (String illegal : ControlVariables.illegalHeroSpawns) {
                    if (tile.contains(illegal)) {
                        legal = false;
                        break;
                    }
                }
                if (legal) {
                    heroStart = tile;
                    break;
                }
            }
        }

        // Save the new Heroes start
        taken.add(heroStart);
        quest.setStartingTile(heroStart);

        // Now repeat for every Monster
        for (String[] monster : monsters) {
            String name = monster[0];
            String monsterPosition = monster[1];
            boolean forceMonsterMutate = !tiles.contains(monsterPosition) || taken.contains(monsterPosition);

            // Non-Lieutenant Monsters have additional restrictions
            List<String> traits = quest.getMonsterTraits();
            boolean lieutenant = true;
            boolean barghestOpen = traits.contains("Dark") || traits.contains("Wilderness") || traits.contains("All");
            boolean dragonOpen = traits.contains("Dark") || traits.contains("Cave") || traits.contains("All");
            boolean dragon = name.contains("Open") && !name.contains("OpenSmall") && dragonOpen;
            boolean barghest = name.contains("Open") && barghestOpen;

            if (!name.contains("lieutenant")) {
                lieutenant = false;
                if (!forceMonsterMutate) {
                    for (String tile : illegalMonsterSpawns)
                        if (monsterPosition.contains(tile)) {
                            forceMonsterMutate = true;
                            break;
                        }
                }
                if (name.contains("Barghest") || barghest) {
                    barghest = true;
                    if (!forceMonsterMutate) {
                        for (String tile : illegalBarghestSpawns)
                            if (monsterPosition.contains(tile)) {
                                forceMonsterMutate = true;
                                break;
                            }
                    }
                }
                if (name.contains("Dragon") || dragon) {
                    barghest = true;
                    dragon = true;
                    if (!forceMonsterMutate) {
                        for (String tile : illegalBarghestSpawns)
                            if (monsterPosition.contains(tile)) {
                                forceMonsterMutate = true;
                                break;
                            }
                    }
                    if (!forceMonsterMutate) {
                        for (String tile : illegalDragonSpawns)
                            if (monsterPosition.contains(tile)) {
                                forceMonsterMutate = true;
                                break;
                            }
                    }
                }
            }
            // Otherwise, mutation chance
            if (!forceMonsterMutate)
                forceMonsterMutate = Random.randInt(10) < 1;

            if (forceMonsterMutate) {
                monsterPosition = "null";
                List<String> illegals = new ArrayList<>(ControlVariables.illegalMonsterSpawns);
                if (barghest)
                    illegals.addAll(ControlVariables.illegalBarghestSpawns);
                if (dragon)
                    illegals.addAll(illegalDragonSpawns);

                for (String tile : tiles) {
                    if (taken.contains(tile)) continue;

                    if (lieutenant) {
                        monsterPosition = tile;
                        break;
                    }

                    boolean legal = true;
                    for (String illegal : illegals) {
                        if (tile.contains(illegal)) {
                            legal = false;
                            break;
                        }
                    }
                    if (legal) {
                        monsterPosition = tile;
                        break;
                    }
                }
            }
            taken.add(monsterPosition);
            monster[1] = monsterPosition;
        }
    }

    static void mutateAct(Quest quest) {
        int mutate = mutate();
        if (mutate < ControlVariables.ACT_MUTATE) {
            int newAct = (quest.getAct() % 2) + 1;
            if (newAct > 1) {
                quest.setStartingXP(quest.getStartingXP() + 5);
                quest.setGold(quest.getGold() + 125);
            }
            else {
                quest.setStartingXP(quest.getStartingXP() - 5);
                quest.setGold(quest.getGold() - 125);
            }
            quest.setAct(newAct);
        }
    }

    static void mutateXP(Quest quest) {
        int mutate = mutate();
        if (mutate < ControlVariables.XP_MUTATE) {
            int newXP = Random.randInt(5) + 5 * (quest.getAct() - 1);
            int newGold = newXP * 25;
            if (newXP > 1) {
                for (int i = 0; i < Random.randInt(newXP); i++) {
                    newGold += (Random.randInt(5) * 25);
                }
            }

            quest.setStartingXP(newXP);
            quest.setGold(newGold);
        }
    }

    static void mutateTraits(Quest quest) {
        List<String> oldTraits = quest.getMonsterTraits();
        boolean add = oldTraits.size() < ControlVariables.TRAITS_MIN;
        boolean remove = oldTraits.size() > ControlVariables.TRAITS_MAX;
        boolean replace = false;

        int mutate = mutate();

        if (!add)
            if (oldTraits.size() < ControlVariables.TRAITS_MAX)
                if (mutate < ControlVariables.ADD_TRAITS)
                    add = true;
        if (!remove)
            if (oldTraits.size() > ControlVariables.TRAITS_MIN)
                if (mutate < (ControlVariables.ADD_TRAITS + ControlVariables.REMOVE_TRAITS))
                    remove = true;
        if (mutate < (ControlVariables.ADD_TRAITS + ControlVariables.REMOVE_TRAITS + ControlVariables.REPLACE_TRAITS))
            replace = true;

        // Remove, in case of 'All' trait
        if (oldTraits.size() < ControlVariables.TRAITS_MIN)
            oldTraits = new ArrayList<>();

        // Add
        if (add) {
            List<String> traits = new ArrayList<>(ControlVariables.TRAITS);
            Collections.shuffle(traits);
            for (String t : traits) {
                if (!oldTraits.contains(t))
                    oldTraits.add(t);
                if (oldTraits.size() >= ControlVariables.TRAITS_MIN)
                    break;
            }
        }
        // Remove
        else if (remove) {
            oldTraits.remove(Random.randInt(oldTraits.size()));
        }
        // Replace
        else if (replace) {
            int r = Random.randInt(oldTraits.size());
            List<String> traits = new ArrayList<>(ControlVariables.TRAITS);
            Collections.shuffle(traits);
            for (String t : traits) {
                if (!oldTraits.contains(t)) {
                    oldTraits.remove(r);
                    oldTraits.add(t);
                    break;
                }
            }
        }
        // Set Traits to 'All'
        else {
            oldTraits = new ArrayList<>();
            oldTraits.add("All");
        }
        quest.setMonsterTraits(oldTraits);
    }

    static GraphBoard getBoardByName(String name) {
        for (GraphBoard board : originalBoards) {
            if (board.getComponentName().equals(name))
                    return board;
        }
        return null;
    }

    static GraphBoard getBoardByName(String name, boolean isFeasible) {
        if (isFeasible) {
            for (Pair<Quest, GraphBoard> quest : feasible) {
                GraphBoard board = quest.b;
                if (board.getComponentName().equals(name))
                    return board;
            }
        }
        else {
            for (Pair<Quest, GraphBoard> quest : infeasible) {
                GraphBoard board = quest.b;
                if (board.getComponentName().equals(name))
                    return board;
            }
        }
        return null;
    }

    static void crossoverMutate(List<BoardNode> crossoverNodes, List<BoardNode> baseNodes) {

        List<String> tiles = new ArrayList<>();

        int transition = 0;
        int endcap = 0;
        int extender = 0;
        for (BoardNode node : baseNodes) {
            String name = node.getComponentName();

            if (name.contains("transition")) {
                transition++;
                name = name.split("-")[0] + "-" + transition;
                node.setComponentName(name);
            }
            else if (name.contains("endcap")) {
                endcap++;
                name = name.split("-")[0] + "-" + endcap;
                node.setComponentName(name);
            }
            else if (name.contains("extender")) {
                extender++;
                name = name.split("-")[0] + "-" + extender;
                node.setComponentName(name);
            }

            tiles.add(node.getComponentName());
        }

        // 10% crossover chance
        for (BoardNode node : crossoverNodes) {
            if (Random.randInt(10) < 1) {
                String name = node.getComponentName();

                if (name.contains("transition")) {
                    if (transition < 2) {
                        transition++;
                        name = name.split("-")[0] + "-" + transition;
                    }
                }
                else if (name.contains("endcap")) {
                    if (endcap < 5) {
                        endcap++;
                        name = name.split("-")[0] + "-" + endcap;
                    }
                }
                else if (name.contains("extender")) {
                    if (extender < 9) {
                        extender++;
                        name = name.split("-")[0] + "-" + extender;
                    }
                }

                // Make sure we don't add duplicate Tiles
                if (!tiles.contains(name)) {
                    tiles.add(name);
                    BoardNode newNode = node.copy();
                    newNode.setComponentName(name);
                    baseNodes.add(newNode);
                }
            }
        }
    }

    static void deletionMutate(List<BoardNode> newNodes) {
        // 10% deletion chance
        List<BoardNode> finalNodes = new ArrayList<>(newNodes);
        for (BoardNode node : newNodes) {
            if (Random.randInt(10) < 1) {
                finalNodes.remove(node);
                for (BoardNode n : finalNodes) {
                    String[] neighbours = ((PropertyStringArray) n.getProperty("neighbours")).getValues();
                    for (int i = 0; i < neighbours.length; i++) {
                        if (neighbours[i].equals(node.getComponentName())) {
                            neighbours[i] = "null";
                            break;
                        }
                    }
                }
            }
        }
    }

    static void rotateMutate (List<BoardNode> nodes) {
        // 10% rotation chance
        for (BoardNode node : nodes) {
            if (Random.randInt(10) < 1) {
                int rotation = Random.randInt(positions.size() - 1) + 1;
                int oldRotate = ((PropertyInt) node.getProperty("orientation")).value;
                node.setProperty(new PropertyInt("orientation", (oldRotate + rotation) % 4));
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                for (int i = 0; i < connections.length; i++) {
                    if (positions.contains(connections[i])) {
                        int index = positions.indexOf(connections[i]);
                        connections[i] = positions.get((index + rotation) % 4);
                    }
                }
            }
        }
    }

    static void assembleBoard(List<BoardNode> nodes) {
        // Randomly assemble the new board
        Map<String, String> pairings = new HashMap<>();
        pairings.put("N-0", "S-0");
        pairings.put("S-0", "N-0");
        pairings.put("E-0", "W-0");
        pairings.put("W-0", "E-0");

        Map<String, Map<String, List<Pair<BoardNode, String>>>> possible = new HashMap<>();

        // First, cleanse the board of any possible connections
        for (BoardNode node : nodes) {
            String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
            Arrays.fill(neighbours, "null");

            Map<String, List<Pair<BoardNode, String>>> connections = new HashMap<>();

            for (String c : ((PropertyStringArray) node.getProperty("connections")).getValues())
                connections.put(c, new ArrayList<>());
            possible.put(node.getComponentName(), connections);
        }
        // Then, go through all possible connections for each node
        for (BoardNode n1 : nodes) {
            for (BoardNode n2 : nodes) {
                if (n1.equals(n2))
                    continue;

                for (String connection : ((PropertyStringArray) n1.getProperty("connections")).getValues()) {
                    String opposite = pairings.get(connection);

                    String[] n2Connects = ((PropertyStringArray) n2.getProperty("connections")).getValues();
                    if (Arrays.asList(n2Connects).contains(opposite)) {
                        Map<String, List<Pair<BoardNode, String>>> link = possible.get(n1.getComponentName());
                        List<Pair<BoardNode, String>> list = link.get(connection);
                        list.add(new Pair<>(n2, opposite));
                    }

                }

            }
        }

        // And get all the unique combinations
        List<String[]> unique = new ArrayList<>();
        Set<String> checked = new HashSet<>();
        for (String tile : possible.keySet()) {
            for (String connection : possible.get(tile).keySet()) {
                String one = tile + ":" + connection;
                for (Pair<BoardNode, String> piece : possible.get(tile).get(connection)) {
                    String two = piece.a.getComponentName() + ":" + piece.b;

                    List<String> pair = Arrays.asList(one, two);
                    Collections.sort(pair);
                    String key = pair.get(0) + "," + pair.get(1);
                    if (checked.add(key)) {
                        unique.add(new String[]{pair.get(0), pair.get(1)});
                    }
                }
            }
        }

        Collections.shuffle(unique);

        Set<String> usedConnects = new HashSet<>();
        Set<String> usedPairs = new HashSet<>();
        List<String[]> finalSet = new ArrayList<>();

        for (String[] pair : unique) {
            String a = pair[0];
            String tileA = a.split(":")[0];
            String b = pair[1];
            String tileB = b.split(":")[0];

            List<String> newPair = Arrays.asList(tileA, tileB);
            Collections.sort(newPair);
            String key = newPair.get(0) + "," + newPair.get(1);

            if (!usedConnects.contains(a) && !usedConnects.contains(b) && !usedPairs.contains(key)) {
                finalSet.add(pair);
                usedConnects.add(a);
                usedConnects.add(b);
                usedPairs.add(key);

            }
        }

        // Lastly, update all the tiles with the new connections
        for (String[] pair : finalSet) {
            String[] first = pair[0].split(":");
            String[] second = pair[1].split(":");

            for (BoardNode node : nodes) {
                if (node.getComponentName().equals(first[0])) {
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    for (int i = 0; i < connections.length; i++) {
                        if (connections[i].equals(first[1])) {
                            String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
                            neighbours[i] = second[0];
                            for (BoardNode n2 : nodes) {
                                if (n2.getComponentName().equals(second[0])) {
                                    node.addNeighbourWithCost(n2);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                if (node.getComponentName().equals(second[0])) {
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    for (int i = 0; i < connections.length; i++) {
                        if (connections[i].equals(second[1])) {
                            String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
                            neighbours[i] = first[0];
                            for (BoardNode n2 : nodes) {
                                if (n2.getComponentName().equals(first[0])) {
                                    node.addNeighbourWithCost(n2);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}

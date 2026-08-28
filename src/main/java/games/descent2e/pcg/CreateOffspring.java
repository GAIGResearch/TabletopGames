package games.descent2e.pcg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.crypto.tink.subtle.Random;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.*;
import games.descent2e.concepts.Quest;
import utilities.Pair;
import utilities.Vector2D;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static core.CoreConstants.*;
import static games.descent2e.DescentConstants.connectionHash;
import static games.descent2e.pcg.ControlVariables.*;
import static games.descent2e.pcg.ControlVariables.illegalDragonSpawns;
import static games.descent2e.pcg.GenerateBoards.*;

public class CreateOffspring {

    int nowServing = 0; // Offspring ID
    boolean nowGenerating = false;

    // How many boards we generate using purely the starting Quests
    private int FIRSTLOOP = 500;

    // How many boards we generate using the feasible/infeasible pools
    private int GENERATIONLOOP = 200;
    private int OFFSPRING = 10;
    private int CHOOSE_INFEASIBLE = 30;

    public HashMap<Integer, GridBoard> boards = new HashMap<>();
    public HashMap<Integer, Map<Integer, GridBoard>> boardTiles = new HashMap<>();
    public HashMap<Integer, int[][]> tileRefs = new HashMap<>();
    public HashMap<Integer, Map<String, Map<Vector2D, Vector2D>>> gridRefs = new HashMap<>();

    public List<Pair<Quest, GraphBoard>> feasible = new ArrayList<>();
    public List<Pair<Quest, GraphBoard>> infeasible = new ArrayList<>();
    public List<HashMap<String, Float>> feasibleFitness = new ArrayList<>();
    public List<HashMap<String, Float>> infeasibleFitness = new ArrayList<>();
    public List<Boolean> feasibleList = new ArrayList<>();

    public Pair<Quest, GraphBoard> bestQuest = null;
    public float bestFitness = 0f;
    public int bestID = 0;

    // MAP-Elites - Saved as <<Value, Value> , <Map ID, Fitness Score>>
    public HashMap<Pair<Float, Float>, Pair<Integer, Float>> map_SizeVsGroups = new HashMap<>(); // Board Size vs Group Count
    public HashMap<Pair<Float, Float>, Pair<Integer, Float>> map_HealthVsGroups = new HashMap<>(); // Total Health vs Group Count
    public HashMap<Pair<Float, Float>, Pair<Integer, Float>> map_HeightVsWidth = new HashMap<>(); // Board Height vs Board Width

    // Counters for tiles allowed multiple times
    int transition = 0;
    int endcap = 0;
    int extender = 0;

    // Limitations imposed by the physical board
    final int transitionLimit = 2;
    final int endcapLimit = 5;
    final int extenderLimit = 9;

    int IDEAL_SIZE = 166;
    int IDEAL_GROUP = 5;
    float IDEAL_HEALTH = 5.872f;
    int IDEAL_HEIGHT = 18; // 572 / 32
    int IDEAL_WIDTH = 15; // 493 / 32

    float W_SIZE = 1;
    float W_GROUPS = 1;
    float W_HEALTH = 1;
    float W_HEIGHT = 0;
    float W_WIDTH = 0;

    private GenerateBoardsGUI gui = null;

    public CreateOffspring() {
    }

    public CreateOffspring(int firstloop, int generationloop, int offspring, int infeasible) {
        FIRSTLOOP = firstloop;
        GENERATIONLOOP = generationloop;
        OFFSPRING = offspring;
        CHOOSE_INFEASIBLE = infeasible;
    }

    public void setIdeals(int size, int groups, float health, int height, int width) {
        IDEAL_SIZE = size;
        IDEAL_GROUP = groups;
        IDEAL_HEALTH = health;
        IDEAL_HEIGHT = height;
        IDEAL_WIDTH = width;
    }

    void setWeights(float size, float group, float health, float height, float width) {
        W_SIZE = size;
        W_GROUPS = group;
        W_HEALTH = health;
        W_HEIGHT = height;
        W_WIDTH = width;
    }

    void setGUI(GenerateBoardsGUI gui) {
        this.gui = gui;
    }

    public void begin(int generationID) throws IOException, InterruptedException, InvocationTargetException {
        FitnessFunction fitfunc = new FitnessFunction(this, IDEAL_SIZE, IDEAL_GROUP, IDEAL_HEALTH, IDEAL_HEIGHT, IDEAL_WIDTH);
        fitfunc.setWeights(W_SIZE, W_GROUPS, W_HEALTH, W_HEIGHT, W_WIDTH);

        /*for (Quest q : originalQuests) {
            System.out.println(q.getBoards());
            HashMap<String, Float> scores = fitfunc.getFitness(this, q, Objects.requireNonNull(getBoardByName(q.getBoards().get(0))));
            System.out.println(scores.get("Fitness"));
            //feasibleFitness.add(fitness);
        }*/


        int originalSize = originalQuests.size();

        nowGenerating = true;

        for (int i = 0; i < FIRSTLOOP; i++) {
            int x = Random.randInt(originalSize);
            int y = Random.randInt(originalSize);
            while (y == x) {
                y = Random.randInt(originalSize);
            }
            Quest one = originalQuests.get(x);
            Quest two = originalQuests.get(y);

            generateOffspring(one, two, fitfunc);
        }

        for (int i = 0; i < GENERATIONLOOP; i++) {
            //System.out.println(FIRSTLOOP + (i * OFFSPRING));
            int choice = Random.randInt(100);

            // Failsafe - force Infeasible Parents
            // if, for whatever reason, we got this far and failed to spawn enough Feasible parents
            if (feasible.size() < 4)
                choice = 0;

            // 30% to pick parents from the Infeasible pool
            Pair<Quest, Quest> parents;
            String isFeasible = "feasible";

            if (choice < CHOOSE_INFEASIBLE) {
                parents = infeasibleParents(fitfunc);
                isFeasible = "infeasible";
            }
            else {
                if (infeasible.size() < 2)
                    parents = new Pair<>(infeasible.get(0).a, infeasible.get(0).a);
                parents = feasibleParents();
            }

            for (int j = 0; j < OFFSPRING; j++) {
                generateOffspring(parents.a, parents.b, isFeasible, fitfunc);
            }
        }

        exportPCGToJSON(true);
        exportPCGToJSON(false);
        exportMAPElitesToJSON(MapElites.Size, MapElites.Groups);
        exportMAPElitesToJSON(MapElites.Health, MapElites.Groups);
        exportMAPElitesToJSON(MapElites.Height, MapElites.Width);

        float feasiblePercent = (100f * feasible.size() / (feasible.size() + infeasible.size()));
        print(!feasible.isEmpty() ? "Complete! Generated " + feasible.size() + " Feasible Boards (" + feasiblePercent + "%), with Best Offspring: PCG-" + bestID + ", Fitness: " + bestFitness :
                "Complete! Failed to generate a single Feasible board (" + infeasible.size() + " Infeasible)!");
        CreateOffspring co = this;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MapElitesGUI main = new MapElitesGUI(co, generationID);
                main.show();
                if (gui!=null)
                    gui.finished();
            }
        });
    }

    void print(String string) throws InterruptedException, InvocationTargetException {
        if (gui != null) {
            gui.print(string);
        }
    }

    private Pair<Quest, Quest> feasibleParents() {
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

    private Pair<Quest, Quest> infeasibleParents(FitnessFunction fitfunc) {
        List<Pair<Integer, Double>> results = new ArrayList<>();
        float decay = 1.5f;
        float sigma = 5f;
        for (int i = 0; i < infeasibleFitness.size(); i++) {
            double calc = getCalc(i, decay, sigma, fitfunc);

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

    private double getCalc(int i, float decay, float sigma, FitnessFunction fitfunc) {
        HashMap<String, Float> score = infeasibleFitness.get(i);
        // Connectedness
        double connect = Math.pow(decay, 1 - score.get("Connectedness"));

        double geometry = score.get("Geometry");

        double repeats = score.get("Repeats");

        double spawning = score.get("Spawning");

        double consistency = Math.pow(decay, 1 - score.get("Consistency"));

        double size = Math.exp(-(Math.pow(score.get("Size") - fitfunc.IDEAL_SIZE, 2)) / Math.pow(2 * sigma, 2));

        double groups = Math.exp(-(Math.pow(score.get("Groups") - fitfunc.IDEAL_GROUP, 2)) / Math.pow(2 * sigma, 2));

        double health = Math.exp(-(Math.pow(score.get("Health") - fitfunc.IDEAL_HEALTH, 2)) / Math.pow(2 * sigma, 2));

        double complexity = Math.pow(decay, 1 - score.get("Complexity"));

        double rules = Math.pow(decay, 1 - score.get("Rules"));

        return Math.sqrt(connect + geometry + repeats + spawning + consistency + size + groups + health + complexity + rules);
    }

    void generateOffspring(Quest one, Quest two, FitnessFunction fitfunc) throws InterruptedException, InvocationTargetException {
        nowServing++;
        Pair<Pair<Quest, GraphBoard>, Boolean> offspring = createOffspring(one, two, "null", fitfunc);

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);
    }

    void generateOffspring(Quest one, Quest two, String isFeasible, FitnessFunction fitfunc) throws InterruptedException, InvocationTargetException {
        nowServing++;
        Pair<Pair<Quest, GraphBoard>, Boolean> offspring = createOffspring(one, two, isFeasible, fitfunc);

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);
    }

    Pair<Pair<Quest, GraphBoard>, Boolean> createOffspring(Quest parent1, Quest parent2, String type, FitnessFunction fitfunc) throws InterruptedException, InvocationTargetException {
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
        List<BoardNode> crossover = new ArrayList<>();
        List<BoardNode> deleted = new ArrayList<>();
        List<BoardNode> cleanedUp = new ArrayList<>();
        List<BoardNode> rotated = new ArrayList<>();
        boolean freeNodes = true;

        List<BoardNode> newNodes = newBoard.getComponents();
        List<BoardNode> oldNodes = otherBoard.getComponents();


        while (freeNodes) {
            freeNodes = false;

            transition = 0;
            endcap = 0;
            extender = 0;

            // Construct Board Assembly with all the new tiles
            crossover = crossoverMutate(oldNodes, newNodes);
            deleted = deletionMutate(crossover);

            cleanedUp.clear();
            //int counter = 1;

            // Reset these back to 0 after crossover
            transition = 0;
            endcap = 0;
            extender = 0;

            for (BoardNode node : deleted) {
                String name = fixNodeName(node.getComponentName());
                BoardNode newNode = new BoardNode(name);
                node.copyComponentTo(newNode);
                newNode.setProperty(new PropertyString("name", name));
                cleanedUp.add(newNode);
                //System.out.println(counter+ ": " + name + "; " + newNode.getComponentID());
                //counter++;
            }

            rotated = rotateMutate(cleanedUp);

            // Final validity check to ensure that we can actually assemble the board without any loose ends
            // We force rotate, add and subtract any end cap pieces (and entrances / exits)
            // so we have an equal number of connecting North and South pairs, and East and West pairs
            Pair<List<BoardNode>, Boolean> lastCheck = addEndcaps(rotated);

            finalNodes.clear();
            finalNodes.addAll(lastCheck.a);
            freeNodes = lastCheck.b;

            if (freeNodes) {
                //System.out.println("Do it again!");
            }
        }

        // One final cleanup
        transition = 0;
        endcap = 0;
        extender = 0;
        for (BoardNode node : finalNodes) {
            String name = fixNodeName(node.getComponentName());
            node.setComponentName(name);
            node.setProperty(new PropertyString("name", name));
            //System.out.println(node.getComponentName() + "; " + node.getComponentID());
        }

        boolean boardReady = false;

        // Now, assemble the board
        // Keep assembling and reassembling until we have a connection that works

        int attempt = 0;
        while (!boardReady) {
            // Give up after 10 attempts
            if (attempt > 10)
                break;
            boardReady = true;

            attempt++;
            //print("Attempt: " + attempt);

            assembleBoard(finalNodes);

            for (BoardNode node : finalNodes) {
                String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();

                for (String neighbour : neighbours) {
                    if (neighbour.equals("null")) {
                        boardReady = false;
                        break;
                    }
                }

                if (!boardReady) break;

                for (String connection : connections) {
                    if (!positions.contains(connection)) {
                        boardReady = false;
                        break;
                    }
                }

                if (!boardReady) break;

                int expected = ((PropertyInt) Objects.requireNonNull(getTileByName(node.getComponentName())).getProperty(nodeHash)).value;

                if (expected != neighbours.length || expected != connections.length) {
                    boardReady = false;
                    break;
                }
            }
        }

        newBoard.clearBoardNodes();
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
        boolean forceMutate = (finalMonsters.size() < GROUP_MIN) || (finalMonsters.size() > GROUP_MAX);

        if (!forceMutate)
            if (mutate() < MONSTER_MUTATE)
                forceMutate = true;

        while (forceMutate) {
            finalMonsters = mutateMonsters(finalMonsters);
            forceMutate = (finalMonsters.size() < GROUP_MIN) || (finalMonsters.size() > GROUP_MAX);
        }

        mutateAct(newQuest);
        mutateXP(newQuest);
        mutateTraits(newQuest);

        finalMonsters = mutatePositions(finalNodes, newQuest, finalMonsters);

        newQuest.setMonsters(finalMonsters);



        String newBoardName = "pcg-" + nowServing;
        newBoard.setComponentName(newBoardName);
        newQuest.setName("PCG-" + nowServing);
        List<String> boards = newQuest.getBoards();
        boards.clear();
        boards.add(newBoardName);


        HashMap<String, Float> scores = fitfunc.getFitness(this, newQuest, newBoard);

        boolean feasible = scores.get("Feasible") > 0f;

        Pair<Quest, GraphBoard> offspring = new Pair<>(newQuest, newBoard);

        if (feasible) {
            feasibleFitness.add(scores);
            addToMAPElites(scores);

            float fitness = scores.get("Fitness");
            if (fitness > bestFitness) {
                bestID = nowServing;
                bestFitness = fitness;
                bestQuest = offspring;
            }
        }
        else
            infeasibleFitness.add(scores);
        feasibleList.add(feasible);

        return new Pair<>(offspring, feasible);
    }

    String getRandomMonster(List<String> oldMonsters) {
        String newMonster = "null";
        String monType = "";
        String otherType = "";
        int type = mutate();
        List<String> toAdd = new ArrayList<>();
        List<String> other = new ArrayList<>();

        if (type < SMALLOPENGROUP)
            return "OpenSmall:group";
        if (type < OPENGROUP)
            return "Open:group";

        if (type >= (100 - LIEUTENANT)) {
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

    List<String[]> mutateMonsters(List<String[]> monsters) {
        List<String[]> newMonsters = new ArrayList<>(monsters);
        int mutate = mutate();

        int size = monsters.size();

        // Force a specific mutation if for whatever reason we're already over or under the limits
        boolean add = size < GROUP_MIN;
        boolean remove = size > GROUP_MAX;

        if (!add)
            if (size < GROUP_MAX)
                if (mutate < ADD_GROUP)
                    add = true;

        if (!remove)
            if (size > GROUP_MIN)
                if (mutate >= (100 - REMOVE_GROUP))
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

    List<String[]> mutatePositions(List<BoardNode> nodes, Quest quest, List<String[]> monsters) {
        List<String> tiles = new ArrayList<>();
        List<String> taken = new ArrayList<>();
        for (BoardNode node : nodes) {
            tiles.add(node.getComponentName());
        }
        Collections.shuffle(tiles);
        String heroStart = quest.getStartingTile();

        // First, check if the Heroes' original starting tile still exists or not
        // Then, roll Mutation chance (10%)
        boolean forceHeroMutate = !tiles.contains(heroStart) || illegalHeroSpawns.contains(heroStart);
        if (!forceHeroMutate)
            forceHeroMutate = Random.randInt(10) < 1;

        if (forceHeroMutate) {
            heroStart = "null";
            for (String tile : tiles) {
                boolean legal = true;
                for (String illegal : illegalHeroSpawns) {
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
                monster[1] = monsterPosition;
                List<String> illegals = new ArrayList<>(illegalMonsterSpawns);
                if (barghest)
                    illegals.addAll(illegalBarghestSpawns);
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

        return monsters;

    }

    void mutateAct(Quest quest) {
        int mutate = mutate();
        if (mutate < ACT_MUTATE) {
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

    void mutateXP(Quest quest) {
        int mutate = mutate();
        if (mutate < XP_MUTATE) {
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

    void mutateTraits(Quest quest) {
        List<String> oldTraits = quest.getMonsterTraits();
        boolean add = oldTraits.size() < TRAITS_MIN;
        boolean remove = oldTraits.size() > TRAITS_MAX;
        boolean replace = false;

        int mutate = mutate();

        if (!add)
            if (oldTraits.size() < TRAITS_MAX)
                if (mutate < ADD_TRAITS)
                    add = true;
        if (!remove)
            if (oldTraits.size() > TRAITS_MIN)
                if (mutate < (ADD_TRAITS + REMOVE_TRAITS))
                    remove = true;
        if (mutate < (ADD_TRAITS + REMOVE_TRAITS + REPLACE_TRAITS))
            replace = true;

        // Remove, in case of 'All' trait
        if (oldTraits.size() < TRAITS_MIN)
            oldTraits = new ArrayList<>();

        // Add
        if (add) {
            List<String> traits = new ArrayList<>(TRAITS);
            Collections.shuffle(traits);
            for (String t : traits) {
                if (!oldTraits.contains(t))
                    oldTraits.add(t);
                if (oldTraits.size() >= TRAITS_MIN)
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
            List<String> traits = new ArrayList<>(TRAITS);
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

    int mutate() {
        return Random.randInt(100);
    }

    GridBoard getTileByName(String name) {
        if (name.contains("-"))
            name = name.split("-")[0];
        for (GridBoard tile : tiles) {
            if (tile.getComponentName().equals(name)) {
                GridBoard copy = tile.copy();
                copy.getProperties().clear();
                for (int prop_key : tile.getProperties().keySet()) {
                    Property newProp = tile.getProperties().get(prop_key).copy();
                    copy.getProperties().put(prop_key, newProp);
                }
                return copy;
            }
        }
        return null;
    }

    GraphBoard getBoardByName(String name) {
        for (GraphBoard board : originalBoards) {
            if (board.getComponentName().equals(name))
                return board;
        }
        return null;
    }

    GraphBoard getBoardByName(String name, boolean isFeasible) {
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

    Pair<Quest, GraphBoard> getQuestByID(int id, boolean isFeasible) {
        if (isFeasible) {
            for (Pair<Quest, GraphBoard> quest : feasible) {
                String name = quest.a.getName();
                if (Integer.parseInt(name.split("-")[1]) == id)
                    return quest;
            }
        }
        else {
            for (Pair<Quest, GraphBoard> quest : infeasible) {
                String name = quest.a.getName();
                if (Integer.parseInt(name.split("-")[1]) == id)
                    return quest;
            }
        }
        return null;
    }

    HashMap<String, Float> getScoresByID(int id, boolean isFeasible) {
        if (isFeasible) {
            for (HashMap<String, Float> fitness : feasibleFitness) {
                if (fitness.get("ID").intValue() == id)
                    return fitness;
            }
        }
        else {
            for (HashMap<String, Float> fitness : infeasibleFitness) {
                if (fitness.get("ID").intValue() == id)
                    return fitness;
            }
        }
        return null;
    }

    List<BoardNode> crossoverMutate(List<BoardNode> crossoverNodes, List<BoardNode> baseNodes) {

        List<BoardNode> retVal = new ArrayList<>();
        List<String> tiles = new ArrayList<>();

        transition = 0;
        endcap = 0;
        extender = 0;
        int entrance = 0;
        int exit = 0;
        for (BoardNode node : baseNodes) {
            String name = node.getComponentName();

            // Make sure that certain tiles can only be placed a certain number of times
            if (name.contains("transition")) {
                if (transition >= 2) continue;
                transition++;
                tiles.add(name);
                retVal.add(node);
            } else if (name.contains("endcap")) {
                if (endcap >= 5) continue;
                endcap++;
                tiles.add(name);
                retVal.add(node);
            } else if (name.contains("extender")) {
                if (extender >= 9) continue;
                extender++;
                tiles.add(name);
                retVal.add(node);
            } else if (name.contains("entrance")) {
                if (entrance > 0) continue;
                entrance++;
                tiles.add(name);
                retVal.add(node);
            } else if (name.contains("exit")) {
                if (exit > 0) continue;
                exit++;
                tiles.add(name);
                retVal.add(node);
            }
            else if (!tiles.contains(name)) {
                tiles.add(name);
                retVal.add(node);
            }
        }

        // 10% crossover chance
        for (BoardNode node : crossoverNodes) {
            if (Random.randInt(10) < 1) {
                String name = node.getComponentName();

                if (name.contains("transition")) {
                    if (transition >= transitionLimit) continue;
                    transition++;
                    tiles.add(name);
                    retVal.add(node);
                    continue;
                } else if (name.contains("endcap")) {
                    if (endcap >= endcapLimit) continue;
                    endcap++;
                    tiles.add(name);
                    retVal.add(node);
                    continue;
                } else if (name.contains("extender")) {
                    if (extender >= extenderLimit) continue;
                    extender++;
                    tiles.add(name);
                    retVal.add(node);
                    continue;
                } else if (name.contains("entrance")) {
                    if (entrance > 0) continue;
                    entrance++;
                    tiles.add(name);
                    retVal.add(node);
                    continue;
                } else if (name.contains("exit")) {
                    if (exit > 0) continue;
                    exit++;
                    tiles.add(name);
                    retVal.add(node);
                    continue;
                }

                // Make sure we don't add duplicate Tiles
                if (!tiles.contains(name)) {
                    retVal.add(node);
                    tiles.add(name);
                }
            }
        }
        /*String result = "";
        for (String tile : tiles) {
            result += tile +"; ";
        }
        System.out.println(result);*/
        return retVal;
    }

    List<BoardNode> deletionMutate(List<BoardNode> newNodes) {
        // 10% deletion chance
        List<BoardNode> retVal = new ArrayList<>(newNodes);
        for (BoardNode node : newNodes) {
            if (Random.randInt(10) < 1) {
                retVal.remove(node);
                for (BoardNode n : retVal) {
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
        return retVal;
    }

    List<BoardNode> rotateMutate (List<BoardNode> nodes) {
        List<BoardNode> retVal = new ArrayList<>(nodes);
        List<BoardNode> north = new ArrayList<>();
        List<BoardNode> south = new ArrayList<>();
        List<BoardNode> east = new ArrayList<>();
        List<BoardNode> west = new ArrayList<>();
        // 10% rotation chance
        for (BoardNode node : retVal) {
            String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
            if (Random.randInt(10) < 1) {
                int rotation = Random.randInt(positions.size() - 1) + 1;
                int oldRotate = ((PropertyInt) node.getProperty("orientation")).value;
                node.setProperty(new PropertyInt("orientation", (oldRotate + rotation) % 4));
                for (int i = 0; i < connections.length; i++) {
                    if (positions.contains(connections[i])) {
                        int index = positions.indexOf(connections[i]);
                        connections[i] = positions.get((index + rotation) % 4);
                        switch(connections[i]) {
                            case "N-0" -> north.add(node);
                            case "E-0" -> east.add(node);
                            case "S-0" -> south.add(node);
                            case "W-0" -> west.add(node);
                        }
                    }
                }
            }
            else {
                for (String connection : connections) {
                    switch (connection) {
                        case "N-0" -> north.add(node);
                        case "E-0" -> east.add(node);
                        case "S-0" -> south.add(node);
                        case "W-0" -> west.add(node);
                    }
                }
            }
        }

        // Edge case, where we have one tile that now is the lone North/South or East/West provider
        // If so, force a rotation
        if (north.size() == 1 && south.size() == 1) {
            BoardNode suspect = north.get(0);
            if (south.contains(suspect)) {
                //System.out.println("Edge case!");
                int oldRotate = ((PropertyInt) suspect.getProperty("orientation")).value;
                suspect.setProperty(new PropertyInt("orientation", (oldRotate + 1) % 4));
                String[] connections = ((PropertyStringArray) suspect.getProperty("connections")).getValues();
                for (int i = 0; i < connections.length; i++) {
                    if (positions.contains(connections[i])) {
                        int index = positions.indexOf(connections[i]);
                        connections[i] = positions.get((index + 1) % 4);
                    }
                }
            }
        }
        if (east.size() == 1 && west.size() == 1) {
            BoardNode suspect = east.get(0);
            if (west.contains(suspect)) {
                //System.out.println("Edge case!");
                int oldRotate = ((PropertyInt) suspect.getProperty("orientation")).value;
                suspect.setProperty(new PropertyInt("orientation", (oldRotate + 1) % 4));
                String[] connections = ((PropertyStringArray) suspect.getProperty("connections")).getValues();
                for (int i = 0; i < connections.length; i++) {
                    if (positions.contains(connections[i])) {
                        int index = positions.indexOf(connections[i]);
                        connections[i] = positions.get((index + 1) % 4);
                    }
                }
            }
        }

        return retVal;
    }

    Pair<List<BoardNode>, Boolean> addEndcaps(List<BoardNode> nodes) {
        boolean freeNodes = false;
        List<BoardNode> retVal = new ArrayList<>(nodes);

        // We include the Entrance and Exit as well as Endcaps
        List<BoardNode> endcaps = new ArrayList<>();
        List<BoardNode> nCaps = new ArrayList<>();
        List<BoardNode> eCaps = new ArrayList<>();
        List<BoardNode> sCaps = new ArrayList<>();
        List<BoardNode> wCaps = new ArrayList<>();

        int north = 0;
        int east = 0;
        int south = 0;
        int west = 0;

        int countA = 0;
        int countB = 0;

        boolean entrance = false;
        boolean exit = false;
        int endcap = 0;

        for (BoardNode node : nodes) {
            String name = node.getComponentName();

            if (name.contains("A"))
                countA++;
            if (name.contains("B"))
                countB++;

            if (name.contains("endcap")) {
                endcap++;
                endcaps.add(node);
            }
            if (name.contains("entrance")) {
                entrance = true;
                endcaps.add(node);
            }
            if (name.contains("exit")) {
                exit = true;
                endcaps.add(node);
            }

            String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
            for (String c : connections) {
                switch (c) {
                    case "N-0" -> {
                        north++;
                        if (endcaps.contains(node))
                            nCaps.add(node);
                    }
                    case "E-0" -> {
                        east++;
                        if (endcaps.contains(node))
                            eCaps.add(node);
                    }
                    case "S-0" -> {
                        south++;
                        if (endcaps.contains(node))
                            sCaps.add(node);
                    }
                    case "W-0" -> {
                        west++;
                        if (endcaps.contains(node))
                            wCaps.add(node);
                    }
                }
            }
        }
        boolean imbalanceNS = false;
        boolean imbalanceEW = false;
        if (north != south) {
            //System.out.println("Imbalance of North and South - N:" + north + "; S:" + south);
            imbalanceNS = true;
        }
        if (east != west) {
            //System.out.println("Imbalance of East and West - E:" + east + "; W:" + west);
            imbalanceEW = true;
        }

        // First, see if there's a way to fix both imbalances, by rotating any existing endcaps
        while (imbalanceNS && imbalanceEW) {
            boolean madeChange = false;
            boolean canStop = true;
            if (!(nCaps.isEmpty() && sCaps.isEmpty()))
                canStop = false;
            if (!(eCaps.isEmpty() && wCaps.isEmpty()))
                canStop = false;
            if (canStop)
                break;

            if (north > south && !nCaps.isEmpty()) {
                BoardNode node = nCaps.remove(0);
                north--;
                madeChange = true;
                if (east < west) {
                    eCaps.add(node);
                    east++;
                    node.setProperty(new PropertyInt("orientation", 1));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "E-0";
                }
                else if (west < east) {
                    wCaps.add(node);
                    west++;
                    node.setProperty(new PropertyInt("orientation", 3));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "W-0";
                }
            }
            if (south > north && !sCaps.isEmpty()) {
                BoardNode node = sCaps.remove(0);
                south--;
                madeChange = true;
                if (east < west) {
                    eCaps.add(node);
                    east++;
                    node.setProperty(new PropertyInt("orientation", 1));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "E-0";
                }
                else if (west < east) {
                    wCaps.add(node);
                    west++;
                    node.setProperty(new PropertyInt("orientation", 3));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "W-0";
                }
            }
            if (east > west && !eCaps.isEmpty()) {
                BoardNode node = eCaps.remove(0);
                east--;
                madeChange = true;
                if (north < south) {
                    nCaps.add(node);
                    north++;
                    node.setProperty(new PropertyInt("orientation", 0));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "N-0";
                }
                else if (south < north) {
                    sCaps.add(node);
                    south++;
                    node.setProperty(new PropertyInt("orientation", 2));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "S-0";
                }
            }
            if (west > east && !wCaps.isEmpty()) {
                BoardNode node = wCaps.remove(0);
                west--;
                madeChange = true;
                if (north < south) {
                    nCaps.add(node);
                    north++;
                    node.setProperty(new PropertyInt("orientation", 0));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "N-0";
                }
                else if (south < north) {
                    sCaps.add(node);
                    south++;
                    node.setProperty(new PropertyInt("orientation", 2));
                    String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                    connections[0] = "S-0";
                }
            }
            if (north == south)
                imbalanceNS = false;
            if (east == west)
                imbalanceEW = false;

            // If, for whatever reason, we cycled through this and couldn't make a single change, abort
            if (!madeChange)
                break;
        }

        // Then, if there's still an imbalance, go and add the tiles in afterwards
        while (imbalanceNS) {
            // As changing North to South is a difference of 2, we need to make sure we're not just flip-flopping the one tile around
            if (north > south+1 && !nCaps.isEmpty()) {
                BoardNode node = nCaps.remove(0);
                north--;
                sCaps.add(node);
                south++;
                node.setProperty(new PropertyInt("orientation", 2));
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                connections[0] = "S-0";
                if (north == south) {
                    imbalanceNS = false;
                }
                continue;
            }
            if (south > north+1 && !sCaps.isEmpty()) {
                BoardNode node = sCaps.remove(0);
                south--;
                nCaps.add(node);
                north++;
                node.setProperty(new PropertyInt("orientation", 0));
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                connections[0] = "N-0";
                if (north == south) {
                    imbalanceNS = false;
                }
                continue;
            }

            BoardNode newNode = null;
            if (!entrance) {
                newNode = new BoardNode(countB > countA ? "entrance1B" : "entrance1A");
                entrance = true;
            }
            else if (!exit) {
                newNode = new BoardNode(countB > countA ? "exit1B" : "exit1A");
                exit = true;
            }
            else if (endcap < endcapLimit) {
                endcap++;
                newNode = new BoardNode(countB > countA ? "endcap1B-" + endcap : "endcap1A-" + endcap);
            }
            if (newNode != null) {
                newNode.setProperty(new PropertyString("name", newNode.getComponentName()));
                newNode.setProperty(new PropertyStringArray("neighbours", neighbourHash, new String[]{"null"}));
                if (north < south) {
                    newNode.setProperty(new PropertyInt("orientation", 0));
                    newNode.setProperty(new PropertyStringArray("connections", connectionHash, new String[]{"N-0"}));
                    north++;
                    nCaps.add(newNode);
                }
                else {
                    newNode.setProperty(new PropertyInt("orientation", 2));
                    newNode.setProperty(new PropertyStringArray("connections", connectionHash, new String[]{"S-0"}));
                    south++;
                    sCaps.add(newNode);
                }
                endcaps.add(newNode);
                retVal.add(newNode);
            }
            // If there's nothing more to be done, give up
            if (entrance && exit && endcap >= endcapLimit) {
                break;
            }
            if (north == south)
                imbalanceNS = false;
        }
        while (imbalanceEW) {
            if (east > west+1 && !eCaps.isEmpty()) {
                BoardNode node = eCaps.remove(0);
                east--;
                wCaps.add(node);
                west++;
                node.setProperty(new PropertyInt("orientation", 3));
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                connections[0] = "W-0";
                if (east == west) {
                    imbalanceEW = false;
                }
                continue;
            }
            if (west > east+1 && !wCaps.isEmpty()) {
                BoardNode node = wCaps.remove(0);
                west--;
                eCaps.add(node);
                east++;
                node.setProperty(new PropertyInt("orientation", 1));
                String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
                connections[0] = "E-0";
                if (east == west) {
                    imbalanceNS = false;
                }
                continue;
            }

            BoardNode newNode = null;
            if (!entrance) {
                newNode = new BoardNode(countB > countA ? "entrance1B" : "entrance1A");
                entrance = true;
            }
            else if (!exit) {
                newNode = new BoardNode(countB > countA ? "exit1B" : "exit1A");
                exit = true;
            }
            else if (endcap < endcapLimit) {
                endcap++;
                newNode = new BoardNode(countB > countA ? "endcap1B-" + endcap : "endcap1A-" + endcap);
            }
            if (newNode != null) {
                newNode.setProperty(new PropertyString("name", newNode.getComponentName()));
                newNode.setProperty(new PropertyStringArray("neighbours", neighbourHash, new String[]{"null"}));
                if (east < west) {
                    newNode.setProperty(new PropertyInt("orientation", 1));
                    newNode.setProperty(new PropertyStringArray("connections", connectionHash, new String[]{"E-0"}));
                    east++;
                    eCaps.add(newNode);
                }
                else {
                    newNode.setProperty(new PropertyInt("orientation", 3));
                    newNode.setProperty(new PropertyStringArray("connections", connectionHash, new String[]{"W-0"}));
                    west++;
                    wCaps.add(newNode);
                }
                endcaps.add(newNode);
                retVal.add(newNode);
            }
            // If there's nothing more to be done, give up
            if (entrance && exit && endcap >= endcapLimit) {
                break;
            }
            if (east == west)
                imbalanceEW = false;
        }
        // If there's still an imbalance, go and remove whatever Endcaps we can as a last resort
        if (imbalanceNS) {
            while (north > south && !nCaps.isEmpty()) {
                north--;
                BoardNode node = nCaps.remove(0);
                endcaps.remove(node);
                retVal.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
            while (south > north && !sCaps.isEmpty()) {
                south--;
                BoardNode node = sCaps.remove(0);
                endcaps.remove(node);
                retVal.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
        }
        if (imbalanceEW) {
            while (east > west && !eCaps.isEmpty()) {
                east--;
                BoardNode node = eCaps.remove(0);
                endcaps.remove(node);
                retVal.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
            while (west > east && !wCaps.isEmpty()) {
                west--;
                BoardNode node = wCaps.remove(0);
                endcaps.remove(node);
                retVal.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
        }

        // If all else fails, we give up - we can't save this board with the current mutations
        if (north != south) {
            //System.out.println("Still an imbalance - North: " + north + "; South: " + south);
            freeNodes = true;
        }
        if (east != west) {
            //System.out.println("Still an imbalance - East: " + east + "; West: " + west);
            freeNodes = true;
        }


        return new Pair<>(retVal, freeNodes);
    }

    void assembleBoard(List<BoardNode> nodes) {
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
            node.clearNeighbours();

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
                        if (list == null) {
                            System.out.println("n1: " + n1.getComponentName() + ": Real Name: " + n1.getProperty("name").toString());
                            System.out.println("n2: " + n2.getComponentName() + ": Real Name: " + n2.getProperty("name").toString());
                            System.out.println("Connection: " + connection);
                        }
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

    void makeConnections(BoardNode node, List<String[]> unique, List<String> usedConnects, List<String> usedPairs, List<String[]> finalSet) {
        String name = node.getComponentName();
        for (String[] pair : unique) {
            if (finalSet.contains(pair))
                continue;

            String a = pair[0];
            String tileA = a.split(":")[0];
            String b = pair[1];
            String tileB = b.split(":")[0];

            if (!name.contains(tileA) && name.contains(tileB))
                continue;

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
    }

    String fixNodeName (String name) {
        if (name.contains("transition")) {
            transition++;
            return name.split("-")[0] + "-" + transition;
        }
        else if (name.contains("endcap")) {
            endcap++;
            return name.split("-")[0] + "-" + endcap;
        }
        else if (name.contains("extender")) {
            extender++;
            return name.split("-")[0] + "-" + extender;
        }
        return name;
    }

    void addToMAPElites(HashMap<String, Float> scores) {
        Pair<Float, Float> mapKeySG = new Pair<>(scores.get("Size"), scores.get("Groups"));
        Pair<Integer, Float> mapResult = new Pair<>(nowServing, scores.get("Fitness"));
        if (map_SizeVsGroups.containsKey(mapKeySG)) {
            Pair<Integer, Float> oldResult = map_SizeVsGroups.get(mapKeySG);
            if (oldResult.b < mapResult.b)
                map_SizeVsGroups.put(mapKeySG, mapResult);
        }
        else
            map_SizeVsGroups.put(mapKeySG, mapResult);

        Pair<Float, Float> mapKeyHG = new Pair<>((float) Math.floor(scores.get("Total Health")), scores.get("Groups"));
        if (map_HealthVsGroups.containsKey(mapKeyHG)) {
            Pair<Integer, Float> oldResult = map_HealthVsGroups.get(mapKeyHG);
            if (oldResult.b < mapResult.b)
                map_HealthVsGroups.put(mapKeyHG, mapResult);
        }
        else
            map_HealthVsGroups.put(mapKeyHG, mapResult);

        Pair<Float, Float> mapKeyHW = new Pair<>(scores.get("Height"), scores.get("Width"));
        if (map_HeightVsWidth.containsKey(mapKeyHW)) {
            Pair<Integer, Float> oldResult = map_HeightVsWidth.get(mapKeyHW);
            if (oldResult.b < mapResult.b)
                map_HeightVsWidth.put(mapKeyHW, mapResult);
        }
        else
            map_HeightVsWidth.put(mapKeyHW, mapResult);
    }

    void exportPCGToJSON(boolean isFeasible) throws IOException {

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
            outputQ.append(",\"act\":").append(quest.getAct());
            outputQ.append(",\"starting-gold\":").append(quest.getGold());
            outputQ.append(",\"starting-xp\":").append(quest.getStartingXP());
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
            Files.writeString(questOutput,prettyQ + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    void exportMAPElitesToJSON(MapElites first, MapElites second) throws IOException {

        HashMap<Pair<Float, Float>, Pair<Integer, Float>> mapElite = new HashMap<>();
        switch(first) {
            case Size:
                if (second.equals(MapElites.Groups))
                    mapElite = map_SizeVsGroups;
                break;
            case Health:
                if (second.equals(MapElites.Groups))
                    mapElite = map_HealthVsGroups;
                break;
            case Height:
                if (second.equals(MapElites.Width))
                    mapElite = map_HeightVsWidth;
                break;
        }

        if (mapElite.isEmpty()) return;

        ObjectMapper mapper = new ObjectMapper();
        String path = "data/descent2e/pcg/mapelites_" + first.name().toLowerCase() + "&" + second.name().toLowerCase() + ".json";
        Path mapEliteOutput = Paths.get(path);
        Files.write(mapEliteOutput,"[\n".getBytes());

        int counter = 0;
        int max = mapElite.size();
        for (Pair<Float, Float> key : mapElite.keySet()) {
            counter++;
            Pair<Integer, Float> result = mapElite.get(key);
            String output = "{\"";
            output += first.name() + "\":" + key.a.intValue();
            output += ",\"" + second.name() + "\":" + key.b.intValue();
            output += ",\"ID\":" + result.a;
            output += ",\"Fitness\":" + result.b;
            output += ",\"Feasible\":" + feasibleList.get(result.a - 1);
            output += "}";
            Object o = mapper.readValue(output, Object.class);
            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            if (counter < max)
                pretty += ",";
            else
                pretty  += "]";
            Files.writeString(mapEliteOutput,pretty + System.lineSeparator(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
}

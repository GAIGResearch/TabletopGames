package games.descent2e.pcg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.crypto.tink.subtle.Random;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.*;
import games.descent2e.concepts.Quest;
import utilities.Hash;
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

    private int crossoverChance = 10; // Must be < 100;
    private int deletionChance = 10;
    private int rotationChance = 10;

    public HashMap<Integer, GridBoard> boards = new HashMap<>();
    public HashMap<Integer, Map<Integer, GridBoard>> boardTiles = new HashMap<>();
    public HashMap<Integer, int[][]> tileRefs = new HashMap<>();
    public HashMap<Integer, Map<String, Map<Vector2D, Vector2D>>> gridRefs = new HashMap<>();

    public List<PCGBoard> feasible = new ArrayList<>();
    public List<PCGBoard> infeasible = new ArrayList<>();
    public List<HashMap<String, Float>> feasibleFitness = new ArrayList<>();
    public List<HashMap<String, Float>> infeasibleFitness = new ArrayList<>();
    public List<Boolean> feasibleList = new ArrayList<>();

    public PCGBoard bestQuest = null;
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
    static final int transitionLimit = 2;
    static final int endcapLimit = 5;
    static final int extenderLimit = 9;

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

    private Set<Integer> savedBoards = new HashSet<>();

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
            PCGBoard one = startingBoards.get(x);
            PCGBoard two = startingBoards.get(y);
            //Quest one = originalQuests.get(x);
            //Quest two = originalQuests.get(y);

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
            Pair<PCGBoard, PCGBoard> parents;
            String isFeasible = "feasible";

            if (choice < CHOOSE_INFEASIBLE) {
                parents = infeasibleParents(fitfunc);
                isFeasible = "infeasible";
            }
            else {
                if (infeasible.size() < 2)
                    parents = new Pair<>(infeasible.get(0), infeasible.get(0));
                else parents = feasibleParents();
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

    private Pair<PCGBoard, PCGBoard> feasibleParents() {
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

        PCGBoard first = feasible.get(results.get(0).a);
        PCGBoard second = feasible.get(results.get(1).a);

        return new Pair<>(first, second);
    }

    private Pair<PCGBoard, PCGBoard> infeasibleParents(FitnessFunction fitfunc) {
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
                PCGBoard one = infeasible.get(results.get(i).a);
                PCGBoard two = infeasible.get(results.get(i+1).a);

                return new Pair<>(one, two);
            }
        }

        // Failsafe, return the last two in the pool
        PCGBoard one = infeasible.get(results.get(n-2).a);
        PCGBoard two = infeasible.get(results.get(n-1).a);
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

    void generateOffspring(PCGBoard one, PCGBoard two, FitnessFunction fitfunc) throws InterruptedException, InvocationTargetException {
        nowServing++;
        System.out.println("Now Serving: " + nowServing);
        Pair<PCGBoard, Boolean> offspring = createOffspring(one, two,"null", fitfunc);

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);
    }

    void generateOffspring(PCGBoard one, PCGBoard two, String isFeasible, FitnessFunction fitfunc) throws InterruptedException, InvocationTargetException {
        nowServing++;
        System.out.println("Now Serving: " + nowServing);
        Pair<PCGBoard, Boolean> offspring = createOffspring(one, two, isFeasible, fitfunc);

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);
    }

    Pair<PCGBoard, Boolean> createOffspring(PCGBoard parent1, PCGBoard parent2, String type, FitnessFunction fitfunc) throws InterruptedException, InvocationTargetException {
        PCGBoard questBase;
        PCGBoard questOther;
        PCGBoard boardBase;
        PCGBoard boardOther;

        // Deciding which Parent is the Base Quest
        int choice = Random.randInt(2);
        if (choice == 0) {
            questBase = parent1.copy();
            questOther = parent2.copy();
        }
        else {
            questBase = parent2.copy();
            questOther = parent1.copy();
        }

        // Now, decide which Parent is the Base Board
        choice = Random.randInt(2);
        if (choice == 0) {
            boardBase = parent1.copy();
            boardOther = parent2.copy();
        }
        else {
            boardBase = parent2.copy();
            boardOther = parent1.copy();
        }

        PCGBoard offspring = PCGBoard.createOffspring(questBase, boardBase, nowServing);

        HashSet<String> tilesUsed = new HashSet<>();
        for (PCGNode tile : offspring.board) {
            tilesUsed.add(tile.name);
        }

        // --- BOARD MUTATIONS ---

        List<PCGNode> finalNodes = new ArrayList<>();
        HashSet<String> crossoverTiles = null;

        boolean freeNodes = true;
        while (freeNodes) {
            List<PCGNode> crossover = new ArrayList<>(offspring.board);
            crossoverTiles = new HashSet<>(tilesUsed);

            // Crossover
            for (PCGNode node : boardOther.board) {
                if (Random.randInt(100) < crossoverChance) {
                    if (!crossoverTiles.contains(node.name)) {
                        crossover.add(node.copy());
                        crossoverTiles.add(node.name);
                    }
                }
            }

            List<PCGNode> toRotate = new ArrayList<>();

            // Deletion
            for (PCGNode node : crossover) {
                if (Random.randInt(100) < deletionChance) {
                    crossoverTiles.remove(node.name);
                }
                else
                    toRotate.add(node.copy());
            }

            // Fix names of Transitions, Endcaps and Extenders
            crossoverTiles.clear();
            freeNodes = rotateMutate(toRotate, crossoverTiles);
            finalNodes = new ArrayList<>(toRotate);
        }

        boolean boardReady = false;

        int attempt = 0;

        while(!boardReady) {
            // Give up after 10 attempts
            if (attempt > 10)
                break;
            attempt++;
            boardReady = true;
            assembleBoard(finalNodes);
            for (PCGNode node : finalNodes) {
                if (node.connects.size() != node.neighbours.size()) {
                    boardReady = false;
                    //System.out.println(attempt + ": I'll try spinning! That's a good trick!");
                    crossoverTiles.clear();
                    rotateMutate(finalNodes, crossoverTiles);
                    break;
                }
            }
        }
        
        offspring.board = finalNodes;
        tilesUsed = crossoverTiles;

        // --- MONSTER MUTATIONS ---

        HashSet<String> monstersUsed = new HashSet<>();

        HashSet<String> positions = new HashSet<>();
        if (tilesUsed.contains(offspring.heroStartingPosition))
            positions.add(offspring.heroStartingPosition);
        else
            offspring.heroStartingPosition = null;

        for (String[] monster : offspring.monsters) {
            if (tilesUsed.contains(monster[1]) && !positions.contains(monster[1]))
                positions.add(monster[1]);
            else
                monster[1] = null;
            if (monster[0].contains("Open")) continue;
            monstersUsed.add(monster[0]);
        }

        // Crossover

        HashSet<String[]> newMonsters = new HashSet<>(offspring.monsters);

        for (String[] monster : questOther.monsters) {
            if (Random.randInt(100) < crossoverChance) {
                String name = monster[0];
                String position = monster[1];
                if (tilesUsed.contains(position) && !positions.contains(position))
                    positions.add(position);
                else
                    position = null;
                String[] toAdd = {name, position};
                if (name.contains("Open"))
                    newMonsters.add(toAdd);
                else if (!monstersUsed.contains(name)) {
                    newMonsters.add(toAdd);
                    monstersUsed.add(name);
                }
            }
        }

        // Deletion
        HashSet<String[]> finalMonsters = new HashSet<>(newMonsters);
        for (String[] monster : newMonsters) {
            if (Random.randInt(100) < deletionChance) {
                finalMonsters.remove(monster);
                positions.remove(monster[1]);
            }
        }

        // Force a mutation if the group sizes are now too big or too small
        boolean forceMutate = (finalMonsters.size() < GROUP_MIN) || (finalMonsters.size() > GROUP_MAX);
        // Or if we roll for it
        if (!forceMutate)
            if (mutate() < MONSTER_MUTATE)
                forceMutate = true;

        while (forceMutate) {
            forceMutate = mutateMonsters(finalMonsters);
        }

        offspring.monsters = finalMonsters;

        boolean legalSpawns = false;
        int attempts = 0;
        while (!legalSpawns) {
            attempts++;
            //System.out.println("Attempt: " + attempts);
            legalSpawns = mutatePositions(offspring, tilesUsed);
            if (attempts > 10) {
                //System.out.println(nowServing + ": Giving up.");
                break; // If we can't get a valid spawn set after 20 attempts, give up; this board is infeasible anyway
            }
        }

        mutateAct(offspring);
        mutateXP(offspring);
        mutateTraits(offspring);

        HashMap<String, Float> scores = fitfunc.getFitness(this, offspring);

        boolean feasible = scores.get("Feasible") > 0f;

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

        /*

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
         */
    }

    String getRandomMonster(HashSet<String> oldMonsters) {
        String monType;
        String otherType;
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
        for (String m : toAdd) {
            if (!oldMonsters.contains(m+monType)) {
                return m + monType;
            }
        }
        for (String m : other) {
            if (!oldMonsters.contains(m + otherType)) {
                return m + otherType;
            }
        }
        return "Open:group";
    }

    boolean mutateMonsters(HashSet<String[]> monsters) {
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
            HashSet<String> currentMonsters = new HashSet<>();
            for (String[] m : monsters) {
                currentMonsters.add(m[0]);
            }
            String[] newMonster = {getRandomMonster(currentMonsters), null};
            monsters.add(newMonster);
        }
        // Remove a Monster from the groups
        else if (remove) {
            String[] result = (String[]) monsters.toArray()[Random.randInt(monsters.size())];
            monsters.remove(result);
        }
        // Replace a Monster from the groups
        else {
            HashSet<String> currentMonsters = new HashSet<>();
            for (String[] m : monsters) {
                currentMonsters.add(m[0]);
            }
            String[] result = (String[]) monsters.toArray()[Random.randInt(monsters.size())];
            monsters.remove(result);
            result[0] = getRandomMonster(currentMonsters);
            monsters.add(result);
        }

        // We want to be within the boundaries - if we fall outside of it, return true - and redo the mutations
        return monsters.size() < GROUP_MIN || monsters.size() > GROUP_MAX;
    }

    boolean mutatePositions(PCGBoard quest, HashSet<String> nodes) {
        List<String> available = new ArrayList<>(nodes);
        HashSet<String> used = new HashSet<>();
        Collections.shuffle(available);
        String heroStart = quest.heroStartingPosition;

        // First, check if the Heroes' original starting tile still exists or not
        // Then, roll Mutation chance (10%)
        boolean forceHeroMutate = !nodes.contains(heroStart) || illegalHeroSpawns.contains(heroStart);
        if (!forceHeroMutate)
            forceHeroMutate = Random.randInt(10) < 1;

        if (forceHeroMutate) {
            heroStart = null;

            if (heroStart == null) {
                for (String tile : nodes) {
                    if (!illegalHeroSpawns.contains(tile.split("-")[0])) {
                        heroStart = tile;
                        break;
                    }
                }
            }
        }

        // Save the new Heroes start
        used.add(heroStart);
        quest.heroStartingPosition = heroStart;

        HashSet<String[]> monsters = new HashSet<>(quest.monsters);

        // Now repeat for every Monster
        for (String[] monster : monsters) {
            String name = monster[0];
            String monsterPosition = monster[1];
            boolean forceMonsterMutate = !nodes.contains(monsterPosition) || !available.contains(monsterPosition);

            // Non-Lieutenant Monsters have additional restrictions
            HashSet<String> traits = quest.monsterTraits;
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
                        forceMonsterMutate = illegalBarghestSpawns.contains(monsterPosition.split("-")[0]);
                    }
                }
                if (name.contains("Dragon") || dragon) {
                    barghest = true;
                    dragon = true;
                    if (!forceMonsterMutate) {
                        String split = monsterPosition.split("-")[0];
                        forceMonsterMutate = illegalBarghestSpawns.contains(split) || illegalDragonSpawns.contains(split);
                    }
                }
            }
            // Otherwise, mutation chance
            if (!forceMonsterMutate)
                forceMonsterMutate = Random.randInt(10) < 1;

            if (forceMonsterMutate) {
                monsterPosition = null;
                monster[1] = monsterPosition;
                List<String> illegals = new ArrayList<>(illegalMonsterSpawns);
                if (barghest)
                    illegals.addAll(illegalBarghestSpawns);
                if (dragon)
                    illegals.addAll(illegalDragonSpawns);

                for (String tile : nodes) {
                    if (used.contains(tile)) continue;

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
            used.add(monsterPosition);
            monster[1] = monsterPosition;
        }

        quest.monsters = monsters;

        for (String[] monster : monsters) {
            if (monster[1] == null || !used.contains(monster[1]))
                return false;
        }

        return heroStart != null;

    }

    void mutateAct(PCGBoard quest) {
        int mutate = mutate();
        if (mutate < ACT_MUTATE) {
            int newAct = (quest.act % 2) + 1;
            if (newAct > 1) {
                quest.startingXP += 5;
                quest.gold += 125;
            }
            else {
                quest.startingXP -= 5;
                quest.gold -= 125;
            }
            quest.act = newAct;
        }
    }

    void mutateXP(PCGBoard quest) {
        int mutate = mutate();
        if (mutate < XP_MUTATE) {
            int newXP = Random.randInt(5) + 5 * (quest.act - 1);
            int newGold = newXP * 25;
            if (newXP > 1) {
                for (int i = 0; i < Random.randInt(newXP); i++) {
                    newGold += (Random.randInt(5) * 25);
                }
            }

            quest.startingXP = newXP;
            quest.gold = newGold;
        }
    }

    void mutateTraits(PCGBoard quest) {
        HashSet<String> oldTraits = new HashSet<>(quest.monsterTraits);
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
            oldTraits.clear();

        // Add
        if (add) {
            List<String> traits = new ArrayList<>(TRAITS);
            Collections.shuffle(traits);
            for (String t : traits) {
                if (oldTraits.contains(t)) continue;
                oldTraits.add(t);
                if (oldTraits.size() >= TRAITS_MIN)
                    break;
            }
        }
        // Remove
        else if (remove) {
            String r = oldTraits.toArray()[Random.randInt(oldTraits.size())].toString();
            oldTraits.remove(r);
        }
        // Replace
        else if (replace) {
            String r = oldTraits.toArray()[Random.randInt(oldTraits.size())].toString();
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
            oldTraits = new HashSet<>();
            oldTraits.add("All");
        }
        quest.monsterTraits = oldTraits;
    }

    int mutate() {
        return Random.randInt(100);
    }

    GridBoard getTileByName(String name) {
        return GenerateBoards.getTileByName(name);
    }

    GraphBoard getBoardByName(String name) {
        for (GraphBoard board : originalBoards) {
            if (board.getComponentName().equals(name))
                return board;
        }
        return null;
    }

    PCGBoard getBoardByName(String name, boolean isFeasible) {
        if (isFeasible) {
            for (PCGBoard quest : feasible) {
                if (quest.name.equals(name))
                    return quest;
            }
        }
        else {
            for (PCGBoard quest : infeasible) {
                if (quest.name.equals(name))
                    return quest;
            }
        }
        return null;
    }

    PCGBoard getQuestByID(int id, boolean isFeasible) {
        if (isFeasible) {
            for (PCGBoard quest : feasible) {
                String name = quest.name;
                if (Integer.parseInt(name.split("-")[1]) == id)
                    return quest;
            }
        }
        else {
            for (PCGBoard quest : infeasible) {
                String name = quest.name;
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

    public void saveBoard(int id) {
        savedBoards.add(id);
    }

    public boolean isBoardSaved(int id) {
        return savedBoards.contains(id);
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

    boolean rotateMutate (List<PCGNode> nodes, HashSet<String> tiles) {
        HashSet<PCGNode> north = new HashSet<>();
        HashSet<PCGNode> south = new HashSet<>();
        HashSet<PCGNode> east = new HashSet<>();
        HashSet<PCGNode> west = new HashSet<>();

        int sideA = 0;
        int sideB = 0;

        transition = 0;
        endcap = 0;
        extender = 0;

        // Fix the names first
        for (PCGNode node : nodes) {
            String name = node.name.split("-")[0];
            if (name.contains("transition")) {
                transition++;
                node.name = name + "-" + transition;
            }
            else if (name.contains("endcap")) {
                endcap++;
                node.name = name + "-" + endcap;
            }
            else if (name.contains("extender")) {
                extender++;
                node.name = name + "-" + extender;
            }
            tiles.add(node.name);

            if (node.name.contains("A"))
                sideA++;
            else if (node.name.contains("B"))
                sideB++;

            tiles.add(node.name);

            // 10% rotation chance
            if (Random.randInt(100) < rotationChance) {
                int rotation = Random.randInt(PCGNode.connections.length - 1) + 1;
                node.rotate(rotation);
            }
            for (Connection c : node.connects) {
                switch (c) {
                    case NORTH -> north.add(node);
                    case EAST -> east.add(node);
                    case SOUTH -> south.add(node);
                    case WEST -> west.add(node);
                }
            }
        }

        // Edge case, where we have one tile that now is the lone North/South or East/West provider
        // If so, force a rotation
        if (north.size() == 1 && south.size() == 1) {
            PCGNode suspect = (PCGNode) north.toArray()[0];
            if (south.contains(suspect)) {
                //System.out.println("Edge case!");
                suspect.rotate(1);
                north.remove(suspect);
                south.remove(suspect);
                for (Connection c : suspect.connects) {
                    switch (c) {
                        case NORTH -> north.add(suspect);
                        case EAST -> east.add(suspect);
                        case SOUTH -> south.add(suspect);
                        case WEST -> west.add(suspect);
                    }
                }
            }
        }
        if (east.size() == 1 && west.size() == 1) {
            PCGNode suspect = (PCGNode) east.toArray()[0];
            if (west.contains(suspect)) {
                //System.out.println("Edge case!");
                suspect.rotate(1);
                east.remove(suspect);
                west.remove(suspect);
                for (Connection c : suspect.connects) {
                    switch (c) {
                        case NORTH -> north.add(suspect);
                        case EAST -> east.add(suspect);
                        case SOUTH -> south.add(suspect);
                        case WEST -> west.add(suspect);
                    }
                }
            }
        }

        return addEndcaps(nodes, tiles, sideA, sideB, north, east, south, west);
    }

    Boolean addEndcaps(List<PCGNode> nodes, HashSet<String> tiles, int sideA, int sideB,
                       HashSet<PCGNode> north, HashSet<PCGNode> east, HashSet<PCGNode> south, HashSet<PCGNode> west) {
        boolean freeNodes = false;

        // We include the Entrance and Exit as well as Endcaps
        HashSet<PCGNode> endcaps = new HashSet<>();
        HashSet<PCGNode> nCaps = new HashSet<>();
        HashSet<PCGNode> eCaps = new HashSet<>();
        HashSet<PCGNode> sCaps = new HashSet<>();
        HashSet<PCGNode> wCaps = new HashSet<>();

        boolean entrance = false;
        boolean exit = false;
        int endcap = 0;

        for (PCGNode node : north) {
            String name = node.name;
            if (name.contains("endcap")) {
                endcap++;
                endcaps.add(node);
                nCaps.add(node);
            }
            if (name.contains("entrance")) {
                entrance = true;
                endcaps.add(node);
                nCaps.add(node);
            }
            if (name.contains("exit")) {
                exit = true;
                endcaps.add(node);
                nCaps.add(node);
            }
        }
        for (PCGNode node : east) {
            String name = node.name;
            if (name.contains("endcap")) {
                endcap++;
                endcaps.add(node);
                eCaps.add(node);
            }
            if (name.contains("entrance")) {
                entrance = true;
                endcaps.add(node);
                eCaps.add(node);
            }
            if (name.contains("exit")) {
                exit = true;
                endcaps.add(node);
                eCaps.add(node);
            }
        }
        for (PCGNode node : south) {
            String name = node.name;
            if (name.contains("endcap")) {
                endcap++;
                endcaps.add(node);
                sCaps.add(node);
            }
            if (name.contains("entrance")) {
                entrance = true;
                endcaps.add(node);
                sCaps.add(node);
            }
            if (name.contains("exit")) {
                exit = true;
                endcaps.add(node);
                sCaps.add(node);
            }
        }
        for (PCGNode node : west) {
            String name = node.name;
            if (name.contains("endcap")) {
                endcap++;
                endcaps.add(node);
                wCaps.add(node);
            }
            if (name.contains("entrance")) {
                entrance = true;
                endcaps.add(node);
                wCaps.add(node);
            }
            if (name.contains("exit")) {
                exit = true;
                endcaps.add(node);
                wCaps.add(node);
            }
        }
        boolean imbalanceNS = false;
        boolean imbalanceEW = false;
        if (north.size() != south.size()) {
            //System.out.println("Imbalance of North and South - N:" + north + "; S:" + south);
            imbalanceNS = true;
        }
        if (east.size() != west.size()) {
            //System.out.println("Imbalance of East and West - E:" + east + "; W:" + west);
            imbalanceEW = true;
        }

        // First, see if there's a way to fix both imbalances, by rotating any existing endcaps
        while (imbalanceNS && imbalanceEW) {
            boolean canStop = nCaps.isEmpty() && sCaps.isEmpty() && eCaps.isEmpty() && wCaps.isEmpty();
            if (canStop)
                break;

            boolean madeChange = false;

            if (north.size() > south.size() && !nCaps.isEmpty()) {
                PCGNode node = (PCGNode) nCaps.toArray()[0];
                nCaps.remove(node);
                north.remove(node);
                madeChange = true;
                if (east.size() < west.size()) {
                    eCaps.add(node);
                    east.add(node);
                    node.orientation = 1;
                    node.connects.set(0, Connection.EAST);
                }
                else if (west.size() < east.size()) {
                    wCaps.add(node);
                    west.add(node);
                    node.orientation = 3;
                    node.connects.set(0, Connection.WEST);
                }
            }
            if (south.size() > north.size() && !sCaps.isEmpty()) {
                PCGNode node = (PCGNode) sCaps.toArray()[0];
                sCaps.remove(node);
                south.remove(node);
                madeChange = true;
                if (east.size() < west.size()) {
                    eCaps.add(node);
                    east.add(node);
                    node.orientation = 1;
                    node.connects.set(0, Connection.EAST);
                }
                else if (west.size() < east.size()) {
                    wCaps.add(node);
                    west.add(node);
                    node.orientation = 3;
                    node.connects.set(0, Connection.WEST);
                }
            }
            if (east.size() > west.size() && !eCaps.isEmpty()) {
                PCGNode node = (PCGNode) eCaps.toArray()[0];
                eCaps.remove(node);
                east.remove(node);
                madeChange = true;
                if (north.size() < south.size()) {
                    nCaps.add(node);
                    north.add(node);
                    node.orientation = 0;
                    node.connects.set(0, Connection.NORTH);
                }
                else if (south.size() < north.size()) {
                    sCaps.add(node);
                    south.add(node);
                    node.orientation = 2;
                    node.connects.set(0, Connection.SOUTH);
                }
            }
            if (west.size() > east.size() && !wCaps.isEmpty()) {
                PCGNode node = (PCGNode) wCaps.toArray()[0];
                wCaps.remove(node);
                west.remove(node);
                madeChange = true;
                if (north.size() < south.size()) {
                    nCaps.add(node);
                    north.add(node);
                    node.orientation = 0;
                    node.connects.set(0, Connection.NORTH);
                }
                else if (south.size() < north.size()) {
                    sCaps.add(node);
                    south.add(node);
                    node.orientation = 2;
                    node.connects.set(0, Connection.SOUTH);
                }
            }
            if (north.size() == south.size())
                imbalanceNS = false;
            if (east.size() == west.size())
                imbalanceEW = false;

            // If, for whatever reason, we cycled through this and couldn't make a single change, abort
            if (!madeChange)
                break;
        }

        // Then, if there's still an imbalance, go and add the tiles in afterwards
        while (imbalanceNS) {
            // As changing North to South is a difference of 2, we need to make sure we're not just flip-flopping the one tile around
            if (north.size() > south.size()+1 && !nCaps.isEmpty()) {
                PCGNode node = (PCGNode) nCaps.toArray()[0];
                nCaps.remove(node);
                north.remove(node);
                sCaps.add(node);
                south.add(node);
                node.orientation = 2;
                node.connects.set(0, Connection.SOUTH);
                if (north.size() == south.size()) {
                    imbalanceNS = false;
                }
                continue;
            }
            if (south.size() > north.size()+1 && !sCaps.isEmpty()) {
                PCGNode node = (PCGNode) sCaps.toArray()[0];
                sCaps.remove(node);
                south.remove(node);
                nCaps.add(node);
                north.add(node);
                node.orientation = 0;
                node.connects.set(0, Connection.NORTH);
                if (north.size() == south.size()) {
                    imbalanceNS = false;
                }
                continue;
            }

            PCGNode newNode = null;
            if (!entrance) {
                newNode = new PCGNode(sideB > sideA ? "entrance1B" : "entrance1A");
                entrance = true;
                newNode.nodeID = 1000 + Random.randInt(1000);
            }
            else if (!exit) {
                newNode = new PCGNode(sideB > sideA ? "exit1B" : "exit1A");
                exit = true;
                newNode.nodeID = 9000 + Random.randInt(1000);
            }
            else if (endcap < endcapLimit) {
                endcap++;
                newNode = new PCGNode(sideB > sideA ? "endcap1B-" + endcap : "endcap1A-" + endcap);
                newNode.nodeID = (endcap * 10000) + Random.randInt(1000);
            }
            if (newNode != null) {
                if (north.size() < south.size()) {
                    newNode.orientation = 0;
                    newNode.connects.add(Connection.NORTH);
                    north.add(newNode);
                    nCaps.add(newNode);
                }
                else {
                    newNode.orientation = 2;
                    newNode.connects.add(Connection.SOUTH);
                    south.add(newNode);
                    sCaps.add(newNode);
                }
                endcaps.add(newNode);
                nodes.add(newNode);
                tiles.add(newNode.name);
            }
            // If there's nothing more to be done, give up
            if (entrance && exit && endcap >= endcapLimit) {
                break;
            }
            if (north.size() == south.size())
                imbalanceNS = false;
        }
        while (imbalanceEW) {
            if (east.size() > west.size()+1 && !eCaps.isEmpty()) {
                PCGNode node = (PCGNode) eCaps.toArray()[0];
                eCaps.remove(node);
                east.remove(node);
                wCaps.add(node);
                west.add(node);
                node.orientation = 3;
                node.connects.set(0, Connection.WEST);
                if (east.size() == west.size()) {
                    imbalanceNS = false;
                }
                continue;
            }
            if (west.size() > east.size()+1 && !wCaps.isEmpty()) {
                PCGNode node = (PCGNode) wCaps.toArray()[0];
                wCaps.remove(node);
                west.remove(node);
                eCaps.add(node);
                east.add(node);
                node.orientation = 1;
                node.connects.set(0, Connection.EAST);
                if (east.size() == west.size()) {
                    imbalanceNS = false;
                }
                continue;
            }

            PCGNode newNode = null;
            if (!entrance) {
                newNode = new PCGNode(sideB > sideA ? "entrance1B" : "entrance1A");
                entrance = true;
            }
            else if (!exit) {
                newNode = new PCGNode(sideB > sideA ? "exit1B" : "exit1A");
                exit = true;
            }
            else if (endcap < endcapLimit) {
                endcap++;
                newNode = new PCGNode(sideB > sideA ? "endcap1B-" + endcap : "endcap1A-" + endcap);
            }
            if (newNode != null) {
                if (east.size() < west.size()) {
                    newNode.orientation = 1;
                    newNode.connects.add(Connection.EAST);
                    east.add(newNode);
                    eCaps.add(newNode);
                }
                else {
                    newNode.orientation = 3;
                    newNode.connects.add(Connection.WEST);
                    west.add(newNode);
                    wCaps.add(newNode);
                }
                endcaps.add(newNode);
                nodes.add(newNode);
                tiles.add(newNode.name);
            }
            // If there's nothing more to be done, give up
            if (entrance && exit && endcap >= endcapLimit) {
                break;
            }
            if (east.size() == west.size())
                imbalanceEW = false;
        }
        // If there's still an imbalance, go and remove whatever Endcaps we can as a last resort
        if (imbalanceNS) {
            while (north.size() > south.size() && !nCaps.isEmpty()) {
                PCGNode node = (PCGNode) nCaps.toArray()[0];
                north.remove(node);
                nCaps.remove(node);
                endcaps.remove(node);
                nodes.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
            while (south.size() > north.size() && !sCaps.isEmpty()) {
                PCGNode node = (PCGNode) sCaps.toArray()[0];
                south.remove(node);
                sCaps.remove(node);
                endcaps.remove(node);
                nodes.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
        }
        if (imbalanceEW) {
            while (east.size() > west.size() && !eCaps.isEmpty()) {
                PCGNode node = (PCGNode) eCaps.toArray()[0];
                east.remove(node);
                eCaps.remove(node);
                endcaps.remove(node);
                nodes.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
            while (west.size() > east.size() && !wCaps.isEmpty()) {
                PCGNode node = (PCGNode) wCaps.toArray()[0];
                west.remove(node);
                wCaps.remove(node);
                endcaps.remove(node);
                nodes.remove(node);
                //System.out.println("Last Resort: Removing " + node.getComponentName());
            }
        }

        // If all else fails, we give up - we can't save this board with the current mutations
        if (north.size() != south.size()) {
            return true;
        }
        return east.size() != west.size();
    }

    void assembleBoard(List<PCGNode> nodes) {
        // Randomly assemble the new board
        Map<Connection, Connection> pairings = new HashMap<>();
        pairings.put(Connection.NORTH, Connection.SOUTH);
        pairings.put(Connection.SOUTH, Connection.NORTH);
        pairings.put(Connection.EAST, Connection.WEST);
        pairings.put(Connection.WEST, Connection.EAST);

        Map<String, Map<Connection, List<Pair<PCGNode, Connection>>>> possible = new HashMap<>();

        // First, cleanse the board of any possible connections
        for (PCGNode node : nodes) {
            node.clearNeighbours();

            Map<Connection, List<Pair<PCGNode, Connection>>> connections = new HashMap<>();

            for (Connection c : node.connects)
                connections.put(c, new ArrayList<>());
            possible.put(node.name, connections);
        }
        // Then, go through all possible connections for each node
        for (PCGNode n1 : nodes) {
            for (PCGNode n2 : nodes) {
                if (n1.equals(n2))
                    continue;
                for (Connection connection : n1.connects) {
                    Connection opposite = pairings.get(connection);
                    if (n2.connects.contains(opposite)) {
                        Map<Connection, List<Pair<PCGNode, Connection>>> link = possible.get(n1.name);
                        List<Pair<PCGNode, Connection>> list = link.get(connection);
                        if (list == null) {
                            System.out.println(Arrays.toString(possible.keySet().toArray()));
                            System.out.println(n1.name + "; " + n1.orientation + "; " + connection + "; " + n2.name + "; " + n2.orientation + "; " + opposite);
                            for (Connection c : n1.connects)
                                System.out.println(c);
                            System.out.println("-");
                            for (Connection c : n2.connects)
                                System.out.println(c);
                            System.out.println("-");
                            for (Connection key : link.keySet()) {
                                System.out.println(key + "; " + link.get(key));
                            }
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
            for (Connection connection : possible.get(tile).keySet()) {
                String one = tile + ":" + connection;
                for (Pair<PCGNode, Connection> piece : possible.get(tile).get(connection)) {
                    String two = piece.a.name + ":" + piece.b;

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

            for (PCGNode node : nodes) {
                if (node.name.equals(first[0])) {
                    for (Connection connection : node.connects) {
                        if (connection.toString().equals(first[1])) {
                            node.neighbours.put(connection, second[0]);
                            break;
                        }
                    }
                }
                if (node.name.equals(second[0])) {
                    for (Connection connection : node.connects) {
                        if (connection.toString().equals(second[1])) {
                            node.neighbours.put(connection, first[0]);
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
        List<PCGBoard> set = feasible;
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

        for (PCGBoard quest : set) {
            counter++;

            StringBuilder outputQ = new StringBuilder("{\"");

            outputQ.append("id\":\"").append(quest.name).append("\"");
            outputQ.append(",\"act\":").append(quest.act);
            outputQ.append(",\"starting-gold\":").append(quest.gold);
            outputQ.append(",\"starting-xp\":").append(quest.startingXP);
            outputQ.append(",\"traits\": [\"").append(String.join("\", \"", quest.monsterTraits)).append("\"]");
            outputQ.append(",\"monsters\": [");
            int monsterMax = quest.monsters.size();
            int monsterCounter = 0;
            for (String[] monster : quest.monsters) {
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
            outputQ.append(",\"boards\": [ \"").append(quest.name.toLowerCase()).append("\"]");
            outputQ.append(",\"starting-tile\": \"").append(quest.heroStartingPosition).append("\"");

            outputQ.append("}");

            // --- BOARD ---

            StringBuilder outputB = new StringBuilder("{");

            outputB.append("\"type\": \"graph\",");
            outputB.append("\"verticesKey\": \"name\",");
            outputB.append("\"neighboursKey\": \"neighbours\",");
            outputB.append("\"maxNeighbours\": -1,");
            outputB.append("\"id\": \"").append(quest.name.toLowerCase()).append("\",");
            outputB.append("\"nodes\": [");
            int nodeCount = 0;
            for (PCGNode node : quest.board) {
                nodeCount++;

                List<Connection> connections = node.connects;
                HashMap<Connection, String> neighbours = node.neighbours;

                outputB.append("{ \"name\": [\"String\", \"").append(node.name).append("\"],");
                outputB.append("\"orientation\": [\"Integer\", ").append(node.orientation).append("],");

                StringBuilder neighbourString = new StringBuilder("\"neighbours\": [\"String[]\", []],");
                StringBuilder connectionString = new StringBuilder("\"connections\": [\"String[]\", []]}");

                /*int neighbourCount = 0;
                for (Connection c : connections) {
                    String connect = switch(c) {
                        case NORTH -> "N-0";
                        case EAST -> "E-0";
                        case SOUTH -> "S-0";
                        case WEST -> "W-0";
                    };
                    neighbourCount++;
                    neighbourString.append("\"").append(node.neighbours.get(c)).append("\"");
                    connectionString.append("\"").append(connect).append("\"");
                    if (neighbourCount < node.neighbours.size()) {
                        neighbourString.append(", ");
                        connectionString.append(", ");
                    }
                    else {
                        neighbourString.append("]],");
                        connectionString.append("]]}");
                    }
                }*/
                outputB.append(neighbourString);
                outputB.append(connectionString);
                if (nodeCount < quest.board.size())
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

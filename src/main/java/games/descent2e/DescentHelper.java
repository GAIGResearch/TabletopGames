package games.descent2e;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Component;
import core.components.Deck;
import core.components.GridBoard;
import core.properties.Property;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.actions.items.RerollAttributeTest;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.*;
import games.descent2e.components.tokens.DToken;
import utilities.LineOfSight;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.playersHash;
import static games.descent2e.components.Figure.Attribute.MovePoints;

public class DescentHelper {

    // Self-Contained Class for various Helper Functions
    // A lot of these were originally within DescentForwardModel, and were originally private
    // But once more classes needed the same code, were moved here to clean it up

    public static List<Integer> getMeleeTargets(DescentGameState dgs, Figure f) {

        // TODO: Check for Reach weapons, so far only affects monsters with Reach passive
        // If the figure has the Reach passive, they can attack up to two spaces away
        boolean reach = false;
        if (f instanceof Monster && ((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.REACH))
        {
            reach = true;
        }

        List<Integer> targets = new ArrayList<>();

        Pair<Integer, Integer> size = f.getSize();
        List<BoardNode> attackingTiles = new ArrayList<>();

        Vector2D currentLocation = f.getPosition();
        BoardNode anchorTile = dgs.masterBoard.getElement(currentLocation.getX(), currentLocation.getY());

        if (size.a > 1 || size.b > 1)
        {
            attackingTiles.addAll(getAttackingTiles(f.getComponentID(), anchorTile, attackingTiles));
        }
        else {
            attackingTiles.add(anchorTile);
        }

        // Find valid neighbours in master graph - used for melee attacks
        for (BoardNode currentTile : attackingTiles) {

            Set<BoardNode> neighbours = currentTile.getNeighbours().keySet();

            // Reach attacks can target up to two spaces away
            if (reach)
            {
                Set<BoardNode> neighboursOfNeighbours = new HashSet<>();
                for (BoardNode neighbour : currentTile.getNeighbours().keySet()) {
                    neighboursOfNeighbours.addAll(neighbour.getNeighbours().keySet());
                }
                neighbours = neighboursOfNeighbours;
                attackingTiles.forEach(neighbours::remove);
            }

            for (BoardNode neighbour : neighbours) {
                if (neighbour == null) continue;
                int neighbourID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
                if (neighbourID != -1) {
                    Figure other = (Figure) dgs.getComponentById(neighbourID);
                    // Checks to make sure that there is a line of sight before approving the attack action
                    if (hasLineOfSight(dgs, ((PropertyVector2D) currentTile.getProperty("coordinates")).values, ((PropertyVector2D) neighbour.getProperty("coordinates")).values)) {
                        if (f instanceof Monster && other instanceof Hero) {
                            // Monster attacks a hero
                            if (!targets.contains(other.getComponentID())) {
                                targets.add(other.getComponentID());
                            }
                        } else if (f instanceof Hero && other instanceof Monster) {
                            // Player attacks a monster

                            // Make sure that the Player only gets one instance of attacking the monster
                            // This was previously an issue when dealing with Large creatures that took up multiple adjacent spaces
                            if (!targets.contains(other.getComponentID())) {
                                targets.add(other.getComponentID());
                            }
                        }
                    }
                }
            }
        }

        return targets;
    }

    public static List<Integer> getRangedTargets(DescentGameState dgs, Figure f) {

        List<Integer> targets = new ArrayList<>();

        Pair<Integer, Integer> size = f.getSize();

        List<BoardNode> attackingTiles = new ArrayList<>();

        Vector2D currentLocation = f.getPosition();
        BoardNode anchorTile = dgs.masterBoard.getElement(currentLocation.getX(), currentLocation.getY());
        attackingTiles.add(anchorTile);

        if (size.a > 1 || size.b > 1)
        {
            attackingTiles.addAll(getAttackingTiles(f.getComponentID(), anchorTile, attackingTiles));
        }

        // Find valid neighbours of neighbours in master graph - used for ranged attacks
        for (BoardNode currentTile : attackingTiles) {
            Set<BoardNode> rangedTargets = currentTile.getNeighbours().keySet();

            // Only finds neighbours up to a set maximum range
            for (int i = 1; i < RangedAttack.MAX_RANGE; i++) {

                Set<BoardNode> startTargets = rangedTargets;

                for (BoardNode possibleTarget : startTargets) {
                    // Collects all possible neighbours of neighbours, and adds them to the list of potential target locations
                    Set<BoardNode> newTargets = new HashSet<>(possibleTarget.getNeighbours().keySet());
                    newTargets.addAll(rangedTargets);
                    rangedTargets = newTargets;
                }
            }

            // Prevents the attacker from trying to shoot itself
            attackingTiles.forEach(rangedTargets::remove);

            for (BoardNode neighbour : rangedTargets) {
                if (neighbour == null) continue;
                int neighbourID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
                if (neighbourID != -1) {
                    Figure other = (Figure) dgs.getComponentById(neighbourID);

                    // Checks to make sure that there is a line of sight before approving the attack action
                    if (hasLineOfSight(dgs, ((PropertyVector2D) currentTile.getProperty("coordinates")).values, ((PropertyVector2D) neighbour.getProperty("coordinates")).values)) {
                        if (f instanceof Monster && other instanceof Hero) {
                            // Monster attacks a hero
                            targets.add(other.getComponentID());
                        } else if (f instanceof Hero && other instanceof Monster) {
                            // Player attacks a monster

                            // Make sure that the Player only gets one instance of attacking the monster
                            // This was previously an issue when dealing with Large creatures that took up multiple adjacent spaces
                            if (!targets.contains(other.getComponentID())) {
                                targets.add(other.getComponentID());
                            }
                        }
                    }
                }
            }
        }

        return targets;
    }

    public static List<BoardNode> getAttackingTiles(Integer f, BoardNode startTile, List<BoardNode> attackingTiles) {

        List<BoardNode> newTiles = new ArrayList<>(attackingTiles);
        newTiles.add(startTile);

        for (BoardNode neighbour : startTile.getNeighbours().keySet()) {
            if (neighbour == null) continue;
            if (newTiles.contains(neighbour)) continue;
            int neighbourID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
            if (neighbourID == f)
            {
                newTiles.addAll(getAttackingTiles(f, neighbour, newTiles));
            }
        }

        Set<BoardNode> tempSet = new HashSet<>(newTiles);
        newTiles.clear();
        newTiles.addAll(tempSet);

        return newTiles;
    }

    public static boolean hasLineOfSight(DescentGameState dgs, Vector2D startPoint, Vector2D endPoint){
        int counter = 0;
        boolean hasLineOfSight = true;

        ArrayList<Vector2D> containedPoints = LineOfSight.bresenhamsLineAlgorithm(startPoint, endPoint);

        BoardNode startTile = dgs.masterBoard.getElement(startPoint.getX(), startPoint.getY());
        int start = ((PropertyInt) startTile.getProperty(playersHash)).value;

        BoardNode targetTile = dgs.masterBoard.getElement(endPoint.getX(), endPoint.getY());
        int target = ((PropertyInt) targetTile.getProperty(playersHash)).value;

        // For each coordinate in the line, check:
        // 1) Does the coordinate have its board node
        // 2) Is the board node empty (no character on location)
        // 3) Is the board node connected to previously checked board node
        // If any of these are false, then there is no LOS
        for (int i = 1; i < containedPoints.size(); i++){

            Vector2D previousPoint = containedPoints.get(i - 1);
            Vector2D point = containedPoints.get(i);

            // Check 1) Does the board node exist at this coordinate
            BoardNode currentTile = dgs.masterBoard.getElement(point.getX(), point.getY());
            if (currentTile == null){
                hasLineOfSight = false;
                break;
            }

            // Check 2) Is the board node empty (or, if either figure is large, not occupied by itself or the target)
            int owner = ((PropertyInt) currentTile.getProperty(playersHash)).value;
            if (owner != -1 && i != containedPoints.size() - 1){
                if (owner != target && owner != start){
                    hasLineOfSight = false;
                    break;
                }
            }

            // Check 3) Is the board node connected to previous board node
            BoardNode previousTile = dgs.masterBoard.getElement(previousPoint.getX(), previousPoint.getY());
            if (!previousTile.getNeighbours().containsKey(currentTile)){
                hasLineOfSight = false;
                break;
            }
        }

        if (hasLineOfSight)
            counter++;

        return (counter > 0);
    }

    public static List<AbstractAction> moveActions(DescentGameState dgs, Figure f) {

        Map<Vector2D, Pair<Double, List<Vector2D>>> allAdjacentNodes = getAllAdjacentNodes(dgs, f);
        //Map<Vector2D, Pair<Double, List<Vector2D>>> allPointOfInterests = getAllPointOfInterests(dgs, f);

        //allAdjacentNodes.putAll(allPointOfInterests);

        // get all potential rotations for the figure
        Map<Pair<Vector2D, Monster.Direction>, Pair<Double, List<Vector2D>>> allPossibleRotations = getPossibleRotationsForMoveActions(allAdjacentNodes, dgs, f);
        List<Move> actions = new ArrayList<>();
        for (Pair<Vector2D, Monster.Direction> loc : allPossibleRotations.keySet()) {
            if (allPossibleRotations.get(loc).a <= f.getAttributeValue(Figure.Attribute.MovePoints)) {
                Move myMoveAction = new Move(f.getComponentID(), allPossibleRotations.get(loc).b, loc.b);
                myMoveAction.updateDirectionID(dgs);
                if(myMoveAction.canExecute(dgs))
                    actions.add(myMoveAction);
            }
        }

        // Sorts the movement actions to always be in the same order (Clockwise NW to W, One Space then Multiple Spaces)
        actions.sort(Comparator.comparingInt(Move::getDirectionID));

        return new ArrayList<>(actions);
    }

    private static Map<Vector2D, Pair<Double,List<Vector2D>>> getAllPointOfInterests(DescentGameState dgs, Figure figure){

        ArrayList<Vector2D> pointsOfInterest = new ArrayList<>();
        Map<Vector2D, Pair<Double,List<Vector2D>>> movePointOfInterest = new HashMap<>();
        if (figure.getTokenType().equals("Monster")) {
            for (Hero h : dgs.heroes) {
                pointsOfInterest.add(h.getPosition());
            }

        } else if (figure.getTokenType().equals("Hero")) {
            for (List<Monster> monsterGroup : dgs.monsters) {
                for (Monster m : monsterGroup) {
                    pointsOfInterest.add(m.getPosition());
                }
            }

            for (DToken dToken : dgs.tokens){
                if (dToken.getPosition() != null){
                    pointsOfInterest.add(dToken.getPosition());
                }
            }
        }

        //For every point of interest find neighbours that are empty and add then as potential move spots
        for (Vector2D point : pointsOfInterest){
            BoardNode figureNode = dgs.masterBoard.getElement(point.getX(), point.getY());
            Set<BoardNode> neighbours = figureNode.getNeighbours().keySet();
            for (BoardNode neighbourNode : neighbours){
                PropertyInt figureOnLocation = (PropertyInt) neighbourNode.getProperty(playersHash);
                if (figureOnLocation.value == -1){
                    Vector2D loc = ((PropertyVector2D) neighbourNode.getProperty(coordinateHash)).values;

                    // TODO: use actual A* instead of 0
                    //Double movementCost =  a_star_distance(figureNode, neighboutNode)
                    Double movementCost = 1.0;
                    List<Vector2D> path = new ArrayList<Vector2D>() {{add(loc);}};  // TODO full path
                    movePointOfInterest.put(loc, new Pair<>(movementCost, path));
                }
            }
        }

        return movePointOfInterest;
    }

    private static HashMap<Vector2D, Pair<Double,List<Vector2D>>> getAllAdjacentNodes(DescentGameState dgs, Figure figure){
        Vector2D figureLocation = figure.getPosition();
        BoardNode figureNode = dgs.getMasterBoard().getElement(figureLocation.getX(), figureLocation.getY());
        String figureType = figure.getTokenType();

        //<Board Node, Cost to get there>
        HashMap<BoardNode, Pair<Double,List<Vector2D>>> expandedBoardNodes = new HashMap<>();
        HashMap<BoardNode, Pair<Double,List<Vector2D>>> nodesToBeExpanded = new HashMap<>();
        HashMap<BoardNode, Pair<Double,List<Vector2D>>> allAdjacentNodes = new HashMap<>();

        nodesToBeExpanded.put(figureNode, new Pair<>(0.0, new ArrayList<>()));
        while (!nodesToBeExpanded.isEmpty()){
            // Pick a node to expand, and remove it from the map
            Map.Entry<BoardNode,Pair<Double,List<Vector2D>>> entry = nodesToBeExpanded.entrySet().iterator().next();
            BoardNode expandingNode = entry.getKey();
            double expandingNodeCost = entry.getValue().a;
            List<Vector2D> expandingNodePath = entry.getValue().b;
            nodesToBeExpanded.remove(expandingNode);

            // Go through all the neighbour nodes
            HashMap<BoardNode, Double> neighbours = expandingNode.getNeighbours();
            for (BoardNode neighbour : neighbours.keySet()){
                Vector2D loc = ((PropertyVector2D) neighbour.getProperty(coordinateHash)).values;

                double costToMoveToNeighbour = expandingNode.getNeighbourCost(neighbour);
                double totalCost = expandingNodeCost + costToMoveToNeighbour;
                List<Vector2D> totalPath = new ArrayList<>(expandingNodePath);
                if (totalPath.contains(loc)) continue;
                totalPath.add(loc);
                boolean isFriendly = false;
                boolean isEmpty = DescentTypes.TerrainType.isWalkableTerrain(neighbour.getComponentName());

                PropertyInt figureOnLocation = (PropertyInt)neighbour.getProperty(playersHash);
                if (figureOnLocation.value != -1) {
                    isEmpty = false;
                    Figure neighbourFigure = (Figure) dgs.getComponentById(figureOnLocation.value);

                    if (neighbourFigure != null) {
                        // If our current figure is the same as our neighbour (in the case of large figures), we can move into the neighbour tile
                        if (figure.equals(neighbourFigure)) {
                            isEmpty = true;
                        }
                        // If our current figure is the same team as the neighbour (Hero or Monster), we can move through it
                        else if (figureType.equals(neighbourFigure.getTokenType())) {
                            isFriendly = true;
                        }
                        // If our current figure is a monster with the Scamper passive, we can move through Hero figures as if they were friendly
                        else if (figureType == "Monster") {
                            if ((((Monster) figure).hasPassive(MonsterAbilities.MonsterPassive.SCAMPER)) && neighbourFigure.getTokenType().equals("Hero"))
                                isFriendly = true;
                        }
                    }
                    // If, for whatever reason, our Heroes are allowed to ignore enemies entirely when moving
                    // We can move through all other figures as if they were friendly
                    if (figure.canIgnoreEnemies())
                    {
                        isFriendly = true;
                    }
                }

                if (isFriendly){
                    //if the node is friendly and not expanded - add it to the expansion list
                    if(!expandedBoardNodes.containsKey(neighbour)){
                        nodesToBeExpanded.put(neighbour, new Pair<>(totalCost, totalPath));
                        //if the node is friendly and expanded but the cost was higher - add it to the expansion list
                    } else if (expandedBoardNodes.containsKey(neighbour) && expandedBoardNodes.get(neighbour).a > totalCost){
                        expandedBoardNodes.remove(neighbour);
                        nodesToBeExpanded.put(neighbour, new Pair<>(totalCost, totalPath));
                    }
                } else if (isEmpty) {
                    //if the node is empty - add it to adjacentNodeList
                    if (!allAdjacentNodes.containsKey(neighbour) || allAdjacentNodes.get(neighbour).a > totalCost){
                        allAdjacentNodes.put(neighbour, new Pair<>(totalCost, totalPath));
                    }
                }
                expandedBoardNodes.put(neighbour, new Pair<>(totalCost, totalPath));
            }
        }

        //Return list of coordinates
        HashMap<Vector2D, Pair<Double,List<Vector2D>>> allAdjacentLocations = new HashMap<>();
        for (BoardNode boardNode : allAdjacentNodes.keySet()){
            Vector2D loc = ((PropertyVector2D) boardNode.getProperty(coordinateHash)).values;
            allAdjacentLocations.put(loc, allAdjacentNodes.get(boardNode));
        }

        return allAdjacentLocations;
    }

    // Pair<final position, final orientation> -> pair<movement cost to get there, list of positions to travel through to get there>
    private static Map<Pair<Vector2D, Monster.Direction>, Pair<Double,List<Vector2D>>> getPossibleRotationsForMoveActions(Map<Vector2D, Pair<Double,List<Vector2D>>> allAdjacentNodes, DescentGameState dgs, Figure figure){

        Map<Pair<Vector2D, Monster.Direction>, Pair<Double,List<Vector2D>>> possibleRotations = new HashMap<>();

        // Go through all adjacent nodes
        for (Map.Entry<Vector2D, Pair<Double,List<Vector2D>>> e : allAdjacentNodes.entrySet()) {
            Vector2D nodeLoc = e.getKey();

            if (figure.getSize().a > 1 || figure.getSize().b > 1) {
                // Only monsters can have a size bigger than 1x1
                Monster m = (Monster) figure;
                Pair<Integer, Integer> monsterSize = m.getSize();

                // Check all possible orientations with this position as the anchor if figure is bigger than 1x1
                // If all spaces occupied are legal, then keep valid options
                for (Monster.Direction d: Monster.Direction.values()) {
                    Vector2D topLeftCorner = m.applyAnchorModifier(nodeLoc.copy(), d);
                    Pair<Integer, Integer> mSize = monsterSize.copy();
                    if (d.ordinal() % 2 == 1) mSize.swap();

                    boolean legal = true;
                    for (int j = 0; j < mSize.a; j++) {
                        for (int i = 0; i < mSize.b; i++) {
                            Vector2D space = new Vector2D(topLeftCorner.getX() + j, topLeftCorner.getY() + i);

                            // If the space is not a neighbour of the current position, then it is not a legal move
                            if (i == 1 || j == 1) {
                                BoardNode target = dgs.getMasterBoard().getElement(topLeftCorner);
                                if (target == null) {
                                    legal = false;
                                    break;
                                }
                                Set<BoardNode> neighbours = target.getNeighbours().keySet();
                                if (!neighbours.contains(dgs.getMasterBoard().getElement(space))) {
                                    legal = false;
                                    break;
                                }
                            }

                            BoardNode spaceOccupied = dgs.getMasterBoard().getElement(space.getX(), space.getY());

                            if (spaceOccupied != null)
                            {
                                PropertyInt figureOnLocation = (PropertyInt) spaceOccupied.getProperty(playersHash);
                                if (!DescentTypes.TerrainType.isWalkableTerrain(spaceOccupied.getComponentName()) ||
                                        figureOnLocation.value != -1 && figureOnLocation.value != figure.getComponentID())
                                {
                                    legal = false;
                                    break;
                                }
                                if (figureOnLocation.value == figure.getComponentID())
                                {
                                    // DOWN = 0, LEFT = 1, UP = 2, RIGHT = 3
                                    // If they are opposite directions, the difference will be 2
                                    // This prevents a wasted action of spinning the figure around without actually moving
                                    if (Math.abs(((Monster) figure).getOrientation().ordinal() - d.ordinal()) == 2)
                                    {
                                        legal = false;
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                legal = false;
                                break;
                            }
                        }
                    }
                    if (legal) {
                        possibleRotations.put(new Pair<>(nodeLoc, d), e.getValue());
                    }
                }
            } else {
                possibleRotations.put(new Pair<>(nodeLoc, Monster.Direction.getDefault()), e.getValue());
            }
        }

        return possibleRotations;
    }

    public static boolean inRange(Vector2D origin, Vector2D target, int range) {
        return (Math.abs(origin.getX() - target.getX()) <= range) && (Math.abs(origin.getY() - target.getY()) <= range);
    }
    public static Vector2D getRange(Vector2D origin, Vector2D target) {
        return new Vector2D(Math.abs(origin.getX() - target.getX()), Math.abs(origin.getY() - target.getY()));
    }
    public static double getDistance(Vector2D origin, Vector2D target) {
        return Math.sqrt(Math.pow(origin.getX() - target.getX(), 2) + Math.pow(origin.getY() - target.getY(), 2));
    }

    public static DescentTypes.AttackType getAttackType(Figure f)
    {
        DescentTypes.AttackType attackType = DescentTypes.AttackType.NONE;
        if (f instanceof Hero) {
            // Examines the Hero's equipment to see what their weapon's range is
            Deck<DescentCard> myEquipment = ((Hero) f).getHandEquipment();
            for (DescentCard equipment : myEquipment.getComponents()) {
                DescentTypes.AttackType temp = equipment.getAttackType();

                // Checks if the Hero can make a melee attack, ranged attack, or both with their current equipment
                if (temp != DescentTypes.AttackType.NONE) {
                    if (attackType == DescentTypes.AttackType.NONE) {
                        attackType = temp;
                    } else if (attackType != temp) {
                        attackType = DescentTypes.AttackType.BOTH;
                    }
                }
            }
        }
        if (f instanceof Monster) {
            attackType = ((Monster) f).getAttackType();
        }
        return attackType;
    }

    public static int reroll(DescentGameState dgs, DescentDice dice) {
        DiceType type = dice.getColour();
        DicePool roll = DicePool.constructDicePool(new HashMap<DiceType, Integer>() {{
            put(type, 1);
        }});
        roll.roll((dgs.getRnd()));
        return roll.getDice(0).getFace();
    }

    public static List<DescentAction> getOtherEquipmentActions(Hero figure) {
        List<DescentAction> actions = new ArrayList<>();
        for (DescentCard equipment : figure.getOtherEquipment().getComponents()) {
            Property passive = equipment.getProperty("passive");
            if (passive != null) {
                String pass = String.valueOf(equipment.getProperty("passive"));
                if (pass.contains(";")) {
                    String[] passives = pass.split(";");
                    for (String s : passives) {
                        if (s.contains("Effect:")) {
                            String[] effect = s.split(":");
                            switch (effect[1]) {
                                case "Reroll":
                                    switch (effect[2]) {
                                        case "AttributeTest":
                                            actions.add(new RerollAttributeTest(figure.getComponentID(), equipment.getComponentID()));
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        return actions;
    }

    public static boolean collision(DescentGameState dgs)
    {
        // Flags up a collision warning if two opposing figures are on the same tile
        // There were occasional instances of where Heroes would move onto Monsters' tiles
        // This is for debugging purposes only
        boolean collision = false;
        List<Pair<Pair<Hero, Monster>, Vector2D>> collisions = new ArrayList<>();
        for (Hero h : dgs.heroes)
        {
            if (h.isOffMap()) continue;
            for (List<Monster> monsterGroup : dgs.monsters)
            {
                for (Monster m : monsterGroup)
                {
                    if (h.getPosition().equals(m.getPosition()))
                    {
                        collision = true;
                        collisions.add(new Pair<>(new Pair<>(h, m), h.getPosition()));
                    }
                }
            }
        }

        // If there are collisions, announce every instance of them
//        if(collision)
//        {
//            for (Pair<Pair<Hero, Monster>, Vector2D> c : collisions)
//            {
//                System.out.println("Collision between " + c.a.a.getComponentName() + " and " + c.a.b.getComponentName() + " at " + c.b);
//            }
//        }
        return collision;
    }

    public static int bfsLee(DescentGameState dgs, Vector2D start, Vector2D end) {
        // Breadth-First Search Lee Algorithm
        // Used to find the shortest path between two points
        // Used for the Heroes/Monsters to find the shortest path to their target enemy

        GridBoard board = dgs.getMasterBoard();

        // Ensure that both start and end points are valid
        if (board.getElement(start) == null || board.getElement(end) == null)
            return -1;

        boolean[][] visited = new boolean[board.getWidth()][board.getHeight()];
        visited[start.getX()][start.getY()] = true;

        // Distance from the source cell is initialized to 0
        Queue<Pair<BoardNode, Integer>> queue = new LinkedList<>();
        queue.add(new Pair<BoardNode, Integer>((BoardNode) board.getElement(start), 0));

        // BFS starting from start cell
        while (!queue.isEmpty())
        {
            Pair<BoardNode, Integer> curr = queue.peek();
            BoardNode node = curr.a;

            Vector2D currLoc = ((PropertyVector2D) node.getProperty(coordinateHash)).values;

            // If we have reached the destination cell, we are done
            if (currLoc.equals(end))
                return curr.b;

            // Otherwise dequeue the front cell in the queue and enqueue its adjacent cells
            queue.poll();

            for (BoardNode neighbour : node.getNeighbours().keySet())
            {
                if (neighbour == null) continue;
                Vector2D nextLoc = ((PropertyVector2D) neighbour.getProperty(coordinateHash)).values;
                if (!checkValid(nextLoc.getX(), nextLoc.getY(), board)) continue;
                if (!visited[nextLoc.getX()][nextLoc.getY()])
                {
                    visited[nextLoc.getX()][nextLoc.getY()] = true;
                    queue.add(new Pair<>(neighbour, curr.b + 1));
                }
            }
        }

        return -1;
    }

    // Check whether given cell(row,col) is a valid cell or not
    static boolean checkValid(int row, int col, GridBoard board)
    {
        return ((row >= 0) && (row < board.getWidth()) && (col >= 0) && (col < board.getHeight()));
    }

    public static String gridCounter(DescentGameState dgs, int figureId, Vector2D startPos, List<Vector2D> positionsTravelled) {
        BoardNode[] grid = dgs.getMasterBoard().flattenGrid();
        int counter = 0;
        StringBuilder coords = new StringBuilder();
        for (Component node : grid) {
            if (node != null) {
                if (((PropertyInt) node.getProperty(playersHash)).value == figureId) {
                    counter++;
                    coords.append(node.getProperty("coordinates").toString()+"; ");
                }
            }
        }

        if (figureId != -1) {
            Figure f = (Figure) dgs.getComponentById(figureId);
            if (counter > (f.getSize().a * f.getSize().b)) {
//                System.out.println("Figure " + figureId + " has more nodes than their size allows: " + counter + " > " + (f.getSize().a * f.getSize().b) + " at " + coords);
                throw new AssertionError("Figure " + figureId + " has more nodes than their size allows: " + counter + " > " + (f.getSize().a * f.getSize().b) + " at " + coords);
            }
        }

        return "Player " + figureId + " has " + counter + " node occurrences: " + coords;
    }

    public static boolean canStillMove(Figure f)
    {
        // Used to check whether it should be legal to EndTurn or not if we still have movement available
        // If we have more MovePoints than our normal maximum
        // Or, if we have any MovePoints left and haven't moved yet
        // Then return true, i.e. do not allow the player to EndTurn
        return (f.getAttribute(MovePoints).getValue() >= f.getAttributeMax(MovePoints) ||
                (f.getAttribute(MovePoints).getValue() != 0 && !f.hasMoved()));
    }

    public static double distanceFromNearestAlly(DescentGameState dgs, Figure f, List<Figure> allies) {
        double minDistance = Double.MAX_VALUE;
        for (Figure ally : allies) {
            if (ally.equals(f)) continue;
            int distance = bfsLee(dgs, f.getPosition(), ally.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        if (minDistance == Double.MAX_VALUE) return -1;
        return minDistance;
    }

    public static double distanceFromFurthestAlly(DescentGameState dgs, Figure f, List<Figure> allies) {
        double maxDistance = 0;
        for (Figure ally: allies) {
            if (ally.equals(f)) continue;
            int distance = bfsLee(dgs, f.getPosition(), ally.getPosition());
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        if (maxDistance == 0) return -1;
        return maxDistance;
    }

    public static double averageDistanceFromAllies(DescentGameState dgs, Figure f, List<Figure> allies) {
        double totalDistance = 0;
        int count = 0;
        for (Figure ally: allies) {
            if (ally.equals(f)) continue;
            int distance = bfsLee(dgs, f.getPosition(), ally.getPosition());
            totalDistance += distance;
            count++;
        }
        if (totalDistance == 0) return -1;
        return totalDistance / count;
    }

    public static int getFirstMissingIndex(List<Monster> figures) {
        int i = 1;
        // We assume that the first figure (index 0) is always the Master
        // We are only interested in the Minions
        for (i = 1; i < figures.size(); i++) {
            if (!figures.get(i).getName().contains(String.valueOf(i)))
            {
                return i;
            }
        }
        return i;
    }

    public static int getFigureIndex(DescentGameState dgs, Figure f)
    {
        if (f instanceof Hero)
        {
            return dgs.getHeroes().indexOf(f);
        }
        else
        {
            Monster m = (Monster) f;

            for (List<Monster> monsterGroup: dgs.getMonsters()) {
                if (monsterGroup.contains(m)) {
                    int index = monsterGroup.indexOf(m);
                    // The Master monster is always first on the list
                    // If a Minion is first, then that means the Master is dead and should be accounted for
                    if (monsterGroup.get(0).getName().contains("minion"))
                    {
                        index++;
                    }
                    return index;
                }
            }
        }
        return -1;
    }

    public static void forcedFatigue(DescentGameState dgs, Figure f, String source) {
        if (!f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) {
            f.getAttribute(Figure.Attribute.Fatigue).increment();
        }
        else {
            f.getAttribute(Figure.Attribute.Health).decrement();
            if (f.getAttribute(Figure.Attribute.Health).isMinimum()) {
                int index = getFigureIndex(dgs, f);
                figureDeath(dgs, f);
                dgs.addDefeatedFigure(f, index, source);
            }
        }
    }

    public static void figureDeath(DescentGameState dgs, Figure f) {
        // All conditions are removed when a figure is defeated
        f.removeAllConditions();
        f.addActionTaken("Defeated");

        //System.out.println(defender.getComponentName() + " defeated!");

        // and the Figure is now defeated
        if (f instanceof Hero) {
            ((Hero)f).setDefeated(dgs,true);
            // Overlord may draw a card TODO
        } else {
            // A monster
            Monster m = (Monster) f;

            m.setAttributeToMin(Figure.Attribute.Health);

            // Remove from board
            Move.remove(dgs, m);

            // Remove from state lists
            for (List<Monster> monsterGroup: dgs.getMonsters()) {
                monsterGroup.remove(m);
            }
        }
    }
}

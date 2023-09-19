package games.descent2e;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.actions.Move;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import games.descent2e.components.tokens.DToken;
import utilities.LineOfSight;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class DescentHelper {

    // Self-Contained Class for various Helper Functions
    // A lot of these were originally within DescentForwardModel, and were originally private
    // But once more classes needed the same code, were moved here to clean it up

    public static List<Integer> getMeleeTargets(DescentGameState dgs, Figure actingFigure) {
        List<Integer> targets = new ArrayList<>();

        Vector2D loc = actingFigure.getPosition();
        GridBoard board = dgs.getMasterBoard();
        List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);

        if (actingFigure instanceof Hero) {
            // Get all monsters that are adjacent to us, and we have line of sight to
            for (List<Monster> monsterGroup : dgs.getMonsters()) {
                for (Monster monster : monsterGroup) {
                    // We only need to check if all 8 adjacent tiles have a monster on them
                    // If we already have 8 monsters on the list, we can stop
                    if (targets.size() >= neighbours.size())
                        break;
                    if (neighbours.contains(monster.getPosition())) {
                        if (hasLineOfSight(dgs, loc, monster.getPosition()))
                            targets.add(monster.getComponentID());
                    }
                }
            }
        }
        else if (actingFigure instanceof Monster) {
            // Get all heroes that are adjacent to us, and we have line of sight to
            for (Hero hero : dgs.getHeroes()) {
                if (neighbours.contains(hero.getPosition())) {
                    if (hasLineOfSight(dgs, loc, hero.getPosition()))
                        targets.add(hero.getComponentID());
                }
            }
        }
        return targets;
    }

    public static List<Integer> getRangedTargets(DescentGameState dgs, Figure actingFigure) {
        List<Integer> targets = new ArrayList<>();

        Vector2D loc = actingFigure.getPosition();

        if (actingFigure instanceof Hero) {
            for (List<Monster> monsterGroup : dgs.getMonsters()) {
                for (Monster monster : monsterGroup) {
                    Vector2D monsterPos = monster.getPosition();
                    if (hasLineOfSight(dgs, loc, monsterPos)) {
                        if (inRange(loc, monsterPos, RangedAttack.MAX_RANGE)) {
                            if (targets.contains(monster.getComponentID()))
                                continue;
                            targets.add(monster.getComponentID());
                        }
                    }
                }
            }
        }
        else if (actingFigure instanceof Monster) {
            for (Hero hero : dgs.getHeroes()) {
                Vector2D heroPos = hero.getPosition();
                if (hasLineOfSight(dgs, loc, heroPos)) {
                    if (inRange(loc, heroPos, RangedAttack.MAX_RANGE)) {
                        if (targets.contains(hero.getComponentID()))
                            continue;
                        targets.add(hero.getComponentID());
                    }
                }
            }
        }
        return targets;
    }

    public static boolean hasLineOfSight(DescentGameState dgs, Vector2D startPoint, Vector2D endPoint){

        int counter = 0;
        boolean hasLineOfSight = true;

        ArrayList<Vector2D> containedPoints = LineOfSight.bresenhamsLineAlgorithm(startPoint, endPoint);

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

            // Check 2) Is the board node empty
            Integer owner = ((PropertyInt) currentTile.getProperty(playersHash)).value;
            if (owner != -1 && i != containedPoints.size() - 1){
                hasLineOfSight = false;
                break;
            }

            // Check 3) Is the board node connected to previous board node
            BoardNode previousTile = dgs.masterBoard.getElement(previousPoint.getX(), previousPoint.getY());
            if (!previousTile.getNeighbours().keySet().contains(currentTile.getComponentID())){
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
                Move myMoveAction = new Move(f, allPossibleRotations.get(loc).b, loc.b);
                myMoveAction.updateDirectionID(dgs);
                actions.add(myMoveAction);
            }
        }

        // Sorts the movement actions to always be in the same order (Clockwise NW to W, One Space then Multiple Spaces)
        Collections.sort(actions, Comparator.comparingInt(Move::getDirectionID));

        List<AbstractAction> sortedActions = new ArrayList<>();
        sortedActions.addAll(actions);

        return sortedActions;
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
                totalPath.add(loc);
                boolean isFriendly = false;
                boolean isEmpty = DescentTypes.TerrainType.isWalkableTerrain(neighbour.getComponentName());

                PropertyInt figureOnLocation = (PropertyInt)neighbour.getProperty(playersHash);
                if (figureOnLocation.value != -1) {
                    isEmpty = false;
                    Figure neighbourFigure = (Figure) dgs.getComponentById(figureOnLocation.value);

                    // If our current figure is the same as our neighbour (in the case of large figures), we can move into the neighbour tile
                    if (figure.equals(neighbourFigure)) {
                        isEmpty = true;
                    }
                    // If our current figure is the same team as the neighbour (Hero or Monster), we can move through it
                    else if (figureType.equals(neighbourFigure.getTokenType())) {
                        isFriendly = true;
                    }
                    // If our current figure is a monster with the Scamper passive, we can move through Hero figures as if they were friendly
                    else if (figureType == "Monster")
                    {
                        if ((((Monster) figure).hasPassive("Scamper")) && neighbourFigure.getTokenType().equals("Hero"))
                            isFriendly = true;
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
                            BoardNode spaceOccupied = dgs.getMasterBoard().getElement(topLeftCorner.getX() + j, topLeftCorner.getY() + i);
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

}

package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.*;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.playersHash;
import static games.descent2e.actions.Triggers.*;
import static games.descent2e.actions.herofeats.HeroicFeatExtraMovement.ExtraMovePhase.*;
import static games.descent2e.actions.herofeats.HeroicFeatExtraMovement.Interrupters.*;

public class HeroicFeatExtraMovement extends DescentAction implements IExtendedSequence {

    // Syndrael Heroic Feat
    String heroName = "Syndrael";

    enum Interrupters {
        HERO, ALLY, OTHERS, ALL
    }

    public enum ExtraMovePhase {
        NOT_STARTED,
        SWAP(ANYTIME, HERO),
        PRE_HERO_MOVE(ANYTIME, HERO),
        POST_HERO_MOVE,
        PRE_ALLY_MOVE(ANYTIME, ALLY),
        POST_ALLY_MOVE,
        ALL_DONE;

        public final Triggers interrupt;
        public final HeroicFeatExtraMovement.Interrupters interrupters;

        ExtraMovePhase(Triggers interruptType, HeroicFeatExtraMovement.Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        ExtraMovePhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    HeroicFeatExtraMovement.ExtraMovePhase phase = NOT_STARTED;
    int heroPlayer;
    int allyPlayer;
    int interruptPlayer;
    Hero hero;
    Hero targetAlly;
    boolean swapped, swapOption = false;
    public HeroicFeatExtraMovement(Hero hero, Hero targetAlly) {
        super(Triggers.HEROIC_FEAT);
        this.hero = hero;
        this.targetAlly = targetAlly;
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String allyName = targetAlly.getName().replace("Hero: ", "");

        return String.format("Heroic Feat: " + heroName + " and " + allyName + " make a free move action");
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        List<AbstractAction> retVal = new ArrayList<>();
        List<AbstractAction> moveActions = new ArrayList<>();
        switch (phase) {
            case SWAP:
                // If we have not yet chosen if we want to swap, we can do so now
                // This ensures we are only asked once
                if (!swapOption) {
                    retVal.add(new SwapOrder(this, hero, targetAlly, false));
                    retVal.add(new SwapOrder(this, hero, targetAlly, true));
                }
                break;
            case PRE_HERO_MOVE:
                moveActions = moveActions(dgs, hero);
                if (!moveActions.isEmpty())
                {
                    retVal.add(new StopMove(hero));
                    retVal.addAll(moveActions);
                }
                break;
            case PRE_ALLY_MOVE:
                moveActions = moveActions(dgs, targetAlly);
                if (!moveActions.isEmpty())
                {
                    retVal.add(new StopMove(targetAlly));
                    retVal.addAll(moveActions);
                }
                break;
            default:
                break;
        }

        return retVal;
    }

    // moveAction, getAllAdjacentNodes and getPossibleRotationsForMoveActions
    // are all identical to DescentForwardModel's code for obtaining the MoveActions
    private List<AbstractAction> moveActions(DescentGameState dgs, Figure f) {

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
    private HashMap<Vector2D, Pair<Double,List<Vector2D>>> getAllAdjacentNodes(DescentGameState dgs, Figure figure){
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
            HashMap<Integer, Double> neighbours = expandingNode.getNeighbours();
            for (Integer neighbourID : neighbours.keySet()){
                BoardNode neighbour = (BoardNode) dgs.getComponentById(neighbourID);
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
                    if (figureType.equals(neighbourFigure.getTokenType())) {
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
    private Map<Pair<Vector2D, Monster.Direction>, Pair<Double,List<Vector2D>>> getPossibleRotationsForMoveActions(Map<Vector2D, Pair<Double,List<Vector2D>>> allAdjacentNodes, DescentGameState dgs, Figure figure){

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
                            if (spaceOccupied != null) {
                                PropertyInt figureOnLocation = (PropertyInt) spaceOccupied.getProperty(playersHash);
                                if (!DescentTypes.TerrainType.isWalkableTerrain(spaceOccupied.getComponentName()) ||
                                        figureOnLocation.value != -1 && figureOnLocation.value != figure.getComponentID()) {
                                    legal = false;
                                    break;
                                }
                            } else {
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

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        // after the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return (phase == ALL_DONE);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);

        heroPlayer = hero.getOwnerId();
        allyPlayer = targetAlly.getOwnerId();

        phase = SWAP;
        interruptPlayer = heroPlayer;

        hero.setAttributeToMax(Figure.Attribute.MovePoints);
        targetAlly.setAttributeToMax(Figure.Attribute.MovePoints);

        movePhaseForward(dgs);

        return true;
    }

    private void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                //System.out.println("Interrupt for player " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == heroPlayer) {
                    // we have completed the loop, and start again with the attacking player
                    executePhase(state);
                    interruptPlayer = heroPlayer;
                }
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (phase.interrupt == null || phase.interrupters == null) return false;
        // first we see if the interruptPlayer is one who may interrupt
        switch (phase.interrupters) {
            case HERO:
                if (interruptPlayer != heroPlayer)
                    return false;
                break;
            case ALLY:
                if (interruptPlayer != allyPlayer)
                    return false;
                break;
            case OTHERS:
                if (interruptPlayer == heroPlayer)
                    return false;
                break;
            case ALL:
                // always fine
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return !_computeAvailableActions(state).isEmpty();
    }

    private void executePhase(DescentGameState state) {
        //System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
            //throw new AssertionError("Should never be executed");
            case SWAP:
                // If we chose not to swap, move to PRE_HERO_MOVE
                if (!swapped)
                    phase = PRE_HERO_MOVE;
                else
                    phase = PRE_ALLY_MOVE;
                break;
            case PRE_HERO_MOVE:
                phase = POST_HERO_MOVE;
                break;
            case POST_HERO_MOVE:
                if (!swapped)
                    phase = PRE_ALLY_MOVE;
                else {
                    hero.setFeatAvailable(false);
                    phase = ALL_DONE;
                }
                break;
            case PRE_ALLY_MOVE:
                phase = POST_ALLY_MOVE;
                break;
            case POST_ALLY_MOVE:
                if (!swapped) {
                    hero.setFeatAvailable(false);
                    phase = ALL_DONE;
                }
                else
                    phase = PRE_HERO_MOVE;
                break;
        }
        // and reset interrupts
    }

    @Override
    public HeroicFeatExtraMovement copy() {
        HeroicFeatExtraMovement retVal = new HeroicFeatExtraMovement(hero, targetAlly);
        retVal.phase = phase;
        retVal.interruptPlayer = interruptPlayer;
        retVal.heroPlayer = heroPlayer;
        retVal.allyPlayer = allyPlayer;
        retVal.swapOption = swapOption;
        retVal.swapped = swapped;
        return retVal;
    }

    public boolean equals(Object obj) {

        if (obj instanceof HeroicFeatExtraMovement) {
            HeroicFeatExtraMovement other = (HeroicFeatExtraMovement) obj;
            return other.hero.equals(hero) && other.targetAlly.equals(targetAlly) &&
                    other.phase == phase && other.interruptPlayer == interruptPlayer &&
                    other.heroPlayer == heroPlayer && other.allyPlayer == allyPlayer &&
                    other.swapOption == swapOption && other.swapped == swapped;
        }
        return false;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return hero.getName().contains(heroName) && hero.isFeatAvailable();
    }

    // Allows us to choose if we should swap the order of the Syndrael and the chosen ally
    // To decide who moves first
    public void swap(boolean swap) {
        swapped = swap;
    }
    public boolean getSwapped() {
        return swapped;
    }
    public void setSwapOption(boolean swap) {
        this.swapOption = swap;
    }
    public boolean getSwapOption() {
        return swapOption;
    }
}

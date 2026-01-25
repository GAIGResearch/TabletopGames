package games.root.components;

import core.CoreConstants;
import core.components.Component;
import games.root.RootParameters;

import java.util.*;

@SuppressWarnings("unused")
public class RootBoardNodeWithRootEdges extends Component {
    public final String identifier;
    protected final Map<RootEdge, RootBoardNodeWithRootEdges> neighbourRootEdgeMapping;  // Neighbours mapping to RootEdge object encapsulating RootEdge information, connecting this node to the one in the map key
    protected final boolean corner;
    protected int catWarriors = 0;
    protected int eyrieWarriors = 0;
    protected int woodlandWarriors = 0;
    protected int vagabond = 0;
    protected int roost = 0;
    protected int workshops = 0;
    protected int sawmill = 0;
    protected int recruiter = 0;
    protected boolean base = false;
    protected int wood = 0;
    protected boolean sympathy = false;
    protected boolean keep = false;
    protected boolean ruins = false;
    protected int currentBuildings = 0;
    protected int maxBuildings = 4;

    public int rulerID;

    public enum ClearingType {
        MOUSE, FOX, RABBIT, Forrest
    }

    private RootParameters.ClearingTypes clearingType;
    private int x;
    private int y;

    public RootBoardNodeWithRootEdges(boolean corner, String identifier) {
        super(CoreConstants.ComponentType.BOARD_NODE, identifier);
        neighbourRootEdgeMapping = new HashMap<>();
        this.corner = corner;
        this.identifier = identifier;
    }

    public RootBoardNodeWithRootEdges(boolean corner, String identifier, RootParameters.ClearingTypes clearingType, int maxBuildings){
        super(CoreConstants.ComponentType.BOARD_NODE, identifier);
        neighbourRootEdgeMapping = new HashMap<>();
        this.corner = corner;
        this.identifier = identifier;
        this.clearingType = clearingType;
        this.maxBuildings = maxBuildings;
    }

    protected RootBoardNodeWithRootEdges(int owner, int ID, boolean corner, String identifier) {
        super(CoreConstants.ComponentType.BOARD_NODE, identifier, ID);
        setOwnerId(owner);
        neighbourRootEdgeMapping = new HashMap<>();
        this.corner = corner;
        this.identifier = identifier;
    }

    public boolean getCorner() {
        return corner;
    }

    public int getRoost() {
        return roost;
    }

    /**
     * Adds a neighbour for this node.
     *
     * @param neighbour - new neighbour of this node.
     */
    public void addNeighbour(RootBoardNodeWithRootEdges neighbour, RootEdge RootEdge) {
        neighbourRootEdgeMapping.put(RootEdge, neighbour);
        neighbour.neighbourRootEdgeMapping.put(RootEdge, this);
    }

    /**
     * Removes neighbour of this node.
     *
     * @param neighbour - neighbour to remove.
     */
    public void removeNeighbour(RootBoardNodeWithRootEdges neighbour, RootEdge RootEdge) {
        neighbourRootEdgeMapping.remove(RootEdge);
        neighbour.neighbourRootEdgeMapping.remove(RootEdge);
    }

    /**
     * @return the neighbours of this node.
     */
    public Set<RootBoardNodeWithRootEdges> getNeighbours() {
        return new HashSet<>(neighbourRootEdgeMapping.values());
    }

    public Map<RootEdge, RootBoardNodeWithRootEdges> getNeighbourRootEdgeMapping() {
        return neighbourRootEdgeMapping;
    }

    public Set<RootEdge> getRootEdges() {
        return new HashSet<>(neighbourRootEdgeMapping.keySet());
    }

    public void setMaxBuildings(int setTo) {
        maxBuildings = setTo;
    }

    /**
     * Copies all node properties to a new instance of this node.
     *
     * @return - a new instance of this node.
     */
    @Override
    public RootBoardNodeWithRootEdges copy() {
        // WARNING: DO not copy this directly, the GraphBoard copies it to correctly assign neighbour references!
        return null;
    }

    @Override
    public String toString() {
        return "{id: " + componentID + "; owner: " + ownerId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RootBoardNodeWithRootEdges that)) return false;
        boolean tmp = corner == that.corner &&
                wood == that.wood &&
                keep == that.keep &&
                clearingType == that.clearingType &&
                catWarriors == that.catWarriors &&
                workshops == that.workshops &&
                sawmill == that.sawmill &&
                recruiter == that.recruiter &&
                roost == that.roost &&
                eyrieWarriors == that.eyrieWarriors &&
                woodlandWarriors == that.woodlandWarriors &&
                base == that.base &&
                vagabond == that.vagabond &&
                ruins == that.ruins &&
                Objects.equals(identifier, that.identifier);
        return Objects.equals(neighbourRootEdgeMapping, that.neighbourRootEdgeMapping) && tmp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, ownerId, identifier, corner, currentBuildings, maxBuildings, clearingType, rulerID, catWarriors, keep, wood, workshops, sawmill, recruiter, eyrieWarriors, roost, woodlandWarriors, sympathy, base, vagabond, ruins);
    }

    public RootBoardNodeWithRootEdges getNeighbour(RootEdge RootEdge) {
        return neighbourRootEdgeMapping.get(RootEdge);
    }

    public RootEdge getRootEdge(RootBoardNodeWithRootEdges neighbour) {
        for (Map.Entry<RootEdge, RootBoardNodeWithRootEdges> e : neighbourRootEdgeMapping.entrySet()) {
            if (e.getValue().equals(neighbour)) return e.getKey();
        }
        return null;
    }

    public void setXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setClearingType(RootParameters.ClearingTypes clearingType) {
        this.clearingType = clearingType;
    }

    public int getWood() {
        return wood;
    }

    public void setWood(int amount) {
        wood = amount;
    }

    public boolean getKeep() {
        return keep;
    }

    public void setKeep() {
        keep = true;
    }

    public void removeKeep() {
        keep = false;
    }

    public void addWood() {
        wood++;
    }

    public void removeWood() {
        wood--;
    }

    public boolean containsWorkshop() {
        return workshops > 0;
    }

    public int getWorkshops() {
        return workshops;
    }

    public void addWorkshop() {
        workshops += 1;
    }

    public void removeWorkshop() {
        workshops -= 1;
    }

    public int getRecruiters() {
        return recruiter;
    }

    public void addRecruiter() {
        recruiter++;
    }

    public void removeRecruiter() {
        recruiter--;
    }

    public int getSawmill() {
        return sawmill;
    }

    public void addSawmill() {
        sawmill++;
    }

    public void removeSawmill() {
        sawmill--;
    }

    public void addCatWarrior() {
        catWarriors++;
    }

    public void addBirdWarrior() {
        eyrieWarriors++;
    }

    public void addWoodlandWarrior() {
        woodlandWarriors++;
    }

    public void addVagabondWarrior() {
        vagabond = 1;
    }

    public void setSympathy(){
        sympathy = true;
    }
    public void unsetSympathy(){
        sympathy = false;
    }

    public void removeCatWarrior() {
        catWarriors--;
        if (catWarriors < 0) {
            System.out.println("Error with cat warrior count");
        }
    }

    public void removeBirdWarrior() {
        eyrieWarriors--;
        if (eyrieWarriors < 0) {
            System.out.println("Error with bird warrior count");
        }
    }

    public void removeWoodlandWarrior() {
        woodlandWarriors--;
        if (woodlandWarriors < 0) {
            System.out.println("Error with woodland warrior count");
        }
    }

    public void removeVagabondWarrior() {
        vagabond = 0;
    }

    public boolean getSympathy() {
        return sympathy;
    }

    public int getWarrior(RootParameters.Factions faction) {
        if (faction == RootParameters.Factions.MarquiseDeCat) {
            return catWarriors;
        } else if (faction == RootParameters.Factions.EyrieDynasties) {
            return eyrieWarriors;
        } else if (faction == RootParameters.Factions.WoodlandAlliance) {
            return woodlandWarriors;
        } else if (faction == RootParameters.Factions.Vagabond) {
            return vagabond;
        }
        return 0;
    }

    public void addWarrior(RootParameters.Factions faction) {
        if (faction == RootParameters.Factions.MarquiseDeCat) {
            catWarriors += 1;
        } else if (faction == RootParameters.Factions.EyrieDynasties) {
            eyrieWarriors += 1;
        } else if (faction == RootParameters.Factions.WoodlandAlliance) {
            woodlandWarriors += 1;
        } else if (faction == RootParameters.Factions.Vagabond) {
            vagabond += 1;
        }
    }

    public void removeWarrior(RootParameters.Factions faction) {
        if (faction == RootParameters.Factions.MarquiseDeCat) {
            catWarriors -= 1;
        } else if (faction == RootParameters.Factions.EyrieDynasties) {
            eyrieWarriors -= 1;
        } else if (faction == RootParameters.Factions.WoodlandAlliance) {
            woodlandWarriors -= 1;
        } else if (faction == RootParameters.Factions.Vagabond) {
            vagabond -= 1;
        }
    }

    public boolean isAttackable(RootParameters.Factions faction) {
        return switch (faction) {
            case MarquiseDeCat -> catWarriors > 0 || wood > 0 || keep || workshops > 0 || sawmill > 0 || recruiter > 0;
            case EyrieDynasties -> eyrieWarriors > 0 || roost > 0;
            case WoodlandAlliance -> woodlandWarriors > 0 || base || sympathy;
            case Vagabond -> vagabond > 0;
        };
    }

    public boolean hasBuilding(RootParameters.BuildingType bt){
        return switch (bt) {
            case Recruiter -> recruiter > 0;
            case Workshop -> workshops > 0;
            case Sawmill -> sawmill > 0;
            case Roost -> roost > 0;
            case FoxBase -> {
                if (clearingType == RootParameters.ClearingTypes.Fox) {
                    yield base;
                }
                yield false;
            }
            case MouseBase -> {
                if (clearingType == RootParameters.ClearingTypes.Mouse) {
                    yield base;
                }
                yield false;
            }
            case RabbitBase -> {
                if (clearingType == RootParameters.ClearingTypes.Rabbit) {
                    yield base;
                }
                yield false;
            }
            case Ruins -> ruins;
        };
    }

    public boolean hasToken(RootParameters.TokenType tt){
        return switch (tt) {
            case Wood -> wood > 0;
            case Sympathy -> sympathy;
            case Keep -> keep;
        };
    }

    public void addToken(RootParameters.TokenType tt){
        switch (tt){
            case Sympathy:
                sympathy = true;
                break;
            case Wood:
                wood++;
                break;
            case Keep:
                keep = true;
                break;
        }
    }

    public void removeToken(RootParameters.TokenType tt){
        switch (tt){
            case Wood:
                if (wood > 0){
                    wood--;
                }
                break;
            case Sympathy:
                sympathy = false;
                break;
            case Keep:
                keep = false;
                break;
        }
    }

    public void build(RootParameters.BuildingType bt) {
        if (currentBuildings >= maxBuildings) {
            //System.out.println(currentBuildings + " " + maxBuildings);
            throw new IllegalCallerException("Trying to build in a full clearing");
        } else {
            switch (bt) {
                case Recruiter:
                    recruiter++;
                    currentBuildings++;
                    break;
                case Workshop:
                    workshops++;
                    currentBuildings++;
                    break;
                case Sawmill:
                    sawmill++;
                    currentBuildings++;
                    break;
                case Roost:
                    if (roost == 0) {
                        roost++;
                    }
                    currentBuildings++;
                    break;
                case RabbitBase:
                    if (clearingType == RootParameters.ClearingTypes.Rabbit) {
                        base = true;
                        currentBuildings++;
                    }
                    break;
                case MouseBase:
                    if (clearingType == RootParameters.ClearingTypes.Mouse) {
                        base = true;
                        currentBuildings++;
                    }
                    break;
                case FoxBase:
                    if (clearingType == RootParameters.ClearingTypes.Fox) {
                        base = true;
                        currentBuildings++;
                    }
                    break;
                case Ruins:
                    ruins = true;
                    currentBuildings++;
            }
        }
    }

    public void removeBuilding(RootParameters.BuildingType bt) {
        switch (bt) {
            case Roost:
                roost--;
                currentBuildings--;
                break;
            case Sawmill:
                sawmill--;
                currentBuildings--;
                break;
            case Workshop:
                workshops--;
                currentBuildings--;
                break;
            case Recruiter:
                recruiter--;
                currentBuildings--;
                break;
            case RabbitBase:
                if (clearingType == RootParameters.ClearingTypes.Rabbit) {
                    base = false;
                    currentBuildings--;
                    break;
                }
                break;
            case MouseBase:
                if (clearingType == RootParameters.ClearingTypes.Mouse) {
                    base = false;
                    currentBuildings--;
                    break;
                }
                break;
            case FoxBase:
                if (clearingType == RootParameters.ClearingTypes.Fox) {
                    base = false;
                    currentBuildings--;
                    break;
                }
                break;
            case Ruins:
                ruins = false;
                currentBuildings--;
                break;

        }

    }

    public RootParameters.ClearingTypes getClearingType() {
        return this.clearingType;
    }

    public boolean hasSympatheticNeighbour(){
        for (RootBoardNodeWithRootEdges neighbour: getNeighbours()){
            if (neighbour.sympathy){
                return true;
            }
        }
        return false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void updateOwner(){
        //Vagabond cannot rule a clearing
        int catCount = catWarriors + workshops + sawmill + recruiter;
        int birdCount = eyrieWarriors + roost;
        int woodlandCount = woodlandWarriors + (base ? 1 : 0);
        if(birdCount != 0 && birdCount >= catCount && birdCount >= woodlandCount){
            rulerID = 1;
        } else if (catCount > birdCount && catCount > woodlandCount) {
            rulerID = 0;
        } else if (woodlandCount > catCount && woodlandCount > birdCount) {
            rulerID = 2;
        } else {
            rulerID = -1;
        }
    }

    public int getMaxBuildings(){
        return maxBuildings;
    }

    public HashMap<RootParameters.BuildingType, Integer> getAllBuildings(){
        return new HashMap<>(){
            {
                put(RootParameters.BuildingType.Roost, roost);
                put(RootParameters.BuildingType.Sawmill, sawmill);
                put(RootParameters.BuildingType.Workshop, workshops);
                put(RootParameters.BuildingType.Recruiter, recruiter);
                put(RootParameters.BuildingType.Ruins, ruins ? 1 : 0);
                put(RootParameters.BuildingType.MouseBase, base && clearingType == RootParameters.ClearingTypes.Mouse ? 1 : 0);
                put(RootParameters.BuildingType.RabbitBase, base && clearingType == RootParameters.ClearingTypes.Rabbit ? 1 : 0);
                put(RootParameters.BuildingType.FoxBase, base && clearingType == RootParameters.ClearingTypes.Fox ? 1 :0);
            }
        };
    }

    public boolean hasBuildingRoom(){
        return currentBuildings < maxBuildings;
    }

    public HashMap<RootParameters.TokenType, Integer> getAllTokens(){
        return new HashMap<>(){
            {
                put(RootParameters.TokenType.Wood, wood);
                put(RootParameters.TokenType.Sympathy, sympathy ? 1 : 0);
                put(RootParameters.TokenType.Keep, keep ? 1 : 0);
            }

        };
    }

    public boolean canMove(int playerID){
        if (rulerID == playerID){return true;}
        return getNeighbours().stream().anyMatch(node -> node.rulerID == playerID);
    }
}

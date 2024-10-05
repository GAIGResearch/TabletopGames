package games.conquest.components;

import core.CoreConstants;
import core.components.Component;
import games.conquest.CQGameState;
import org.apache.hadoop.hdfs.protocol.proto.ErasureCodingProtos;
import scala.collection.immutable.Vector2;
import utilities.Vector2D;

import java.util.HashMap;
import java.util.HashSet;

/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class Troop extends Component {
    private final TroopType troopType;

    private final int range;
    private final int cost;
    private final int damage;
    private final int movement;

    private int health;
    private int healthBoost = 0;
    private int distanceMoved = 0;
    private Vector2D location;
    HashSet<CommandType> appliedCommands = new HashSet<>();

    public Troop(TroopType troop, int ownerID) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Troop");
        setOwnerId(ownerID);
        troopType = troop;
        range = troop.range;
        cost = troop.cost;
        damage = troop.damage;
        movement = troop.movement;
        health = troop.health;
    }

    protected Troop(int componentID, TroopType troop, int ownerID) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Troop", componentID);
        setOwnerId(ownerID);
        troopType = troop;
        range = troop.range;
        cost = troop.cost;
        damage = troop.damage;
        movement = troop.movement;
        health = troop.health;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return troopType is this class (e.g. GTComponent) and NOT the super class Component.
     * <p>
     * <b>IMPORTANT</b>: This should have the same componentID
     * (using the protected constructor on the Component super class which takes this as an argument).
     * </p>
     * <p>The function should also call the {@link Component#copyComponentTo(Component)} method, passing in as an
     * argument the new copy you've made.</p>
     * <p>If all variables in this class are final or effectively final, then you can just return <code>`this`</code>.</p>
     */
    @Override
    public Troop copy() {
        Troop copy = new Troop(componentID, troopType, getOwnerId());
        copy.health = health;
        copy.healthBoost = healthBoost;
        copy.distanceMoved = distanceMoved;
        copy.setLocation(location);
//        copyComponentTo(copy); // TODO: ?
        return copy;
    }

    public Vector2D getLocation() {
        return location;
    }

    /**
     * Set location to a specific point, without changing the moved distance
     * @param loc new location
     */
    public void setLocation(Vector2D loc) {
        location = loc;
    }
    public void setLocation(int x, int y) {
        location = new Vector2D(x, y);
    }

    public boolean move(Cell target, CQGameState cqgs) {
        HashMap<Vector2D, Integer> locationToTroopMap = cqgs.getLocationToTroopMap();
        int distance = cqgs.getDistance(location, target.position);
        if (distance > getMovement()) return false; // do nothing, can't move there
        cqgs.moveTroop(this.getComponentID(), location, target.position);
        location = target.position;
        distanceMoved += distance;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Troop)) return false;
        if (!super.equals(o)) return false; // this already checks for equality of all final values (through the componentID)
        Troop troop = (Troop) o;
        return troop.getHealth() == this.getHealth() &&
               troop.getMovement() == this.getMovement(); // TODO: check applied commands hash
    }

    @Override
    public int hashCode() {
        // TODO: include all class variables (if any).
        return super.hashCode();
    }

    // step function; two different behaviours whether it will be my turn next, or the enemy's turn
    public void step(int currentPlayer) {
        // (cmd is applied by enemy) XOR (it is my turn next)
        // if cmd is applied by self (not enemy), and it is my turn next, remove command
        // if cmd is applied by enemy, and it is their (not my) turn next, remove command
        appliedCommands.removeIf(cmd -> cmd.enemy ^ (currentPlayer != getOwnerId()));
        if (currentPlayer != getOwnerId()) {
            // if my turn starts again, health boosts are removed
            healthBoost = 0;
            distanceMoved = 0;
        }
    }

    // Check if a command is applied to this troop
    public boolean hasCommand(CommandType type) {
        return appliedCommands.contains(type);
    }

    // Get allowed movement remaining.
    public int getMovement() {
        if (hasCommand(CommandType.Chastise)) return 0;
        if (hasCommand(CommandType.Charge)) return movement * 2 - distanceMoved;
        return movement - distanceMoved;
    }
    public int getHealth() {
        return health + healthBoost;
    }
    public boolean isAlive() {
        return health > 0;
    }
    public int getDamage() {
        if (hasCommand(CommandType.BattleCry)) return damage * 2;
        return damage;
    }
    public int getRange() {
        return range;
    }
    public TroopType getTroopType() {
        return troopType;
    }
    public char getTroopID() {
        return troopType.troopID;
    }
    public String getTroopName() {
        return troopType.troopName;
    }
    public void applyCommand(CommandType type) {
        if (type.equals(CommandType.BattleCry) || type.equals(CommandType.Stoicism)) {
            healthBoost += type.health;
        } else if (type.equals(CommandType.Bombard)) {
            if (healthBoost > 0) healthBoost -= 100;
            else health -= 100;
        }
        appliedCommands.add(type);
    }

    public HashSet<CommandType> getAppliedCommands() {
        return appliedCommands;
    }

    /**
     * Deal damage to this troop
     * @param dmg Amount of damage to apply
     * @return 0 if this troop survives; the cost of the troop if it died.
     */
    public int damage(int dmg) {
        if (getHealth() < dmg) {
            // Troop dies, award points
            health = 0;
            healthBoost = 0;
            return cost;
        }
        if (healthBoost >= dmg) {
            // No actual damage is taken, only boost is reduced. Troop survives.
            healthBoost -= dmg;
            return 0;
        } else if (healthBoost > 0) {
            // There is a health boost, but it gets fully depleted.
            dmg -= healthBoost;
            healthBoost = 0;
        }
        if (health > dmg) {
            // Troop survives, award 0 points.
            health -= dmg;
            return 0;
        } else {
            // Troop dies, award points
            health = 0;
            return cost;
        }
    }
}

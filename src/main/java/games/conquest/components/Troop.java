package games.conquest.components;

import core.CoreConstants;
import core.components.Component;
import games.conquest.CQGameState;
import utilities.Vector2D;

import java.util.HashSet;
import java.util.Objects;

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
        copy.setLocation(getLocation().copy());
        copy.appliedCommands = (HashSet<CommandType>) appliedCommands.clone();
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

    public boolean move(Cell target, CQGameState cqgs) {
        int distance = cqgs.getDistance(cqgs.getCell(getLocation()), target);
        if (distance > getMovement()) return false; // do nothing, can't move there
        if (!cqgs.moveTroop(this, target.position))
            throw new AssertionError("Could not move troop: troop not found.");
        setLocation(target.position);
        distanceMoved += distance;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Troop)) return false;
        if (!super.equals(o)) return false; // this already checks for equality of all final values (through the componentID)
        Troop troop = (Troop) o;
        return troop.ownerId == ownerId &&
               troop.getHealth() == this.getHealth() &&
               troop.getMovement() == this.getMovement() &&
               troop.appliedCommands.equals(appliedCommands);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(), ownerId, troopType, health, healthBoost, distanceMoved, location, appliedCommands
        );
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
    public boolean hasMoved() {
        return distanceMoved > 0;
    }

    // Get allowed movement remaining.
    public int getMovement() {
        if (hasCommand(CommandType.Chastise)) return 0;
        if (hasCommand(CommandType.Charge)) return movement * 2 - distanceMoved;
        return movement - distanceMoved;
    }
    public int getUnboostedHealth() {
        return health;
    }
    public int getHealth() {
        return health + healthBoost;
    }
    public boolean isAlive() {
        return health > 0;
    }
    public int getDamage() {
        if (hasCommand(CommandType.BattleCry)) return damage + 200;
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
        if (dmg > 100 && appliedCommands.contains(CommandType.ShieldWall))
            dmg = 100;
        int reward = getDmgReward(dmg);
        if (getHealth() <= dmg) {
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
        } else {
            // Troop dies, award points; redundancy just to be sure.
            health = 0;
        }
        return reward;
    }

    /**
     * Does NOT damage the troop, but ONLY calculates what the reward would be for damaging the troop.
     * This can be used to gauge whether or not a troop would die from dealing a certain amount of damage.
     * @param dmg
     * @return
     */
    public int getDmgReward(int dmg) {
        if (dmg > 100 && appliedCommands.contains(CommandType.ShieldWall))
            dmg = 100;
        if (getHealth() <= dmg) {
            return cost;
        }
        return 0;
    }

    /**
     * Only called when the troop is the last troop remaining for a player,
     * when starting their turn. Since it's not possible to chastise the last remaining
     * troop, this gets undone whenever it happened before killing the second to last troop
     * There will be no refund if this happens.
     */
    public void removeChastise() {
        if (hasCommand(CommandType.Chastise)) {
            appliedCommands.remove(CommandType.Chastise);
        }
    }

    @Override
    public String toString() {
        return troopType.troopName + " " + getLocation();
    }
}

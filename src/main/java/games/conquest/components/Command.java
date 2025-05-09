package games.conquest.components;

import core.CoreConstants;
import core.components.Card;
import core.components.Component;

import java.util.Objects;

/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class Command extends Card {
    private final CommandType commandType;
    private final int cost;
    private final int baseCooldown;
    private int cooldown;

    public Command(CommandType command, int ownerID) {
        super("Command");
        setOwnerId(ownerID);
        commandType = command;
        cost = command.cost;
        baseCooldown = command.cooldown;
        cooldown = 0;
    }

    protected Command(int componentID, CommandType command, int ownerID) {
        super("Command", componentID);
        setOwnerId(ownerID);
        commandType = command;
        cost = command.cost;
        baseCooldown = command.cooldown;
        cooldown = 0;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return commandType is this class (e.g. GTComponent) and NOT the super class Component.
     * <p>
     * <b>IMPORTANT</b>: This should have the same componentID
     * (using the protected constructor on the Component super class which takes this as an argument).
     * </p>
     * <p>The function should also call the {@link Component#copyComponentTo(Component)} method, passing in as an
     * argument the new copy you've made.</p>
     * <p>If all variables in this class are final or effectively final, then you can just return <code>`this`</code>.</p>
     */
    @Override
    public Command copy() {
        Command copy = new Command(componentID, commandType, getOwnerId());
        copy.cooldown = cooldown;
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Command) || !super.equals(o)) return false;
        Command cmd = (Command) o;
        return cmd.ownerId == ownerId && cmd.cooldown == this.cooldown && cmd.commandType.equals(commandType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ownerId, commandType, cooldown);
    }

    public void step() {
        if (cooldown > 0) {
            cooldown--;
        }
    }
    public void use() {
        cooldown = baseCooldown;
    }
    // Winds of Fate chooses this command; drop cooldown.
    public void reset() {
        cooldown = 0;
    }

    public int getCooldown() {
        return cooldown;
    }
    public int getCost() {
        return cost;
    }
    public CommandType getCommandType() {
        return commandType;
    }

    public String toString() {
        return commandType.toString() + " command (" + cooldown + "/" + commandType.cooldown + ")";
    }
}

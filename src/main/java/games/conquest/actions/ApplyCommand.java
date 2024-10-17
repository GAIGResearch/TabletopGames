package games.conquest.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.conquest.CQGameState;
import games.conquest.components.Command;
import games.conquest.components.CommandType;
import games.conquest.components.Troop;
import utilities.Vector2D;

import java.util.HashSet;
import java.util.List;

/**
 * <p>Actions are unit things players can do in the game (e.g. play a card, move a pawn, roll dice, attack etc.).</p>
 * <p>Actions in the game can (and should, if applicable) extend one of the other existing actions, in package {@link core.actions}.
 * Or, a game may simply reuse one of the existing core actions.</p>
 * <p>Actions may have parameters, so as not to duplicate actions for the same type of functionality,
 * e.g. playing card of different types (see {@link games.sushigo.actions.ChooseCard} action from SushiGo as an example).
 * Include these parameters in the class constructor.</p>
 * <p>They need to extend at a minimum the {@link AbstractAction} super class and implement the {@link AbstractAction#execute(AbstractGameState)} method.
 * This is where the main functionality of the action should be inserted, which modifies the given game state appropriately (e.g. if the action is to play a card,
 * then the card will be moved from the player's hand to the discard pile, and the card's effect will be applied).</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>MUST NOT</b> keep references to game components. Instead, store the {@link Component#getComponentID()}
 * in variables for any components that must be referenced in the action. Then, in the execute() function,
 * use the {@link AbstractGameState#getComponentById(int)} function to retrieve the actual reference to the component,
 * given your componentID.</p>
 */
public class ApplyCommand extends CQAction {
    public ApplyCommand(int pid, int command, CommandType type) {
        this(pid, command, (Vector2D) null, type);
    }
    public ApplyCommand(int pid, int command, Troop target, CommandType type) {
        this(pid, command, target == null ? null : target.getLocation(), type);
    }
    public ApplyCommand(int pid, int command, Vector2D target, CommandType type) {
        super(pid, command, target);
        cmdType = type;
    }

    @Override
    public boolean canExecute(CQGameState cqgs) {
        Command cmd = (Command) cqgs.getComponentById(cmdHighlight != -1 ? cmdHighlight : cqgs.cmdHighlight);
        if (cmd.getCooldown() > 0) return false;
        if (cmd.getCost() > cqgs.getCommandPoints()) return false;
        if (isWindsOfFate()) {
            return !cqgs.getCommands(cqgs.getCurrentPlayer(), false).isEmpty();
        } else {
            Troop target = cqgs.getTroopByLocation(highlight != null ? highlight : cqgs.highlight);
            if (target == null) return false; // only Winds of Fate can be applied without target.
            if ((target.getOwnerId() == cqgs.getCurrentPlayer()) ^ !cmdType.enemy) return false; // apply on self XOR use enemy-targeting command
            if (cmdType != CommandType.Charge) return true; // All non-Charge commands can be applied any time.
            else // no use in applying Charge on a troop after it has already moved; prevent this from happening to aid MCTS
                return cqgs.getGamePhase() == CQGameState.CQGamePhase.SelectionPhase || cqgs.getGamePhase() == CQGameState.CQGamePhase.MovementPhase;
        }
    }

    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        CQGameState cqgs = (CQGameState) gs;
        if (cmdHighlight == -1) cmdHighlight = cqgs.cmdHighlight;
        Command cmd = (Command) cqgs.getComponentById(cmdHighlight);
        if (!cqgs.spendCommandPoints(playerId, cmd.getCost())) return false;
        if (isWindsOfFate()) {
            HashSet<Command> hs = cqgs.getCommands(playerId, false);
            Command[] cooldowns = hs.toArray(new Command[hs.size()]);
            cooldowns[cqgs.getRnd().nextInt(hs.size())].reset(); // reset selected command
        } else {
            if (highlight == null) highlight = cqgs.highlight;
            Troop target = cqgs.getTroopByLocation(highlight);
            target.applyCommand(cmdType);
        }
        cqgs.useCommand(playerId, cmd);
        return gs.setActionInProgress(this);
    }

    public boolean isWindsOfFate() {
        return cmdType == CommandType.WindsOfFate;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public ApplyCommand copy() {
        return new ApplyCommand(playerId, cmdHighlight, highlight, cmdType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof  ApplyCommand)) return false;
        ApplyCommand acObj = ((ApplyCommand) obj);
        if (acObj.playerId != playerId || acObj.cmdHighlight != cmdHighlight) return false;
        if (highlight == null && acObj.highlight == null) return true;
        return acObj.highlight.equals(highlight);
    }

    @Override
    public String _toString() {
        return "Apply Command";
    }
    @Override
    public String toString() {
        String str = "Apply";
        if (cmdType != null) str += " " + cmdType;
        else str += " Command";
        if (highlight != null) str += "->" + highlight;
        return str;
    }
}

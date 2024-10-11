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
    public ApplyCommand(int pid, Command command) {
        super(pid, command, null);
    }
    public ApplyCommand(int pid, Command command, Troop target) {
        super(pid, command, target == null ? null : target.getLocation());
    }

    @Override
    public boolean canExecute(CQGameState cqgs) {
        Command cmd = cmdHighlight != null ? cmdHighlight : cqgs.cmdHighlight;
        if (cmd.getCooldown() > 0) return false;
        if (cmd.getCost() > cqgs.getCommandPoints()) return false;
        CommandType cmdType = cmd.getCommandType();
        if (cmdType == CommandType.WindsOfFate) {
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
        if (cmdHighlight == null) cmdHighlight = cqgs.cmdHighlight;
        if (!cqgs.spendCommandPoints(playerId, cmdHighlight.getCost())) return false;
        if (cmdHighlight.getCommandType() == CommandType.WindsOfFate) {
            HashSet<Command> hs = cqgs.getCommands(playerId, false);
            Command[] cooldowns = hs.toArray(new Command[hs.size()]);
            cooldowns[cqgs.getRnd().nextInt(hs.size())].reset(); // reset selected command
        } else {
            if (highlight == null) highlight = cqgs.highlight;
            Troop target = cqgs.getTroopByLocation(highlight);
            target.applyCommand(cmdHighlight.getCommandType());
        }
        cqgs.useCommand(playerId, cmdHighlight);
        return gs.setActionInProgress(this);
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public ApplyCommand copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: need to check pid?
        return obj instanceof ApplyCommand;
    }

    @Override
    public int hashCode() {
        // TODO: return the hash of all other variables in the class
        return 0;
    }

    @Override
    public String _toString() {
        return "Apply Command";
    }
    @Override
    public String toString() {
        String str = "Apply";
        if (cmdHighlight != null) str += " " + cmdHighlight;
        else str += " Command";
        if (highlight != null) str += "->" + highlight;
        return str;
    }
}

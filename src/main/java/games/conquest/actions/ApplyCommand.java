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
    public ApplyCommand(int pid, int command, CommandType type, int stateHash) {
        this(pid, command, (Vector2D) null, type, stateHash);
    }
    public ApplyCommand(int pid, int command, Troop target, CommandType type, int stateHash) {
        this(pid, command, target == null ? null : target.getLocation(), type, stateHash);
    }
    public ApplyCommand(int pid, int command, Vector2D target, CommandType type, int stateHash) {
        super(pid, command, target, stateHash);
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
            if (!cmd.getCommandType().phases.contains((CQGameState.CQGamePhase) cqgs.getGamePhase())) return false;
            Troop target = cqgs.getTroopByLocation(highlight != null ? highlight : cqgs.highlight);
            if (target == null) return false; // only Winds of Fate can be applied without target.
            if (target.hasCommand(cmd.getCommandType())) return false; // no repeat application of the same command on the same troop
            if ((target.getOwnerId() == cqgs.getCurrentPlayer()) ^ !cmdType.enemy) return false; // apply on self XOR use enemy-targeting command
            return switch (cmdType) {
                // Game rules state that chastise can't be used on the last remaining troop:
                case Chastise -> {
                    // no chastising last non-chastised troop. If there are >2 troops, you can't chastise them all.
                    // If there are 2 troops, you could use Winds of Fate to chastise both, but that's not allowed.
                    // If there's 1 troop, chastise is never allowed.
                    int s = cqgs.getTroops(target.getOwnerId()).size();
                    yield s > 2 || (s == 2 && !cqgs.hasChastisedTroop(target.getOwnerId()));
                }

                // Only allow charging on the selected troop; others won't be able to move anyway
                case Charge -> cqgs.getSelectedTroop() == target;

                // only allow healing damaged targets:
                case Regenerate -> target.getUnboostedHealth() < target.getTroopType().health;

                // It makes no sense to apply shield wall if you have 100 hp to begin with; either way, any hit is a 1-hit KO:
                // (Note: applying BattleCry or Stoicism will then allow you to use this command _after_ applying that command)
                case ShieldWall -> target.getHealth() > 100;

                default -> true;
            };
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
        return new ApplyCommand(playerId, cmdHighlight, highlight, cmdType, stateHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ApplyCommand)) return false;
        ApplyCommand acObj = ((ApplyCommand) obj);
        if (acObj.stateHash != stateHash) return false;
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

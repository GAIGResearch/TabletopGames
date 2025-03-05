package games.conquest.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.conquest.CQGameState;
import games.conquest.components.Command;
import games.conquest.components.CommandType;
import games.conquest.components.Troop;
import utilities.Vector2D;

import java.util.Objects;

import java.util.List;

public abstract class CQAction extends AbstractAction implements IExtendedSequence {
    public final int playerId;
    Vector2D highlight;
    int cmdHighlight = -1;
    CommandType cmdType = null;
    private boolean executed = false;
    // Keeps track of the state hash, in order to guarantee .equals() and .hashCode() are unique
    // Otherwise two of the same action type targeting the same square are flagged as equal
    int stateHash = 0;

    // This tracks the selected / targeted troop. It's used to be able to call toString() with information whether the troop dies.
    Troop target = null;
    Troop selected = null;


    public CQAction(int pid, Vector2D highlight, CQGameState state) {
        this(pid, highlight, state.hashCode(), state.getSelectedTroop(), state.getTroopByLocation(highlight));
    }
    public CQAction(int pid, int cmd, Vector2D highlight, int stateHash, Troop target) {
        this(pid, highlight, stateHash, null, target);
        cmdHighlight = cmd;
    }
    public CQAction(int pid, Vector2D highlight, int stateHash, Troop selected, Troop target) {
        playerId = pid;
        this.highlight = highlight;
        this.stateHash = stateHash;
        this.selected = selected;
        this.target = target;
    }

    public boolean targetsTroop(Troop troop) {
        if (troop == null) return false;
        return troop.getLocation().equals(highlight);
    }
    public CommandType getCmdType() {
        return cmdType;
    }

    /**
     * Check if the highlighted cell corresponds with the highlight set for the given action
     * @param cmp position to compare highlight with
     * @param cmd (optional) command to compare highlight with
     * @return `true` if the board's highlight `cmp` (and `cmd`) is equal to the action's `highlight` (and `cmdHighlight`); `false` otherwise.
     */
    public boolean checkHighlight(Vector2D cmp, Command cmd) {
        if (cmd == null) return false;
        if (cmd.getCommandType() == CommandType.WindsOfFate) return cmd.getCommandType().equals(cmdType);
        else return cmp.equals(highlight) && cmd.getCommandType().equals(cmdType);
    }
    public Vector2D getHighlight() {
        return highlight;
    }

    public abstract boolean canExecute(CQGameState cqgs);

    /**
     * Compares highlighted cell and/or command with the expected highlights for a given action.
     * @param highlight The highlighted cell
     * @param cmdHighlight The highlighted command
     * @return `true` if highlight(s) correspond to action's highlight(s); `false` otherwise
     */
    public boolean compareHighlight(Vector2D highlight, Command cmdHighlight) {
        if (this instanceof ApplyCommand) return this.checkHighlight(highlight, cmdHighlight);
        else return highlight.equals(this.getHighlight());
    }

    /**
     * Forward Model delegates to this from {@link core.StandardForwardModel#computeAvailableActions(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param gs The current game state
     * @return the list of possible actions for the {@link AbstractGameState#getCurrentPlayer()}.
     * These may be instances of this same class, with more choices between different values for a not-yet filled in parameter.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        return ((CQGameState) gs).getAvailableActions();
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    abstract String _toString();
    @Override
    public String toString() {
        String str = "";
        if (selected != null) {
            str += selected + " ";
        }
        str += _toString();
        if (target != null) {
            str += " " + target;
        } else if (highlight != null)
            str += " " + highlight;
        return str;
    }

    @Override
    public abstract boolean execute(AbstractGameState gs);

    @Override
    public abstract CQAction copy();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return Objects.hash(playerId, highlight, selected, target, cmdHighlight, executed, stateHash);
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

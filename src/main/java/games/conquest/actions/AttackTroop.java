package games.conquest.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.conquest.CQGameState;
import games.conquest.components.CommandType;
import games.conquest.components.Troop;
import utilities.Vector2D;

/**
 * <p>The extended actions framework supports 2 use-cases: <ol>
 *     <li>A sequence of decisions required to complete an action (e.g. play a card in a game area - which card? - which area?).
 *     This avoids very large action spaces in favour of more decisions throughout the game (alternative: all unit actions
 *     with parameters supplied at initialization, all combinations of parameters computed beforehand).</li>
 *     <li>A sequence of actions triggered by specific decisions (e.g. play a card which forces another player to discard a card - other player: which card to discard?)</li>
 * </ol></p>
 * <p>Extended actions should implement the {@link IExtendedSequence} interface and appropriate methods, as detailed below.</p>
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link EndTurn} apply here as well.</p>
 */
public class AttackTroop extends CQAction {
    public AttackTroop(int pid, Vector2D target) {
        super(pid, target);
    }

    @Override
    public boolean canExecute(CQGameState cqgs) {
        Troop selected = cqgs.getSelectedTroop();
        if (selected == null || cqgs.getGamePhase() == CQGameState.CQGamePhase.RallyPhase)
            return false; // can't attack before selecting a troop, or after a previous attack.
        Troop target = cqgs.getTroopByLocation(highlight != null ? highlight : cqgs.highlight);
        if (target.getOwnerId() == selected.getOwnerId())
            return false; // can't attack own troop
        int distance = cqgs.getCell(target.getLocation()).getChebyshev(selected.getLocation());
        return distance <= selected.getRange();
    }

    /**
     * <p>Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.</p>
     * <p>In extended sequences, this function makes a call to the
     * {@link AbstractGameState#setActionInProgress(IExtendedSequence)} method with the argument <code>`this`</code>
     * to indicate that this action has multiple steps and is now in progress. This call could be wrapped in an <code>`if`</code>
     * statement if sometimes the action simply executes an effect in one step, or all parameters have values associated.</p>
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        CQGameState cqgs = (CQGameState) gs;
        Troop target = cqgs.getTroopByLocation(highlight != null ? highlight : cqgs.highlight);
        Troop selected = cqgs.getSelectedTroop();
        if (target == null || selected == null) return false;
        int distance = cqgs.getCell(target.getLocation()).getChebyshev(selected.getLocation());
        if (distance > selected.getRange()) return false;
        boolean counterAttack = distance <= target.getRange();
        int dmg = selected.getDamage(),
            counterDmg = target.getDamage();
        boolean vigilance = counterAttack && target.hasCommand(CommandType.Vigilance);
        int reward;
        if (vigilance) {
            // Vigilance is applied and the target is within range; counterattack first
            reward = selected.damage(counterDmg);
        } else {
            // No Vigilance, or outside the target's range
            reward = target.damage(dmg);
        }
        cqgs.gainCommandPoints(vigilance ? playerId ^ 1 : playerId, reward);
        if (reward > 0) {
            cqgs.setGamePhase(CQGameState.CQGamePhase.RallyPhase);
            return true; // no counterattack possible, end execution
        }
        // Counterattack, as the troop survived
        if (vigilance) {
            // we applied vigilance before, so now my own troop gets to strike
            reward = target.damage(dmg);
        } else if (counterAttack) {
            // no vigilance, but within range to counterattack afterward
            reward = selected.damage(counterDmg);
        }
        cqgs.gainCommandPoints(vigilance ? playerId : playerId ^ 1, reward);
        cqgs.setGamePhase(CQGameState.CQGamePhase.RallyPhase);
        return true;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public AttackTroop copy() {
        return new AttackTroop(playerId, highlight);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AttackTroop)) return false;
        AttackTroop mtObj = (AttackTroop) obj;
        return mtObj.playerId == playerId && mtObj.highlight.equals(highlight);
    }

    @Override
    public String _toString() {
        return "Attack Troop";
    }
}

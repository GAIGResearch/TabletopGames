package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.components.BoardNode;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.attack.ChainAttack;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.getNeighboursInRange;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;

public class FireBreath extends ChainAttack {

    // A counter for how many times this has been enabled by external actions
    // i.e. how many times a Shadow Dragon has spent the Fire Breath surge in a single attack
    // So that we don't get stuck in a loop, we can only execute that many Fire Breaths
    public static int enabled = 0;
    public FireBreath(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures, 4);
        this.isFreeAttack = true;
        this.isMelee = true;
        // We don't care about attackMissed() checking for range, so we count it as Melee
        // the Shadow Dragon's basic attack is Melee anyway, even if this should probably be Ranged
    }

    @Override
    public boolean execute(DescentGameState state) {

        state.setActionInProgress(this);

        defendingFigure = defendingFigures.get(0);
        index = 0;

        attackingPlayer = state.getComponentById(attackingFigure).getOwnerId();
        defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();
        interruptPlayer = attackingPlayer;

        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);

        // We don't care about checking for the Shadow passive, as that only applies for Heroes
        // We do however need to account for Night Stalker in case we are torching our own Barghests
        DicePool defencePool = defender.getDefenceDice();
        state.setDefenceDicePool(defencePool);
        NightStalker.addNightStalker(state, attacker, defender);

        // We have already made our attack rolls in the MeleeAttack that triggered this action
        // So we skip straight to PRE_DAMAGE phase
        phase = PRE_DEFENCE_ROLL;

        // DO NOT super.execute(state) - this would reroll our dice and we would lose the attack rolls
        // We have to act as though we are midway through an existing MultiAttack
        result = getInitialResult(state);

        attacker.setCurrentAttack(this);
        defender.setCurrentAttack(this);

        // As this is an Interrupt Attack, we do not need to disable all Interrupt Attacks
        // Only decrement how many times we can use Fire Breath now that we have used it
        decreaseEnabled();

        movePhaseForward(state);

        // This is a free attack, so we don't need to increment the number of actions executed
        attacker.setHasAttacked(true);
        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        return isEnabled() && super.canExecute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Chain Attack by ", "Fire Breath by ");
    }

    @Override
    public String toString() {
        return super.toString().replace("Chain Attack by ", "Fire Breath by ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FireBreath that = (FireBreath) o;
        return Objects.equals(defendingFigures, that.defendingFigures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), enabled);
    }

    @Override
    public ChainAttack copy() {
        FireBreath retValue = new FireBreath(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public static Set<FireBreath> constructFireBreath(DescentGameState dgs, int attackingFigure, int defendingFigure)
    {
        Set<FireBreath> fireBreaths = new HashSet<>();

        Set<FireBreath> allBreaths = new HashSet<>();
        // We have already attacked the defending figure with this attack, so we should ignore it as a target
        List<Integer> targets = new ArrayList<>();
        FireBreath fireBreath;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        Vector2D position = target.getPosition();
        List<Vector2D> positions = new ArrayList<>();
        positions.add(position);
        Set<BoardNode> tiles = new HashSet<>();
        tiles = getNeighboursInRange(dgs, position, 4);

        // Go through and collect all the possible tiles that contain a Figure
        for (BoardNode tile : tiles) {
            int neighbourID = ((PropertyInt) tile.getProperty(playersHash)).value;
            // Ignore any empty tiles
            if (neighbourID == -1) continue;
            Vector2D pos = ((PropertyVector2D) tile.getProperty("coordinates")).values;

            // Add any tiles that have Figures, to examine later
            if(positions.contains(pos)) continue;
            positions.add(pos);

            // We can immediately create a new Fire Breath action by pairing the tile's figure with the original target
            fireBreath = new FireBreath(attackingFigure, List.of(neighbourID));
            if (!allBreaths.contains(fireBreath)) allBreaths.add(fireBreath);
        }



        // Now that we have all possible Fire Breaths, we only return those we know we can execute
        for (FireBreath breath: allBreaths) {
            if (breath.canExecute(dgs)) {
                fireBreaths.add(breath);
            }
        }
        return fireBreaths;
    }

    public static void decreaseEnabled() {
        FireBreath.enabled = Math.min(FireBreath.enabled - 1, 0);
    }

    public static void increaseEnabled() {
        FireBreath.enabled++;
    }

    public static boolean isEnabled() {
        return FireBreath.enabled > 0;
    }

    public static void disable() {
        FireBreath.enabled = 0;
    }
}

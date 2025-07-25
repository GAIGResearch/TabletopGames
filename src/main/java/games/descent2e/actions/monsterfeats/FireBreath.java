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
    public FireBreath(int attackingFigure, List<Integer> defendingFigures, int distance, List<Vector2D> pathway) {
        super(attackingFigure, defendingFigures, distance, pathway);
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
        return super.getString(gameState).replace("Chain Attack", "Fire Breath");
    }

    @Override
    public String toString() {
        return super.toString().replace("Chain Attack", "Fire Breath");
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
        FireBreath retValue = new FireBreath(attackingFigure, defendingFigures, distance, pathway);
        copyComponentTo(retValue);
        return retValue;
    }

    public static Set<FireBreath> constructFireBreath(DescentGameState dgs, int attackingFigure, int defendingFigure)
    {
        Set<FireBreath> fireBreaths = new HashSet<>();

        Set<FireBreath> allBreaths = new HashSet<>();
        // We have already attacked the defending figure with this attack, so we should ignore it as a target
        List<List<Integer>> targets = new ArrayList<>();
        List<List<Vector2D>> paths = new ArrayList<>();
        FireBreath fireBreath;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        Vector2D step0 = target.getPosition();
        BoardNode startPosition = dgs.getMasterBoard().getElement(step0);

        for (BoardNode tile1 : startPosition.getNeighbours().keySet())
        {
            boolean valid1 = false;
            Vector2D step1 = ((PropertyVector2D) tile1.getProperty("coordinates")).values;

            // If a valid target is here, add it to the list of targets
            int neighbourID1 = ((PropertyInt) tile1.getProperty(playersHash)).value;
            if (neighbourID1 != -1 && neighbourID1 != defendingFigure) {
                valid1 = true;
                List<Integer> newTargets = new ArrayList<>();
                newTargets.add(neighbourID1);

                List<Vector2D> newPath = new ArrayList<>();
                newPath.add(step0);
                newPath.add(step1);

                if (!targets.contains(newTargets)) {
                    targets.add(newTargets);
                    paths.add(newPath);
                }
            }

            for (BoardNode tile2 : tile1.getNeighbours().keySet())
            {
                boolean valid2 = false;
                if (tile2.equals(startPosition)) continue; // Ignore the original target tile

                Vector2D step2 = ((PropertyVector2D) tile2.getProperty("coordinates")).values;

                int neighbourID2 = ((PropertyInt) tile2.getProperty(playersHash)).value;
                if (neighbourID2 != -1 && neighbourID2 != defendingFigure && neighbourID2 != neighbourID1) {
                    valid2 = true;

                    List<Integer> newTargets = new ArrayList<>();
                    List<Vector2D> newPath = new ArrayList<>();

                    // If the first tile was not empty, that target must come first
                    if(valid1) {
                        newTargets.add(neighbourID1);
                    }
                    newTargets.add(neighbourID2);

                    newPath.add(step0);
                    newPath.add(step1);
                    newPath.add(step2);

                    if (!targets.contains(newTargets)) {
                        targets.add(newTargets);
                        paths.add(newPath);
                    }
                }

                for (BoardNode tile3 : tile2.getNeighbours().keySet())
                {
                    if (tile3.equals(startPosition)) continue; // Ignore the original target tile
                    if (tile3.equals(tile1)) continue; // Ignore the tile we just came from

                    Vector2D step3 = ((PropertyVector2D) tile3.getProperty("coordinates")).values;

                    int neighbourID3 = ((PropertyInt) tile3.getProperty(playersHash)).value;
                    if (neighbourID3 != -1 && neighbourID3 != defendingFigure &&
                            neighbourID3 != neighbourID1 && neighbourID3 != neighbourID2) {

                        List<Integer> newTargets = new ArrayList<>();
                        List<Vector2D> newPath = new ArrayList<>();

                        // If the first or second tiles was not empty, those targets must come first
                        if(valid1) {
                            newTargets.add(neighbourID1);
                        }
                        if(valid2) {
                            newTargets.add(neighbourID2);
                        }
                        newTargets.add(neighbourID3);

                        newPath.add(step0);
                        newPath.add(step1);
                        newPath.add(step2);
                        newPath.add(step3);
                        if (!targets.contains(newTargets)) {
                            targets.add(newTargets);
                            paths.add(newPath);
                        }
                    }
                }
            }
        }

        // Now we have all the possible targets, we can create a Fire Breath for each
        for (int i = 0; i < targets.size(); i++) {
            List<Integer> targetList = targets.get(i);
            if(targetList.isEmpty() || targetList.contains(defendingFigure)) continue;
            fireBreath = new FireBreath(attackingFigure, targetList, 4, paths.get(i));
            allBreaths.add(fireBreath);
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

package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.archetypeskills.PrayerOfPeace;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.*;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.PRE_DEFENCE_ROLL;

public class BlastAttack extends MultiAttack {

    public static boolean enabled = false;
    public Vector2D position;               // The centre point of the Blast
    public BlastAttack(int attackingFigure, List<Integer> defendingFigures, Vector2D position) {
        super(attackingFigure, defendingFigures);
        this.position = position;
        this.isFreeAttack = true;
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
        disable();

        movePhaseForward(state);

        // This is a free attack, so we don't need to increment the number of actions executed
        attacker.setHasAttacked(true);
        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        if (!isEnabled()) return false;
        Figure f = dgs.getActingFigure();

        if (!PrayerOfPeace.canAttackPrayer(dgs, f)) return false;

        if (!isFreeAttack) {
            if (f.getNActionsExecuted().isMaximum()) return false;
        }

        BoardNode startTile = dgs.getMasterBoard().getElement(position);
        int originID = startTile.getProperty(playersHash).getHashKey();
        List<Integer> neighbourTargets = new ArrayList<>();
        for (BoardNode neighbour : startTile.getNeighbours().keySet())
        {
            int targetID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
            if (targetID != -1 && targetID != originID)
                    if (!neighbourTargets.contains(targetID))
                        neighbourTargets.add(targetID);
        }

        for (int defendingFigure : defendingFigures)
        {
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;

            if (Air.checkAir(dgs, f, target)) {
                // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
                return false;
            }

            if (!neighbourTargets.contains(defendingFigure))
                return false;
        }
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Multi Attack", "Blast Attack");
    }

    @Override
    public String toString() {
        return super.toString().replace("Multi Attack", "Blast Attack");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BlastAttack that = (BlastAttack) o;
        return Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), enabled, position);
    }

    @Override
    public BlastAttack copy() {
        BlastAttack retValue = new BlastAttack(attackingFigure, defendingFigures, position);
        copyComponentTo(retValue);
        return retValue;
    }

    public static Set<BlastAttack> constructBlasts(DescentGameState dgs, int attackingFigure, int defendingFigure)
    {
        Set<BlastAttack> blasts = new HashSet<>();

        // TODO What to do if we killed the Initial Target earlier?
        Figure attacker = (Figure) dgs.getComponentById(attackingFigure);
        Figure initialTarget = (Figure) dgs.getComponentById(defendingFigure);

        Pair<Integer, Integer> size = initialTarget.getSize();
        List<BoardNode> attackingTiles = new ArrayList<>();

        Vector2D currentLocation = initialTarget.getPosition();
        GridBoard board = dgs.getMasterBoard();
        BoardNode anchorTile = board.getElement(currentLocation);

        if (size.a > 1 || size.b > 1) {
            attackingTiles.addAll(getAttackingTiles(initialTarget.getComponentID(), anchorTile, attackingTiles));
        } else {
            attackingTiles.add(anchorTile);
        }

        List<Integer> targets = new ArrayList<>();

        if (attackingTiles.size() == 1) {
            Set<BoardNode> neighbours = anchorTile.getNeighbours().keySet();
            for (BoardNode neighbour : neighbours) {
                int targetID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
                if (targetID == -1 || targetID == defendingFigure)
                    continue;
                if (!targets.contains(targetID)) {
                    Figure target = (Figure) dgs.getComponentById(targetID);
                    if (Air.checkAir(dgs, attacker, target))
                        continue;
                    targets.add(targetID);
                }
            }
            if (!targets.isEmpty()) {
                BlastAttack blast = new BlastAttack(attackingFigure, targets, ((PropertyVector2D) anchorTile.getProperty(coordinateHash)).values);
                if (blast.canExecute(dgs))
                    blasts.add(blast);
            }
        }

        else {
            for (BoardNode currentTile : attackingTiles)
            {
                targets = new ArrayList<>();
                Set<BoardNode> neighbours = currentTile.getNeighbours().keySet();
                for (BoardNode neighbour : neighbours)
                {
                    int targetID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
                    if (targetID == -1 || targetID == defendingFigure)
                        continue;
                    if (!targets.contains(targetID)) {
                        Figure target = (Figure) dgs.getComponentById(targetID);
                        if (Air.checkAir(dgs, attacker, target))
                            continue;
                        targets.add(targetID);
                    }
                }
                if (!targets.isEmpty()) {
                    BlastAttack blast = new BlastAttack(attackingFigure, targets, ((PropertyVector2D) currentTile.getProperty(coordinateHash)).values);
                    if (blast.canExecute(dgs))
                        blasts.add(blast);
                }
            }
        }

        return blasts;
    }

    public static boolean isEnabled() {
        return BlastAttack.enabled;
    }

    public static void enable() {
        BlastAttack.enabled = true;
    }

    public static void disable() {
        BlastAttack.enabled = false;
    }
}

package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.actions.items.RerollAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.*;

public class Howl extends TriggerAttributeTest {

    List<Integer> heroes;
    int heroIndex = 0;
    public Howl(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets.get(0));
        this.heroes = targets;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Howl";
    }

    @Override
    public String toString() {
        return "Howl";
    }

    @Override
    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);
        attackingPlayer = state.getComponentById(attackingFigure).getOwnerId();
        defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();

        phase = PRE_TEST;
        interruptPlayer = attackingPlayer;

        movePhaseForward(state);

        Figure monster = (Figure) state.getComponentById(attackingFigure);
        monster.getNActionsExecuted().increment();
        monster.setHasAttacked(true);
        monster.addActionTaken(toString());

        return true;
    }

    void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                // we need to get a decision from this player
            } else {
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == attackingPlayer) {
                    // we have completed the loop, and start again with the attacking player
                    executePhase(state);
                    interruptPlayer = attackingPlayer;
                }
            }
        } while (!foundInterrupt && phase != GetAttributeTests.ALL_DONE);
    }

    void executePhase(DescentGameState state) {
        //System.out.println("Executing phase " + phase);
        // System.out.println(heroIndex + " " + heroes.size());
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
            //throw new AssertionError("Should never be executed");
            case PRE_TEST:
                phase = INDEX_CHECK;
                break;
            case INDEX_CHECK:
                if (heroIndex < heroes.size() - 1) {
                    phase = PRE_TEST;
                    heroIndex++;
                    super.defendingFigure = heroes.get(heroIndex);
                    super.defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();;
                } else {
                    phase = POST_TEST;
                }
                break;
            case POST_TEST:
                phase = ALL_DONE;
                break;
        }
        // and reset interrupts
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        Monster monster = (Monster) dgs.getComponentById(attackingFigure);
        List<AbstractAction> retVal = new ArrayList<>();

        // System.out.println(((Figure) dgs.getComponentById(defendingFigure)).getName());

        HowlTest howlTest = new HowlTest(defendingFigure, Figure.Attribute.Willpower, attackingFigure, monster.getNActionsExecuted().getValue());

        if (howlTest.canExecute(dgs)) {
            retVal.add(howlTest);
        }

        if (retVal.isEmpty())
        {
            List<AbstractAction> superRetVal = super._computeAvailableActions(state);
            if (superRetVal != null && !superRetVal.isEmpty())
            {
                retVal.addAll(superRetVal);
            }
        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // after the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return phase == ALL_DONE;
    }

    @Override
    public Howl copy() {
        Howl retVal = new Howl(attackingFigure, heroes);
        copyComponentTo(retVal);
        return retVal;
    }

    public void copyComponentTo(Howl retVal)
    {
        retVal.heroIndex = heroIndex;
        super.copyComponentTo(retVal);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.HOWL));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Howl howl = (Howl) o;
        return heroIndex == howl.heroIndex && Objects.equals(heroes, howl.heroes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroes, heroIndex);
    }
}

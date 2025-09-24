package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.List;
import java.util.Objects;

import static games.descent2e.actions.AttributeTest.TestPhase.*;
import static games.descent2e.actions.Triggers.*;
import static games.descent2e.actions.AttributeTest.Interrupters.*;

public abstract class AttributeTest extends DescentAction implements IExtendedSequence {

    public enum Interrupters {
        TESTER, OTHERS, ALL
    }

    public enum TestPhase {
        NOT_STARTED,
        PRE_TEST_ROLL (FORCED, TESTER),
        POST_TEST_ROLL (ROLL_OWN_DICE, TESTER),
        ALL_DONE;

        public final Triggers interrupt;
        public final AttributeTest.Interrupters interrupters;

        TestPhase(Triggers interruptType, AttributeTest.Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        TestPhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    protected final int testingFigure;
    protected int testingPlayer;
    protected String testingName;
    protected String attributeTestName;
    protected int sourceFigure;
    protected AttributeTest.TestPhase phase = NOT_STARTED;
    protected int interruptPlayer;
    protected Figure.Attribute attribute;
    protected int attributeValue = 0;
    protected int penaltyToAttribute = 0;
    protected int penaltyToRoll = 0;
    protected boolean result = false;

    protected boolean skip = false;



    public AttributeTest(int testingFigure, Figure.Attribute attribute, int sourceFigure) {
        super(FORCED);
        this.testingFigure = testingFigure;
        this.attribute = attribute;
        this.sourceFigure = sourceFigure;
    }

    @Override
    public boolean execute(DescentGameState state)
    {
        state.setActionInProgress(this);

        testingPlayer = state.getComponentById(testingFigure).getOwnerId();
        phase = PRE_TEST_ROLL;
        interruptPlayer = testingPlayer;
        Figure tester = (Figure) state.getComponentById(testingFigure);
        setTestingName(tester.getName().replace("Hero: ", ""));
        attributeValue = tester.getAttributeValue(attribute);

        //announceTestDebug(state);

        movePhaseForward(state);

        // When executing an attribute test we need to:
        // 1) Obtain the attribute and its value to test
        // 2) Roll the dice
        // 3) Possibly invoke re-roll options (via interrupts)
        // 4) Unlike attacks, tests can only be interrupted after the roll
        // 5) Lastly, we allow the results to be made public

        if (sourceFigure != 0)
        {
            Figure source = (Figure) state.getComponentById(sourceFigure);
            source.addActionTaken(toStringWithResult());
        }
        else
            tester.addActionTaken(toStringWithResult());

        return true;
    }

    private void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                //       System.out.println("Interrupt for player " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                skip = false;
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == testingPlayer) {
                    // we have completed the loop, and start again with the testing player
                    executePhase(state);
                    interruptPlayer = testingPlayer;
                }
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (phase.interrupt == null || phase.interrupters == null) return false;
        // first we see if the interruptPlayer is one who may interrupt
        switch (phase.interrupters) {
            case TESTER:
                if (interruptPlayer != testingPlayer)
                    return false;
                break;
            case OTHERS:
                if (interruptPlayer == testingPlayer)
                    return false;
                break;
            case ALL:
                // always fine
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return !_computeAvailableActions(state).isEmpty();
    }

    private void executePhase(DescentGameState state) {
        // System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
            //throw new AssertionError("Should never be executed");
            case PRE_TEST_ROLL:
                // roll dice
                testAttribute(state);
                phase = POST_TEST_ROLL;
                break;
            case POST_TEST_ROLL:
                // Any rerolls are executed via interrupts
                testResult(state);
                resolveTest(state, testingFigure, result);
                phase = ALL_DONE;
                //System.out.println(this.toString() + " (" + this.getString(state)  + ") done!");
                break;
        }
        // and reset interrupts
    }

    private void testAttribute(DescentGameState dgs)
    {
        Figure f = (Figure) dgs.getComponentById(testingFigure);

        // Only Heroes and Lieutenant Monsters can make Attribute Tests

        if (!(f instanceof Hero))
        {
            // By default, regular Monsters will always fail, as they have no attributes
            if (!((Monster) f).isLieutenant())
            {
                result = false;
                return;
            }
        }

        dgs.setAttributeDicePool(DicePool.constructDicePool("GREY", "BLACK"));

        dgs.getAttributeDicePool().roll(dgs.getRnd());

        testResult(dgs);
    }

    public void testResult(DescentGameState dgs)
    {
        int roll = dgs.getAttributeDicePool().getShields();

        // Normally, both penalties remain at 0, however the Overlord can influence either
        result = (roll + penaltyToRoll) <= (attributeValue - penaltyToAttribute);

        //System.out.println("Attribute Test Result: " + result + " (Roll: " + roll + ", Attribute: " + attributeValue + ")");
    }


    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        return;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        if (phase.interrupt == null) {
            System.out.println(phase + " " + phase.interrupt + " " + phase.interrupters + " " + interruptPlayer);
            throw new AssertionError("Should not be reachable");
        }
        DescentGameState state = (DescentGameState) gs;
        List<AbstractAction> retValue = state.getInterruptActionsFor(interruptPlayer, phase.interrupt);
        if (phase == POST_TEST_ROLL) {
            if (!retValue.isEmpty())
                retValue.add(new EndCurrentPhase());
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // after the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return (phase == ALL_DONE);
    }

    // returns a copy of the action
    public abstract AttributeTest _copy();

    @Override
    public AttributeTest copy() {
        AttributeTest retValue = _copy();
        retValue.testingPlayer = testingPlayer;
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        retValue.attribute = attribute;
        retValue.attributeValue = attributeValue;
        retValue.penaltyToAttribute = penaltyToAttribute;
        retValue.penaltyToRoll = penaltyToRoll;
        retValue.result = result;
        retValue.testingName = testingName;
        return retValue;
    }



    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;  // TODO
    }

    public Figure.Attribute getAttribute()
    {
        return attribute;
    }
    public void setAttribute(Figure.Attribute a)
    {
        attribute = a;
    }

    public void addPenaltyToAttribute(int penalty)
    {
        penaltyToAttribute += penalty;
    }

    public int getPenaltyToAttribute()
    {
        return penaltyToAttribute;
    }

    public void addPenaltyToRoll(int penalty)
    {
        penaltyToRoll += penalty;
    }

    public int getPenaltyToRoll()
    {
        return penaltyToRoll;
    }

    public boolean getResult()
    {
        return result;
    }

    public int getTestingFigure()
    {
        return testingFigure;
    }

    public void setTestingName(String name)
    {
        testingName = name;
    }

    public String getTestingName()
    {
        return testingName;
    }

    public String getAttributeTestName()
    {
        return attributeTestName;
    }

    public void setSourceFigure (int source)
    {
        sourceFigure = source;
    }
    public int getSourceFigure()
    {
        return sourceFigure;
    }

    public String toString() {
        return attribute + " Attribute Test by " + testingName;
    }

    public String toStringWithResult() {
        return toString() + " - " + (result ? "Passed" : "Failed");
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public TestPhase getPhase() {
        return phase;
    }
    public boolean getSkip()
    {
        return skip;
    }
    public void setSkip(boolean s)
    {
        skip = s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        AttributeTest that = (AttributeTest) o;
        return testingFigure == that.testingFigure && testingPlayer == that.testingPlayer &&
                sourceFigure == that.sourceFigure &&
                interruptPlayer == that.interruptPlayer && attributeValue == that.attributeValue &&
                penaltyToAttribute == that.penaltyToAttribute && penaltyToRoll == that.penaltyToRoll &&
                result == that.result && skip == that.skip && Objects.equals(testingName, that.testingName) &&
                Objects.equals(attributeTestName, that.attributeTestName) && phase == that.phase &&
                attribute == that.attribute;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), testingFigure, testingPlayer, testingName, attributeTestName,
                sourceFigure, phase, interruptPlayer, attribute, attributeValue, penaltyToAttribute, penaltyToRoll,
                result, skip);
    }
}

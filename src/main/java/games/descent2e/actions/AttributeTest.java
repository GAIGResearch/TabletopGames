package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import org.w3c.dom.Attr;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static games.descent2e.actions.AttributeTest.TestPhase.*;
import static games.descent2e.actions.AttributeTest.Interrupters.*;

// TODO: perform an attribute test
public class AttributeTest extends DescentAction implements IExtendedSequence {

    enum Interrupters {
        TESTER, OTHERS, ALL
    }

    public enum TestPhase {
        NOT_STARTED,
        PRE_TEST_ROLL,
        POST_TEST_ROLL,
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

    final int testingFigure;
    int testingPlayer;
    String testingName;
    AttributeTest.TestPhase phase = NOT_STARTED;
    int interruptPlayer;
    Figure.Attribute attribute;
    int attributeValue = 0;
    int penaltyToAttribute = 0;
    int penaltyToRoll = 0;
    boolean result = false;

    public AttributeTest(Set<Triggers> triggerPoint, int testingFigure) {
        super(triggerPoint);
        this.testingFigure = testingFigure;
    }

    @Override
    public boolean execute(DescentGameState state)
    {
        state.setActionInProgress(this);
        phase = PRE_TEST_ROLL;

        testingPlayer = state.getComponentById(testingFigure).getOwnerId();
        interruptPlayer = testingPlayer;
        Figure tester = (Figure) state.getComponentById(testingFigure);

        state.setAttributeDicePool(DicePool.constructDicePool("GREY", "BLACK"));
        state.getAttributeDicePool().roll(state.getRandom());

        attributeValue = tester.getAttributeValue(attribute);

        result = testAttribute(state, attributeValue);

        phase = ALL_DONE;

        return true;
    }

    

    private boolean testAttribute(DescentGameState dgs, int attribute)
    {
        dgs.setAttributeDicePool(DicePool.constructDicePool("GREY", "BLACK"));

        dgs.getAttributeDicePool().roll(dgs.getRandom());

        int roll = dgs.getAttributeDicePool().getShields();

        phase = POST_TEST_ROLL;

        // Normally, both penalties remain at 0, however the Overlord can influence either
        if ((roll + penaltyToRoll) <= (attributeValue - penaltyToAttribute))
        {
            return true;
        }

        else
        {
            return false;
        }
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return null;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return 0;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return false;
    }

    @Override
    public AttributeTest copy() {
        AttributeTest retValue = new AttributeTest(triggerPoints, testingFigure);
        retValue.testingPlayer = testingPlayer;
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        retValue.attribute = attribute;
        retValue.attributeValue = attributeValue;
        retValue.penaltyToAttribute = penaltyToAttribute;
        retValue.penaltyToRoll = penaltyToRoll;
        retValue.result = result;
        return retValue;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AttributeTest)
        {
            AttributeTest other = (AttributeTest) obj;
            return  other.testingPlayer == testingPlayer &&
                    other.phase == phase &&
                    other.interruptPlayer == interruptPlayer &&
                    other.attribute == attribute &&
                    other.attributeValue == attributeValue &&
                    other.penaltyToAttribute == penaltyToAttribute &&
                    other.penaltyToRoll == penaltyToRoll &&
                    other.result == result;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testingFigure, testingPlayer, phase.ordinal(), interruptPlayer,
                attribute, attributeValue, penaltyToAttribute, penaltyToRoll, result);
    }

    public void setAttribute(Figure.Attribute a)
    {
        attribute = a;
    }

    public void setPenaltyToAttribute(int penalty)
    {
        penaltyToAttribute = penalty;
    }

    public void setPenaltyToRoll(int penalty)
    {
        penaltyToRoll = penalty;
    }

    public boolean getResult()
    {
        return result;
    }

    public String toString() {
        return attribute + " Attribute Test by " + testingName;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

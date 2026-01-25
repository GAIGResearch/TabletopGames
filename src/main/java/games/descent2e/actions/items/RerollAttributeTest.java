package games.descent2e.actions.items;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;
import javassist.runtime.Desc;

import java.util.Objects;

public class RerollAttributeTest extends DescentAction {
    int figureID = -1;
    int cardID;

    public RerollAttributeTest(int figureID, int cardID) {
        super(Triggers.ROLL_OWN_DICE);
        this.figureID = figureID;
        this.cardID = cardID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DicePool dice = ((DescentGameState) gameState).getAttributeDicePool();
        String cardName = gameState.getComponentById(cardID).getProperty("name").toString();
        String attributeTestName = gameState.currentActionInProgress() instanceof AttributeTest ? ((AttributeTest) gameState.currentActionInProgress()).getAttributeTestName().split(":")[0] : "";
        String retval = cardName + ": Reroll " + attributeTestName + " (";
        int size = dice.getComponents().size();
        for (int i = 0; i < size; i++) {
            DescentDice d = dice.getDice(i);
            retval += d.getColour() + " [Result: " + d.getShielding() + "]";
            if (i < size - 1) retval += ", ";
        }
        retval += ")";
        return retval;
    }

    public String toString() {
        return "REROLL_ATTRIBUTE_TEST";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.getAttributeDicePool().roll(dgs.getRnd());
        Figure f = (Figure) dgs.getComponentById(figureID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        //System.out.println("Exhausting Lucky Charm reroll!");
        f.exhaustCard(card);
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return new RerollAttributeTest(figureID, cardID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (figureID == -1) return false;
        Figure f = (Figure) dgs.getComponentById(figureID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (f.isExhausted(card)) return false;
        if (f instanceof Hero)
        {
            if (!((Hero) f).getOtherEquipment().contains(card)) return false;
        }
        IExtendedSequence action =  dgs.currentActionInProgress();
        if (action == null) return false;
        if (action instanceof AttributeTest) {
            AttributeTest test = (AttributeTest) action;
            // Reroll should only be available if they failed the test
            // There is no point in exhausting the card if they passed
            return !test.getSkip() && test.getTestingFigure() == figureID && test.getPhase() == AttributeTest.TestPhase.POST_TEST_ROLL && !test.getResult();
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RerollAttributeTest that = (RerollAttributeTest) o;
        return figureID == that.figureID && cardID == that.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID, cardID);
    }
}

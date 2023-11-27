package games.descent2e.actions.items;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;

import java.util.Objects;

public class RerollAttributeTest extends DescentAction {
    int figureID = -1;
    DescentCard card;
    public RerollAttributeTest(int figureID, DescentCard card) {
        super(Triggers.ROLL_OWN_DICE);
        this.figureID = figureID;
        this.card = card;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        DicePool dice = ((DescentGameState) gameState).getAttributeDicePool();
        String cardname = card.getProperty("name").toString();
        String retval = cardname + ": Reroll (";
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
        dgs.getAttributeDicePool().roll(dgs.getRandom());
        Figure f = (Figure) dgs.getComponentById(figureID);
        f.exhaustCard(card);
        return true;
    }

    @Override
    public DescentAction copy() {
        return new RerollAttributeTest(figureID, card);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (figureID == -1) return false;
        Figure f = (Figure) dgs.getComponentById(figureID);
        if (f.isExhausted(card)) return false;
        if (f instanceof Hero)
        {
            if (!((Hero) f).getOtherEquipment().contains(card)) return false;
        }
        IExtendedSequence action =  Objects.requireNonNull(dgs.currentActionInProgress());
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
        return figureID == that.figureID && Objects.equals(card, that.card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID, card);
    }
}

package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;
import java.util.Set;

public class HowlTest extends AttributeTest {

    String attributeTestName;
    public HowlTest(int testingFigure, Figure.Attribute attribute, int sourceFigure, int testCount) {
        super(testingFigure, attribute);
        this.attributeTestName = "Howl (Willpower) Test";
        super.setSourceFigure(sourceFigure);
        super.setTestCount(testCount);
        attributeTestName = "Howl (Willpower) Test: " + sourceFigure + "-" + testCount;
    }

    @Override
    public void announceTestDebug (DescentGameState dgs)
    {
        System.out.println(((Figure) dgs.getComponentById(super.getTestingFigure())).getName() + " must make a Howl Test!");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Howl (Willpower) Test (" + super.getTestCount() + ") by " + super.getSourceFigure() + " on " + super.getTestingFigure();
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Howl (Willpower) Test!");
        }
        else
        {
            //System.out.println("Failed Howl (Willpower) Test!");
            String test = "Howl of " + ((Figure) dgs.getComponentById(super.getSourceFigure())).getName();
            DescentHelper.forcedFatigue(dgs, f, test);
        }
    }

    public HowlTest copy()
    {
        HowlTest retVal = new HowlTest(this.getTestingFigure(), this.getAttribute(), this.getSourceFigure(), this.getTestCount());
        retVal.testingPlayer = testingPlayer;
        retVal.phase = phase;
        retVal.interruptPlayer = interruptPlayer;
        retVal.attributeValue = attributeValue;
        retVal.attributeTestName = attributeTestName;
        retVal.penaltyToAttribute = penaltyToAttribute;
        retVal.penaltyToRoll = penaltyToRoll;
        retVal.result = result;
        retVal.setTestingName(this.getTestingName());
        return retVal;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof HowlTest)
        {
            HowlTest other = (HowlTest) obj;
            return this.getTestingFigure() == other.getTestingFigure() &&
                    this.getAttribute() == other.getAttribute() &&
                    this.getSourceFigure() == other.getSourceFigure() &&
                    this.getTestCount() == other.getTestCount() &&
                    this.attributeTestName.equals(other.getAttributeTestName()) &&
                    super.equals(obj);
        }
        return false;
    }

    public String getAttributeTestName()
    {
        return attributeTestName;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.getTestingFigure());

        // We can only make each attribute test once per turn - if we have already taken it, we can't make another attempt
        return !f.hasAttributeTest(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributeTestName);
    }
}
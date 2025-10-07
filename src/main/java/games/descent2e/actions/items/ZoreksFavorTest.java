package games.descent2e.actions.items;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

import static games.descent2e.DescentHelper.getFigureIndex;

public class ZoreksFavorTest extends AttributeTest {

    int targetFigure;
    public ZoreksFavorTest(int lieutenant, Figure.Attribute attribute, int targetFigure) {
        super(lieutenant, attribute, lieutenant);
        this.targetFigure = targetFigure;
        attributeTestName = "Shield of Zorek's Favor (Might) Test: " + lieutenant + "; Target: " + targetFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure target = (Figure) gameState.getComponentById(targetFigure);

        String testingName = f.getName().replace("Hero: ", "");
        String targetName = target.getName().replace("Hero: ", "");

        return "Shield of Zorek's Favor (Might) Test by " + testingName + " targeting " + targetName;
    }

    @Override
    public String toString() {
        return "Shield of Zorek's Favor (Might) Test by " + super.getSourceFigure() + " targeting " + targetFigure;
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; Reflect Test Result As Damage)" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (f.hasBonus(DescentTypes.SkillBonus.ZoreksFavor))
            f.removeBonus(DescentTypes.SkillBonus.ZoreksFavor);

        if (result)
        {
            //System.out.println("Passed Shield of Zorek's Favor (Might) Test!");
            int reflect = dgs.getAttributeDicePool().getShields();
            Figure t = (Figure) dgs.getComponentById(targetFigure);
            t.getAttribute(Figure.Attribute.Health).decrement(reflect);

            if (t.getAttribute(Figure.Attribute.Health).isMinimum()) {
                int index1 = getFigureIndex(dgs, t);
                int index2 = getFigureIndex(dgs, f);

                // Death
                DescentHelper.figureDeath(dgs, t);
                // Add to the list of defeated figures this turn
                dgs.addDefeatedFigure(t, index1, f, index2);
            }

        }
        else
        {
            //System.out.println("Failed Shield of Zorek's Favor (Might) Test!");
        }
    }

    @Override
    public ZoreksFavorTest _copy() {
        return new ZoreksFavorTest(sourceFigure, attribute, targetFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ZoreksFavorTest test) {
            return super.equals(test) && targetFigure == test.targetFigure;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetFigure);
    }
}
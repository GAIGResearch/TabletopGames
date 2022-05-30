package games.descent2e.components.tokens;

import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.tokens.AcolyteAction;
import games.descent2e.actions.tokens.TradeAcolyteAction;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;

public class Acolyte extends DToken {
    ArrayList<DescentAction> effects;
    static final int speedModifierForOwner = -1;

    public Acolyte(DescentTypes.DescentToken tokenType) {
        super(tokenType, null);
        effects = new ArrayList<>();
        effects.add(new AcolyteAction(getComponentID()));
        effects.add(new TradeAcolyteAction(getComponentID()));
    }

    @Override
    public void setOwnerId(int ownerId, DescentGameState dgs) {
        if (this.ownerId != -1) {
            Hero hero = dgs.getHeroes().get(ownerId-1);
            int curMax = hero.getAttribute(Figure.Attribute.MovePoints).getMaximum();
            hero.getAttribute(Figure.Attribute.MovePoints).setMaximum(curMax + speedModifierForOwner*-1); // Return
            for (DescentAction ef: effects) {
                hero.removeAbility(ef);
            }
        }
        super.setOwnerId(ownerId, dgs);
        if (this.ownerId != -1) {
            Hero hero = dgs.getHeroes().get(ownerId-1);
            int curMax = hero.getAttribute(Figure.Attribute.MovePoints).getMaximum();
            hero.getAttribute(Figure.Attribute.MovePoints).setMaximum(curMax + speedModifierForOwner); // Add modifier
            for (DescentAction ef: effects) {
                hero.addAbility(ef);
            }
        }
    }
}

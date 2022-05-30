package games.descent2e.components.tokens;

import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.tokens.AcolyteAction;
import games.descent2e.components.Hero;

public class Acolyte extends DToken {
    AcolyteAction effect;
    static final int speedModifierForOwner = -1;

    public Acolyte(DescentTypes.DescentToken tokenType) {
        super(tokenType, null);
        effect = new AcolyteAction();
    }

    @Override
    public void setOwnerId(int ownerId, DescentGameState dgs) {
        if (this.ownerId != -1) {
            Hero hero = dgs.getHeroes().get(ownerId);
            int curMax = hero.getSpeed().getMaximum();
            hero.getSpeed().setMaximum(curMax + speedModifierForOwner*-1); // Return
            hero.removeAbility(effect);
        }
        super.setOwnerId(ownerId, dgs);
        if (this.ownerId != -1) {
            Hero hero = dgs.getHeroes().get(ownerId);
            int curMax = hero.getSpeed().getMaximum();
            hero.getSpeed().setMaximum(curMax + speedModifierForOwner); // Add modifier
            hero.addAbility(effect);
        }
    }
}

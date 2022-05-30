package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.Acolyte;

import static games.descent2e.actions.Triggers.END_TURN;

// TODO: If end turn adjacent to 1 or more wounded clergy, choose 1 adjacent wounded clergy to escort
public class AcolyteAction extends TokenAction {

    public AcolyteAction() {
        super(-1, END_TURN);
    }

    public AcolyteAction(int acolyteTokenID) {
        super(acolyteTokenID, END_TURN);
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Acolyte acolyte = (Acolyte) gs.getComponentById(tokenID);
        Hero hero = gs.getHeroes().get(acolyte.getOwnerId()-1);
        return false;
    }
}

package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.components.GridBoard;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.tokens.SearchAction;
import games.descent2e.actions.tokens.TokenAction;
import games.descent2e.components.*;
import games.descent2e.components.cards.SearchCard;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static games.descent2e.components.DiceType.BROWN;
import static utilities.Utils.getNeighbourhood;

/**
 * Draw random search card and add to player
 */
public class Lurk extends SearchAction {
    public static int cardID = -1;
    public Lurk() {
        super();
        this.freeSearch = true;
    }

    public static HashMap<DiceType, Integer> lurkDice = new HashMap<DiceType, Integer>() {{
        put(BROWN, 1);
    }};

    @Override
    public Lurk _copy() {
        Lurk lurk = new Lurk();
        copyComponentsTo(lurk);
        return lurk;
    }

    @Override
    public boolean canExecute(DescentGameState gs) {
        Figure f = gs.getActingFigure();
        if (f == null) return false;
        if (f.hasBonus(DescentTypes.SkillBonus.Lurk)) return false;
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;
        DescentCard card = (DescentCard) gs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;
        return super.canExecute(gs);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof Lurk;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Lurk: Free Search Action and +1 Brown Defence Die";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = gs.getActingFigure();
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        DescentCard card = (DescentCard) gs.getComponentById(cardID);
        if (card != null)
            f.exhaustCard(card);
        f.addBonus(DescentTypes.SkillBonus.Lurk);
        return super.execute(gs);
    }

    public static void setCardID(int id) {
        cardID = id;
    }

    public static void addLurkDice(DescentGameState state)
    {
        List<DescentDice> dice = new ArrayList<>(state.getDefenceDicePool().copy().getComponents());
        dice.addAll(DicePool.constructDicePool(lurkDice).getComponents());
        DicePool newPool = new DicePool(dice);
        state.setDefenceDicePool(newPool);
    }
}

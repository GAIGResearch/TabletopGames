package games.descent2e.actions.items;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;

import java.util.HashMap;
import java.util.Objects;

public class RerollShield extends Shield {

    int dice = -1;

    public RerollShield(int figureID, int cardID, int dice) {
        super(figureID, cardID, 0);
        this.dice = dice;
    }

    public RerollShield(int figureID, int cardID, int value, int dice) {
        super(figureID, cardID, value);
        this.dice = dice;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        // Create a new Dice Pool, roll it, then set the old dice to the new result
        DescentDice die = dgs.getDefenceDicePool().getDice(dice);
        DicePool reroll = DicePool.constructDicePool(new HashMap<DiceType, Integer>() {{
            put(die.getColour(), 1);
        }});
        reroll.roll(dgs.getRnd());
        die.setFace(reroll.getDice(0).getFace());

        return super.execute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String name = gameState.getComponentById(cardID).toString();
        DescentDice die = ((DescentGameState) gameState).getDefenceDicePool().getDice(dice);
        name += ": Reroll Dice: " + die.getColour() + " (Shield: " + die.getShielding() + ")";
        if (value > 0)
            return name + " and add +" + value + " Shield to defense roll";
        return name;
    }

    @Override
    public String toString() {
        if (value > 0) return "Shield " + cardID + ": Reroll dice" + dice + " and add +" + value + " to defense roll";
        return "Shield " + cardID + ": Reroll dice " + dice;
    }

    @Override
    public RerollShield copy() {
        return new RerollShield(figureID, cardID, value, dice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RerollShield shield = (RerollShield) o;
        return dice == shield.dice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dice);
    }
}

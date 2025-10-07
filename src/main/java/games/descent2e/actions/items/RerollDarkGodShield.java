package games.descent2e.actions.items;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RerollDarkGodShield extends Shield {

    List<Integer> dice = new ArrayList<>();

    public RerollDarkGodShield(int figureID, int cardID, List<Integer> dice) {
        super(figureID, cardID, 0);
        this.dice = dice;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        // Create a new Dice Pool, roll it, then compare results
        // If the new Pool's results are better than the old Pool, replace the Dice

        List<DescentDice> oldDice = new ArrayList<>();
        List<Integer> newFaces = new ArrayList<>();
        int oldResult = 0;
        int newResult = 0;
        for (int d : dice) {
            DescentDice die = dgs.getDefenceDicePool().getDice(d);
            oldDice.add(die);
            oldResult += die.getShielding();

            DicePool reroll = DicePool.constructDicePool(new HashMap<DiceType, Integer>() {{
                put(die.getColour(), 1);
            }});
            reroll.roll(dgs.getRnd());
            newFaces.add((reroll.getDice(0).getFace()));
            newResult += reroll.getDice(0).getShielding();
        }

        //System.out.println(newResult > oldResult ? "True" : "False");
        if (newResult > oldResult) {
            for (int i = 0; i < dice.size(); i++) {
                DescentDice die = dgs.getDefenceDicePool().getDice(dice.get(i));
                die.setFace(newFaces.get(i));
            }
        }

        return super.execute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String name = gameState.getComponentById(cardID).toString();
        DescentDice die = ((DescentGameState) gameState).getDefenceDicePool().getDice(dice.get(0));
        name += ": Reroll Dice: " + die.getColour() + " (Shield: " + die.getShielding() + ")";
        for (int i = 1; i < dice.size(); i++) {
            die = ((DescentGameState) gameState).getDefenceDicePool().getDice(dice.get(i));
            name += " and Dice: " + die.getColour() + " (Shield: " + die.getShielding() + ")";
        }
        return name;
    }

    @Override
    public String toString() {
       String string = "Shield of the Dark God " + cardID + ": Reroll dice" + dice.get(0);
       for (int i = 1; i < dice.size(); i++) {
           string += " and " + dice.get(i);
       }
       return string;
    }

    @Override
    public RerollDarkGodShield copy() {
        return new RerollDarkGodShield(figureID, cardID, dice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RerollDarkGodShield shield = (RerollDarkGodShield) o;
        return dice == shield.dice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dice);
    }
}

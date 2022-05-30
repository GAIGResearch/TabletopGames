package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.components.DescentDice;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// TODO: allows rolling dice with some effect
public class RollDie extends AbstractAction {
    HashMap<String, Integer> desiredDice;
    HashMap<String, List<DescentDice>> actualDice;

    public RollDie(HashMap<String, Integer> dice) {
        // Maps from color of die to how many of that color should be rolled
        this.desiredDice = dice;
        this.actualDice = new HashMap<>();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState) gs;
        Random r = new Random();
        r.setSeed(dgs.getGameParameters().getRandomSeed());
        List<DescentDice> gameDice = dgs.getDice();
        for (Map.Entry<String, Integer> entry : desiredDice.entrySet()) {
            List<DescentDice> result = gameDice.stream()
                    .filter(a -> Objects.equals(a.getColour(), entry.getKey()))
                    .collect(Collectors.toList());
            int amount = entry.getValue();
            for (int i = 0; i < amount; i++) {
                DescentDice d = result.get(i);
                d.roll(r);
                if (actualDice.get(entry.getKey()) == null) {
                    List<DescentDice> init = new ArrayList<DescentDice>();
                    actualDice.put(entry.getKey(), init);
                }
                List<DescentDice> rolled = actualDice.get(entry.getKey());
                rolled.add(d);
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new RollDie(desiredDice);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RollDie;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Roll die";
    }
}

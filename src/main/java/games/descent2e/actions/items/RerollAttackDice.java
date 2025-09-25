package games.descent2e.actions.items;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.searchcards.UsePowerPotion;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;
import games.descent2e.components.Figure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RerollAttackDice extends DescentAction {
    
    int userID;
    List<Integer> dice = new ArrayList<>();
    
    public RerollAttackDice(int userID, int dice) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.dice = List.of(dice);
    }

    public RerollAttackDice(int userID, List<Integer> dice) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.dice = dice;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        List<DescentDice> oldDice = new ArrayList<>();
        for (Integer d : dice) {
            oldDice.add(dgs.getAttackDicePool().getDice(d));
        }

        List<Integer> newFaces = reroll(dgs, oldDice);
        for (int i = 0; i < oldDice.size(); i++)
        {
            oldDice.get(i).setFace(newFaces.get(i));
        }

        Figure f = (Figure) dgs.getComponentById(userID);
        f.addActionTaken(toString());

        return true;
    }

    public List<Integer> reroll(DescentGameState dgs, List<DescentDice> dice) {
        List<Integer> newFaces = new ArrayList<>();
        for (DescentDice die : dice)
        {
            int face = die.getFace();
            DicePool reroll = DicePool.constructDicePool(new HashMap<DiceType, Integer>() {{
                put(die.getColour(), 1);
            }});
            reroll.roll(dgs.getRnd());
            //System.out.println("Old Result: " + face + " (Range: " + die.getRange() + ", Surge: " + die.getSurge() + ", Damage: " + die.getDamage() +")");
            face = reroll.getDice(0).getFace();
            //System.out.println("New Result: " + face + " (Range: " + reroll.getDice(0).getRange() + ", Surge: " + reroll.getDice(0).getSurge() + ", Damage: " + reroll.getDice(0).getDamage() + ")");
            newFaces.add(face);
        }
        return newFaces;
    }

    @Override
    public DescentAction copy() {
        return new RerollAttackDice(userID, dice);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RerollAttackDice other)) return false;
        return userID == other.userID && Objects.equals(dice, other.dice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, dice);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DescentDice die = ((DescentGameState) gameState).getAttackDicePool().getDice(dice.get(0));
        String string = "Reroll Dice: " + die.getColour() + " (Face: " + die.getFace() + ", Range: " + die.getRange() + ", Damage: " + die.getDamage() + ", Surge: " + die.getSurge() + ")";
        for (int i = 1; i < dice.size(); i++) {
            die = ((DescentGameState) gameState).getAttackDicePool().getDice(dice.get(i));
            string += " and " + die.getColour() + " (Face: " + die.getFace() + ", Range: " + die.getRange() + ", Damage: " + die.getDamage() + ", Surge: " + die.getSurge() + ")";
        }
        return string;
    }

    @Override
    public String toString() {
        String string = "REROLL_DICE_" + dice.get(0);
        for (int i = 1; i < dice.size(); i++) {
            string += "_&_" + dice.get(i);
        }
        return string;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (dice.isEmpty()) return false;

        Figure f = dgs.getActingFigure();
        if (f.getComponentID() != userID) return false;

        IExtendedSequence action = dgs.currentActionInProgress();
        if (action == null) return false;
        return (f.getOwnerId() == action.getCurrentPlayer(dgs));
        //return (action instanceof MeleeAttack);
    }

    public List<Integer> getDice() {
        return dice;
    }

    public int getDiceSize() {
        return getDice().size();
    }
}

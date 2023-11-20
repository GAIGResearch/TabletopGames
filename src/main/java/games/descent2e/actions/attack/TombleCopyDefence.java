package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;
import games.descent2e.components.Hero;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TombleCopyDefence extends DescentAction {

    // Tomble Burrowell Hero Ability
    String heroName = "Tomble Burrowell";
    Hero tomble, ally;
    List<DescentDice> defenceDice;
    public TombleCopyDefence(Hero hero, Hero ally) {
        super(Triggers.ROLL_OTHER_DICE);
        this.tomble = hero;
        this.ally = ally;
        this.defenceDice = hero.getDefenceDice().getComponents();

    }

    @Override
    public boolean execute(DescentGameState dgs) {
        DicePool newDice = HeroAbilities.tomble(dgs, ally);
        dgs.setDefenceDicePool(newDice);
        return true;
    }

    @Override
    public TombleCopyDefence copy() {
        return new TombleCopyDefence(tomble, ally);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // We can only copy an ally's Defence Pool if they are adjacent to us
        Vector2D position = tomble.getPosition();
        Vector2D otherPosition = ally.getPosition();
        if (Math.abs(position.getX() - otherPosition.getX()) <= 1 && Math.abs(position.getY() - otherPosition.getY()) <= 1) {

            // This is to check if any new dice have been added yet, to ensure we only copy one ally's pool per attack
            // On the first attempt, the current Defence Pool should be identical to Tomble's personal Defence Pool
            // By adding our chosen ally's Defence Dice to Tomble's Pool, it should always be larger than the current Defence Pool
            // Once the new dice have been added, the current Pool should be identical to the merged Pool
            // Therefore any further attempts will always return false
            List<DescentDice> myDice = new ArrayList<>();
            myDice.addAll(dgs.getDefenceDicePool().getComponents());

            List<DescentDice> mergedDice = new ArrayList<>();
            mergedDice.addAll(tomble.getDefenceDice().getComponents());
            mergedDice.addAll(ally.getDefenceDice().getComponents());

            return myDice.size() < mergedDice.size();
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TombleCopyDefence) {
            TombleCopyDefence other = (TombleCopyDefence) obj;
            return other.tomble.equals(tomble) && other.ally.equals(ally) && other.defenceDice.equals(defenceDice);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tomble, ally, defenceDice);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String allyName = ally.getName().replace("Hero: ", "");
        List<DiceType> diceTypes = new ArrayList<>();
        for (DescentDice dice : ally.getDefenceDice().getComponents()) {
            diceTypes.add(dice.getColour());
        }
        return "Hero Ability: Add " + allyName + "'s dice (" + diceTypes + ") to Defence Pool.";
    }

    public String toString() {
        return "COPY_DEFENSE_DICE_FROM_" + ally.getComponentID() + " : " + tomble.getComponentID();
    }
}
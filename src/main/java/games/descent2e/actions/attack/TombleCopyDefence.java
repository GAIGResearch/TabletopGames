package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;
import games.descent2e.components.Figure;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TombleCopyDefence extends DescentAction {

    // Tomble Burrowell Hero Ability
    int tomble, ally;
    public TombleCopyDefence(int hero, int ally) {
        super(Triggers.ROLL_OTHER_DICE);
        this.tomble = hero;
        this.ally = ally;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        DicePool newDice = HeroAbilities.tomble(dgs, tomble, ally);
        dgs.setDefenceDicePool(newDice);
        ((Figure) dgs.getComponentById(tomble)).addActionTaken(toString());
        return true;
    }

    @Override
    public TombleCopyDefence copy() {
        return new TombleCopyDefence(tomble, ally);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // We can only copy an ally's Defence Pool if they are adjacent to us
        Figure tomble = (Figure) dgs.getComponentById(this.tomble);
        Figure ally = (Figure) dgs.getComponentById(this.ally);
        Vector2D position = tomble.getPosition();
        Vector2D otherPosition = ally.getPosition();
        if (DescentHelper.inRange(position, otherPosition, 1)) {

            // This is to check if any new dice have been added yet, to ensure we only copy one ally's pool per attack
            // On the first attempt, the current Defence Pool should be identical to Tomble's personal Defence Pool
            // By adding our chosen ally's Defence Dice to Tomble's Pool, it should always be larger than the current Defence Pool
            // Once the new dice have been added, the current Pool should be identical to the merged Pool
            // Therefore any further attempts will always return false
            List<DescentDice> myDice = new ArrayList<>(dgs.getDefenceDicePool().getComponents());

            List<DescentDice> mergedDice = new ArrayList<>();
            mergedDice.addAll(tomble.getDefenceDice().getComponents());
            mergedDice.addAll(ally.getDefenceDice().getComponents());

            return myDice.size() < mergedDice.size();
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TombleCopyDefence other) {
            return other.tomble == tomble && other.ally == ally;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tomble, ally);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure ally = (Figure) gameState.getComponentById(this.ally);
        String allyName = ally.getName().replace("Hero: ", "");
        List<DiceType> diceTypes = new ArrayList<>();
        for (DescentDice dice : ally.getDefenceDice().getComponents()) {
            diceTypes.add(dice.getColour());
        }
        return "Hero Ability: Add " + allyName + "'s dice (" + diceTypes + ") to Defence Pool.";
    }

    public String toString() {
        return "COPY_DEFENSE_DICE_FROM_" + ally + " : " + tomble;
    }
}
package games.descent2e.components;

import core.CoreConstants;
import core.components.Component;
import core.interfaces.IComponentContainer;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameData;
import games.descent2e.DescentGameState;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class DicePool extends Component implements IComponentContainer<DescentDice> {

    public static DicePool empty = new DicePool(Collections.emptyList());
    public static DicePool revive;
    public static DicePool heal;

    List<DescentDice> dice;
    boolean rolled = false;
    int[] rerolls;

    public static DicePool constructDicePool(String... args) {
        Map<DiceType, Integer> wip = new LinkedHashMap<>();
        for (String d : args) {
            DiceType dt = DiceType.valueOf(d.toUpperCase(Locale.ROOT));
            if (wip.containsKey(dt))
                wip.put(dt, wip.get(dt) + 1);
            else
                wip.put(dt, 1);
        }
        return DicePool.constructDicePool(wip);
    }

    public static DicePool constructDicePool(Map<DiceType, Integer> details) {
        // Get / Set variables
        List<DescentDice> dice = new ArrayList<>();

        // Find right dice to act upon
        for (Map.Entry<DiceType, Integer> entry : details.entrySet()) {
            DescentDice result = DescentDice.masterDice.stream()
                    .filter(a -> a.getColour() == entry.getKey())
                    .findFirst().orElseThrow(() -> new AssertionError("Die not found : " + entry.getKey()));
            int amount = entry.getValue();

            for (int i = 0; i < amount; i++)
                dice.add(result.copy());

        }
        return new DicePool(dice);
    }

    public DicePool(List<DescentDice> dice) {
        super(CoreConstants.ComponentType.AREA, "DicePool");
        this.dice = dice.stream().map(DescentDice::copy).collect(Collectors.toList());
        rerolls = new int[dice.size()];
    }
    private DicePool(int componentID) {
        super(CoreConstants.ComponentType.AREA, componentID);
    }

    public void roll(Random r) {
        rolled = true;
        for (DescentDice d : dice)
            d.roll(r);
    }
    public void setFace(int number, int face) {
        dice.get(number).setFace(face);
    }

    public int getNumber(DiceType type) {
        return (int) dice.stream().filter(d -> d.getColour() == type).count();
    }
    public int getDamage() {
        return dice.stream().mapToInt(DescentDice::getDamage).sum();
    }
    public int getShields() {return dice.stream().mapToInt(DescentDice::getShielding).sum();}
    public int getSurge() {
        return dice.stream().mapToInt(DescentDice::getSurge).sum();
    }
    public int getRange() {return dice.stream().mapToInt(DescentDice::getRange).sum();}
    public DescentDice getDice(int i) {
        return dice.get(i);
    }
    public void setDice(List<DescentDice> dice)
    {
        this.dice = dice;
    }
    public boolean hasRolled() {
        return rolled;
    }

    @Override
    public DicePool copy() {
        DicePool retValue = new DicePool(this.componentID);
        retValue.dice = dice.stream().map(DescentDice::copy).collect(Collectors.toList());
        retValue.rerolls = rerolls.clone();
        retValue.rolled = rolled;
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public List<DescentDice> getComponents() {
        return dice;
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
        // as yet no Dice are ever rolled secretly
    }

    @Override
    public String toString() {
        return String.format("Range: %d, Damage: %d, Surge: %d, Shields: %d", getRange(), getDamage(), getSurge(), getShields());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DicePool dicePool = (DicePool) o;
        return rolled == dicePool.rolled && Objects.equals(dice, dicePool.dice) && Arrays.equals(rerolls, dicePool.rerolls);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dice, rolled);
        result = 31 * result + Arrays.hashCode(rerolls);
        return result;
    }
}

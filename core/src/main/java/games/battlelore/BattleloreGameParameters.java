package games.battlelore;

import core.AbstractParameters;
import java.util.Objects;

public class BattleloreGameParameters extends AbstractParameters {
    String dataPath;
    public int hexWidth = 12; //A..L in odd numbers, A..K in even numbers.
    public int hexHeight = 9; //1-9
    public int troopCountInSquad = 3;
    public final int WIN_SCORE = 4;
    private int meleeRange = 1;
    private int rangedRange = 5;

    public BattleloreGameParameters(String dataPath) {
        this.dataPath = dataPath;
        setMaxRounds(100);
    }

    @Override
    protected AbstractParameters _copy() {
        BattleloreGameParameters copy = new BattleloreGameParameters(dataPath);
        copy.hexWidth = hexWidth;
        copy.hexHeight = hexHeight;
        return copy;
    }

    public int getTroopRange(boolean isMelee){
        return isMelee ? meleeRange : rangedRange;
    }

    public boolean isWeakAttacker(int attackerSize){
        return attackerSize <= 1;
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) {return false;}
        if (super.equals(o)) return false;

        BattleloreGameParameters that = (BattleloreGameParameters) o;
        return hexWidth == that.hexWidth &&
                hexHeight == that.hexHeight &&
                dataPath.equals(that.dataPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hexWidth, hexHeight, dataPath);
    }
}

package games.toads;

import breeze.signal.OptMethod;

public class BattleResult {

    private final ToadCard attackerField;
    private final ToadCard defenderField;
    private final ToadCard attackerFlank;
    private final ToadCard defenderFlank;

    public BattleResult(ToadCard attackerField, ToadCard defenderField, ToadCard attackerFlank, ToadCard defenderFlank) {
        this.attackerField = attackerField;
        this.defenderField = defenderField;
        this.attackerFlank = attackerFlank;
        this.defenderFlank = defenderFlank;
    }

    int[] calculate(ToadGameState state) {

        ToadParameters params = (ToadParameters) state.getGameParameters();

        int[] result = new int[2];
        int AField = attackerField.value;
        int AFlank = attackerFlank.value;
        int DField = defenderField.value;
        int DFlank = defenderFlank.value;


        if (attackerField.ability != null) {
            AField = attackerField.ability.updatedValue(attackerField.value, defenderField.value, true);
        }
        if (defenderField.ability != null) {
            DField = defenderField.ability.updatedValue(defenderField.value, attackerField.value, false);
        }
        if (attackerFlank.ability != null) {
            AFlank = attackerFlank.ability.updatedValue(attackerFlank.value, defenderFlank.value, true);
        }
        if (defenderFlank.ability != null) {
            DFlank = defenderFlank.ability.updatedValue(defenderFlank.value, attackerFlank.value, false);
        }

        if (AField > DField) {
            result[0]++;
        } else if (AField < DField) {
            result[1]++;
        }
        if (AFlank > DFlank) {
            result[0]++;
        } else if (AFlank < DFlank) {
            result[1]++;
        }
        return result;
    }

}

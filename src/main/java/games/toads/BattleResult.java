package games.toads;

import games.toads.abilities.*;

public class BattleResult {

    private ToadCard attackerField;
    private ToadCard defenderField;
    private ToadCard attackerFlank;
    private ToadCard defenderFlank;
    private final int attacker;

    public BattleResult(int attacker, ToadCard attackerField, ToadCard defenderField, ToadCard attackerFlank, ToadCard defenderFlank) {
        this.attackerField = attackerField;
        this.defenderField = defenderField;
        this.attackerFlank = attackerFlank;
        this.defenderFlank = defenderFlank;
        this.attacker = attacker;
    }

    /**
     *
     * @param state Game State
     * @return int[] with the number of battles won by each player (in player order)
     */
    int[] calculate(ToadGameState state) {

        ToadParameters params = (ToadParameters) state.getGameParameters();
        int round = state.getRoundCounter();

        boolean[] tricksterUsed = new boolean[2];

        if (params.useTactics && !(attackerFlank.ability instanceof Assassin) && !(defenderFlank.ability instanceof Assassin)) {
            // we apply tactics if this is enabled, and neither player has played an Assassin (which negates the tactics of the other side)

            // For the moment (given small number of cards), I'll hard-code this
            if (attackerFlank.value == 3) { // Trickster
                ToadCard temp = attackerField;
                attackerField = attackerFlank;
                attackerFlank = temp;
                tricksterUsed[0] = true;
            }
            if (defenderFlank.value == 3) { // Trickster
                ToadCard temp = defenderField;
                defenderField = defenderFlank;
                defenderFlank = temp;
                tricksterUsed[1] = true;
            }

            if (attackerFlank.value == 2 && !tricksterUsed[0]) { // Scout
                state.seeOpponentsHand(attacker);
            }
            if (defenderFlank.value == 2 && !tricksterUsed[1]) { // Scout
                state.seeOpponentsHand(1 - attacker);
            }
        }


        int[] result = new int[2];
        int AField = attackerField.value;
        int AFlank = attackerFlank.value;
        int DField = defenderField.value;
        int DFlank = defenderFlank.value;

        if (params.useTactics && !(attackerFlank.ability instanceof Assassin) && !(defenderFlank.ability instanceof Assassin)) {
            // we apply tactics if this is enabled, and neither player has played an Assassin (which negates the tactics of the other side)

            // For the moment (given small number of cards), I'll hard-code this
            if (!tricksterUsed[0]) {
                if (attackerFlank.value == 5) { // Berserker
                    AFlank += state.battlesWon[round][1 - attacker];
                }
                if (attackerFlank.value == 6 && !(attackerField.ability instanceof Bomb)) { // Icon Bearer
                    AField += 1;
                }
            }

            if (!tricksterUsed[1]) {
                if (defenderFlank.value == 5) { // Berserker
                    DFlank += state.battlesWon[round][attacker];
                }
                if (defenderFlank.value == 6 && !(defenderField.ability instanceof AssaultCannon)) { // Icon Bearer
                    DField += 1;
                }
            }
        }
        if (attackerField.ability != null) {
            AField += attackerField.ability.deltaToValue(attackerField.value, defenderField.value, true);
        }
        if (attackerFlank.ability != null) {
            AFlank += attackerFlank.ability.deltaToValue(attackerFlank.value, defenderFlank.value, true);
        }
        if (defenderField.ability != null) {
            DField += defenderField.ability.deltaToValue(defenderField.value, attackerField.value, false);
        }
        if (defenderFlank.ability != null) {
            DFlank += defenderFlank.ability.deltaToValue(defenderFlank.value, attackerFlank.value, false);
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
        int[] retValue = new int[2];
        // now put in correct player order (result is attacker/defender)
        retValue[attacker] = result[0];
        retValue[1 - attacker] = result[1];
        return retValue;
    }

}

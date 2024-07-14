package games.toads;

import games.toads.abilities.*;

import java.util.Arrays;

public class BattleResult {

    private ToadCard attackerField;
    private ToadCard defenderField;
    private ToadCard attackerFlank;
    private ToadCard defenderFlank;
    private final int attacker;
    public boolean[] frogOverride = new boolean[2];

    public BattleResult(int attacker, ToadCard attackerField, ToadCard defenderField, ToadCard attackerFlank, ToadCard defenderFlank) {
        this.attackerField = attackerField;
        this.defenderField = defenderField;
        this.attackerFlank = attackerFlank;
        this.defenderFlank = defenderFlank;
        this.attacker = attacker;
    }

    /**
     * @param state Game State
     * @return int[] with the number of battles won by each player (in player order)
     */
    int[] calculate(ToadGameState state) {

        ToadParameters params = (ToadParameters) state.getGameParameters();
        int round = state.getRoundCounter();

        boolean[] tricksterUsed = new boolean[2];
        if (params.useTactics) {
            // assassins copy their ally's tactics
            if (attackerFlank.tactics instanceof Assassin) {
                attackerFlank = new ToadCard("Assassin with copied tactics", attackerFlank.value, attackerFlank.ability, attackerField.tactics);
            }
            if (defenderFlank.tactics instanceof Assassin) {
                defenderFlank = new ToadCard("Assassin with copied tactics", defenderFlank.value, defenderFlank.ability, defenderField.tactics);
            }
        }
        boolean saboteurStopsTactics = (attackerFlank.tactics instanceof Saboteur || defenderFlank.tactics instanceof Saboteur);

        if (params.useTactics && !saboteurStopsTactics) {
            // we apply tactics if this is enabled, and neither player has played a Saboteur (which negates the tactics of the other side)

            // we first swap cards with Trickster before recording the base values

            // For the moment (given small number of cards), I'll hard-code this
            if (attackerFlank.tactics instanceof Trickster) { // Trickster
                ToadCard temp = attackerField;
                attackerField = attackerFlank;
                attackerFlank = temp;
                tricksterUsed[0] = true;
            }
            if (defenderFlank.tactics instanceof Trickster) { // Trickster
                ToadCard temp = defenderField;
                defenderField = defenderFlank;
                defenderFlank = temp;
                tricksterUsed[1] = true;
            }
        }

        // then we record the base battle results
        int[] result = new int[2];
        int AField = attackerField.value;
        int AFlank = attackerFlank.value;
        int DField = defenderField.value;
        int DFlank = defenderFlank.value;

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

        if (params.useTactics && !saboteurStopsTactics) {
            // we apply tactics if this is enabled, and neither player has played a Saboteur (which negates the tactics of the other side)
            // For the moment (given small number of cards), I'll hard-code this

            if (tricksterUsed[0]) {
                AField += attackerFlank.value / 2;
            }
            if (tricksterUsed[1]) {
                DField += defenderFlank.value / 2;
            }

            if (!tricksterUsed[0]) {
                if (attackerFlank.tactics instanceof Scout) { // Scout
                    state.seeOpponentsHand(attacker);
                    AField++;
                }
                if (attackerFlank.tactics instanceof Berserker) { // Berserker
                    AFlank += state.battlesWon[round][1 - attacker];
                }
                if (attackerFlank.tactics instanceof GeneralOne) {
                    frogOverride[0] = true;
                }
                if (attackerFlank.tactics instanceof GeneralTwo) {
                    AField += state.battlesTied[round];
                }
                // Now we apply the IconBearer's Ally activation ability
                if (attackerFlank.tactics instanceof IconBearer) {
                    if (attackerField.tactics instanceof Berserker) {
                        AField += state.battlesWon[round][1 - attacker];
                    }
                    if (attackerField.tactics instanceof Scout) {
                        state.seeOpponentsHand(attacker);
                        AFlank++;
                    }
                    if (attackerField.tactics instanceof GeneralOne) {
                        frogOverride[0] = true;
                    }
                    if (attackerField.tactics instanceof GeneralTwo) {
                        AFlank += state.battlesTied[round];
                    }
                }
            }

            if (!tricksterUsed[1]) {
                if (defenderFlank.tactics instanceof Scout) { // Scout
                    state.seeOpponentsHand(1 - attacker);
                    DField++;
                }
                if (defenderFlank.tactics instanceof Berserker) { // Berserker
                    DFlank += state.battlesWon[round][attacker];
                }
                if (defenderFlank.tactics instanceof GeneralOne) {
                    frogOverride[1] = true;
                }
                if (defenderFlank.tactics instanceof GeneralTwo) {
                    DField += state.battlesTied[round];
                }

                if (defenderFlank.tactics instanceof IconBearer) {
                    if (DField == AField - 1)
                        DField++;

                    if (defenderField.tactics instanceof Berserker) {
                        DField += state.battlesWon[round][attacker];
                    }
                    if (defenderField.tactics instanceof Scout) {
                        state.seeOpponentsHand(1 - attacker);
                        DFlank++;
                    }
                    if (defenderField.tactics instanceof GeneralOne) {
                        frogOverride[1] = true;
                    }
                    if (defenderField.tactics instanceof GeneralTwo) {
                        DFlank += state.battlesTied[round];
                    }
                }
            }

            // then we apply the IconBearer's tie-breaking (which must be done after everything else)
            if (!tricksterUsed[0] && attackerFlank.tactics instanceof IconBearer) {
                if (AField == DField - 1)
                    AField++;

                if (attackerField.tactics instanceof IconBearer) {
                    if (AFlank == DFlank - 1)
                        AFlank++;
                }
            }
            if (!tricksterUsed[1] && defenderFlank.tactics instanceof IconBearer) {
                if (DField == AField - 1)
                    DField++;

                if (defenderField.tactics instanceof IconBearer) {
                    if (DFlank == AFlank - 1)
                        DFlank++;
                }
            }
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
        boolean[] temp = Arrays.copyOf(frogOverride, 2);
        frogOverride[attacker] = temp[0];
        frogOverride[1 - attacker] = temp[1];
        return retValue;
    }

}

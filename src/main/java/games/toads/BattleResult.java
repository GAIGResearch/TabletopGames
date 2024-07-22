package games.toads;

import core.interfaces.IExtendedSequence;
import games.toads.abilities.*;

import java.util.*;

public class BattleResult {

    private ToadCard attackerField;
    private ToadCard defenderField;
    private ToadCard attackerFlank;
    private ToadCard defenderFlank;
    private int AField;
    private int AFlank;
    private int DField;
    private int DFlank;
    private final int attacker;
    private final boolean[] frogOverride = new boolean[2];
    private final boolean[] activatedFields = new boolean[2];
    private final boolean[] activatedFlanks = new boolean[2];
    private final List<IExtendedSequence> postBattleActions = new ArrayList<>();
    private boolean battleComplete = false;

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

        if (battleComplete)
            throw new AssertionError("BattleResult object can only be used once");
        battleComplete = true;

        ToadParameters params = (ToadParameters) state.getGameParameters();
        int round = state.getRoundCounter();

        if (params.useTactics) {
            // assassins copy their ally's tactics
            if (attackerFlank.tactics instanceof Assassin) {
                attackerFlank = new ToadCard("Assassin with copied tactics", attackerFlank.value, ToadConstants.ToadCardType.ASSASSIN, attackerFlank.ability, attackerField.tactics);
            }
            if (defenderFlank.tactics instanceof Assassin) {
                defenderFlank = new ToadCard("Assassin with copied tactics", defenderFlank.value, ToadConstants.ToadCardType.ASSASSIN, defenderFlank.ability, defenderField.tactics);
            }
        }
        boolean saboteurStopsTactics = (attackerFlank.tactics instanceof Saboteur || defenderFlank.tactics instanceof Saboteur);
        activatedFlanks[0] = true;
        activatedFlanks[1] = true;

        // then we record the base battle results
        int[] result = new int[2];
        AField = attackerField.value;
        AFlank = attackerFlank.value;
        DField = defenderField.value;
        DFlank = defenderFlank.value;

        if (params.useTactics && !saboteurStopsTactics) {
            // we apply tactics if this is enabled, and neither player has played a Saboteur (which negates the tactics of the other side)

            // we first swap cards with Trickster before recording the base values

            // For the moment (given small number of cards), I'll hard-code this
            if (attackerFlank.tactics instanceof Trickster) { // Trickster
                swapFieldAndFlank(0);
            } else if (attackerFlank.tactics instanceof IconBearer && attackerField.tactics instanceof Trickster) {
                swapFieldAndFlank(0);
                activatedFlanks[0] = true;
            }

            if (defenderFlank.tactics instanceof Trickster) { // Trickster
                swapFieldAndFlank(1);
            } else if (defenderFlank.tactics instanceof IconBearer && defenderField.tactics instanceof Trickster) {
                swapFieldAndFlank(1);
                activatedFlanks[1] = true;
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

        if (params.useTactics && !saboteurStopsTactics) {
            // we apply tactics if this is enabled, and neither player has played a Saboteur (which negates the tactics of the other side)
            // For the moment (given small number of cards), I'll hard-code this

            if (activatedFields[0] && attackerField.tactics instanceof Trickster) { // Trickster
                AField += attackerFlank.value / 2;
            }
            if (activatedFields[1] && defenderField.tactics instanceof Trickster) { // Trickster
                DField += defenderFlank.value / 2;
            }
            if (activatedFlanks[0] && attackerFlank.tactics instanceof Trickster) { // Trickster
                AFlank += attackerField.value / 2;
            }
            if (activatedFlanks[1] && defenderFlank.tactics instanceof Trickster) { // Trickster
                DFlank += defenderField.value / 2;
            }

            if (activatedFlanks[0]) {
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
                if (attackerFlank.tactics instanceof AssaultCannon) {
                    postBattleActions.add(new AssaultCannonInterrupt(attacker));
                }
                // Now we apply the IconBearer's Ally activation ability
                if (attackerFlank.tactics instanceof IconBearer) {
                    activatedFields[0] = true;
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
                    if (attackerField.tactics instanceof AssaultCannon) {
                        postBattleActions.add(new AssaultCannonInterrupt(attacker));
                    }
                }
            }

            if (activatedFlanks[1]) {
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
                if (defenderFlank.tactics instanceof AssaultCannon) {
                    postBattleActions.add(new AssaultCannonInterrupt(1 - attacker));
                }

                if (defenderFlank.tactics instanceof IconBearer) {
                    activatedFields[1] = true;
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
                    if (defenderField.tactics instanceof AssaultCannon) {
                        postBattleActions.add(new AssaultCannonInterrupt(1 - attacker));
                    }
                }
            }

            // then we apply the IconBearer's tie-breaking (which must be done after everything else)
            if (activatedFlanks[0] && attackerFlank.tactics instanceof IconBearer) {
                if (AField == DField - 1)
                    AField++;

                if (attackerField.tactics instanceof IconBearer) {
                    if (AFlank == DFlank - 1)
                        AFlank++;
                }
            }
            if (activatedFlanks[1] && defenderFlank.tactics instanceof IconBearer) {
                if (DField == AField - 1)
                    DField++;

                if (defenderField.tactics instanceof IconBearer) {
                    if (DFlank == AFlank - 1)
                        DFlank++;
                }
            }

            if (activatedFields[0] && attackerField.tactics instanceof IconBearer) {
                if (AFlank == DFlank - 1)
                    AFlank++;
            }
            if (activatedFields[1] && defenderField.tactics instanceof IconBearer) {
                if (DFlank == AFlank - 1)
                    DFlank++;
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

    private void swapFieldAndFlank(int player) {

        // then activation state
        boolean tempBool = activatedFields[player];
        activatedFields[player] = activatedFlanks[player];
        activatedFlanks[player] = tempBool;

        // then also swap the values
        if (player == 0) {
            ToadCard temp = attackerField;
            attackerField = attackerFlank;
            attackerFlank = temp;
            int tempVal = AField;
            AField = AFlank;
            AFlank = tempVal;
        } else if (player == 1) {
            ToadCard temp = defenderField;
            defenderField = defenderFlank;
            defenderFlank = temp;
            int tempVal = DField;
            DField = DFlank;
            DFlank = tempVal;
        }

    }

    public boolean getFrogOverride(int player) {
        return frogOverride[player];
    }

    public List<IExtendedSequence> getPostBattleActions() {
        return postBattleActions;
    }
}

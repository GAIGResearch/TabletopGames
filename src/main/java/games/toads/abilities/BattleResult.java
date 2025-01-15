package games.toads.abilities;

import core.interfaces.IExtendedSequence;
import games.toads.*;
import games.toads.actions.AssaultCannonInterrupt;
import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.*;

public class BattleResult {

    ToadCard attackerField;
    ToadCard defenderField;
    ToadCard attackerFlank;
    ToadCard defenderFlank;
    int AField;
    int AFlank;
    int DField;
    int DFlank;
    final int attacker;
    final boolean[] frogOverride = new boolean[2];
    final boolean[] activatedFields = new boolean[2];
    final boolean[] activatedFlanks = new boolean[2];
    final List<IExtendedSequence> postBattleActions = new ArrayList<>();
    boolean battleComplete = false;
    boolean useTactics = false;
    ToadGameState state;
    PriorityQueue<Tactic> tacticsToApply = new PriorityQueue<>(Comparator.comparingInt(t -> t.priority));

    record Tactic(int priority, boolean isAttacker, boolean isFlank, ToadAbility.BattleEffect effect) {
    }

    public BattleResult(ToadGameState state, int attacker, ToadCard attackerField, ToadCard defenderField, ToadCard attackerFlank, ToadCard defenderFlank) {
        this.attackerField = attackerField;
        this.defenderField = defenderField;
        this.attackerFlank = attackerFlank;
        this.defenderFlank = defenderFlank;
        this.attacker = attacker;
        this.state = state;
    }

    /**
     * BattleResults is a combination data and logic class that calculates the result of a battle.
     * It returns the number of battles won by each player, and also stores any post-battle actions that need to be taken.
     * It is designed not to be copyable, with the main calculate method treated as 'atomic' from the
     * perspective of the forward model - so it does not support the taking of Actions during a battle.
     * These need to be taken before or after resolution.
     *
     * @return int[] with the number of battles won by each player (in player order)
     */
    public int[] calculate() {

        if (battleComplete)
            throw new AssertionError("BattleResult object can only be used once");
        battleComplete = true;

        ToadParameters params = (ToadParameters) state.getGameParameters();
        useTactics = (boolean) params.getParameterValue("useTactics");


        // then we record the base battle results
        int[] result = new int[2];
        AField = attackerField.value;
        AFlank = attackerFlank.value;
        DField = defenderField.value;
        DFlank = defenderFlank.value;


        if (useTactics) {
            activatedFlanks[0] = true;
            activatedFlanks[1] = true;
            // activate flank cards and add their tactics
            if (attackerFlank.tactics != null) {
                for (Pair<Integer, ToadAbility.BattleEffect> effect : attackerFlank.tactics.tactics()) {
                    tacticsToApply.add(new Tactic(effect.a, true, true, effect.b));
                }
            }
            if (defenderFlank.tactics != null) {
                for (Pair<Integer, ToadAbility.BattleEffect> effect : defenderFlank.tactics.tactics()) {
                    tacticsToApply.add(new Tactic(effect.a, false, true, effect.b));
                }
            }
        }

        // then apply tactics
        while (!tacticsToApply.isEmpty() && tacticsToApply.peek().priority < 0) {
            Tactic tactic = tacticsToApply.poll();
            tactic.effect.apply(tactic.isAttacker, tactic.isFlank, this);
        }

        // apply CardModifiers at priority 0
        if (attackerField.ability != null) {
            for (ToadAbility.CardModifier modifier : attackerField.ability.attributes()) {
                AField += modifier.apply(true, false, this);
            }
        }
        if (attackerFlank.ability != null) {
            for (ToadAbility.CardModifier modifier : attackerFlank.ability.attributes()) {
                AFlank += modifier.apply(true, true, this);
            }
        }
        if (defenderField.ability != null) {
            for (ToadAbility.CardModifier modifier : defenderField.ability.attributes()) {
                DField += modifier.apply(false, false, this);
            }
        }
        if (defenderFlank.ability != null) {
            for (ToadAbility.CardModifier modifier : defenderFlank.ability.attributes()) {
                DFlank += modifier.apply(false, true, this);
            }
        }

        // then apply tactics that occur after Card Modifiers
        while (!tacticsToApply.isEmpty()) {
            Tactic tactic = tacticsToApply.poll();
            tactic.effect.apply(tactic.isAttacker, tactic.isFlank, this);
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

    void swapFieldAndFlank(int player) {

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

        // we map any tactic for the cards to be identical, but with isFlank marker reversed
        List<Tactic> newTactics = tacticsToApply.stream().map(t -> {
            if (t.isAttacker() == (player == 0)) {
                return new Tactic(t.priority, t.isAttacker, !t.isFlank, t.effect);
            } else {
                return t;
            }
        }).toList();
        tacticsToApply.clear();
        tacticsToApply.addAll(newTactics);
    }

    public int getCurrentValue(boolean isAttacker, boolean isFlank) {
        return isAttacker ? (isFlank ? AFlank : AField) : (isFlank ? DFlank : DField);
    }

    public void addValue(boolean isAttacker, boolean isFlank, int value) {
        if (isAttacker) {
            if (isFlank) {
                AFlank += value;
            } else {
                AField += value;
            }
        } else {
            if (isFlank) {
                DFlank += value;
            } else {
                DField += value;
            }
        }
    }

    public boolean isActivated(boolean isAttacker, boolean isFlank) {
        return isAttacker ? (isFlank ? activatedFlanks[0] : activatedFields[0]) : (isFlank ? activatedFlanks[1] : activatedFields[1]);
    }

    public void setActivation(boolean isAttacker, boolean isFlank, boolean value) {
        if (isAttacker) {
            if (isFlank) {
                activatedFlanks[0] = value;
            } else {
                activatedFields[0] = value;
            }
        } else {
            if (isFlank) {
                activatedFlanks[1] = value;
            } else {
                activatedFields[1] = value;
            }
        }
        // and also add/remove the tactics
        if (useTactics) {
            if (value && getCard(isAttacker, isFlank).tactics != null)
                getCard(isAttacker, isFlank).tactics.tactics().forEach(effect -> tacticsToApply.add(new Tactic(effect.a, isAttacker, isFlank, effect.b)));
            else
                tacticsToApply.removeIf(t -> t.isAttacker() == isAttacker && t.isFlank() == isFlank);
        }
    }

    public ToadCard getOpponent(boolean isAttacker, boolean isFlank) {
        return getCard(!isAttacker, isFlank);
    }

    public ToadCard getAlly(boolean isAttacker, boolean isFlank) {
        return getCard(isAttacker, !isFlank);
    }

    public ToadCard getCard(boolean isAttacker, boolean isFlank) {
        return isAttacker ? (isFlank ? attackerFlank : attackerField) : (isFlank ? defenderFlank : defenderField);
    }

    public void setCard(boolean isAttacker, boolean isFlank, ToadCard card) {
        if (isAttacker) {
            if (isFlank) {
                AFlank += card.value - attackerFlank.value;
                attackerFlank = card;
            } else {
                AField += card.value - attackerField.value;
                attackerField = card;
            }
        } else {
            if (isFlank) {
                DFlank += card.value - defenderFlank.value;
                defenderFlank = card;
            } else {
                DField += card.value - defenderField.value;
                defenderField = card;
            }
        }
    }

    public boolean getFrogOverride(int player) {
        return frogOverride[player];
    }

    public List<IExtendedSequence> getPostBattleActions() {
        return postBattleActions;
    }
}

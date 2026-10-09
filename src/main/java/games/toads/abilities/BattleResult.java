package games.toads.abilities;

import core.interfaces.IExtendedSequence;
import games.toads.*;
import games.toads.ToadConstants.ToadCardType;
import games.toads.actions.AssaultCannonInterrupt;
import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.*;

public class BattleResult {

    ToadCard attackerField;
    ToadCard defenderField;
    ToadCard attackerFlank;
    ToadCard defenderFlank;
    double AField;
    double AFlank;
    double DField;
    double DFlank;
    final int attacker;
    final boolean[] frogOverride = new boolean[2];
    final boolean[] activatedFields = new boolean[2];
    final boolean[] activatedFlanks = new boolean[2];
    final List<IExtendedSequence> postBattleActions = new ArrayList<>();
    boolean battleComplete = false;
    boolean useTactics = false;
    ToadGameState state;
    PriorityQueue<Tactic> tacticsToApply = new PriorityQueue<>(Comparator.comparingInt(t -> t.priority));
    // the group of tactics being run, the values before it started, and the cards it has blocked
    List<Tactic> currentGroup = new ArrayList<>();
    double[] snapshot = new double[4];
    final List<boolean[]> pendingBlocks = new ArrayList<>();
    // tieBreak[side][lane]: side 0 is the Attacker, lane 0 the Field
    final boolean[][] tieBreak = new boolean[2][2];

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
        snapshot = new double[]{AField, AFlank, DField, DFlank};


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
        applyTactics(0);

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
        applyTactics(Integer.MAX_VALUE);

        for (boolean isFlank : new boolean[]{false, true}) {
            int winner = resolveLane(isFlank);
            if (winner >= 0)
                result[winner]++;
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

    /**
     * Runs the queued tactics with a priority below maxPriority, in groups that share a priority.
     */
    private void applyTactics(int maxPriority) {
        while (!tacticsToApply.isEmpty() && tacticsToApply.peek().priority < maxPriority) {
            int priority = tacticsToApply.peek().priority;
            currentGroup = new ArrayList<>();
            while (!tacticsToApply.isEmpty() && tacticsToApply.peek().priority == priority)
                currentGroup.add(tacticsToApply.poll());
            // the Attacker's tactics run first, and the Flank's before the Field's
            currentGroup.sort(Comparator.comparing((Tactic t) -> !t.isAttacker).thenComparing(t -> !t.isFlank));
            snapshot = new double[]{AField, AFlank, DField, DFlank};
            for (int i = 0; i < currentGroup.size(); i++) {
                Tactic tactic = currentGroup.get(i);
                tactic.effect.apply(tactic.isAttacker, tactic.isFlank, this);
            }
            // blocks take effect only once the whole group has run, so two Bodyguards block each other
            for (boolean[] target : pendingBlocks) {
                ToadCard card = getCard(target[0], target[1]);
                if (card.tactics == null || card.tactics.canBeBlocked())
                    setActivation(target[0], target[1], false);
            }
            pendingBlocks.clear();
        }
        currentGroup = new ArrayList<>();
    }

    /**
     * The winner of a lane: 0 for the Attacker, 1 for the Defender, or -1 for a tie.
     */
    private int resolveLane(boolean isFlank) {
        ToadCard attackerCard = getCard(true, isFlank);
        ToadCard defenderCard = getCard(false, isFlank);
        // a Siege Cannon ignores Strength: it loses in Defence, and in Attack wins unless it faces a Saboteur
        if (attackerCard.type == ToadCardType.SIEGE_CANNON)
            return defenderCard.type == ToadCardType.SABOTEUR ? 1 : 0;
        if (defenderCard.type == ToadCardType.SIEGE_CANNON)
            return 0;
        // an Assassin beats a General (the printed value of both Generals)
        if (attackerCard.type == ToadCardType.ASSASSIN && defenderCard.value == ToadConstants.ASSASSIN_KILLS)
            return 0;
        if (defenderCard.type == ToadCardType.ASSASSIN && attackerCard.value == ToadConstants.ASSASSIN_KILLS)
            return 1;
        double attackerValue = getCurrentValue(true, isFlank);
        double defenderValue = getCurrentValue(false, isFlank);
        if (attackerValue != defenderValue)
            return attackerValue > defenderValue ? 0 : 1;
        // a tie is broken only if exactly one side breaks ties
        int lane = isFlank ? 1 : 0;
        if (tieBreak[0][lane] != tieBreak[1][lane])
            return tieBreak[0][lane] ? 0 : 1;
        return -1;
    }

    /**
     * Swaps a player's Field and Flank cards, with their values, activation, queued Tactics and tie-break flags.
     * player is 0 for the Attacker, 1 for the Defender. (Public so that tests can build a swapping ability.)
     */
    public void swapFieldAndFlank(int player) {

        // then activation state
        boolean tempBool = activatedFields[player];
        activatedFields[player] = activatedFlanks[player];
        activatedFlanks[player] = tempBool;

        // then also swap the values
        if (player == 0) {
            ToadCard temp = attackerField;
            attackerField = attackerFlank;
            attackerFlank = temp;
            double tempVal = AField;
            AField = AFlank;
            AFlank = tempVal;
        } else if (player == 1) {
            ToadCard temp = defenderField;
            defenderField = defenderFlank;
            defenderFlank = temp;
            double tempVal = DField;
            DField = DFlank;
            DFlank = tempVal;
        }
        boolean tempTieBreak = tieBreak[player][0];
        tieBreak[player][0] = tieBreak[player][1];
        tieBreak[player][1] = tempTieBreak;

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
        // and likewise any tactics of the current group that have yet to run
        currentGroup.replaceAll(t -> t.isAttacker() == (player == 0) ? new Tactic(t.priority, t.isAttacker, !t.isFlank, t.effect) : t);
    }

    public double getCurrentValue(boolean isAttacker, boolean isFlank) {
        return isAttacker ? (isFlank ? AFlank : AField) : (isFlank ? DFlank : DField);
    }

    public void addValue(boolean isAttacker, boolean isFlank, double value) {
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

    /**
     * The value of a card as it stood before the current group of Tactics (those sharing a priority) started.
     */
    public double getSnapshotValue(boolean isAttacker, boolean isFlank) {
        return snapshot[(isAttacker ? 0 : 2) + (isFlank ? 1 : 0)];
    }

    /**
     * Blocks the Tactics of a card. The block takes effect once every Tactic of the current group has run, and has
     * no effect on a card whose ability cannot be blocked.
     */
    public void block(boolean isAttacker, boolean isFlank) {
        pendingBlocks.add(new boolean[]{isAttacker, isFlank});
    }

    /**
     * The card wins its lane if the lane is tied (unless the opposing card also breaks ties).
     */
    public void setTieBreak(boolean isAttacker, boolean isFlank) {
        tieBreak[isAttacker ? 0 : 1][isFlank ? 1 : 0] = true;
    }

    public boolean hasTieBreak(boolean isAttacker, boolean isFlank) {
        return tieBreak[isAttacker ? 0 : 1][isFlank ? 1 : 0];
    }

    public boolean getFrogOverride(int player) {
        return frogOverride[player];
    }

    public List<IExtendedSequence> getPostBattleActions() {
        return postBattleActions;
    }
}

package games.risk.gui;

import games.risk.RiskUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The chances in a Risk battle, worked out exactly: of each outcome of one roll, and of taking a territory by
 * attacking roll after roll (a Blitz), with six-sided dice and the defender winning ties (see
 * {@link RiskUtils#battleLosses}).
 */
public final class RiskOdds {

    // rollOdds by attack and defence dice
    private static final Map<Integer, double[]> rolls = new HashMap<>();

    private RiskOdds() {
    }

    /**
     * The chance that the attacker loses k armies in one roll, for k from 0 to min(attackDice, defendDice) (the
     * defender loses the rest of that many).
     */
    public static synchronized double[] roll(int attackDice, int defendDice) {
        return rolls.computeIfAbsent(attackDice * 10 + defendDice, k -> {
            int pairs = Math.min(attackDice, defendDice);
            double[] p = new double[pairs + 1];
            int n = attackDice + defendDice;
            int outcomes = (int) Math.pow(6, n);
            int[] a = new int[attackDice], d = new int[defendDice];
            for (int code = 0; code < outcomes; code++) {
                int c = code;
                for (int i = 0; i < attackDice; i++, c /= 6) a[i] = c % 6 + 1;
                for (int i = 0; i < defendDice; i++, c /= 6) d[i] = c % 6 + 1;
                p[RiskUtils.battleLosses(a, d)[0]] += 1.0 / outcomes;
            }
            return p;
        });
    }

    /**
     * The chance of taking a territory held by defenders armies, attacking from one with attackers armies (one of
     * which must stay behind) roll after roll with the most dice allowed, until the territory falls or one army is
     * left. The defender rolls the most dice allowed.
     */
    public static double capture(int attackers, int defenders, int maxAttackDice, int maxDefendDice) {
        return capture(attackers, defenders, maxAttackDice, maxDefendDice, new HashMap<>());
    }

    private static double capture(int attackers, int defenders, int maxAttackDice, int maxDefendDice,
                                  Map<Long, Double> memo) {
        if (defenders <= 0) return 1;
        if (attackers <= 1) return 0;
        long key = attackers * 100_000L + defenders;
        Double known = memo.get(key);
        if (known != null) return known;
        int a = Math.min(maxAttackDice, attackers - 1), d = Math.min(maxDefendDice, defenders);
        double[] p = roll(a, d);
        int pairs = Math.min(a, d);
        double result = 0;
        for (int lost = 0; lost <= pairs; lost++)
            result += p[lost] * capture(attackers - lost, defenders - (pairs - lost), maxAttackDice, maxDefendDice, memo);
        memo.put(key, result);
        return result;
    }

    /**
     * A percentage, rounded, with "<1" and ">99" for the extremes that are not certain.
     */
    static String percent(double p) {
        if (p <= 0) return "0%";
        if (p >= 1) return "100%";
        long r = Math.round(p * 100);
        if (r == 0) return "<1%";
        if (r == 100) return ">99%";
        return r + "%";
    }
}

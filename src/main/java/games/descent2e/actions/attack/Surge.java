package games.descent2e.actions.attack;

import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;

import java.util.Objects;
import java.util.function.BiConsumer;

// These should be immutable - do not add internal state
public enum Surge {
    // a = attack action, s = state (OBVIOUSLY!)
    RANGE_PLUS_1(1, (a, s) -> a.addRange(1)),
    RANGE_PLUS_2(1, (a, s) -> a.addRange(2)),
    RANGE_PLUS_3(1, (a, s) -> a.addRange(3)),
    DAMAGE_AND_RANGE_PLUS_1(1, (a, s) -> {
        a.addDamage(1);
        a.addRange(1);
    }),
    DAMAGE_PLUS_1(1, (a, s) -> a.addDamage(1)),
    DAMAGE_PLUS_2(1, (a, s) -> a.addDamage(2)),
    DAMAGE_PLUS_3(1, (a, s) -> a.addDamage(3)),
    DAMAGE_PLUS_5(2, (a, s) -> a.addDamage(5)),
    DAMAGE_PLUS_1_TWICE(2, (a, s) -> a.addDamage(2)),
    DAMAGE_PLUS_2_TWICE(2, (a, s) -> a.addDamage(4)),
    PIERCE_1(1, (a, s) -> a.addPierce(1)),
    PIERCE_2(1, (a, s) -> a.addPierce(2)),
    PIERCE_3(1, (a, s) -> a.addPierce(3)),
    MENDING_1(1, (a,s) -> a.addMending(1)),
    MENDING_2(1, (a,s) -> a.addMending(2)),
    MENDING_3(1, (a,s) -> a.addMending(3)),
    DISEASE(1, (a, s) -> a.setDiseasing(true)),
    IMMOBILIZE(1, (a, s) -> a.setImmobilizing(true)),
    POISON(1, (a, s) -> a.setPoisoning(true)),
    STUN(1, (a, s) -> a.setStunning(true)),
    SHADOW(1, (a, s) -> a.setShadow(true)),
    UNSEEN(1, (a, s) -> a.setShadow(true)),
    RECOVER_1_HEART(1, (a, s) -> a.addMending(1)),
    RECOVER_1_FATIGUE(1, (a, s) -> a.addFatigueHeal(1)),
    RECOVER_2_FATIGUE(1, (a, s) -> a.addFatigueHeal(2)),
    BLAST(1, (a, s) -> a.addInterruptAttack("Blast")),
    RUNIC_KNOWLEDGE(1, (a, s) -> {
        // Runic Knowledge can still be activated if we spent Fatigue to our maximum before we checked to remove it
        // Thus, this disables its effect if we have maximum Fatigue and cannot ForceFatigue
        Figure f = s.getActingFigure();
        if(!f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) {
            f.getAttribute(Figure.Attribute.Fatigue).increment();
            a.addDamage(2);
            return;
        }
        // Even if we're already at maximum Fatigue, if we used another Surge to recover, we can still use it
        if ((a.getFatigueHeal() > 0)) {
            a.addFatigueDamage(1);
            a.addDamage(2);
            return;
        }

        // Worse comes to worst, so long as we have the Health for it, we can Force Fatigue
        if ((f.getAttributeValue(Figure.Attribute.Health) - 1 > f.getAttributeMin(Figure.Attribute.Health))) {
            DescentHelper.forcedFatigue(s, f, "Runic Knowledge Surge");
            a.addDamage(2);
        }
    }),

    DEATH_RAGE(1, (a, s) -> {
        // Add +1 Damage for every 2 Health we are missing
        Figure f = s.getActingFigure();
        int extraDamage = (f.getAttributeMax(Figure.Attribute.Health) - f.getAttributeValue(Figure.Attribute.Health)) / 2;
        a.addDamage(extraDamage);
    }),

    FIRE_BREATH(1, (a, s) -> a.addInterruptAttack("Fire Breath")),

    // Lieutenants' Surges
    SUBDUE(1, (a, s) -> a.setSubdue(true)),
    BLOOD_CALL(1, (a, s) -> a.setLeeching(true)),
    WITHER(1, (a, s) -> a.addFatigueDamage(1)),
    KNOCKBACK(1, (a, s) -> a.addInterruptAttack("Knockback"));

    private final BiConsumer<MeleeAttack, DescentGameState> lambda;
    private final int surgesUsed;

    Surge(int surges, BiConsumer<MeleeAttack, DescentGameState> lambda) {
        this.lambda = lambda;
        surgesUsed = surges;
    }

    public void apply(MeleeAttack attack, DescentGameState state) {
        if (surgesUsed > attack.surgesToSpend) {
            throw new AssertionError(String.format("%s: Requires %d surges and we only have %d to spend.", toString(), surgesUsed, attack.surgesToSpend));
        }

        // TODO: Record which Surges have been used to avoid re-use!
        attack.surgesToSpend -= surgesUsed;

        lambda.accept(attack, state);
    }

    public int getSurgesUsed()
    {
        return surgesUsed;
    }

}

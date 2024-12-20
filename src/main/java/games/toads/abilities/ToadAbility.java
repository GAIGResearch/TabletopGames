package games.toads.abilities;

import utilities.Pair;

import java.util.List;
import java.util.Map;

public interface ToadAbility {

    /**
     * A BattleEffect modified the BattleResult directly with the effect of the card.
     * These can be quite wide-ranging.
     * The BattleEffect is only applied if the card is Activated (so this can be assumed)
     *
     */
    @FunctionalInterface
    interface BattleEffect {
        void apply(boolean isAttacker, boolean isFlank, BattleResult result);
    }

    /**
     * A CardModifier modifies the value of the card in the BattleResult.
     * This should not amend the BattleResult directly, but only return the modifier to be applied
     * to the value of the card played.
     * This is always called for all cards, and is independent of Activation (or Tactics being enabled)
     */
    @FunctionalInterface
    interface CardModifier {
        int apply(boolean isAttacker, boolean isFlank, BattleResult result);
    }

    /**
     * This returns a tactical effects implemented by this card (when Activated)
     * The Integer in the Pair is the priority order of the Effect
     * All BattleEffects will be applied in increasing order of priority (so lower numbers will be applied first)
     * It is possible that a BattleEffect adds more BattleEffects to the BattleResult, so this can be recursive
     * (for example if a card Activates another one)
     * Currently ALL such BattleEffects added will be applied, even if we have passed that point in the priority order
     *
     * There is one fixed point in this priory order:
     * Any CardModifiers will be implemented at Priority = 10.
     */
    default List<Pair<Integer, BattleEffect>> tactics() {
        return List.of();
    }

    /**
     * This returns a list of CardModifiers that will be applied to the card value in the BattleResult.
     */
    default List<CardModifier> attributes() {
        return List.of();
    }

}

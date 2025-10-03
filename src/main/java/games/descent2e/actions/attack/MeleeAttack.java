package games.descent2e.actions.attack;

import com.sun.xml.bind.v2.model.annotation.Quick;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.archetypeskills.*;
import games.descent2e.actions.items.RerollAttackDice;
import games.descent2e.actions.items.RerollShield;
import games.descent2e.actions.items.Shield;
import games.descent2e.actions.monsterfeats.*;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.*;

import static games.descent2e.DescentHelper.*;
import static games.descent2e.actions.Triggers.*;
import static games.descent2e.actions.archetypeskills.Lurk.addLurkDice;
import static games.descent2e.actions.archetypeskills.PrayerOfHealing.addArmorOfFaithDice;
import static games.descent2e.actions.archetypeskills.PrayerOfHealing.addDivineFuryDice;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;
import static games.descent2e.actions.attack.MeleeAttack.Interrupters.*;

public class MeleeAttack extends DescentAction implements IExtendedSequence {

    enum Interrupters {
        ATTACKER, DEFENDER, OTHERS, ALL
    }

    // The two argument constructor for AttackPhase specifies
    // which trigger is relevant (the first), and which players can use actions at this point (the second)
    public enum AttackPhase {
        NOT_STARTED,
        PRE_ATTACK_ROLL(START_ATTACK, DEFENDER),
        POST_ATTACK_ROLL(ROLL_OWN_DICE, ATTACKER),
        PRE_SURGE(CHANGE_SURGE, OTHERS),
        SURGE_DECISIONS(SURGE_DECISION, ATTACKER),
        ATTRIBUTE_TEST(FORCED, ATTACKER), // This is used for attribute tests that can be triggered by this attack
        PRE_DEFENCE_ROLL(ROLL_OTHER_DICE, DEFENDER),
        POST_DEFENCE_ROLL(ROLL_OWN_DICE, DEFENDER),
        PRE_DAMAGE(TAKE_DAMAGE, DEFENDER),
        POST_DAMAGE(ROLL_OWN_DICE, DEFENDER),
        NEXT_TARGET(FORCED, ATTACKER),        // This is only used in MultiAttacks, where we repeat the attack on the next target
        INTERRUPT_ATTACK(ACTION_POINT_SPEND, ATTACKER),   // This is used for extra attacks (e.g. Fire Breath) that can trigger from Surges from this attack
        ALL_DONE;

        public final Triggers interrupt;
        public final Interrupters interrupters;

        AttackPhase(Triggers interruptType, Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        AttackPhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    protected final int attackingFigure;
    protected int attackingPlayer;
    protected String attackerName;
    protected int defendingFigure;
    protected int defendingPlayer;
    protected String defenderName;

    protected int substituteFigure;
    protected int substitutePlayer;
    protected String substituteName;
    protected boolean substitute;

    protected AttackPhase phase = NOT_STARTED;
    protected int interruptPlayer;
    int surgesToSpend;
    int extraSurges;
    int extraRange, pierce, extraDamage, extraDefence, mending, fatigueHeal;
    int swapDefence = 0;
    protected boolean hasReach;
    boolean isDiseasing;
    boolean isImmobilizing;
    boolean isPoisoning;
    boolean isStunning;
    boolean leeching = false;
    boolean subdue = false;
    boolean hasShadow = false;
    boolean hitShadow = false;

    int damage;
    int range;
    boolean skip = false;
    boolean reduced = false;
    protected boolean isMelee = true;
    protected boolean isFreeAttack = false;

    // Some attacks have the 'ignores range' feature - they override this such that minRange = Integer.MIN_VALUE
    protected int minRange = 0;

    public String result = "";

    Set<Surge> surgesUsed = new HashSet<>();

    public MeleeAttack(int attackingFigure, int defendingFigure, boolean reach) {
        super(ACTION_POINT_SPEND);
        this.attackingFigure = attackingFigure;
        this.defendingFigure = defendingFigure;
        this.hasReach = reach;
    }

    @Override
    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);
        attackingPlayer = state.getComponentById(attackingFigure).getOwnerId();
        defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();

        phase = PRE_ATTACK_ROLL;
        interruptPlayer = attackingPlayer;
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        DicePool attackPool = attacker.getAttackDice();
        DicePool defencePool = defender.getDefenceDice();

        state.setAttackDicePool(attackPool);
        state.setDefenceDicePool(defencePool);

        // Check for Night Stalker passive
        // This is only applicable for non-adjacent attacks, i.e. Ranged Attacks or Melee with Reach
        if (!isMelee || hasReach)
        {
            NightStalker.addNightStalker(state, attacker, defender);
        }

        // Check for potential Archetype Skill bonuses
        if (attacker.hasBonus(DescentTypes.SkillBonus.DivineFury)) {
            addDivineFuryDice(state);
            attacker.removeBonus(DescentTypes.SkillBonus.DivineFury);       // Only for the first attack made with it
        }
        if (defender.hasBonus(DescentTypes.SkillBonus.ArmorOfFaith))
            addArmorOfFaithDice(state);

        if (defender.hasBonus(DescentTypes.SkillBonus.Lurk))
            addLurkDice(state);

        // Check if the target has the Shadow passive and if we are adjacent to it
        // If we are, the Hero must spend a Surge on Shadow to hit it
        // Only need to check once for all Monsters
        if (!hasShadow) {
            if (checkShadow(state, attacker, defender)) {
                hasShadow = true;
                SurgeAttackAction shadowSurge = new SurgeAttackAction(Surge.SHADOW, attackingFigure);
                if (!attacker.getAbilities().contains(shadowSurge)) {
                    attacker.addAbility(new SurgeAttackAction(Surge.SHADOW, attackingFigure));
                }
            } else {
                // Only enable the Shadow Surge if the target has the Shadow passive
                SurgeAttackAction shadowSurge = new SurgeAttackAction(Surge.SHADOW, attackingFigure);
                if (attacker.getAbilities().contains(shadowSurge)) {
                    attacker.removeAbility(shadowSurge);
                }
            }
        }

        // Likewise, Thief Heroes using Unseen require a Surge to be hit
        // So we check for that here
        SurgeAttackAction unseenSurge = new SurgeAttackAction(Surge.UNSEEN, attackingFigure);
        if (defender.hasBonus(DescentTypes.SkillBonus.Unseen))
        {
            if (!attacker.getAbilities().contains(unseenSurge))
            {
                attacker.addAbility(new SurgeAttackAction(Surge.UNSEEN, attackingFigure));
            }
        }
        else
        {
            if (attacker.getAbilities().contains(unseenSurge))
            {
                attacker.removeAbility(unseenSurge);
            }
        }

        result = getInitialResult(state);

        // Only count as an action if it is an Attack action, not a Free Attack action
        if (!isFreeAttack) attacker.getNActionsExecuted().increment();

        attacker.setHasAttacked(true);

        movePhaseForward(state);

        removeInterruptAttacks(state);

        // When executing a melee attack we need to:
        // 1) roll the dice (with possible interrupt beforehand)
        // 2) Possibly invoke re-roll options (via interrupts)
        // 3) and then - if there are any surges - decide how to use them
        // 4) and then get the target to roll their defence dice
        // 5) with possible rerolls
        // 6) then do the damage
        // 7) target can use items/abilities to modify damage

        return true;
    }

    protected void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                // System.out.println("Melee Attack Interrupt: " + phase + ", Interrupter:" + phase.interrupters + ", Interrupt:" + phase.interrupt + ", Player: " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                skip = false;
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == attackingPlayer) {
                    // we have completed the loop, and start again with the attacking player
                    executePhase(state);
                    interruptPlayer = attackingPlayer;
                }
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (phase.interrupt == null || phase.interrupters == null) return false;
        if (phase == SURGE_DECISIONS && surgesToSpend == 0) return false;
        // first we see if the interruptPlayer is one who may interrupt
        switch (phase.interrupters) {
            case ATTACKER:
                if (interruptPlayer != attackingPlayer)
                    return false;
                break;
            case DEFENDER:
                if (interruptPlayer != defendingPlayer)
                    return false;
                break;
            case OTHERS:
                if (interruptPlayer == attackingPlayer)
                    return false;
                break;
            case ALL:
                // always fine
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        if (phase == SURGE_DECISIONS || phase == POST_ATTACK_ROLL || phase == POST_DEFENCE_ROLL || phase == PRE_DAMAGE)
            return _computeAvailableActions(state).size() > 1;

        return !_computeAvailableActions(state).isEmpty();
    }

    protected void executePhase(DescentGameState state) {
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        // System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
                //throw new AssertionError("Should never be executed");
            case PRE_ATTACK_ROLL:
                // Get the weapon's stats, if they have any modifiers
                // Only Heroes and Lieutenants can have weapons

                attacker.setCurrentAttack(this);
                defender.setCurrentAttack(this);

                if (attacker instanceof Hero)
                    getWeaponBonuses(state, attackingFigure, true, true);
                // if (attacker instanceof Monster && ((Monster) attacker).isLieutenant()) getWeaponBonuses(state, attackingFigure, false, true);

                // Roll dice
                damageRoll(state);
                state.getActingFigure().setRerolled(false);
                phase = POST_ATTACK_ROLL;
                break;
            case POST_ATTACK_ROLL:
                phase = PRE_SURGE;
                break;
            case PRE_SURGE:
                // Any rerolls are executed via interrupts
                // once done we see how many surges we have to spend
                // If for whatever reason we must subtract Surges, clamp the minimum to 0
                surgesToSpend = Math.max(0, state.getAttackDicePool().getSurge() + extraSurges);
                phase = surgesToSpend > 0 ? SURGE_DECISIONS : ATTRIBUTE_TEST;
                break;
            case SURGE_DECISIONS:
                // any surge decisions are executed via interrupts
                surgesUsed.clear();
                phase = ATTRIBUTE_TEST;
                break;
            case ATTRIBUTE_TEST:
                // Yes, ordinarily, Attribute Tests are done either at the start of the attack (before dice are rolled)
                // or at the end of the attack (when we know if we've hit or not).
                // But because we delete the target upon killing them, if we kill the target we don't know if we have to perform the check.
                // Likewise putting a single stage here saves us having to do two stages before and after.
                // If it turns out to be that big of a deal I'll change it later, but for now it works well.
                phase = PRE_DEFENCE_ROLL;
                break;
            case PRE_DEFENCE_ROLL:

                // Allow us to swap again for another attack if needs be
                NotMe.setSwapped(false);

                if (attackMissed(state)) // no damage done, so can skip the defence roll
                {
                    //System.out.println(this.toString() + " (" + this.getString(state)  + ") missed!");
                    result += "Missed; Damage: " + damage + "; Range: " + range;
                    attacker.addActionTaken(toStringWithResult());

                    // Even if our initial attack missed, any addition interrupt attacks might still hit
                    phase = INTERRUPT_ATTACK;
                }
                else {

                    // If an interrupt action has caused a target substitution, call it here
                    checkSubstitute(state);

                    if (defender instanceof Hero) getWeaponBonuses(state, defendingFigure, true, false);
                    // if (defender instanceof Monster && ((Monster) defender).isLieutenant()) getWeaponBonuses(state, defendingFigure, false, false);

                    defenceRoll(state);
                    phase = POST_DEFENCE_ROLL;
                }
                break;
            case POST_DEFENCE_ROLL:
                calculateDamage(state);
                phase = PRE_DAMAGE;
                break;
            case PRE_DAMAGE:
                phase = POST_DAMAGE;
                break;
            case POST_DAMAGE:
                applyDamage(state);
                attacker.addActionTaken(toStringWithResult());
                phase = INTERRUPT_ATTACK;
                //System.out.println(this.toString() + " (" + this.getString(state)  + ") done!");
                break;
            case INTERRUPT_ATTACK:
                phase = ALL_DONE;
        }
        // and reset interrupts
    }

    protected void checkSubstitute(DescentGameState dgs)
    {
        if (substitute)
        {
            Figure attacker = (Figure) dgs.getComponentById(attackingFigure);
            Figure oldDefender = (Figure) dgs.getComponentById(defendingFigure);
            Figure newDefender = (Figure) dgs.getComponentById(substituteFigure);
            DicePool defencePool = newDefender.getDefenceDice();
            dgs.setDefenceDicePool(defencePool);
            if(!checkAdjacent(dgs, attacker, oldDefender))
                if ((newDefender instanceof Monster) && (((Monster) newDefender).hasPassive(MonsterAbilities.MonsterPassive.NIGHTSTALKER)))
                    NightStalker.addPool(dgs);

            defendingFigure = substituteFigure;
            defendingPlayer = substitutePlayer;
            defenderName = substituteName;
            result = getInitialResult(dgs);
        }
    }

    protected void defenceRoll(DescentGameState state) {
        state.getDefenceDicePool().roll(state.getRnd());
    }

    protected void damageRoll(DescentGameState state) {
        state.getAttackDicePool().roll(state.getRnd());
    }

    protected void getWeaponBonuses(DescentGameState state, int figure, boolean hero, boolean isAttacker)
    {
        if (hero)
        {
            Hero f = (Hero) state.getComponentById(figure);
            Deck<DescentCard> myEquipment = f.getAllEquipment();
            for (DescentCard equipment : myEquipment.getComponents())
            {
                // Apply Armour dice
                if (!isAttacker) {
                    if (equipment.equals(f.getArmor())) {
                        PropertyStringArray defense = ((PropertyStringArray) equipment.getProperty("defensePower"));
                        if (defense != null) {
                            List<DescentDice> dice = new ArrayList<>(state.getDefenceDicePool().copy().getComponents());
                            dice.addAll(DicePool.constructDicePool(defense.getValues()).getComponents());
                            DicePool newPool = new DicePool(dice);
                            state.setDefenceDicePool(newPool);
                        }
                    }
                }


                String action = String.valueOf(equipment.getProperty("action"));
                if(action.contains(";"))
                {
                    String[] actions = action.split(";");
                    for (String s : actions)
                    {
                        if (s.contains("Effect:"))
                        {
                            String[] effect = s.split(":");
                            switch (effect[1]) {
                                case "AdjacentFoeDamage" -> {
                                    if (isAttacker)
                                        if (checkAdjacent(state, f, (Figure) state.getComponentById(defendingFigure)))
                                            addDamage(Integer.parseInt(effect[2]));
                                }
                                case "Damage" -> {
                                    if (isAttacker)
                                        addDamage(Integer.parseInt(effect[2]));
                                }
                                case "Range" -> {
                                    if (isAttacker)
                                        addRange(Integer.parseInt(effect[2]));
                                }
                                case "Pierce" -> {
                                    if (isAttacker)
                                        addPierce(Integer.parseInt(effect[2]));
                                }
                                case "EmptyHand" -> {
                                    if (isAttacker) {
                                        Deck<DescentCard> hand = f.getHandEquipment();
                                        if (hand.getSize() == 1)
                                            if (hand.get(0).equals(equipment))
                                                addDamage(Integer.parseInt(effect[2]));
                                    }
                                }

                                case "RerollAttack" -> {
                                    if (isAttacker) {
                                        f.refreshCard(equipment);
                                        String colour = effect[2].toUpperCase();
                                        boolean any = colour.contains("ANY");
                                        for (int i = 0; i < state.getAttackDicePool().getSize(); i++) {
                                            if (any || state.getAttackDicePool().getDice(i).getColour().toString().toUpperCase().equals(colour)) {
                                                RerollAttackDice reroll = new RerollAttackDice(figure, equipment.getComponentID(), i);
                                                if (!f.hasAbility(reroll))
                                                    f.addAbility(reroll);
                                            }
                                        }
                                    }
                                }

                                case "Shield" -> {
                                    if (!isAttacker) {
                                        Shield shield = new Shield(figure, equipment.getComponentID(), Integer.parseInt(effect[2]));
                                        if (!f.hasAbility(shield))
                                            f.addAbility(shield);
                                    }
                                }
                                case "ShieldOrReroll" -> {
                                    if (!isAttacker) {
                                        Shield shield = new Shield(figure, equipment.getComponentID(), Integer.parseInt(effect[2]));
                                        if (!f.hasAbility(shield))
                                            f.addAbility(shield);
                                        for (int i = 0; i < state.getDefenceDicePool().getSize(); i++) {
                                            RerollShield rerollShield = new RerollShield(figure, equipment.getComponentID(), i);
                                            if (!f.hasAbility(rerollShield))
                                                f.addAbility(rerollShield);
                                        }
                                    }
                                }
                                case "ShieldAndReroll" -> {
                                    if (!isAttacker) {
                                        for (int i = 0; i < state.getDefenceDicePool().getSize(); i++) {
                                            RerollShield rerollShield = new RerollShield(figure, equipment.getComponentID(), Integer.parseInt(effect[2]), i);
                                            if (!f.hasAbility(rerollShield))
                                                f.addAbility(rerollShield);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else
        {
            // Monster f = (Monster) state.getComponentById(attackingFigure);
            return;
        }

    }

    protected void calculateDamage(DescentGameState state) {
        Figure attacker = (Figure) state.getComponentById(attackingFigure);

        damage = state.getAttackDicePool().getDamage() + extraDamage;

        if (attacker.hasBonus(DescentTypes.SkillBonus.Sneaky))
            damage += Sneaky.beSneaky(defendingFigure) ? 1 : 0;

        Figure defender = (Figure) state.getComponentById(defendingFigure);
        if (defender.hasBonus(DescentTypes.SkillBonus.ShieldMinimum)) {
            if (defender instanceof Hero hero)
                for (DescentDice dice : state.getDefenceDicePool().getComponents())
                    if (dice.getShielding() == 0) {
                        for (DescentCard item : hero.getAllEquipment()) {
                            PropertyString action = (PropertyString) item.getProperty("action");
                            if (action == null) {
                                action = (PropertyString) item.getProperty("passive");
                                if (action == null) continue;
                            }
                            if (action.value.contains("ShieldMinimum:"))
                                addDefence(Integer.parseInt(action.value.split("ShieldMinimum:")[1]));
                        }
                }
        }

        int defence = swapDefence > 0 ? swapDefence : state.getDefenceDicePool().getShields();
        defence += extraDefence - pierce;

        // Leoric of the Book's Hero Ability
        // If a Monster is within 3 spaces of Leoric, its attacks deal -1 Heart (to a minimum of 1)
        if (attacker instanceof Monster)
        {
            damage = HeroAbilities.leoric(state, attacker, damage);
        }

        if (defence < 0) defence = 0;
        damage = Math.max(damage - defence, 0);
    }
    protected void applyDamage(DescentGameState state) {

        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        defenderName = defender.getComponentName().replace("Hero: ", "");

        // If the attacker is a Hero, target has the Shadow passive and we did not spend a Shadow surge, automatically miss
        if (attacker instanceof Hero) {
            if (hasShadow && !hitShadow) {
                if (defender instanceof Monster && ((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.SHADOW) && !surgesUsed.contains(Surge.SHADOW)) {
                    //System.out.println("Missed due to Shadow passive, and no surge spent to counter.");
                    result += "Missed; Damage: " + damage + "; Range: " + range;
                    return;
                }
            }
        }

        if (defender.hasBonus(DescentTypes.SkillBonus.Unseen)) {
            if (!hitShadow) {
                if (!surgesUsed.contains(Surge.UNSEEN)) {
                    //System.out.println("Missed due to Thief's Unseen, and no surge spent to counter.");
                    result += "Missed; Damage: " + damage + "; Range: " + range;
                    return;
                }
            }
        }

        int startingHealth = defender.getAttribute(Figure.Attribute.Health).getValue();
        if (startingHealth - damage <= 0) {

            result += "Kill; Damage: " + damage + "; Range: " + range;

            int index1 = getFigureIndex(state, defender);
            int index2 = getFigureIndex(state, attacker);

            // Though rare, some attacks like a Dragon's Fire Breath or an Elemental's Fire can defeat teammate Monsters
            // We need to check here so that getActingFigure() doesn't return an out of bounds error
            if (attacker instanceof Monster && defender instanceof Monster)
            {
                List<Monster> monsters = state.getCurrentMonsterGroup();
                if (monsters != null)
                {
                    // Check if both attacker and defender are the same kind of Monster
                    if (monsters.contains(attacker) && monsters.contains(defender))
                    {
                        if(monsters.indexOf(attacker) >= monsters.indexOf(defender))
                        {
                            state.teamkill();
                        }
                    }
                }
            }

            // Death
            DescentHelper.figureDeath(state, defender);

            // Add to the list of defeated figures this turn
            state.addDefeatedFigure(defender, index1, attacker, index2);

        } else {
            result += "Hit; Damage: " + damage + "; Range: " + range;
            // Conditions only apply if damage is done
            if (damage > 0)
                applyConditions(defender);
            defender.setAttribute(Figure.Attribute.Health, Math.max(startingHealth - damage, 0));
        }

        // If our attack is leeching, add the damage to our Mending
        if (leeching)
        {
            addMending(damage);
        }

        if(mending > 0)
        {
            attacker.incrementAttribute(Figure.Attribute.Health, mending);
        }
        // Only Heroes can recover Fatigue from extra Surges
        if(fatigueHeal > 0 && attacker instanceof Hero)
        {
            attacker.decrementAttribute(Figure.Attribute.Fatigue, fatigueHeal);
        }
        // Likewise, only Heroes can receive Fatigue penalties from a Monster's Surges
        if(fatigueHeal < 0 && defender instanceof Hero)
        {
            defender.incrementAttribute(Figure.Attribute.Fatigue, -fatigueHeal);
        }
    }

    public boolean attackMissed(DescentGameState state) {
        range = state.getAttackDicePool().getRange();
        damage = state.getAttackDicePool().getDamage();
        return state.getAttackDicePool().hasRolled() && (
                range < minRange || damage == 0);
    }

    public void applyConditions(Figure defender)
    {
        // Applies the Diseased condition
        if (isDiseasing) {
            defender.addCondition(DescentTypes.DescentCondition.Disease);
            result += "; Diseased";
        }
        // Applies the Immobilized condition
        if (isImmobilizing) {
            immobilize(defender);
            result += "; Immobilized";
        }
        // Applies the Poisoned condition
        if (isPoisoning) {
            defender.addCondition(DescentTypes.DescentCondition.Poison);
            result += "; Poisoned";
        }
        // Applies the Stunned condition
        if (isStunning) {
            defender.addCondition(DescentTypes.DescentCondition.Stun);
            result += "; Stunned";
        }
    }

    @Override
    public MeleeAttack copy() {
        MeleeAttack retValue = new MeleeAttack(attackingFigure, defendingFigure, hasReach);
        copyComponentTo(retValue);
        return retValue;
    }
    public void copyComponentTo(MeleeAttack retValue) {
        retValue.attackingPlayer = attackingPlayer;
        retValue.defendingPlayer = defendingPlayer;
        retValue.substituteFigure = substituteFigure;
        retValue.substitutePlayer = substitutePlayer;
        retValue.substituteName = substituteName;
        retValue.substitute = substitute;
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        retValue.surgesToSpend = surgesToSpend;
        retValue.extraSurges = extraSurges;
        retValue.extraRange = extraRange;
        retValue.extraDamage = extraDamage;
        retValue.extraDefence = extraDefence;
        retValue.swapDefence = swapDefence;
        retValue.mending = mending;
        retValue.fatigueHeal = fatigueHeal;
        retValue.surgesUsed = new HashSet<>(surgesUsed);
        retValue.pierce = pierce;
        retValue.isDiseasing = isDiseasing;
        retValue.isImmobilizing = isImmobilizing;
        retValue.isPoisoning = isPoisoning;
        retValue.isStunning = isStunning;
        retValue.hasShadow = hasShadow;
        retValue.hitShadow = hitShadow;
        retValue.leeching = leeching;
        retValue.subdue = subdue;
        retValue.damage = damage;
        retValue.range = range;
        retValue.skip = skip;
        retValue.reduced = reduced;
        retValue.result = result;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);

        // Check for abilities that prevent attacking
        if (!PrayerOfPeace.canAttackPrayer(dgs, f)) return false;

        if (Air.checkAir(dgs, f, target)) {
            // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
            return false;
        }

        return checkAllSpaces(dgs, f, target, getRange(), true);
    }

    protected int getRange()
    {
        return hasReach ? 2 : 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MeleeAttack) {
            MeleeAttack other = (MeleeAttack) obj;
            return other.attackingFigure == attackingFigure && other.surgesToSpend == surgesToSpend &&
                    other.extraSurges == extraSurges && other.extraDamage == extraDamage &&
                    other.extraDefence == extraDefence && other.swapDefence == swapDefence && other.hasReach == hasReach &&
                    other.isDiseasing == isDiseasing && other.isImmobilizing == isImmobilizing &&
                    other.isPoisoning == isPoisoning && other.isStunning == isStunning &&
                    other.extraRange == extraRange && other.pierce == pierce &&
                    other.mending == mending && other.leeching == leeching && other.subdue == subdue &&
                    other.hasShadow == hasShadow && other.hitShadow == hitShadow &&
                    other.attackingPlayer == attackingPlayer && other.defendingFigure == defendingFigure &&
                    other.fatigueHeal == fatigueHeal && other.surgesUsed.equals(surgesUsed) &&
                    other.defendingPlayer == defendingPlayer && other.phase == phase &&
                    other.damage == damage && other.range == range &&
                    other.interruptPlayer == interruptPlayer && other.skip == skip &&
                    other.substitute == substitute && other.substituteFigure == substituteFigure &&
                    other.substitutePlayer == substitutePlayer && other.substituteName == substituteName &&
                    other.reduced == reduced && other.result.equals(result);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackingFigure, attackingPlayer, defendingFigure, pierce, hasReach, substitute, substituteFigure, substitutePlayer, substituteName,
                extraRange, isDiseasing, isImmobilizing, isPoisoning, isStunning, extraDamage, extraDefence, swapDefence, mending, leeching, subdue, fatigueHeal, hasShadow, hitShadow,
                surgesUsed, defendingPlayer, phase.ordinal(), interruptPlayer, surgesToSpend, extraSurges, damage, range, skip, reduced, result);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return shortString(gameState);
    }

    public String shortString(AbstractGameState gameState) {
        // TODO: Extend this to pull in details of card and figures involved
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();

        // Sometimes the game will remove the dead enemy off the board before
        // it can state in the Action History the attack that killed them
        if (gameState.getComponentById(defendingFigure) != null) {
            defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        }
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");
        return String.format("Melee Attack by " + attackerName + " on " + defenderName + "; " + result);
    }

    public String longString(AbstractGameState gameState) {

        attackerName = gameState.getComponentById(attackingFigure).getComponentName();

        // Sometimes the game will remove the dead enemy off the board before
        // it can state in the Action History the attack that killed them
        if (gameState.getComponentById(defendingFigure) != null) {
            defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        }
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");
        return String.format("Melee Attack by " + attackerName + "(" + attackingPlayer
                + ") on " + defenderName + "(" + defendingPlayer + "). "
                + "Phase: " + phase + ". Interrupt player: " + interruptPlayer
                + ". Surges to spend: " + surgesToSpend
                + ". Extra Surges: " + extraSurges
                + ". Has reach: " + hasReach
                + ". Extra range: " + extraRange
                + ". Pierce: " + pierce
                + ". Extra damage: " + extraDamage
                + ". Extra defence: " + extraDefence
                + ". Swapped defence: " + swapDefence
                + ". Mending: " + mending
                + ". Fatigue Heal: " + fatigueHeal
                + ". Disease: " + isDiseasing
                + ". Immobilize: " + isImmobilizing
                + ". Poison: " + isPoisoning
                + ". Stun: " + isStunning
                + ". Leeching: " + leeching
                + ". Subdue: " + subdue
                + ". Has Shadow: " + hasShadow
                + ". Hit Shadow: " + hitShadow
                + ". Damage: " + damage
                + ". Range: " + range
                + ". Skip: " + skip
                + ". Reduced: " + reduced
                + ". " + result
                + ". Surges used: " + surgesUsed.toString()
        );
    }

    @Override
    public String toString() {
        int target = substitute ? substituteFigure : defendingFigure;
        return String.format("Melee Attack by %d on %d", attackingFigure, target);
    }

    public String toStringWithResult()
    {
        return toString() + " - " + result;
    }

    public void registerSurge(Surge surge) {
        if (surgesUsed.contains(surge))
            throw new IllegalArgumentException(surge + " has already been used");
        surgesUsed.add(surge);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        if (phase.interrupt == null) {
            System.out.println(phase + "; " + phase.interrupt);
        }
        DescentGameState state = (DescentGameState) gs;
        List<AbstractAction> retValue = state.getInterruptActionsFor(interruptPlayer, phase.interrupt);
        // now we filter this for any that have been used

        if (phase == PRE_SURGE) {
            if (!retValue.isEmpty())
                retValue.add(new EndCurrentPhase());
        }

        if (phase == SURGE_DECISIONS) {

            retValue.removeIf(a -> {
               if (a instanceof SurgeAttackAction) {
                   SurgeAttackAction surge = (SurgeAttackAction)a;
                   // Runic Knowledge shouldn't be usable if we don't have the Fatigue to use
                   // It can be used for ForceFatigue, but we shouldn't use it if we only have 1 Health left
                   // Thus, only allow it if it is the last Surge available, to prevent unnecessary self-damage
                   if (surge.surge == Surge.RUNIC_KNOWLEDGE) {
                       Figure attacker = (Figure) state.getComponentById(attackingFigure);
                       if (attacker.getAttribute(Figure.Attribute.Fatigue).isMaximum())
                           if (fatigueHeal < 1)
                               if (attacker.getAttributeValue(Figure.Attribute.Health) - 1 <= attacker.getAttributeMin(Figure.Attribute.Health))
                                   if (surgesToSpend > 1)
                                       return true;
                   }
                   return surgesUsed.contains(surge.surge);
               }
               return false;
            });
            retValue.add(new EndSurgePhase());
        }
        if (phase == POST_ATTACK_ROLL) {
            boolean reroll = false;
            for (AbstractAction action : retValue)
            {
                if (action instanceof TarhaAbilityReroll)
                {
                    reroll = true;
                    break;
                }
            }
            if (reroll)
                retValue.add(new EndRerollPhase());
            else retValue.add(new EndCurrentPhase());
        }
        if (phase == PRE_DEFENCE_ROLL) {
            // Applying Subdue should override any subsequent actions
            if (subdue)
            {
                List<AbstractAction> subdueActions = new ArrayList<>();
                for (DescentTypes.DescentCondition condition : DescentTypes.DescentCondition.values())
                {
                    Subdue subdueAction;

                    // Baron Zachareth versus Runemaster Mage Hero
                    // It's the same action, just a different toString()
                    if (state.getComponentById(attackingFigure) instanceof Monster)
                        subdueAction = new Subdue(defendingFigure, condition);
                    else subdueAction = new RunicSorceryStatus(defendingFigure, condition);

                    if (subdueAction.canExecute(state)) {
                        subdueActions.add(subdueAction);
                    }
                }
                if (!subdueActions.isEmpty())
                    return subdueActions;
            }
        }
        if (phase == POST_DEFENCE_ROLL || phase == PRE_DAMAGE) {
            retValue.add(new EndCurrentPhase());
        }

        // We check for any interrupt attacks that can be used after this attack has concluded
        if (phase == INTERRUPT_ATTACK)
        {
            Figure attacker = (Figure) state.getComponentById(attackingFigure);
            Figure target = (Figure) state.getComponentById(defendingFigure);
            List<AbstractAction> interruptAttacks = new ArrayList<>();
            if (target != null &&
                    !target.getAttribute(Figure.Attribute.Health).isMinimum()) {
                for (String attack : state.getInterruptAttacks()) {

                    // Blast
                    if (attack.contains(BlastAttack.name)) {
                        Set<BlastAttack> blastAttacks = BlastAttack.constructBlasts(state, attackingFigure, defendingFigure);
                        if (!blastAttacks.isEmpty())
                            interruptAttacks.addAll(blastAttacks);
                        continue;
                    }

                    // Fire Breath
                    if (attack.contains(FireBreath.name)) {
                        Set<FireBreath> fireBreath = FireBreath.constructFireBreath(state, attackingFigure, defendingFigure);
                        if (!fireBreath.isEmpty())
                            interruptAttacks.addAll(fireBreath);
                        continue;
                    }

                    // Knockback - both Splig and Crossbow
                    if (attack.contains(Knockback.name)) {
                        List<Knockback> knockbacks = new ArrayList<>();
                        int distance = Integer.parseInt(attack.split(":")[1]);
                        target.setOffMap(true);
                        target.setAttribute(Figure.Attribute.MovePoints, distance);

                        Vector2D startPos = target.getPosition();
                        List<Vector2D> spaces = getForcedMovePositions(state, startPos, distance);
                        List<Monster.Direction> orientations = new ArrayList<>();
                        orientations.add(Monster.Direction.DOWN);
                        if (target instanceof Monster m) {
                            if (m.getSize().a > 1 || m.getSize().b > 1) {
                                orientations.add(Monster.Direction.LEFT);
                                orientations.add(Monster.Direction.UP);
                                orientations.add(Monster.Direction.RIGHT);
                            }
                        }
                        for (Vector2D pos : spaces) {
                            for (Monster.Direction orientation : orientations) {
                                Knockback knockback = new Knockback(defendingFigure, attackingFigure, startPos, pos, orientation, distance);
                                if (knockback.canExecute(state))
                                    knockbacks.add(knockback);
                            }
                        }

                        if (!knockbacks.isEmpty()) {
                            knockbacks.sort(Comparator.comparing(ForcedMove::getOrientation));
                            interruptAttacks.addAll(knockbacks);
                        }

                        continue;
                    }

                    // Distant Damage - Magic Staff
                    if (attack.contains(ExtraDamage.distant)) {
                        int range = Integer.parseInt(attack.split(":")[1]);
                        int dmg = Integer.parseInt(attack.split(":")[2]);

                        List<Integer> targets = new ArrayList<>();

                        if (attacker instanceof Hero) {
                            for (List<Monster> monsters : state.getMonsters()) {
                                for (Monster m : monsters) {
                                    if (m.getComponentID() == defendingFigure) continue;
                                    if (getRangeAllSpaces(state, target, m) <= range)
                                        targets.add(m.getComponentID());
                                }
                            }
                        }
                        if (attacker instanceof Monster) {
                            for (Hero hero : state.getHeroes()) {
                                if (hero.getComponentID() == defendingFigure) continue;
                                if (getRangeAllSpaces(state, target, hero) <= range)
                                    targets.add(hero.getComponentID());
                            }
                        }

                        for (int t : targets) {
                            ExtraDamage extraDamage = new ExtraDamage(attackingFigure, t, dmg, range);
                            extraDamage.setName(attack);
                            if (extraDamage.canExecute(state))
                                interruptAttacks.add(extraDamage);
                        }
                        continue;
                    }

                    // Adjacent Damage - Mace of Kellos, Dawnblade
                    if (attack.contains(ExtraDamage.adjacent)) {
                        String[] split = attack.split(":");
                        boolean targetAll = split[1].contains("All");
                        boolean maxDamage = split[2].contains("Full");

                        int dmg = maxDamage ? damage : Integer.parseInt(split[2]);

                        List<Integer> targets = getMeleeTargets(state, attacker, false);

                        if (targetAll) {
                            ExtraDamage extraDamage = new ExtraDamage(attackingFigure, targets, dmg, 1);
                            extraDamage.setName(attack);
                            if (extraDamage.canExecute(state))
                                interruptAttacks.add(extraDamage);
                        }

                        else {

                            for (int t : targets) {
                                if (t == defendingFigure) continue;
                                ExtraDamage extraDamage = new ExtraDamage(attackingFigure, t, dmg);
                                extraDamage.setName(attack);
                                if (extraDamage.canExecute(state))
                                    interruptAttacks.add(extraDamage);
                            }
                        }
                        continue;
                    }
                }

                // We apply these interruptions after the Attack has been fully completed
                if (interruptAttacks.isEmpty()) {
                    if (target.hasBonus(DescentTypes.SkillBonus.CounterAttack)) {
                        CounterAttack counterAttack = new CounterAttack(defendingFigure, attackingFigure);
                        if (counterAttack.canExecute(state))
                            interruptAttacks.add(counterAttack);
                    }

                    if (attacker.hasBonus(DescentTypes.SkillBonus.QuickCasting)) {
                        Set<QuickCasting> quickCastings = QuickCasting.constructQuickCasting(state, attackingFigure);
                        if (!quickCastings.isEmpty())
                            interruptAttacks.addAll(quickCastings);
                    }
                }
            }

            if (!interruptAttacks.isEmpty())
            {
                retValue.addAll(interruptAttacks);
                // We don't have to make the attack if we don't want to
                // e.g. if the only legal target for Fire Breath to hit is the Dragon itself
                retValue.add(new EndCurrentPhase());
            }

        }

        // We check for any attribute tests that must occur as consequence for this attack
        if (phase == ATTRIBUTE_TEST) {
            //Figure attacker = (Figure) state.getComponentById(attackingFigure);
            Figure defender = (Figure) state.getComponentById(defendingFigure);

            if (defender instanceof Monster && ((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.AFTERSHOCK))
            {
                AftershockTest aftershock = new AftershockTest(attackingFigure, Figure.Attribute.Willpower, defendingFigure);
                if (aftershock.canExecute(state)) {
                    retValue.add(aftershock);
                }
            }
        }

        // Remove any duplicate actions from the return list
        return new ArrayList<>(new HashSet<>(retValue));
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {

        if (action instanceof NotMe) {
            DescentGameState dgs = (DescentGameState) state;
            substituteFigure = ((NotMe) action).getVictim();

            // Only swap if the defender chose to swap
            if (((NotMe) action).getResult() == 1 && substituteFigure != -1 && defendingFigure != substituteFigure) {

                substitute = true;

                Figure attacker = (Figure) state.getComponentById(attackingFigure);
                Figure splig = (Figure) state.getComponentById(defendingFigure);
                Figure defender = (Figure) state.getComponentById(substituteFigure);
                substitutePlayer = defender.getOwnerId();
                substituteName = defender.getName().replace("Hero: ", "");

                if (checkAdjacent(dgs, attacker, splig)) {
                    SurgeAttackAction shadowSurge = new SurgeAttackAction(Surge.SHADOW, attackingFigure);
                    if ((attacker instanceof Hero) && (defender instanceof Monster) &&
                            (((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.SHADOW))) {
                        hasShadow = true;
                        if (!attacker.getAbilities().contains(shadowSurge))
                            attacker.addAbility(new SurgeAttackAction(Surge.SHADOW, attackingFigure));
                    } else {
                        hasShadow = false;
                        if (attacker.getAbilities().contains(shadowSurge))
                            attacker.removeAbility(shadowSurge);
                    }
                }
            }
        }

        // After the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return phase == ALL_DONE;
    }

    public void addRange(int rangeBonus) {
        extraRange += rangeBonus;
    }
    public void addPierce(int pierceBonus) {
        pierce += pierceBonus;
    }
    public void addMending(int mendBonus) {
        mending += mendBonus;
    }
    public void addFatigueHeal(int fatigueHealBonus) {
        fatigueHeal += fatigueHealBonus;
    }
    public void addFatigueDamage(int fatiguePenalty) {
        fatigueHeal -= fatiguePenalty;
    }
    public int getFatigueHeal()
    {
        return fatigueHeal;
    }
    public void setDiseasing(boolean disease) {
        isDiseasing = disease;
    }
    public void setImmobilizing(boolean immobilize) {
        isImmobilizing = immobilize;
    }
    public void setPoisoning(boolean poison) {
        isPoisoning = poison;
    }
    public void setStunning(boolean stun) {
        isStunning = stun;
    }

    public void setLeeching(boolean leech) {
        leeching = leech;
    }
    public void setSubdue(boolean subdue) {
        this.subdue = subdue;
    }
    public boolean isSubdue() {
        return subdue;
    }
    public void setShadow(boolean shadow) {
        hitShadow = shadow;
    }
    public void addDamage(int damageBonus) {
        extraDamage += damageBonus;
    }
    public void reduceDamage (int damageReduction) {
        damage = Math.max(0, damage - damageReduction);
        reduced = true;
    }
    public boolean getReduced()
    {
        return reduced;
    }
    public void addDefence(int defenceBonus) {
        extraDefence += defenceBonus;
    }
    public int getDamage() {
        return damage;
    }
    public void swapDefence(int swap) {
        swapDefence = swap;
    }

    public int getSwapDefence() {
        return swapDefence;
    }

    public int getExtraDamage() {
        return extraDamage;
    }
    public int getExtraDefence() {
        return extraDefence;
    }
    public int getPierce() {
        return pierce;
    }

    public int getDefendingFigure()
    {
        return defendingFigure;
    }
    public void setDefendingFigure(int d)
    {
        defendingFigure = d;
    }

    public void addExtraSurge(int s)
    {
        extraSurges += s;
    }

    public int getExtraSurge()
    {
        return extraSurges;
    }

    public AttackPhase getPhase() {
        return phase;
    }
    public boolean getSkip()
    {
        return skip;
    }

    public void setSkip(boolean s)
    {
        skip = s;
    }

    public String getInitialResult(DescentGameState dgs)
    {
        Figure defender = (Figure) dgs.getComponentById(defendingFigure);
        return "Target: " + defender.getComponentName().replace("Hero: ", "") + "; Result: ";
    }

    public void addInterruptAttack (DescentGameState dgs, String attack)
    {
        // Enables an Interrupt Attack based on the Surge spent
        // Current Interrupt Attacks:
        // Blast
        // Fire Breath (Shadow Dragon)
        // Knockback (Splig, Crossbow)
        // Quickened Casting (Runemaster)
        // TODO
        // Damage 1 To All Adjacent (Mace of Kellos)
        // Damage 1 To 3 Spaces Away (Magic Staff)
        // Damage Equal To 1 Adjacent (Dawnblade)
        // Zorek's Favor
        dgs.addInterruptAttack(attack);
    }

    public void removeInterruptAttacks(DescentGameState dgs)
    {
        // Switch off all interrupt attacks that have been enabled
        // So that we don't accidentally enable them again for different attacks
        dgs.clearInterruptAttacks();
    }
}

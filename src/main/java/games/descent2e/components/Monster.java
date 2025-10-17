package games.descent2e.components;

import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.DescentTypes.AttackType;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.actions.monsterfeats.NotMe;
import utilities.Pair;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Monster extends Figure {

    boolean lieutenant = false;
    Deck<DescentCard> lieutenantInventory = new Deck<>("Overlord Relics", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
    boolean flying = false;
    AttackType attackType = AttackType.NONE;
    public enum Direction {
        DOWN(new Vector2D()),
        LEFT(new Vector2D(-1,0)),
        UP(new Vector2D(-1,-1)),
        RIGHT(new Vector2D(0,-1));

        // If this is applied to figure's position anchor, then the returned position is the top-left corner of the figure
        final Vector2D anchorModifier;
        Direction(Vector2D anchorModifier) {
            this.anchorModifier = anchorModifier;
        }
        public static Direction getDefault() { return DOWN; }
    }

    /*
     Medium monsters might be rotated clockwise by:
      0 degrees (orientation=0): anchor is top-left (0,0)
      90 degrees (orientation=1): width <-> height, anchor is top-right (1,0)
      180 degrees (orientation=2): anchor is bottom-right (1,1)
      270 degrees (orientation=3): width <-> height, anchor is bottom-left (0,1)
     */
    Direction orientation = Direction.getDefault();

    protected List<Surge> surges = new ArrayList<>();
    protected List<MonsterAbilities.MonsterPassive> passives = new ArrayList<>();
    protected List<MonsterAbilities.MonsterAbility> actions = new ArrayList<>();

    public Monster() {
        super("Monster", -1);
    }

    protected Monster(String name, int nActionsPossible) {
        super(name, nActionsPossible);
    }

    protected Monster(String name, Counter actions, int ID) {
        super(name, actions, ID);
    }

    public Direction getOrientation() {
        return orientation;
    }

    // Use monster values to find top-left corner of monster, given its size
    public Vector2D applyAnchorModifier() {
        return applyAnchorModifier(position.copy(), orientation);
    }
    // Use external values to find top-left corner of monster, given its size
    public Vector2D applyAnchorModifier(Vector2D pos, Direction d) {
        Pair<Integer,Integer> mSize = size.copy();
        if (d.ordinal() % 2 == 1) mSize.swap();
        pos.add((mSize.a-1) * d.anchorModifier.getX(), (mSize.b-1) * d.anchorModifier.getY());
        return pos;
    }

    public void setOrientation(int orientation) {
        this.orientation = Direction.values()[orientation];
    }
    public void setOrientation(Direction orientation) {
        this.orientation = orientation;
    }

    @Override
    public Monster copy() {
        Monster copy = new Monster(componentName, nActionsExecuted.copy(), componentID);
        copyComponentTo(copy);
        return copy;
    }

    protected Monster copyComponentTo(Monster copy) {
        copy.orientation = orientation;
        copy.attackType = attackType;
        copy.lieutenant = lieutenant;
        copy.flying = flying;
        copy.surges = new ArrayList<>(surges);
        copy.passives = new ArrayList<>(passives);
        copy.actions = new ArrayList<>(actions);
        if (lieutenantInventory != null) copy.lieutenantInventory = lieutenantInventory.copy();
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster(componentName, nActionsExecuted.getMaximum());
        copyComponentTo(copy);
        return copy;
    }

    public boolean canEquip(DescentCard c) {
        if (!lieutenant) return false;
        PropertyBoolean heroRelic = (PropertyBoolean) c.getProperty("heroRelic");
        if (heroRelic == null) return false;
        if (heroRelic.value) return false;
        PropertyString passive = (PropertyString) c.getProperty("passive");
        if (passive != null) {
            if (passive.value.contains("Effect:EquipLimit:")) {
                String limit = passive.value.split("Effect:EquipLimit:")[1];
                return componentName.contains(limit.split(";")[0]);
            }
        }
        return true;
    }

    public void equip(DescentCard c) {
        if (canEquip(c)) {
            lieutenantInventory.add(c);

            // Apply any surges, if possible
            for (Surge surge : c.getWeaponSurges()) {
                SurgeAttackAction s = new SurgeAttackAction(surge, componentID);
                addAbility(s);
            }

            // Obtain the action, or passive, property of the Item
            PropertyString action = (PropertyString) c.getProperty("action");
            if (action == null) {
                action = (PropertyString) c.getProperty("passive");
                if (action == null) return;
            }

            if (!action.value.contains(";Effect")) return;

            String[] effects = action.value.split(";");

            for (String effect : effects) {
                if (Objects.equals(effect, effects[0])) continue;
                String[] split = effect.split(":");

                switch (split[1]) {
                    case "Duskblade" -> {
                        addBonus(DescentTypes.SkillBonus.Duskblade);
                    }
                    case "StaffOfShadows" -> {
                        addBonus(DescentTypes.SkillBonus.StaffOfShadows);
                    }
                    case "BonesOfWoe" -> {
                        addBonus(DescentTypes.SkillBonus.BonesOfWoe);
                    }
                    case "ShieldZoreksFavor" -> {
                        addBonus(DescentTypes.SkillBonus.ZoreksFavor);
                    }
                    case "ScorpionsKiss" -> {
                        addBonus(DescentTypes.SkillBonus.ScorpionsKiss);
                    }
                    case "BecomeRanged" -> {
                        setAttackType("ranged");
                    }
                }
            }
        }
    }

    public void unequip(DescentCard c) {
        lieutenantInventory.remove(c);

        // Remove Surges
        for (Surge surge : c.getWeaponSurges()) {
            SurgeAttackAction s = new SurgeAttackAction(surge, componentID);
            removeAbility(s);
        }

        // Obtain the action, or passive, property of the Item
        PropertyString action = (PropertyString) c.getProperty("action");
        if (action == null) {
            action = (PropertyString) c.getProperty("passive");
            if (action == null) return;
        }

        if (!action.value.contains(";Effect")) return;

        String[] effects = action.value.split(";");

        for (String effect : effects) {
            if (Objects.equals(effect, effects[0])) continue;
            String[] split = effect.split(":");

            switch (split[1]) {
                case "Duskblade" -> {
                    removeBonus(DescentTypes.SkillBonus.Duskblade);
                }
                case "StaffOfShadows" -> {
                    removeBonus(DescentTypes.SkillBonus.StaffOfShadows);
                }
                case "BonesOfWoe" -> {
                    removeBonus(DescentTypes.SkillBonus.BonesOfWoe);
                }
                case "ShieldZoreksFavor" -> {
                    removeBonus(DescentTypes.SkillBonus.ZoreksFavor);
                }
                case "ScorpionsKiss" -> {
                    removeBonus(DescentTypes.SkillBonus.ScorpionsKiss);
                }
                case "BecomeRanged" -> {
                    // This only affects Baron Zachareth, who by default is melee
                    // We're assuming that the only Lieutenants who get impacted by this are melee originally
                    setAttackType("melee");
                }
            }
        }
    }

    public void setAttackType(String attack)
    {
        switch (attack)
        {
            case "melee":
                this.attackType = AttackType.MELEE;
                break;
            case "ranged":
                this.attackType = AttackType.RANGED;
                break;
            default:
                this.attackType = AttackType.NONE;
        }
    }

    public void setLieutenant(boolean lieutenant) {
        this.lieutenant = lieutenant;
    }

    public boolean isLieutenant() {
        return lieutenant;
    }

    public void addInventory(DescentCard card) {
        if (lieutenant)
            lieutenantInventory.add(card);
    }

    public void removeInventory(DescentCard card) {
        if (lieutenantInventory.contains(card))
            lieutenantInventory.remove(card);
    }

    public void resetInventory() {
        lieutenantInventory.clear();
    }

    public Deck<DescentCard> getInventory() {
        return lieutenantInventory;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setPassivesAndSurges(String[] abilities) {
        for (String ability : abilities) {
            if (ability.contains("Surge")) {
                addSurge(ability);
            } else {
                for (MonsterAbilities.MonsterPassive passive : MonsterAbilities.MonsterPassive.values())
                {
                    if (passive.name().equals(ability.toUpperCase()))
                    {
                        addPassive(passive);
                        // Scamper or Fly allow us to ignore enemies whilst moving
                        if (passive == MonsterAbilities.MonsterPassive.SCAMPER|| passive == MonsterAbilities.MonsterPassive.FLY) {
                            setCanIgnoreEnemies(true);
                        }

                        if (passive == MonsterAbilities.MonsterPassive.NOTME)
                            addAbility(new NotMe(getComponentID()));

                        break;
                    }
                }
            }
        }

    }
    public void setActions(String[] abilities) {
        for (String ability : abilities) {
            for (MonsterAbilities.MonsterAbility action : MonsterAbilities.MonsterAbility.values())
            {
                if (action.name().equals(ability.toUpperCase()))
                {
                    addAction(action);
                    break;
                }
            }
        }
    }

    public void addSurge(String ability)
    {
        String surge = ability;
        if (surges == null) {
            surges = new ArrayList<>();
        }

        // If there is a number in the Surge (such as a Range increment or Damage increase), we need to extract it
        String number = surge.replaceAll("[^\\d.]", "");

        // For simplicity's sake, we are assuming that number never goes above 3
        // As no monster has a Surge with a value greater than 3 in the base game
        // Otherwise we will need to fix the Surges called
        if (!number.isEmpty()) {
            if (Integer.parseInt(number) > 3)
                number = "3";
        }

        // Get rid of the unwanted characters in the string
        surge = surge.replace("Surge: ", "");
        surge = surge.toUpperCase(Locale.ROOT).replaceAll( "[^A-Z]", "");

        switch (surge) {
            case "RANGE" -> surges.add(Surge.valueOf("RANGE_PLUS_" + number));
            case "HEART" -> surges.add(Surge.valueOf("DAMAGE_PLUS_" + number));
            case "PIERCE" -> surges.add(Surge.valueOf("PIERCE_" + number));
            case "MEND" -> surges.add(Surge.valueOf("MENDING_" + number));
            case "DISEASE" -> surges.add(Surge.DISEASE);
            case "POISON" -> surges.add(Surge.POISON);
            case "IMMOBILIZE" -> surges.add(Surge.IMMOBILIZE);
            case "STUN" -> surges.add(Surge.STUN);
            case "FIREBREATH" -> surges.add(Surge.FIRE_BREATH);

            // Lieutenants' Surges
            case "SUBDUE" -> surges.add(Surge.SUBDUE);
            case "BLOODCALL" -> surges.add(Surge.BLOOD_CALL);
            case "WITHER" -> surges.add(Surge.WITHER);
            case "KNOCKBACK" -> surges.add(Surge.KNOCKBACK_3);
            default -> {
            }
        }
    }
    public void removeSurge (Surge surge)
    {
        surges.remove(surge);
    }
    public void addPassive(MonsterAbilities.MonsterPassive ability)
    {
        if (passives == null) {
            passives = new ArrayList<>();
        }
        passives.add(ability);
    }
    public void removePassive(MonsterAbilities.MonsterPassive ability)
    {
        passives.remove(ability);
    }
    public void addAction(MonsterAbilities.MonsterAbility action)
    {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add(action);
    }
    public void removeAction(MonsterAbilities.MonsterAbility action)
    {
        actions.remove(action);
    }

    public void removeAll()
    {
        if (surges != null) {
            surges = new ArrayList<>();
        }
        if (passives != null) {
            passives = new ArrayList<>();
        }
        if (actions != null) {
            actions = new ArrayList<>();
        }
    }

    public List<Surge> getSurges() {
        return surges;
    }
    public List<MonsterAbilities.MonsterPassive> getPassives() {
        return passives;
    }
    public List<MonsterAbilities.MonsterAbility> getActions() {
        return actions;
    }

    public boolean hasPassive(MonsterAbilities.MonsterPassive ability)
    {
        if (passives != null) {
            return passives.contains(ability);
        }
        return false;
    }
    public boolean hasSurge (Surge surge)
    {
        if (surges != null) {
            return surges.contains(surge);
        }
        return false;
    }
    public boolean hasAction(MonsterAbilities.MonsterAbility action)
    {
        if (actions != null) {
            return actions.contains(action);
        }
        return false;
    }

    public AttackType getAttackType()
    {
        return attackType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Monster monster)) return false;
        if (!super.equals(o)) return false;
        return lieutenant == monster.lieutenant &&
                lieutenantInventory.equals(monster.lieutenantInventory) &&
                flying == monster.flying &&
                orientation == monster.orientation &&
                attackType == monster.attackType &&
                surges.equals(monster.surges) &&
                passives.equals(monster.passives) &&
                actions.equals(monster.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lieutenant, lieutenantInventory, flying, orientation.ordinal(), attackType, surges, passives, actions);
    }
}

package games.descent2e.components;

import core.components.Counter;
import games.descent2e.DescentTypes.AttackType;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import utilities.Pair;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Monster extends Figure {

    boolean lieutenant = false;
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
        copy.surges = new ArrayList<>(surges);
        copy.passives = new ArrayList<>(passives);
        copy.actions = new ArrayList<>(actions);
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster(componentName, nActionsExecuted.getMaximum());
        copyComponentTo(copy);
        return copy;
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
        if (Integer.parseInt(number) > 3)
            number = "3";

        // Get rid of the unwanted characters in the string
        surge = surge.replace("Surge: ", "");
        surge = surge.toUpperCase(Locale.ROOT).replaceAll( "[^A-Z]", "");

        switch (surge)
        {
            case "RANGE":
                surges.add(Surge.valueOf("RANGE_PLUS_" + number));
                break;
            case "HEART":
                surges.add(Surge.valueOf("DAMAGE_PLUS_" + number));
                break;
            case "PIERCE":
                surges.add(Surge.valueOf("PIERCE_"+ number));
                break;
            case "MEND":
                surges.add(Surge.valueOf("MENDING_"+ number));
                break;
            case "DISEASE":
                surges.add(Surge.DISEASE);
                break;
            case "POISON":
                surges.add(Surge.POISON);
                break;
            case "IMMOBILIZE":
                surges.add(Surge.IMMOBILIZE);
                break;
            case "STUN":
                surges.add(Surge.STUN);
                break;
            case "FIREBREATH":
                // TODO - Implement FIREBREATH for Surges, when we get to the Dragons
                //surges.add(Surge.FIREBREATH);
                break;
            default:
                break;
        }
    }
    public void removeSurge (Surge surge)
    {
        if (surges.contains(surge))
        {
            surges.remove(surge);
        }
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
        if (passives.contains(ability)) {
            passives.remove(ability);
        }
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
        if (actions.contains(action)) {
            actions.remove(action);
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
                orientation == monster.orientation &&
                attackType == monster.attackType &&
                surges.equals(monster.surges) &&
                passives.equals(monster.passives) &&
                actions.equals(monster.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lieutenant, orientation.ordinal(), attackType, surges, passives, actions);
    }
}

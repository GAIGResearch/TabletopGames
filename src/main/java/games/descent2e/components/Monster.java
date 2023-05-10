package games.descent2e.components;

import core.components.Counter;
import games.descent2e.DescentTypes.AttackType;
import utilities.Pair;
import utilities.Vector2D;

public class Monster extends Figure {

    AttackType attackType = AttackType.NONE;
    public enum Direction {
        DOWN(new Vector2D()),
        LEFT(new Vector2D(-1,0)),
        UP(new Vector2D(-1,-1)),
        RIGHT(new Vector2D(0,-1));

        // If this is applied to figure's position anchor, then the returned position is the top-left corner of the figure
        Vector2D anchorModifier;
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

    public Monster() {
        super("Monster", -1);
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
        copy.orientation = orientation;
        copy.attackType = attackType;
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster();
        copy.orientation = orientation;
        copy.attackType = attackType;
        super.copyComponentTo(copy);
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

    public AttackType getAttackType() {
        return attackType;
    }
}

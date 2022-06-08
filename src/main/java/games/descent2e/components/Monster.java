package games.descent2e.components;

public class Monster extends Figure {

    /*
     Medium monsters might be rotated clockwise by:
      0 degrees (orientation=0)
      90 degrees (orientation=1): width <-> height
      180 degrees (orientation=2)
      270 degrees (orientation=3): width <-> height
     */
    int orientation=0;

    public Monster() {
        super("Monster");
    }

    protected Monster(String name, int ID) {
        super(name, ID);
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    @Override
    public Monster copy() {
        Monster copy = new Monster(componentName, componentID);
        copy.orientation = orientation;
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster();
        copy.orientation = orientation;
        super.copyComponentTo(copy);
        return copy;
    }

}

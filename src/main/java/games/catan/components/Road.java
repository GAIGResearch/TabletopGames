package games.catan.components;

public class Road implements Copiable {
    private int owner;
    private static int counter = 0;
    private int id;

    public Road(int owner){
        this.owner = owner;
        this.id = counter++;
    }
    private Road(int owner, int id){
        this.owner = owner;
        this.id = id;
    }

    public int getOwner(){
        return owner;
    }

    public void setOwner(int playerID){
        this.owner = playerID;
    }

    public int getID() {
        return id;
    }

    public Road copy(){
        Road copy = new Road(owner, id);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Road){
            Road otherAction = (Road)obj;
            return id == otherAction.id && owner == otherAction.owner;
        }
        return false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }

    @Override
    public int hashCode() {
        return id + owner * 31 + 27482;
    }
}

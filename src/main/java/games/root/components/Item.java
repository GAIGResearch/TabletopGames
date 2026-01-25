package games.root.components;

import core.CoreConstants;
import core.components.Component;

import java.util.Objects;

public class Item extends Component {
    public enum ItemType{
        sword,
        boot,
        crossbow,
        torch,
        hammer,
        tea,
        coin,
        bag
    }

    public final ItemType itemType;
    public boolean refreshed = true;
    public boolean damaged = false;

    public Item(CoreConstants.ComponentType type, ItemType itemType){
        super(type, itemType.toString());
        this.itemType = itemType;
    }

    private Item(CoreConstants.ComponentType type, ItemType itemType, int componentID){
        super(type, itemType.toString(), componentID);
        this.itemType = itemType;
    }

    @Override
    public Item copy() {
        Item item =  new Item(type, itemType, componentID);
        item.damaged = damaged;
        item.refreshed = refreshed;
        return  item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item item)) return false;
        if (!super.equals(o)) return false;
        return refreshed == item.refreshed && damaged == item.damaged && itemType == item.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), itemType, refreshed, damaged);
    }
}

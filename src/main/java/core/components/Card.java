package core.components;

import core.CoreConstants;
import core.properties.PropertyString;

import static core.CoreConstants.nameHash;

public class Card extends Component {

    public Card() {
        super(CoreConstants.ComponentType.CARD);
    }
    public Card(String name){
        super(CoreConstants.ComponentType.CARD, name);
    }

    protected Card(String name, int ID){
        super(CoreConstants.ComponentType.CARD, name, ID);
    }

    @Override
    public Card copy(){
        Card copy = new Card(componentName, componentID);
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public String toString() {
        PropertyString hashName =  (PropertyString)getProperty(nameHash);
        return hashName != null ? hashName.value : componentName;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

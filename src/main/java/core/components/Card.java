package core.components;

import core.content.PropertyString;
import utilities.Utils.ComponentType;

import static utilities.CoreConstants.nameHash;

public class Card extends Component {

    public Card() {
        super(ComponentType.CARD);
    }

    public Card(String name){
        super(ComponentType.CARD, name);
    }

    @Override
    public Card copy(){
        Card copy = new Card(componentName);
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public String toString() {
        return ((PropertyString)getProperty(nameHash)).value;
    }
}

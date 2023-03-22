package games.catan.components;

import core.CoreConstants;
import core.components.Component;

public class Road extends Component {

    public Road(int owner){
        super(CoreConstants.ComponentType.TOKEN, "Road");
        setOwnerId(owner);
    }
    private Road(int owner, int id){
        super(CoreConstants.ComponentType.TOKEN, "Road", id);
        setOwnerId(owner);
    }

    public Road copy(){
        return new Road(ownerId, componentID);
    }
}

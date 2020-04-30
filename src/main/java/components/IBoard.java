package components;

import content.Property;

import java.util.List;

public interface IBoard {
    List getBoardNodes();

    IBoardNode getNodeByProperty(int prop_id, Property p);

    IBoardNode getNode(int hashID, String value);

    IBoard copy();

    String getNameID();

    void setType();
}

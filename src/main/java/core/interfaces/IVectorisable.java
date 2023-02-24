package core.interfaces;

import core.actions.AbstractAction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public interface IVectorisable {

    // Gets observations as a JSON
    String getObservationJson();

    double[] getObservationVector();

    double[] getNormalizedObservationVector();

    // Gets the size of the observation vector
    int getObservationSpace();
}

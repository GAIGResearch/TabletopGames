package games.dotsboxes;

import core.interfaces.IStatisticLogger;
import utilities.StateFeatureListener;

public class DBFeatureListener extends StateFeatureListener {
    public DBFeatureListener(IStatisticLogger logger) {
        super(logger, new DBStateFeatures());
    }
}

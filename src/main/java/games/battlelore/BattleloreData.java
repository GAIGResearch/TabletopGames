package games.battlelore;

import core.AbstractGameData;
import core.components.*;
import games.battlelore.components.Unit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BattleloreData extends AbstractGameData {
    private List<Unit> units;

    @Override
    public void load(String dataPath) {
        units = Unit.loadUnits(dataPath + "units.json");
    }

    public List<Unit> getUnits() {
        return units;
    }

    public BattleloreData copy() {
        BattleloreData data = new BattleloreData();
        data.units = new ArrayList<>();
        for (Unit i : units) data.units.add((Unit) i.copy());

        return data;
    }
}

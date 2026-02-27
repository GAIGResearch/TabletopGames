package games.thegame.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class SelectRowIntent {
    public ArrayList<Integer> cantRows;
    public ArrayList<Integer> intendedRows;

    public SelectRowIntent() {
        cantRows = new ArrayList<>();
        intendedRows = new ArrayList<>();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("I intent to select rows with this preference: ");
        for(int i : intendedRows) sb.append(i).append(",");
        if(!cantRows.isEmpty()) {
            sb.append("- I can't play in ");
            for (int i : cantRows) sb.append(i).append(",");
        }
        return sb.toString();
    }
}

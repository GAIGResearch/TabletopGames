package pandemic;

import core.Board;
import core.BoardNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class PandemicBoard extends Board {
    private static String dataPath = "data/pandemicBoard.json";  // Path to json data file describing the pandemic board

    public PandemicBoard() {
        boardNodes = new ArrayList<>();
    }

    /**
     * Class specific implementation. Pandemic boards specify node neighbours by name. After processing all nodes,
     * neighbours are added.
     * @param path - path in which json file containing board can be found.
     */
    @Override
    public void loadBoard(String path) {
        super.loadBoard(path);

        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(dataPath)) {
            //Read JSON file
            JSONArray cityList = (JSONArray) jsonParser.parse(reader);

            // 2. Assign neighbours when all boardNodes created.
            for (Object o: cityList) {
                JSONObject city = (JSONObject)o;
                String name = (String) city.get("name");
                BoardNode bn = getNodeByName(name);
                if (bn != null) {
                    HashSet<String> neighbours = getNeighbours(city);
                    for (BoardNode n : boardNodes) {
                        if (neighbours.contains(n.getName())) {
                            bn.addNeighbour(n);
                            n.addNeighbour(bn);
                        }
                    }
                } else {
                    System.out.println("Node " + name + " not found in built board nodes.");
                }
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a hashset of neighbour names from a JSON object
     * @param city - JSON object representing a Pandemic city.
     * @return - hashset of the city's neighbours.
     */
    private HashSet<String> getNeighbours(JSONObject city) {
        HashSet<String> neighbours = new HashSet<>();
        for (Object o: (JSONArray) city.get("neighbours")) {
            neighbours.add((String)o);
        }
        return neighbours;
    }

    @Override
    public Board copy() {
        Board copy = new PandemicBoard();
        super.copyTo(copy);
        return null;
    }

    /**
     * Main method for testing.
     */
    public static void main(String[] args) throws InterruptedException {
        PandemicBoard pb = new PandemicBoard();
        pb.loadBoard(dataPath);
//        for (BoardNode b : pb.boardNodes) {
//            System.out.println(b);
//        }
        PandemicGUI gui = new PandemicGUI(pb);
        while (true) {
            gui.repaint();
            Thread.sleep(100);
        }
    }
}

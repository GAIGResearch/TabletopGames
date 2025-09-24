package games.tictactoe;

import core.AbstractGameState;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TTTFeatures implements IStateFeatureVector, IStateFeatureJSON {

    @Override
    public String[] names() {
        String[] names = new String[]{
                "0,0", "0,1", "0,2",
                "1,0", "1,1", "1,2",
                "2,0", "2,1", "2,2"};
        return names;
    }

    @Override
    public String getObservationJson(AbstractGameState gameState, int playerId) {
        JSONObject json = new JSONObject();
        TicTacToeGameState tttgs = (TicTacToeGameState) gameState;
        String playerSymbol = (playerId == 0) ? "x" : "o";
        for (int x = 0; x < tttgs.gridBoard.getWidth(); x++) {
            for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                String cellSymbol = tttgs.gridBoard.getElement(x, y).getComponentName();
                if (cellSymbol.equals(playerSymbol)) {
                    json.put(x + "," + y, 1);
                } else if (cellSymbol.equals(".")) {
                    json.put(x + "," + y, 0);
                } else {
                    json.put(x + "," + y, -1);
                }
            }
        }
        return json.toJSONString();
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        TicTacToeGameState tttgs = (TicTacToeGameState) state;
        List<Double> listVec = new ArrayList<>();
        String playerSymbol = (playerID == 0) ? "x" : "o";
        for (int x = 0; x < tttgs.gridBoard.getWidth(); x++) {
            for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                String cellSymbol = tttgs.gridBoard.getElement(x, y).getComponentName();
                if (cellSymbol.equals(playerSymbol)) {
                    listVec.add(1.0);
                } else if (cellSymbol.equals(".")) {
                    listVec.add(0.0);
                } else {
                    listVec.add(-1.0);
                }
            }
        }
        return listVec.stream().mapToDouble(i -> i).toArray();
    }

//    @Override
//    public double[] getNormalizedObservationVector() {
//        List<Double> listVec = new ArrayList<>();
//        String playerSymbol = (getCurrentPlayer() == 0) ? "x" : "o";
//        for (int x = 0; x < gridBoard.getWidth(); x++) {
//            for (int y = 0; y < gridBoard.getHeight(); y++) {
//                String cellSymbol = gridBoard.getElement(x, y).toString();
//                if (cellSymbol.equals(playerSymbol)) {
//                    listVec.add(1.0);
//                } else if (cellSymbol.equals(".")) {
//                    listVec.add(0.5);
//                } else {
//                    listVec.add(0.0);
//                }
//            }
//        }
//        return listVec.stream().mapToDouble(i -> i).toArray();
//    }

}

package games.sushigo.testing;

import games.sushigo.SGGameState;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public final class SGFileWriter {

    public static ArrayList<Integer> winners = new ArrayList<Integer>();
    private static String winnerToPrint = " ";
    private static int testId = 1;

    private SGFileWriter(){

    }
    public static void WriteWinrateToFile() {
        try{
            FileWriter myWriter = new FileWriter("SGWinrateReport.txt", true);
            CheckForWinner();
            myWriter.write(testId + " Winner is: " + winnerToPrint);
            myWriter.write("\n");
            myWriter.close();
            testId++;
        }
        catch (IOException e){
            //Nothing
        }

    }

    private static void CheckForWinner(){
        for (int i = 0; i < winners.size(); i++){
            winnerToPrint = " ";
            winnerToPrint += winners.get(i);
        }

        winners.clear();
    }

    public static void AddWinner(Integer winner){
        winners.add(winner);
    }
}

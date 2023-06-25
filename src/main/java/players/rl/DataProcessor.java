package players.rl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import games.tictactoe.TicTacToeStateVector;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataProcessor {

    public final String DB_PATH = "src/main/java/players/rl/resources/qWeights/database.csv";

    public final String[] header = { "id", "alpha", "gamma", "epsilon", "solver", "# games", "startId" };

    public String[] formatData(int id, RLParams params, RLTrainingParams trainingParams, int fromId) {
        return new String[] {
                Integer.toString(id),
                Double.toString(trainingParams.alpha),
                Double.toString(trainingParams.gamma),
                Double.toString(params.epsilon),
                trainingParams.solver.name(),
                Integer.toString(trainingParams.nGames),
                Integer.toString(fromId)
        };
    }

    public void ReadDatabase() {
        try (CSVReader reader = new CSVReader(new FileReader(DB_PATH))) {
            String[] nextLine;
            reader.skip(2); // Title and header
            while ((nextLine = reader.readNext()) != null) {
                for (String value : nextLine) {
                    System.out.print(value + " ");
                }
                System.out.println();
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public void WriteDatabase(int id, RLParams params, RLTrainingParams trainingParams, int fromId) {
        try {
            File file = new File(DB_PATH);
            boolean fileExists = file.exists();
            if (!fileExists)
                file.createNewFile();
            file.setWritable(true);
            CSVWriter writer = new CSVWriter(new FileWriter(file, true)); // Append mode
            if (!fileExists)
                writer.writeNext(header);
            writer.writeNext(formatData(id, params, trainingParams, 0));
            writer.close();
            file.setReadOnly();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DataProcessor dp = new DataProcessor();
        dp.WriteDatabase(0, new RLParams(new TicTacToeStateVector()), new RLTrainingParams(), 10);
    }
}

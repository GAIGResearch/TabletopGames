package players.heuristics;

import core.interfaces.ILearner;
import weka.core.matrix.LinearRegression;
import weka.core.matrix.Matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleRegressionLearner implements ILearner {

    String[] descriptions;
    double[] coefficients;

    @Override
    public void learnFrom(String... files) {
        String[] header = new String[0];
        List<double[]> data = new ArrayList<>();
        for (String file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                header = reader.readLine().split("\\t");
                while (reader.ready()) {
                    double[] datum = Arrays.stream(reader.readLine().split("\\t")).mapToDouble(Double::parseDouble).toArray();
                    data.add(datum);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new AssertionError("Problem reading file " + file);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                throw new AssertionError("Problem parsing data as numeric : " + file);
            }
        }

        // now convert data to [][]
        // we assume (for the moment) that the columns are: GameID, Player, Round, Turn, CurrentScore... Win, Ordinal, FinalScore
        // with ... representing the game specific features
        if (!header[0].equals("GameID") || !header[1].equals("Player") || !header[2].equals("Round") || !header[3].equals("Turn") || !header[4].equals("CurrentScore")) {
            throw new AssertionError("Unexpected starting header entries");
        }
        if (!header[header.length - 1].equals("FinalScore") || !header[header.length - 2].equals("Ordinal") || !header[header.length - 3].equals("Win")) {
            throw new AssertionError("Unexpected final header entries");
        }
        double[][] dataArray = new double[data.size()][];
        double[][] win = new double[data.size()][1];
        double[] ordinal = new double[data.size()];
        double[] finalScore = new double[data.size()];
        double[] currentScore = new double[data.size()];
        for (int i = 0; i < dataArray.length; i++) {
            double[] allData = data.get(i);
            win[i][0] = allData[header.length - 3];
            ordinal[i] = allData[header.length - 2];
            finalScore[i] = allData[header.length - 1];
            currentScore[i] = allData[4];
            double[] regressionData = new double[header.length - 5];
            System.arraycopy(allData, 2, regressionData, 0, regressionData.length);
            dataArray[i] = regressionData;
        }

        // We now have all the data loaded, so can run regression
        Matrix X = new Matrix(dataArray);
        Matrix Y = new Matrix(win);

        LinearRegression regression = new LinearRegression(X, Y, 0.01);
        coefficients = regression.getCoefficients();
        descriptions = new String[coefficients.length];
        System.arraycopy(header, 2, descriptions, 0, coefficients.length);

    }

    @Override
    public boolean writeToFile(String file) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(String.join("\t", descriptions) + "\n");
            writer.write(Arrays.stream(coefficients).mapToObj(d -> String.format("%.4g", d)).collect(Collectors.joining("\t")));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String name() {
        return "OLS";
    }

}

package players.learners;

import core.interfaces.ILearner;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractLearner implements ILearner {

    protected double[][] dataArray;
    protected String[] header;
    protected double[][] win;
    protected double[][] ordinal;
    protected double[][] finalScore;
    protected double[][] currentScore;
    protected ArrayList<Attribute> attributes;
    String[] descriptions;

    protected void loadData(String... files) {
        List<double[]> data = new ArrayList<>();
        for (String file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                header = reader.readLine().split("\\t");
                descriptions = new String[header.length - 8];
                System.arraycopy(header, 5, descriptions, 0, descriptions.length);
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
            throw new AssertionError("Unexpected starting header entries " + String.join("", header));
        }
        if (!header[header.length - 1].equals("FinalScore") || !header[header.length - 2].equals("Ordinal") || !header[header.length - 3].equals("Win")) {
            throw new AssertionError("Unexpected final header entries " + String.join("", header));
        }
        attributes = new ArrayList<>();
        for (int i = 5; i < header.length - 3; i++)
            attributes.add(new Attribute(header[i]));
        dataArray = new double[data.size()][];
        win = new double[data.size()][1];
        ordinal = new double[data.size()][1];
        finalScore = new double[data.size()][1];
        currentScore = new double[data.size()][1];
        double maxRound = 0.0;
        for (int i = 0; i < dataArray.length; i++) {
            double[] allData = data.get(i);
            if (allData[2] > maxRound)
                maxRound = allData[2];
            win[i][0] = allData[header.length - 3];
            ordinal[i][0] = allData[header.length - 2];
            finalScore[i][0] = allData[header.length - 1];
            currentScore[i][0] = allData[4];
            double[] regressionData = new double[header.length - 8];
            System.arraycopy(allData, 5, regressionData, 0, regressionData.length);
            dataArray[i] = regressionData;
        }

    }

    protected Instances createInstances() {
        List<String> values = new ArrayList<>();
        values.add("1");  // so that 'Win' is the first category, which means the coefficients are easier to interpret
        values.add("0");
        attributes.add(new Attribute("Win", values));
        Instances dataInstances = new Instances("data", attributes, dataArray.length);
        for (int i = 0; i < dataArray.length; i++) {
            double[] record = dataArray[i];
            double[] XandY = new double[record.length + 1];
            System.arraycopy(record, 0, XandY, 0, record.length);
            XandY[record.length] = win[i][0];
            dataInstances.add(new DenseInstance(1.0, XandY));
        }
        dataInstances.setClassIndex(attributes.size() - 1);
        return dataInstances;
    }
}

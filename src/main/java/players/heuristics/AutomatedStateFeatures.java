package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import utilities.Pair;
import utilities.Utils;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class AutomatedStateFeatures implements IStateFeatureVector {

    enum featureType {
        RAW, ENUM, RANGE
    }
    int buckets = 3;  // TODO: Extend this to be configurable for each underlying numeric feature independently
    IStateFeatureVector underlyingVector;
    String[] featureNames;
    int[] underlyingIndices;
    Enum<?>[] enumValues;
    featureType[] featureTypes;
    Pair<Number, Number>[] featureRanges;

    public AutomatedStateFeatures(IStateFeatureVector underlyingVector) {
        this.underlyingVector = underlyingVector;
    }
    public AutomatedStateFeatures(String jsonDescription) {
        // TODO: load from a JSON file
    }
    public void writeToJSON(String destination) {
        // TODO: write to a JSON file
    }

    public void processData(String inputFile, String outputFile) {
        // load files...the columns should correspond to the underlying vector
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", inputFile);
        List<String> headers = data.a;
        List<List<String>> dataRows = data.b;

        // We need to check that the data is consistent with the underlying vector
        String[] underlyingNames = underlyingVector.names();
        if (underlyingNames.length != headers.size()) {
            throw new IllegalArgumentException("Number of columns in data file does not match underlying vector");
        }

        // We now want to convert the dataRows into dataColumns
        List<List<String>> dataColumns = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            dataColumns.add(new ArrayList<>());
        }
        for (List<String> row : dataRows) {
            if (row.size() != headers.size()) {
                System.err.println("Warning: Skipping row with inconsistent number of columns: " + row);
                continue; // Skip rows with inconsistent number of columns
            }
            for (int i = 0; i < headers.size(); i++) {
                dataColumns.get(i).add(row.get(i));
            }
        }

        // for each column, determine the type of data
        for (int i = 0; i < headers.size(); i++) {
            List<String> columnData = dataColumns.get(i);

            // Check if the column is numeric
            boolean isNumeric = true;
            List<Double> numericValues = new ArrayList<>();
            for (String value : columnData) {
                if (!isNumeric(value)) {
                    isNumeric = false;
                    break;
                }
                try {
                    numericValues.add(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    System.out.println("Error parsing number: " + value);
                    isNumeric = false;
                    break;
                }
            }
            if (!isNumeric) {
                // Check if the column is an enum
            }
        }

        // 1. Enum.
        // We create one feature for each possible value of the enum.
        // this is actually independent of the data

        // 2. Numeric.
        // we extract the relevant data from the files, and divide this into buckets.
        // we then create a feature for each bucket, setting the relevant boundaries

        // 3. Other (not currently supported)
        // throw an exception
    }


    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        // TODO: implement this
        return new double[0];
    }

    @Override
    public Object[] featureVector(AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String[] names() {
        // TODO: implement this
        return underlyingVector.names();
    }

    @Override
    public Class<?>[] types() {
        return new Class[0];
    }
}

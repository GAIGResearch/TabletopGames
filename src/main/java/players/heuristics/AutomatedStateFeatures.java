package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import utilities.Pair;
import utilities.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
        try {
            List<String[]> columnsData = extractColumnsToArray(inputFile);

            if (columnsData != null && !columnsData.isEmpty()) {
                // Print the extracted data (for demonstration)
                for (int i = 0; i < columnsData.size(); i++) {
                    System.out.println("Column " + (i + 1) + ": " + Arrays.toString(columnsData.get(i)));
                }
            } else {
                System.out.println("No data extracted or file not found.");
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }


        // for each column, determine the type of data

        // 1. Enum.
        // We create one feature for each possible value of the enum.
        // this is actually independent of the data

        // 2. Numeric.
        // we extract the relevant data from the files, and divide this into buckets.
        // we then create a feature for each bucket, setting the relevant boundaries

        // 3. Other (not currently supported)
        // throw an exception
    }

    public static List<String[]> extractColumnsToArray(String filePath) throws IOException {
        List<String[]> columns = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String headerLine = reader.readLine(); // Read the header line

            if (headerLine == null) {
                return columns; // Empty file
            }

            String[] headers = headerLine.split("\t");
            int numColumns = headers.length;

            // Initialize ArrayLists to store data for each column
            List<String>[] columnLists = new List[numColumns];
            for (int i = 0; i < numColumns; i++) {
                columnLists[i] = new ArrayList<>();
            }

            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                String[] values = dataLine.split("\t");

                // Ensure that each row has the expected number of columns
                if (values.length == numColumns) {
                    for (int i = 0; i < numColumns; i++) {
                        columnLists[i].add(values[i]);
                    }
                } else {
                    System.err.println("Warning: Skipping row with inconsistent number of columns: " + dataLine);
                }
            }

            // Convert ArrayLists to String arrays
            for (int i = 0; i < numColumns; i++) {
                columns.add(columnLists[i].toArray(new String[0]));
            }

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing reader: " + e.getMessage());
                }
            }
        }

        return columns;
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        // TODO: implement this
    }

    @Override
    public Object[] featureVector(AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String[] names() {
        return underlyingVector.names();
    }
}

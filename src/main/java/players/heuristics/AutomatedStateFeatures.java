package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class AutomatedStateFeatures implements IStateFeatureVector {

    enum featureType {
        RAW, ENUM, STRING, RANGE, TARGET
    }

    int buckets = 3;  // TODO: Extend this to be configurable for each underlying numeric feature independently
    IStateFeatureVector underlyingVector;
    List<String> newFeatureNames = new ArrayList<>();
    List<featureType> newFeatureTypes = new ArrayList<>();
    List<Object> newEnumValues = new ArrayList<>();
    List<Pair<Number, Number>> newFeatureRanges = new ArrayList<>();
    List<Integer> underlyingFeatureIndices = new ArrayList<>();
    List<Class<?>> newFeatureClasses = new ArrayList<>();

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
        // while allowing for additional columns (for target values)
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", inputFile);
        List<String> headers = data.a;
        List<List<String>> dataRows = data.b;

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

            // now determine is the column is one of those specified in the underlying vector
            int underlyingindex = -1;
            for (int j = 0; j < underlyingVector.names().length; j++) {
                if (underlyingVector.names()[j].equals(headers.get(i))) {
                    underlyingindex = j;
                    break;
                }
            }
            if (underlyingindex == -1) {
                // this column is not in the underlying vector
                // so we do not generate any features
                // we do however keep it to write to the output file
                newFeatureNames.add(headers.get(i));
                newFeatureTypes.add(featureType.TARGET);
                newEnumValues.add(null);
                newFeatureRanges.add(null);
                underlyingFeatureIndices.add(-1);
                newFeatureClasses.add(calculateClass(columnData));
            } else {
                Class<?> columnType = underlyingVector.types()[underlyingindex];
                String columnName = headers.get(i);
                System.out.println("Processing column: " + columnName + " of type: " + columnType);

                if (!columnType.isEnum()) {
                    newFeatureNames.add(columnName);
                    newEnumValues.add(null);
                    newFeatureRanges.add(null);
                    underlyingFeatureIndices.add(underlyingindex);
                    newFeatureTypes.add(featureType.RAW);
                }

                // TODO: Consider what to do (if anything) for conditional features
                // i.e. should we distinguish between a 0 and a "" in a numeric column?
                // with "" being a missing value that is excluded and *NOT* counted as a zero when
                // calculating the buckets
                // in fact, the issue is that with some columns, zero will mean "not populated", and in others
                // it will mean "populated with zero"...there is no easy way to distinguish between these

                if (columnType.equals(Boolean.class) || columnType.equals(boolean.class)) {
                    // we do nothing - a RAW feature is all we need
                } else if (columnType.equals(Integer.class) || columnType.equals(int.class)) {
                    // we then add a one-hot feature for each bucket
                    List<Integer> numericValues = validateIntColumnData(columnData);
                    Collections.sort(numericValues);
                    for (int b = 0; b < buckets; b++) {
                        int lowerBound = numericValues.get((int) Math.floor((double) (b * numericValues.size()) / buckets));
                        int upperBound = numericValues.get((int) Math.floor((double) ((b + 1) * numericValues.size()) / buckets));
                        if (b == 0) {
                            lowerBound = Integer.MIN_VALUE;
                        }
                        if (b == buckets - 1) {
                            upperBound = Integer.MAX_VALUE;
                        }
                        newFeatureRanges.add(new Pair<>(lowerBound, upperBound));
                        newFeatureNames.add(columnName + "_B" + b);
                        newFeatureTypes.add(featureType.RANGE);
                        newEnumValues.add(null);
                        underlyingFeatureIndices.add(underlyingindex);
                    }

                } else if (columnType.equals(Double.class) || columnType.equals(double.class)) {// we add a raw feature for this column
                    // we then add a one-hot feature for each bucket
                    List<Double> numericValues = validateDoubleColumnData(columnData);
                    if (numericValues.isEmpty()) {
                        System.err.println("Warning: Skipping column with non-numeric data: " + columnName);
                        continue; // Skip columns with non-numeric data
                    }
                    // we sort the total set of values in order and then divide this into buckets
                    Collections.sort(numericValues);
                    for (int b = 0; b < buckets; b++) {
                        double lowerBound = numericValues.get((int) Math.floor((double) (b * numericValues.size()) / buckets));
                        double upperBound = numericValues.get((int) Math.floor((double) ((b + 1) * numericValues.size()) / buckets));
                        if (b == 0) {
                            lowerBound = Double.NEGATIVE_INFINITY;
                        }
                        if (b == buckets - 1) {
                            upperBound = Double.POSITIVE_INFINITY;
                        }
                        newFeatureRanges.add(new Pair<>(lowerBound, upperBound));
                        newFeatureNames.add(columnName + "_B" + b);
                        newFeatureTypes.add(featureType.RANGE);
                        newEnumValues.add(null);
                        underlyingFeatureIndices.add(underlyingindex);
                    }
                } else if (columnType.isEnum()) {
                    Class<Enum> enumClass = (Class<Enum>) columnType;
                    Set<Enum<?>> uniqueValues = new HashSet<>();
                    Enum[] enumValues = enumClass.getEnumConstants();
                    // we then add one feature per enum value as a one-hot encoding

                    for (int count = 0; count < enumValues.length; count++) {
                        Enum<?> enumValue = enumValues[count];
                        uniqueValues.add(enumValue);
                        newFeatureNames.add(columnName + "_" + enumValue.name());
                        newFeatureTypes.add(featureType.ENUM);
                        newEnumValues.add(enumValue);
                        underlyingFeatureIndices.add(underlyingindex);
                    }
                } else if (columnType.equals(String.class)) {
                    // in this case, we proceed much like the enum case
                    // we add a one-hot feature for each unique value (but if there are more than 10 different values,
                    // we just skip the field completely)
                    Set<String> uniqueValues = new HashSet<>(columnData);
                    if (uniqueValues.size() > 10) {
                        System.err.println("Warning: Skipping column with too many unique values: " + columnName);
                        continue; // Skip columns with too many unique values
                    }
                    for (String value : uniqueValues) {
                        newFeatureNames.add(columnName + "_" + value);
                        newFeatureTypes.add(featureType.STRING);
                        newEnumValues.add(value);
                        underlyingFeatureIndices.add(underlyingindex);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported column type: " + columnType + " for column: " + columnName);

                }
            }
        }

        // Now we convert the dataRows into the new feature space
        List<List<Object>> newDataRows = new ArrayList<>();
        for (List<String> row : dataRows) {
            if (row.size() != headers.size()) {
                System.err.println("Warning: Skipping row with inconsistent number of columns: " + row);
                continue; // Skip rows with inconsistent number of columns
            }
            List<Object> newRow = new ArrayList<>();
            for (int j = 0; j < newFeatureNames.size(); j++) {
                String value = row.get(underlyingFeatureIndices.get(j));
                if (newFeatureTypes.get(j) == featureType.RANGE) {
                    double numericValue = Double.parseDouble(value);
                    Pair<Number, Number> range = newFeatureRanges.get(j);
                    if (numericValue >= range.a.doubleValue() && numericValue < range.b.doubleValue()) {
                        newRow.add(1);
                    } else {
                        newRow.add(0);
                    }
                } else if (newFeatureTypes.get(j) == featureType.ENUM) {
                    Enum<?> enumValue = (Enum<?>) newEnumValues.get(j);
                    if (enumValue.name().equals(value)) {
                        newRow.add(1);
                    } else {
                        newRow.add(0);
                    }
                } else if (newFeatureTypes.get(j) == featureType.STRING) {
                    String stringValue = (String) newEnumValues.get(j);
                    if (stringValue.equals(value)) {
                        newRow.add(1);
                    } else {
                        newRow.add(0);
                    }
                } else if (newFeatureTypes.get(j) == featureType.TARGET) {
                    // Just add the original value
                    newRow.add(value);
                } else {
                    // RAW or TARGET feature
                    if (newFeatureClasses.get(j) == Integer.class || newFeatureClasses.get(j) == int.class) {
                        newRow.add(Integer.parseInt(value));
                    } else if (newFeatureClasses.get(j) == Double.class || newFeatureClasses.get(j) == double.class) {
                        newRow.add(Double.parseDouble(value));
                    } else if (newFeatureClasses.get(j) == Boolean.class || newFeatureClasses.get(j) == boolean.class) {
                        newRow.add(Boolean.parseBoolean(value));
                    } else {
                        // Handle other types as needed
                        System.err.println("Warning: Unsupported type for column: " + newFeatureNames.get(j));
                    }
                }
            }
            newDataRows.add(newRow);
        }

        // Now we write the new data to the output file
        Utils.writeDataWithHeader("\t", newFeatureNames, newDataRows, outputFile);
    }

    private Class<?> calculateClass(List<String> columnData) {
        // Check if all values are numeric
        boolean allNumeric = true;
        for (String value : columnData) {
            if (!isNumeric(value)) {
                allNumeric = false;
                break;
            }
        }

        // Determine the class based on the data type
        if (allNumeric) {
            if (columnData.stream().allMatch(value -> value.contains("."))) {
                return Double.class;
            } else {
                return Integer.class;
            }
        } else {
            // If not all values are numeric, we can assume it's a String or Enum
            return String.class; // or Enum.class if you want to handle enums
        }
    }

    private List<Double> validateDoubleColumnData(List<String> columnData) {
        List<Double> numericValues = new ArrayList<>();
        for (String value : columnData) {
            if (isNumeric(value)) {
                numericValues.add(Double.parseDouble(value));
            } else {
                System.err.println("Warning: Skipping non-numeric value: " + value);
            }
        }
        return numericValues;
    }

    private List<Integer> validateIntColumnData(List<String> columnData) {
        List<Integer> numericValues = new ArrayList<>();
        for (String value : columnData) {
            if (isNumeric(value)) {
                numericValues.add(Integer.parseInt(value));
            } else {
                System.err.println("Warning: Skipping non-numeric value: " + value);
            }
        }
        return numericValues;
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

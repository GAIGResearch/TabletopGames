package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class AutomatedStateFeatures implements IStateFeatureVector {

    enum featureType {
        RAW, ENUM, STRING, RANGE, TARGET
    }

    int buckets = 3;  // TODO: Extend this to be configurable for each underlying numeric feature independently
    IStateFeatureVector underlyingVector;
    List<String> featureNames = new ArrayList<>();
    List<featureType> featureTypes = new ArrayList<>();
    List<Object> enumValues = new ArrayList<>();
    List<Pair<Number, Number>> featureRanges = new ArrayList<>();
    List<Integer> featureIndices = new ArrayList<>();

    public AutomatedStateFeatures(IStateFeatureVector underlyingVector) {
        this.underlyingVector = underlyingVector;
    }

    public AutomatedStateFeatures(String jsonDescription) {
        // TODO: load from a JSON file
    }

    @SuppressWarnings("unchecked")
    public void writeToJSON(String destination) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("class", this.getClass().getName());
        jsonObject.put("buckets", buckets);
        jsonObject.put("underlyingVector", underlyingVector.getClass().getName());
        // instead of writing each of the remaining fields as an array,
        // we want an array of JSONObjects, one per feature that has subfields for name, type, enumValue, range, and index
        List<JSONObject> featureObjects = new ArrayList<>();
        for (int i = 0; i < featureNames.size(); i++) {
            JSONObject featureObject = new JSONObject();
            featureObject.put("name", featureNames.get(i));
            featureObject.put("type", featureTypes.get(i).toString());
            if (featureTypes.get(i) == featureType.ENUM || featureTypes.get(i) == featureType.STRING) {
                featureObject.put("enumValue", enumValues.get(i).toString());
            } else if (featureTypes.get(i) == featureType.RANGE) {
                Pair<Number, Number> range = featureRanges.get(i);
                featureObject.put("range", "[" + range.a + ", " + range.b + "]");
            }
            featureObject.put("index", featureIndices.get(i));
            featureObjects.add(featureObject);
        }
        jsonObject.put("features", featureObjects);

        // Write the JSON object to the specified destination
        JSONUtils.writeJSON(jsonObject, destination);
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        // we first extract the underlying vector to get the raw data
        Object[] underlyingVectorData = underlyingVector.featureVector(state, playerID);

        // then we iterate over the automated features and generate these from the raw data
        double[] featureVector = new double[featureNames.size()];
        for (int i = 0; i < featureNames.size(); i++) {
            int underlyingIndex = featureIndices.get(i);
            if (underlyingIndex == -1) {
                throw new IllegalArgumentException("Feature index cannot be -1");
            } else {
                Object value = underlyingVectorData[underlyingIndex];
                switch (featureTypes.get(i)) {
                    case RAW:
                        featureVector[i] = ((Number) value).doubleValue();
                        break;
                    case ENUM:
                        Enum<?> enumValue = (Enum<?>) value;
                        featureVector[i] = enumValue.equals(enumValues.get(i)) ? 1 : 0;
                        break;
                    case STRING:
                        String stringValue = (String) value;
                        featureVector[i] = stringValue.equals(enumValues.get(i)) ? 1 : 0;
                        break;
                    case RANGE:
                        double numericValue = ((Number) value).doubleValue();
                        Pair<Number, Number> range = featureRanges.get(i);
                        if (numericValue >= range.a.doubleValue() && numericValue < range.b.doubleValue()) {
                            featureVector[i] = 1;
                        } else {
                            featureVector[i] = 0;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported type: " + featureTypes.get(i));
                }
            }
        }
        return featureVector;
    }

    @Override
    public Object[] featureVector(AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String[] names() {
        return featureNames.toArray(new String[0]);
    }

    public void processData(String inputFile, String outputFile) {
        List<String> newFeatureNames = new ArrayList<>();
        List<featureType> newFeatureTypes = new ArrayList<>();
        List<Object> newEnumValues = new ArrayList<>();
        List<Pair<Number, Number>> newFeatureRanges = new ArrayList<>();
        List<Integer> underlyingFeatureIndices = new ArrayList<>();
        List<Class<?>> newFeatureClasses = new ArrayList<>();

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
                    newFeatureClasses.add(columnType);
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
                        newFeatureClasses.add(Integer.class);
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
                        newFeatureClasses.add(Double.class);
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
                        newFeatureClasses.add(Boolean.class);
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
                        newFeatureClasses.add(Boolean.class);
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

        // and finally we set the new feature names, types, ranges, and indices (ignoring any TARGET features)
        for (int i = 0; i < newFeatureNames.size(); i++) {
            if (underlyingFeatureIndices.get(i) != -1) {
                featureNames.add(newFeatureNames.get(i));
                featureTypes.add(newFeatureTypes.get(i));
                enumValues.add(newEnumValues.get(i));
                featureRanges.add(newFeatureRanges.get(i));
                featureIndices.add(underlyingFeatureIndices.get(i));
            }
        }
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

}

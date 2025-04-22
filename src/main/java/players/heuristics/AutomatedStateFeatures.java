package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import games.dominion.metrics.DomStateFeaturesReduced;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class AutomatedStateFeatures implements IStateFeatureVector, IToJSON {

    enum featureType {
        RAW, ENUM, STRING, RANGE, TARGET
    }

    int buckets = 5;  // TODO: Extend this to be configurable for each underlying numeric feature independently
    IStateFeatureVector underlyingVector;
    List<String> featureNames = new ArrayList<>();
    List<featureType> featureTypes = new ArrayList<>();
    List<Object> enumValues = new ArrayList<>();
    List<Pair<Number, Number>> featureRanges = new ArrayList<>();
    List<Integer> featureIndices = new ArrayList<>();

    public AutomatedStateFeatures(IStateFeatureVector underlyingVector) {
        this.underlyingVector = underlyingVector;
    }

    public AutomatedStateFeatures(JSONObject json) {
        if (json.get("class") == null || json.get("class").toString().isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON file: missing 'class' field");
        }
        if (!json.get("class").toString().equals(this.getClass().getName())) {
            throw new IllegalArgumentException("Invalid JSON file: class mismatch");
        }
        // now populate each of the class fields from JSON
        buckets = Integer.parseInt(json.get("buckets").toString());
        underlyingVector = JSONUtils.loadClassFromJSON((JSONObject) json.get("underlyingVector"));
        JSONArray features = (JSONArray) json.get("features");
        for (Object feature : features) {
            JSONObject featureObject = (JSONObject) feature;
            String name = featureObject.get("name").toString();
            featureNames.add(name);
            String type = featureObject.get("type").toString();
            switch (type) {
                case "RAW" -> {
                    featureTypes.add(featureType.RAW);
                    enumValues.add(null);
                    featureRanges.add(null);
                }
                case "ENUM" -> {
                    featureTypes.add(featureType.ENUM);
                    enumValues.add(featureObject.get("enumValue"));
                    featureRanges.add(null);
                }
                case "STRING" -> {
                    featureTypes.add(featureType.STRING);
                    enumValues.add(featureObject.get("enumValue"));
                    featureRanges.add(null);
                }
                case "RANGE" -> {
                    featureTypes.add(featureType.RANGE);
                    String rangeString = featureObject.get("range").toString();
                    String[] rangeParts = rangeString.replaceAll("[\\[\\]]", "").split(",");
                    Number lowerBound =  Double.parseDouble(rangeParts[0]);
                    Number upperBound = Double.parseDouble(rangeParts[1]);
                    featureRanges.add(new Pair<>(lowerBound, upperBound));
                }
                default -> throw new IllegalArgumentException("Unsupported type: " + type);
            }
            int index = Integer.parseInt(featureObject.get("index").toString());
            featureIndices.add(index);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("class", this.getClass().getName());
        jsonObject.put("buckets", buckets);
        JSONObject underlyingVector = new JSONObject();
        underlyingVector.put("class", this.underlyingVector.getClass().getName());
        jsonObject.put("underlyingVector", underlyingVector);
        // instead of writing each of the remaining fields as an array,
        // we want an array of JSONObjects, one per feature that has subfields for name, type, enumValue, range, and index
        JSONArray featureObjects = new JSONArray();
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
        return jsonObject;
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
        Map<Integer, Integer> underlyingIndexToRowIndex = new HashMap<>();

        // load files...the columns should correspond to the underlying vector
        // while allowing for additional columns (for target values)
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", inputFile);
        List<String> headers = data.a;
        List<List<String>> dataRows = data.b;
        Map<String, Integer> targetData = new HashMap<>();

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
                Class<?> columnType = calculateClass(columnData);
                newFeatureClasses.add(columnType);
                targetData.put(headers.get(i), i);
            } else {
                Class<?> columnType = underlyingVector.types()[underlyingindex];
                underlyingIndexToRowIndex.put(underlyingindex, i);
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

                if (columnType.equals(Boolean.class) || columnType.equals(boolean.class)) {
                    // we do nothing - a RAW feature is all we need
                } else if (columnType.equals(Integer.class) || columnType.equals(int.class) ||
                        columnType.equals(Double.class) || columnType.equals(double.class)) {
                    // we then add a one-hot feature for each bucket

                    Class<?> numericClass = columnType.equals(Integer.class) || columnType.equals(int.class) ?
                            Integer.class : Double.class;
                    List<Pair<Number, Number>> proposedFeatureRanges = calculateFeatureRanges(columnData, buckets, numericClass);
                    for (int b = 0; b < proposedFeatureRanges.size(); b++) {
                        Pair<Number, Number> range = proposedFeatureRanges.get(b);
                        newFeatureTypes.add(featureType.RANGE);
                        newFeatureRanges.add(range);
                        newFeatureNames.add(columnName + "_B" + b);
                        newEnumValues.add(null);
                        newFeatureClasses.add(numericClass);
                        underlyingFeatureIndices.add(underlyingindex);
                    }

                } else if (columnType.isEnum()) {
                    Class<Enum> enumClass = (Class<Enum>) columnType;
                    Set<Enum<?>> uniqueValues = new HashSet<>();
                    Enum[] enumValues = enumClass.getEnumConstants();
                    // we then add one feature per enum value as a one-hot encoding

                    for (Enum<?> enumValue : enumValues) {
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
                if (newFeatureTypes.get(j) == featureType.TARGET) {
                    newRow.add(row.get(targetData.get(newFeatureNames.get(j))));
                } else {
                    String value = row.get(underlyingIndexToRowIndex.get(underlyingFeatureIndices.get(j)));
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
                    } else {
                        // RAW feature
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

        // remove the suffix from output file, and replace with json
        String jsonOutputFile = outputFile.substring(0, outputFile.lastIndexOf('.')) + ".json";
        JSONObject outputJson = toJSON();
        JSONUtils.writeJSON(outputJson, jsonOutputFile);
    }

    private <T extends Number> List<Pair<Number, Number>> calculateFeatureRanges(List<T> numericValues, int buckets, List<Number> exclusions) {
        List<Pair<Number, Number>> featureRanges = new ArrayList<>();
        for (int b = 0; b < buckets; b++) {
            Number lowerBound = b == 0 ? Double.NEGATIVE_INFINITY :
                    numericValues.get((b * numericValues.size()) / buckets);
            Number upperBound = b == buckets - 1 ? Double.POSITIVE_INFINITY :
                    numericValues.get(((b + 1) * numericValues.size()) / buckets);
            featureRanges.add(new Pair<>(lowerBound, upperBound));
        }
        // now we detect any ranges with the same start and end bounds
        List<Number> newExclusions = featureRanges.stream()
                .filter(pair -> pair.a.equals(pair.b))
                .map(pair -> pair.a)
                .toList();
        if (!newExclusions.isEmpty()) {
            List<T> filteredValues = numericValues.stream()
                    .filter(value -> !newExclusions.contains(value))
                    .toList();
            List<Number> allExclusions = new ArrayList<>(exclusions);
            allExclusions.addAll(newExclusions);
            featureRanges = filteredValues.isEmpty()
                    ? new ArrayList<>()
                    : calculateFeatureRanges(filteredValues, buckets - newExclusions.size(), allExclusions);

            // we now create one Range for each of the excluded values
            for (Number exclusion : newExclusions) {
                // we then need to find the value in the data that bracket the exclusion (defaulting to +/- infinity)
                Number nextHighestValue = Double.POSITIVE_INFINITY;
                for (T value : numericValues) {  // this relies on the fact that the values are sorted
                    if (value.doubleValue() > exclusion.doubleValue()) {
                        nextHighestValue = value;
                        break;
                    }
                }

                // we check if the exclusion is already in the ranges
                List<Pair<Number, Number>> toRemove = new ArrayList<>();
                List<Pair<Number, Number>> toAdd = new ArrayList<>();
                for (Pair<Number, Number> range : featureRanges) {
                    if (range.a.doubleValue() < exclusion.doubleValue() && range.b.doubleValue() > exclusion.doubleValue()) {
                        // we need to remove this range, and split it into two
                        toRemove.add(range);
                        toAdd.add(Pair.of(range.a, exclusion));
                        toAdd.add(Pair.of(nextHighestValue, range.b));
                    }
                }
                // remove the ranges that we split
                featureRanges.removeAll(toRemove);
                featureRanges.addAll(toAdd);
                // then add the exclusion as a range
                featureRanges.add(new Pair<>(exclusion, nextHighestValue));
            }

            // and sort the features to be in ascending order
            featureRanges.sort(Comparator.comparingDouble(pair -> pair.a.doubleValue() + pair.b.doubleValue() * 0.0001));


            // then go up the feature ranges to check that they enclose the real line, and that the end point of one is
            // the start point of the next
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < featureRanges.size() - 1; i++) {
                Pair<Number, Number> range1 = featureRanges.get(i);
                Pair<Number, Number> range2 = featureRanges.get(i + 1);
   //             System.out.println("Checking ranges: " + range1 + " and " + range2);
                // if two ranges start at the same point, then we remove the first one (the one with the earlier end point)
                if (range1.a.equals(range2.a)) {
                    toRemove.add(i);
     //               System.out.println("Removing range: " + range1 + " because it is equal to: " + range2);
                    continue;
                }
                if (range1.b.doubleValue() != range2.a.doubleValue()) {
                    range1.b = range2.a;
                }
            }
            // remove the ranges that we split (this has to be by index, as some ranges may be duplicated, and we don't want to remove both)
            for (int i = toRemove.size() - 1; i >= 0; i--) {
                featureRanges.remove((int) toRemove.get(i));
            }
            // if the first range has an end point that is equal to the lowest value in the data, then we remove it
            // and amend the next range to start at minus infinity
            if (featureRanges.get(0).b.doubleValue() <= numericValues.get(0).doubleValue()) {
                featureRanges.remove(0);
                featureRanges.get(0).a = Double.NEGATIVE_INFINITY;
            }
        }

        return featureRanges;
    }

    private List<Pair<Number, Number>> calculateFeatureRanges(List<String> columnData, int buckets, Class<?> clazz) {
        List<Double> doubleValues;
        List<Integer> integerValues;
        if (clazz == Double.class) {
            doubleValues = validateDoubleColumnData(columnData);
            Collections.sort(doubleValues);
            return calculateFeatureRanges(doubleValues, buckets, Collections.emptyList());
        } else if (clazz == Integer.class) {
            integerValues = validateIntColumnData(columnData);
            Collections.sort(integerValues);
            return calculateFeatureRanges(integerValues, buckets, Collections.emptyList());
        } else {
            throw new IllegalArgumentException("Unsupported class type: " + clazz);
        }
    }

    private Class<?> calculateClass(List<String> columnData) {
        // Check if all values are numeric
        boolean allNumeric = true;
        for (String value : columnData) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
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
            try {
                numericValues.add(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Skipping non-numeric double value: " + value);
                return Collections.emptyList();
            }
        }
        return numericValues;
    }

    private List<Integer> validateIntColumnData(List<String> columnData) {
        List<Integer> numericValues = new ArrayList<>();
        for (String value : columnData) {
            try {
                numericValues.add(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Skipping non-numeric integer value: " + value);
                return Collections.emptyList();
            }
        }
        return numericValues;
    }


    public static void main(String[] args) {
        AutomatedStateFeatures asf = new AutomatedStateFeatures(new DomStateFeaturesReduced());
        String inputFile = "C:\\TAG\\DominionFeaturesASF\\DomStateFeatures001.txt"; // Replace with your input file path
        String outputFile = "C:\\TAG\\DominionFeaturesASF\\ASF_22Apr.txt"; // Replace with your output file path
        asf.processData(inputFile, outputFile);
    }
}

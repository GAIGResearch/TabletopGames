//package players.heuristics;
//
//import org.json.simple.JSONObject;
//import utilities.JSONUtils;
//import utilities.Pair;
//import utilities.Utils;
//
//import java.util.*;
//
//public class OldASF {
//    public List<List<Object>> processData(String outputFile, String... inputFiles) {
//        // inputFiles contain the raw data.
//        // There can be two types of columns:
//        // 1. Columns that refer to existing featureNames. These are detected by matching the names.
//        // 2. Columns that are not features. These are copied over into the output file without becoming features.
//
//        // The point of processData is to also add in new columns for features that do not yet have columns
//        // 1. Bucketing of numeric features (range features).
//        //          For the moment we always recalculate bucket features from scratch. (TODO: improve this?)
//        //          As the data distribution may have changed.
//        // 2. One-hot encoding of enum features
//        //          We check the enum, and check to see if all of the expected columns are present.
//        // 3. One-hot encoding of string features
//        //          As for enum features
//        // 4. One-hot encoding of boolean features
//        //          As for enum features
//        // 5. Interaction columns between any two of the above features
//        //          if this already exists in the data, we do not add it again
//
//        List<String> newFeatureNames = new ArrayList<>();
//        List<featureType> newFeatureTypes = new ArrayList<>();
//        List<Object> newEnumValues = new ArrayList<>();
//        List<Pair<Number, Number>> newFeatureRanges = new ArrayList<>();
//        List<Integer> underlyingFeatureIndices = new ArrayList<>();
//        List<Class<?>> newFeatureClasses = new ArrayList<>();
//        List<Pair<Integer, Integer>> newInteractions = new ArrayList<>();
//        Map<Integer, Integer> underlyingIndexToRowIndex = new HashMap<>();
//
//        // load files...the columns should correspond to the underlying vector
//        // while allowing for additional columns (for target values)
//        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", inputFiles);
//        List<String> headers = data.a;
//        List<List<String>> dataRows = data.b;
//        Map<String, Integer> targetData = new HashMap<>();
//
//        // We now want to convert the dataRows into dataColumns
//        List<List<String>> dataColumns = new ArrayList<>();
//        for (int i = 0; i < headers.size(); i++) {
//            dataColumns.add(new ArrayList<>());
//        }
//        for (List<String> row : dataRows) {
//            if (row.size() != headers.size()) {
//                //         System.err.println("Warning: Skipping row with inconsistent number of columns: " + row);
//                continue; // Skip rows with inconsistent number of columns
//            }
//            for (int i = 0; i < headers.size(); i++) {
//                dataColumns.get(i).add(row.get(i));
//            }
//        }
//
//        // for each column, determine the type of data
//        featureLoop:
//        for (int i = 0; i < headers.size(); i++) {
//            List<String> columnData = dataColumns.get(i);
//            //      System.out.println("Processing column: " + headers.get(i));
//
//            // is the column one which is already processed?
//            // if so, we can take the information directly from the previous features
//            if (previousFeatures != null) {
//                for (int j = 0; j < previousFeatures.featureNames.size(); j++) {
//                    if (previousFeatures.featureNames.get(j).equals(headers.get(i))) {
//                        newFeatureNames.add(previousFeatures.featureNames.get(j));
//                        newFeatureTypes.add(previousFeatures.featureTypes.get(j));
//                        newEnumValues.add(previousFeatures.enumValues.get(j));
//                        newFeatureRanges.add(previousFeatures.featureRanges.get(j));
//                        newInteractions.add(previousFeatures.interactions.get(j));
//                        underlyingFeatureIndices.add(previousFeatures.featureIndices.get(j));
//                        if (previousFeatures.featureTypes.get(j) == featureType.RAW)
//                            underlyingIndexToRowIndex.put(j, i);
//                        // We just check the first 20 values to determine the class (to avoid performance issues)
//                        newFeatureClasses.add(calculateClass(columnData, false));
//                        //       System.out.println("Skipped " + headers.get(i) + " as already processed");
//                        continue featureLoop;
//                    }
//                }
//            }
//
//            // now determine if the column is one of those specified in the underlying vector
//            // see if we have a feature that matches this name
//
//            // TODO: We actually have two cases. One in which we already have featureNames (i.e. we are re-processing)
//            // and one in which we are processing for the first time
//            int featureIndex = featureNames.indexOf(headers.get(i));
//            if (featureIndex == -1) {
//                // this column is not in the underlying vector, or is already processed
//                // so we do not generate any features
//                // we do however keep it to write to the output file
//                newFeatureNames.add(headers.get(i));
//                newFeatureTypes.add(featureType.TARGET);
//                newEnumValues.add(null);
//                newFeatureRanges.add(null);
//                newInteractions.add(null);
//                underlyingFeatureIndices.add(-1);
//                Class<?> columnType = calculateClass(columnData, true);
//                newFeatureClasses.add(columnType);
//                targetData.put(headers.get(i), i);
//            } else {
//                int underlyingIndex = getUnderlyingIndex(featureIndex);
//                Class<?> columnType = types()[featureIndex];
//                if (featureTypes.get(featureIndex) == featureType.RAW)
//                    underlyingIndexToRowIndex.put(underlyingIndex, i);
//                String columnName = headers.get(i);
//                //           System.out.println("Processing column: " + columnName + " of type: " + columnType);
//
//                if (!columnType.isEnum()) {
//                    newFeatureNames.add(columnName);
//                    newFeatureClasses.add(columnType);
//                    newEnumValues.add(null);
//                    newInteractions.add(null);
//                    newFeatureRanges.add(null);
//                    underlyingFeatureIndices.add(underlyingIndex);
//                    newFeatureTypes.add(featureType.RAW);
//                }
//
//                if (columnType.equals(Boolean.class) || columnType.equals(boolean.class)) {
//                    // we do nothing - a RAW feature is all we need
//                } else if (columnType.equals(Integer.class) || columnType.equals(int.class) ||
//                        columnType.equals(Double.class) || columnType.equals(double.class)) {
//                    // we then add a one-hot feature for each bucket
//
//                    Class<?> numericClass = columnType.equals(Integer.class) || columnType.equals(int.class) ?
//                            Integer.class : Double.class;
//                    if (buckets[underlyingIndex] > 1) {
//                        List<Pair<Number, Number>> proposedFeatureRanges = calculateFeatureRanges(columnData, buckets[underlyingIndex], numericClass);
//                        for (int b = 0; b < proposedFeatureRanges.size(); b++) {
//                            Pair<Number, Number> range = proposedFeatureRanges.get(b);
//                            newFeatureTypes.add(featureType.RANGE);
//                            newFeatureRanges.add(range);
//                            newFeatureNames.add(columnName + "_B" + b);
//                            newEnumValues.add(null);
//                            newInteractions.add(null);
//                            newFeatureClasses.add(numericClass);
//                            underlyingFeatureIndices.add(underlyingIndex);
//                        }
//                    }
//
//                } else if (columnType.isEnum()) {
//                    Class<Enum> enumClass = (Class<Enum>) columnType;
//                    Set<Enum<?>> uniqueValues = new HashSet<>();
//                    Enum[] enumValues = enumClass.getEnumConstants();
//                    // we then add one feature per enum value as a one-hot encoding
//
//                    for (Enum<?> enumValue : enumValues) {
//                        uniqueValues.add(enumValue);
//                        newFeatureNames.add(columnName + "_" + enumValue.name());
//                        newFeatureTypes.add(featureType.ENUM);
//                        newFeatureClasses.add(Boolean.class);
//                        newEnumValues.add(enumValue);
//                        newInteractions.add(null);
//                        newFeatureRanges.add(null);
//                        underlyingFeatureIndices.add(underlyingIndex);
//                    }
//                } else if (columnType.equals(String.class)) {
//                    // in this case, we proceed much like the enum case
//                    // we add a one-hot feature for each unique value (but if there are more than 10 different values,
//                    // we just skip the field completely)
//                    Set<String> uniqueValues = new HashSet<>(columnData);
//                    if (uniqueValues.size() > 10) {
//                        System.err.println("Warning: Skipping column with too many unique values: " + columnName);
//                        continue; // Skip columns with too many unique values
//                    }
//                    for (String value : uniqueValues) {
//                        newFeatureNames.add(columnName + "_" + value);
//                        newFeatureTypes.add(featureType.STRING);
//                        newFeatureClasses.add(Boolean.class);
//                        newEnumValues.add(value);
//                        newInteractions.add(null);
//                        newFeatureRanges.add(null);
//                        underlyingFeatureIndices.add(underlyingIndex);
//                    }
//                } else {
//                    throw new IllegalArgumentException("Unsupported column type: " + columnType + " for column: " + columnName);
//
//                }
//            }
//        }
//
//        // Now a second pass to pick up new interactions that need to be added
//        for (int i = 0; i < featureNames.size(); i++) {
//            if (featureTypes.get(i) != featureType.INTERACTION) {
//                continue; // Only interested in interactions
//            }
//            // then check to see if this is in newFeatureNames
//            if (newFeatureNames.contains(featureNames.get(i)))
//                continue;
//
//            if (previousFeatures == null) {
//                throw new IllegalArgumentException("Cannot process interaction " + featureNames.get(i) + " without previous features");
//            }
//
//            Pair<Integer, Integer> interactionIndices = interactions.get(i);
//            // as a safety check we compare the featureNames and newFeatureNames at the specified indices to ensure
//            // these are the same
//            // The indexing of newFeatureNames includes all TARGET features. These are not included in the indexing of previousFeatures
//            List<String> newFeatureNamesExcludingTargets = new ArrayList<>();
//            for (int j = 0; j < newFeatureNames.size(); j++) {
//                if (newFeatureTypes.get(j) != featureType.TARGET) {
//                    newFeatureNamesExcludingTargets.add(newFeatureNames.get(j));
//                }
//            }
//            String nameOrig1 = previousFeatures.featureNames.get(interactionIndices.a);
//            String nameOrig2 = previousFeatures.featureNames.get(interactionIndices.b);
//            String nameNew1 = newFeatureNamesExcludingTargets.get(interactionIndices.a);
//            String nameNew2 = newFeatureNamesExcludingTargets.get(interactionIndices.b);
//            if (!nameOrig1.equals(nameNew1) || !nameOrig2.equals(nameNew2)) {
//                String errorString = "Interaction " + featureNames.get(i) + " does not match previous features: " +
//                        nameOrig1 + " vs " + nameNew1 + ", " + nameOrig2 + " vs " + nameNew2;
//                throw new AssertionError(errorString);
//            }
//
//            newFeatureNames.add(featureNames.get(i));
//            newFeatureTypes.add(featureType.INTERACTION);
//            newEnumValues.add(null);
//            newFeatureRanges.add(null);
//            newInteractions.add(Pair.of(interactionIndices.a, interactionIndices.b));
//            underlyingFeatureIndices.add(-1);
//            newFeatureClasses.add(Double.class);
//        }
//
//        // update feature names, types, ranges, and indices (ignoring any TARGET features)
//        featureNames.clear();
//        featureTypes.clear();
//        enumValues.clear();
//        featureRanges.clear();
//        featureIndices.clear();
//        interactions.clear();
//        int[] featureToRowIndex = new int[newFeatureNames.size()];  // formally bigger than needed
//        int featuresFound = 0;
//        for (int i = 0; i < newFeatureNames.size(); i++) {
//            if (newFeatureTypes.get(i) != featureType.TARGET) {
//                featureNames.add(newFeatureNames.get(i));
//                featureTypes.add(newFeatureTypes.get(i));
//                enumValues.add(newEnumValues.get(i));
//                featureRanges.add(newFeatureRanges.get(i));
//                featureIndices.add(underlyingFeatureIndices.get(i));
//                interactions.add(newInteractions.get(i));
//                featureToRowIndex[featuresFound] = i;
//                featuresFound++;
//            }
//        }
//
//        // Now we convert the dataRows into the new feature space
//        List<List<Object>> newDataRows = new ArrayList<>();
//        for (List<String> row : dataRows) {
//            if (row.size() != headers.size()) {
//                System.err.println("Warning: Skipping row with inconsistent number of columns: " + row);
//                continue; // Skip rows with inconsistent number of columns
//            }
//            List<Object> newRow = new ArrayList<>();
//            for (int j = 0; j < newFeatureNames.size(); j++) {
//                if (newFeatureTypes.get(j) == featureType.TARGET) {
//                    newRow.add(row.get(targetData.get(newFeatureNames.get(j))));
//                } else if (newFeatureTypes.get(j) == featureType.INTERACTION) {
//                    // we need to find the two features that are being interacted
//                    Pair<Integer, Integer> interaction = newInteractions.get(j);
//                    double firstValue = Double.parseDouble(row.get(featureToRowIndex[interaction.a]));
//                    double secondValue = Double.parseDouble(row.get(featureToRowIndex[interaction.b]));
//                    newRow.add(firstValue * secondValue);
//                } else {
//                    String value = row.get(underlyingIndexToRowIndex.get(underlyingFeatureIndices.get(j)));
//                    if (newFeatureTypes.get(j) == featureType.RANGE) {
//                        double numericValue = Double.parseDouble(value);
//                        Pair<Number, Number> range = newFeatureRanges.get(j);
//                        if (numericValue >= range.a.doubleValue() && numericValue < range.b.doubleValue()) {
//                            newRow.add(1);
//                        } else {
//                            newRow.add(0);
//                        }
//                    } else if (newFeatureTypes.get(j) == featureType.ENUM) {
//                        Enum<?> enumValue = (Enum<?>) newEnumValues.get(j);
//                        if (enumValue.name().equals(value)) {
//                            newRow.add(1);
//                        } else {
//                            newRow.add(0);
//                        }
//                    } else if (newFeatureTypes.get(j) == featureType.STRING) {
//                        String stringValue = (String) newEnumValues.get(j);
//                        if (stringValue.equals(value)) {
//                            newRow.add(1);
//                        } else {
//                            newRow.add(0);
//                        }
//                    } else {
//                        // RAW feature
//                        if (newFeatureClasses.get(j) == Integer.class || newFeatureClasses.get(j) == int.class) {
//                            newRow.add(Integer.parseInt(value));
//                        } else if (newFeatureClasses.get(j) == Double.class || newFeatureClasses.get(j) == double.class) {
//                            newRow.add(Double.parseDouble(value));
//                        } else if (newFeatureClasses.get(j) == Boolean.class || newFeatureClasses.get(j) == boolean.class) {
//                            newRow.add(Boolean.parseBoolean(value));
//                        } else {
//                            // Handle other types as needed
//                            System.err.println("Warning: Unsupported type for column: " + newFeatureNames.get(j));
//                        }
//                    }
//                }
//            }
//            newDataRows.add(newRow);
//        }
//
//        // Now we write the new data to the output file
//        Utils.writeDataWithHeader("\t", newFeatureNames, newDataRows, outputFile);
//
//        // remove the suffix from output file, and replace with json
//        String jsonOutputFile = outputFile.substring(0, outputFile.lastIndexOf('.')) + ".json";
//        JSONObject outputJson = toJSON();
//        JSONUtils.writeJSON(outputJson, jsonOutputFile);
//        return newDataRows;
//}

package evaluation.features;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AutomatedFeatures implements IStateFeatureVector, IActionFeatureVector, IToJSON {

    public static boolean debug = false;


    public enum featureType {
        RAW, ENUM, STRING, RANGE, INTERACTION, TARGET
    }

    int defaultBuckets = 1;
    public final IStateFeatureVector underlyingState;
    public final IActionFeatureVector underlyingAction;
    String[] underlyingNames;
    Class<?>[] underlyingTypes;

    List<String> removedFeatureNames = new ArrayList<>();

    List<String> featureNames = new ArrayList<>();
    List<featureType> featureTypes = new ArrayList<>();
    List<Object> enumValues = new ArrayList<>();
    List<Pair<Number, Number>> featureRanges = new ArrayList<>();
    List<Integer> featureIndices = new ArrayList<>();
    List<List<Integer>> interactions = new ArrayList<>();
    int[] buckets;

    public AutomatedFeatures(IStateFeatureVector underlyingStateVector, IActionFeatureVector underlyingActionVector) {
        this.underlyingState = underlyingStateVector;
        this.underlyingAction = underlyingActionVector;
        validateUnderlyingVector();
    }

    public AutomatedFeatures(IStateFeatureVector underlyingStateVector) {
        this.underlyingState = underlyingStateVector;
        this.underlyingAction = null;
        validateUnderlyingVector();
    }

    public AutomatedFeatures(IActionFeatureVector underlyingActionVector) {
        this.underlyingState = null;
        this.underlyingAction = underlyingActionVector;
        validateUnderlyingVector();
    }

    private void validateUnderlyingVector() {
        if (underlyingState != null) {
            underlyingNames = underlyingState.names();
            underlyingTypes = underlyingState.types();
        } else {
            underlyingNames = new String[0];
            underlyingTypes = new Class<?>[0];
        }
        if (underlyingAction != null) {
            String[] tempNames = new String[underlyingNames.length + underlyingAction.names().length];
            System.arraycopy(underlyingNames, 0, tempNames, 0, underlyingNames.length);
            System.arraycopy(underlyingAction.names(), 0, tempNames, underlyingNames.length, underlyingAction.names().length);
            underlyingNames = tempNames;
            Class[] tempTypes = new Class<?>[underlyingTypes.length + underlyingAction.types().length];
            System.arraycopy(underlyingTypes, 0, tempTypes, 0, underlyingTypes.length);
            System.arraycopy(underlyingAction.types(), 0, tempTypes, underlyingTypes.length, underlyingAction.types().length);
            underlyingTypes = tempTypes;
        }
        buckets = new int[underlyingNames.length];
        Arrays.fill(buckets, defaultBuckets);

        // we then always add the underlying features as RAW features; these will always occupy the first |s| + |a| features
        for (int i = 0; i < underlyingNames.length; i++) {
            Class<?> type = underlyingTypes[i];
            if (type == Double.class || type == double.class || type == Integer.class || type == int.class)
                addFeature(new AutomatedFeatures.ColumnDetails(underlyingNames[i], AutomatedFeatures.featureType.RAW,
                        null, null, i, type, null));
        }
    }

    public AutomatedFeatures(JSONObject json) {
        if (json.get("class") == null || json.get("class").toString().isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON file: missing 'class' field");
        }
        if (!json.get("class").toString().equals(this.getClass().getName())) {
            throw new IllegalArgumentException("Invalid JSON file: class mismatch");
        }
        // now populate each of the class fields from JSON
        defaultBuckets = Integer.parseInt(json.get("defaultBuckets").toString());
        underlyingState = json.containsKey("underlyingState") ?
                JSONUtils.loadClassFromJSON((JSONObject) json.get("underlyingState"))
                : null;
        underlyingAction = json.containsKey("underlyingAction") ?
                JSONUtils.loadClassFromJSON((JSONObject) json.get("underlyingAction"))
                : null;
        validateUnderlyingVector(); // this sets up all RAW features; only others need to come from JSON

        JSONArray features = (JSONArray) json.getOrDefault("features", new JSONArray());
        for (Object feature : features) {
            JSONObject featureObject = (JSONObject) feature;
            String name = featureObject.get("name").toString();
            featureNames.add(name);
            String type = featureObject.get("type").toString();
            int index = Integer.parseInt(featureObject.get("index").toString());
            featureIndices.add(index);
            switch (type) {
                case "ENUM" -> {
                    featureTypes.add(featureType.ENUM);
                    String enumString = (String) featureObject.get("enumValue");
                    // Now we convert this to the correct enum class
                    Class<? extends Enum> enumClass = (Class<? extends Enum>) underlyingTypes[index];
                    enumValues.add(Enum.valueOf(enumClass, enumString));
                    featureRanges.add(null);
                    interactions.add(null);
                }
                case "STRING" -> {
                    featureTypes.add(featureType.STRING);
                    enumValues.add(featureObject.get("enumValue"));
                    featureRanges.add(null);
                    interactions.add(null);
                }
                case "RANGE" -> {
                    featureTypes.add(featureType.RANGE);
                    String rangeString = featureObject.get("range").toString();
                    String[] rangeParts = rangeString.replaceAll("[\\[\\]]", "").split(",");
                    Number lowerBound = Double.parseDouble(rangeParts[0].trim());
                    Number upperBound = Double.parseDouble(rangeParts[1].trim());
                    featureRanges.add(new Pair<>(lowerBound, upperBound));
                    enumValues.add(null);
                    interactions.add(null);
                }
                default -> throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }
    }

    public List<Integer> underlyingOneHotIndices(int i) {
        // we return the underlying indices for which one of the components of this feature
        // is a one-hot variable
        if (getFeatureType(i) == featureType.ENUM || getFeatureType(i) == featureType.RANGE) {
            return List.of(getUnderlyingIndex(i));
        }
        if (getFeatureType(i) == featureType.INTERACTION) {
            return interactions.get(i).stream().map(this::underlyingOneHotIndices).flatMap(List::stream).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void addInteraction(int first, int second) {
        if (first < 0 || first >= featureNames.size() || second < 0 || second >= featureNames.size()) {
            throw new IllegalArgumentException("Invalid feature indices for interaction: " + first + ", " + second);
        }
        List<Integer> interactionIndices = new ArrayList<>();
        int numberOfInteractions = 0;
        if (interactions.get(first) != null) {
            interactionIndices.addAll(interactions.get(first));
            numberOfInteractions += interactions.get(first).size();
        } else {
            interactionIndices.add(first);
            numberOfInteractions++;
        }
        if (interactions.get(second) != null) {
            interactionIndices.addAll(interactions.get(second));
            numberOfInteractions += interactions.get(second).size();
        } else {
            interactionIndices.add(second);
            numberOfInteractions++;
        }
        if (interactionIndices.size() != numberOfInteractions) {
            throw new AssertionError("Invalid interaction indices for interaction: " + first + ", " + second);
        }
        addFeature(
                new AutomatedFeatures.ColumnDetails(
                        featureNames.get(first) + ":" + featureNames.get(second),
                        featureType.INTERACTION, null, null, -1, Double.class,
                        interactionIndices
                )
        );
    }

    public void removeFeature(int i) {
        if (i < 0 || i >= featureNames.size()) {
            throw new IllegalArgumentException("Invalid feature index: " + i);
        }
        // we do not remove from buckets, underlyingNames or underlyingTypes as those are all indexed from the (unchanged) underlying vector

        // we also record this name, so that if we do not insert it back in from the file in processNewFeature
        removedFeatureNames.add(featureNames.get(i));

        featureNames.remove(i);
        featureTypes.remove(i);
        enumValues.remove(i);
        featureRanges.remove(i);
        featureIndices.remove(i);
        interactions.remove(i);

        // we now need to rework interactions to account for the removed feature
        for (int j = 0; j < interactions.size(); j++) {
            List<Integer> interaction = interactions.get(j);
            if (interaction == null) continue; // this is not an interaction
            if (interaction.contains(i)) {
                throw new IllegalArgumentException("Cannot remove feature involved in interaction: " + i);
            }
            // Update all indices in the interaction list that are greater than i
            List<Integer> updatedInteraction = new ArrayList<>();
            for (int k = 0; k < interaction.size(); k++) {
                int idx = interaction.get(k);
                if (idx > i)
                    updatedInteraction.add(idx - 1);
                else
                    updatedInteraction.add(idx);
            }
            interactions.set(j, updatedInteraction);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("class", this.getClass().getName());
        jsonObject.put("defaultBuckets", defaultBuckets);
        if (this.underlyingState != null) {
            JSONObject underlyingState = new JSONObject();
            underlyingState.put("class", this.underlyingState.getClass().getName());
            jsonObject.put("underlyingState", underlyingState);
        }
        if (this.underlyingAction != null) {
            JSONObject underlyingAction = new JSONObject();
            underlyingAction.put("class", this.underlyingAction.getClass().getName());
            jsonObject.put("underlyingAction", underlyingAction);
        }
        // instead of writing each of the remaining fields as an array,
        // we want an array of JSONObjects, one per feature that has subfields for name, type, enumValue, range, and index
        JSONArray featureObjects = new JSONArray();
        for (int i = 0; i < featureNames.size(); i++) {
            if (featureTypes.get(i) == featureType.RAW)
                continue;
            JSONObject featureObject = new JSONObject();
            featureObject.put("name", featureNames.get(i));
            featureObject.put("type", featureTypes.get(i).toString());
            if (featureTypes.get(i) == featureType.ENUM || featureTypes.get(i) == featureType.STRING) {
                featureObject.put("enumValue", enumValues.get(i).toString());
            } else if (featureTypes.get(i) == featureType.RANGE) {
                Pair<Number, Number> range = featureRanges.get(i);
                featureObject.put("range", "[" + range.a + ", " + range.b + "]");
            } else if (featureTypes.get(i) == featureType.INTERACTION) {
                continue; // we do not consider Interactions as features...a coefficient can just be a : delimited list of the underlying components
                // featureObject.put("interaction", "[" + interactions.get(i).a + ", " + interactions.get(i).b + "]");
            }
            featureObject.put("index", featureIndices.get(i));
            featureObjects.add(featureObject);
        }
        jsonObject.put("features", featureObjects);
        return jsonObject;
    }

    public AutomatedFeatures copy() {
        AutomatedFeatures copy = new AutomatedFeatures(underlyingState, underlyingAction);
        copy.defaultBuckets = this.defaultBuckets;
        copy.featureNames = new ArrayList<>(this.featureNames);
        copy.featureTypes = new ArrayList<>(this.featureTypes);
        copy.enumValues = new ArrayList<>(this.enumValues);
        copy.interactions = new ArrayList<>(this.interactions);
        copy.featureRanges = new ArrayList<>(this.featureRanges);
        copy.featureIndices = new ArrayList<>(this.featureIndices);
        copy.buckets = buckets.clone();
        copy.removedFeatureNames = new ArrayList<>(this.removedFeatureNames);
        return copy;
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        // we first extract the underlying vector to get the raw data
        Object[] underlyingVectorData;
        if (underlyingState != null) {
            underlyingVectorData = underlyingState.featureVector(state, playerID);
        } else {
            underlyingVectorData = new Object[0];
        }
        if (underlyingAction != null) {
            Object[] actionVectorData = underlyingAction.featureVector(action, state, playerID);
            Object[] temp = new Object[underlyingVectorData.length + actionVectorData.length];
            System.arraycopy(underlyingVectorData, 0, temp, 0, underlyingVectorData.length);
            System.arraycopy(actionVectorData, 0, temp, underlyingVectorData.length, actionVectorData.length);
            underlyingVectorData = temp;
        }
        return buildCompositeFeatures(underlyingVectorData);
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        // in this case we just have to worry about the state vector
        if (underlyingState == null)
            return new double[0];
        Object[] underlyingVectorData = underlyingState.featureVector(state, playerID);
        return buildCompositeFeatures(underlyingVectorData);
    }

    private double[] buildCompositeFeatures(Object[] underlyingVectorData) {
        // then we iterate over the automated features and generate these from the raw data
        double[] featureVector = new double[featureNames.size()];
        for (int i = 0; i < featureNames.size(); i++) {
            int underlyingIndex = featureIndices.get(i);
            if (underlyingIndex == -1) continue; // Interactions covered on second pass
            Object value = underlyingVectorData[underlyingIndex];
            switch (featureTypes.get(i)) {
                case RAW:
                    featureVector[i] = ((Number) value).doubleValue();
                    break;
                case ENUM:
                    Enum<?> enumValue = (Enum<?>) value;
                    if (enumValue == null)
                        featureVector[i] = 0;
                    else
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
                case INTERACTION:
                    break; // Handled in the second pass
                default:
                    throw new IllegalArgumentException("Unsupported type: " + featureTypes.get(i));
            }
        }
        // second pass for interactions now that we have the raw data
        for (int i = 0; i < featureNames.size(); i++) {
            if (Objects.requireNonNull(featureTypes.get(i)) == featureType.INTERACTION) {
                List<Integer> interaction = interactions.get(i);
                featureVector[i] = 1.0;
                for (int index : interaction) {
                    featureVector[i] *= featureVector[index];
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
    public Object[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String[] names() {
        return featureNames.toArray(new String[0]);
    }

    @Override
    public Class<?>[] types() {
        return featureTypes.toArray(new Class[0]);
    }

    public void setBuckets(int index, int buckets) {
        this.buckets[index] = buckets;
    }

    public int getBuckets(int index) {
        return this.buckets[index];
    }

    public featureType getFeatureType(int index) {
        return featureTypes.get(index);
    }

    public int getUnderlyingIndex(int index) {
        return featureIndices.get(index);
    }


    public String getUnderlyingName(int underlyingIndex) {
        return underlyingNames[underlyingIndex];
    }

    public record ColumnDetails(String name, featureType type, Object enumValue, Pair<Number, Number> range,
                                int underlyingIndex, Class<?> clazz, List<Integer> interaction) {

        public ColumnDetails updateType(featureType newType) {
            return new ColumnDetails(name, newType, enumValue, range, underlyingIndex, clazz, interaction);
        }
    }

    public List<ColumnDetails> getColumnDetails() {
        List<ColumnDetails> columnDetails = new ArrayList<>();
        for (int i = 0; i < featureNames.size(); i++) {
            columnDetails.add(new ColumnDetails(
                    featureNames.get(i),
                    featureTypes.get(i),
                    enumValues.get(i),
                    featureRanges.get(i),
                    featureIndices.get(i),
                    featureIndices.get(i) == -1 ? Double.class : underlyingTypes[featureIndices.get(i)],
                    interactions.get(i)
            ));
        }
        return columnDetails;
    }

    public List<List<Object>> processData(String outputFile, int maxRecords, String... inputFiles) {
        // inputFiles contain the raw data.
        // There can be two types of columns:
        // 1. Columns that refer to existing featureNames. These are detected by matching the names.
        // 2. Columns that are not features. These are copied over into the output file without becoming features.

        // The point of processData is to also add in new columns for features that do not yet have columns
        // 1. Bucketing of numeric features (range features).
        //          For the moment we always recalculate bucket features from scratch.
        //          As the data distribution may have changed.
        // 2. One-hot encoding of enum features
        //          We check the enum, and check to see if all of the expected columns are present.
        // 3. One-hot encoding of string features
        //          As for enum features
        // 4. One-hot encoding of boolean features
        //          As for enum features
        // 5. Interaction columns between any two of the above features
        //          if this already exists in the data, we do not add it again

        List<ColumnDetails> newColumnDetails = new ArrayList<>(); // will be populated with new columns
        Map<Integer, Integer> underlyingIndexToDataIndex = new HashMap<>();
        List<ColumnDetails> startingFeatures = getColumnDetails();

        // load files...the columns should correspond to the underlying vector
        // while allowing for additional columns (for target values)
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", inputFiles);
        List<String> headers = data.a;
        List<List<String>> dataRows = data.b;

        // We now want to convert the dataRows into dataColumns
        List<List<String>> dataColumns = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            dataColumns.add(new ArrayList<>());
        }
        int count = 0;
        for (List<String> row : dataRows) {
            if (row.size() != headers.size()) {
                System.err.println("Warning: Skipping row with inconsistent number of columns: " + row);
                continue; // Skip rows with inconsistent number of columns
            }
            for (int i = 0; i < headers.size(); i++) {
                dataColumns.get(i).add(row.get(i));
            }
            count++;
            if (maxRecords > 0 && count >= maxRecords) {
                break; // Stop processing if we reached the maximum number of records
            }
        }
        List<List<?>> newDataColumns = new ArrayList<>(); // set up to take the new data (especially where we can just copy this from the old)

        // Loop over all underlyingNames/Types to determine if the current features match with the data
        // if they do match then we can pull over the relevant details from the current set up
        // if they do not match, then we need to calculate from scratch

        for (int i = 0; i < underlyingNames.length; i++) {
            String columnName = underlyingNames[i];
            Class<?> columnType = underlyingTypes[i];

            if (!headers.contains(columnName)) {
                //            System.out.println("Underlying data " + inputFiles[0] + " is missing column: " + columnName);
                continue;
            }

            int columnIndex = headers.indexOf(columnName);
            underlyingIndexToDataIndex.put(i, columnIndex);

            if (columnType.equals(Boolean.class)) {
                // Boolean column: Just add raw column directly (no bucketing is relevant here)
                newColumnDetails.add(new ColumnDetails(
                        columnName, featureType.RAW, null, null, i, columnType, null
                ));
                newDataColumns.add(validateBooleanColumnData(dataColumns.get(columnIndex)));
                if (debug) System.out.println("Adding Boolean column " + columnName);
            } else if (columnType.equals(Double.class) || columnType.equals(double.class) ||
                    columnType.equals(Integer.class) || columnType.equals(int.class)) {
                // Numeric column: Check for RAW column and a RANGE column for each BUCKET
                // The RAW data has to be present, so we can always copy that over
                // Add RAW feature for column
                newColumnDetails.add(new ColumnDetails(
                        columnName, featureType.RAW, null, null, i, columnType, null
                ));
                if (columnType.equals(Double.class) || columnType.equals(double.class)) {
                    newDataColumns.add(validateDoubleColumnData(dataColumns.get(columnIndex)));
                } else {
                    newDataColumns.add(validateIntColumnData(dataColumns.get(columnIndex)));
                }
                if (debug) System.out.println("Adding Numeric column " + columnName);

                // then we check for buckets in the data
                List<String> expectedBucketColumns = IntStream.range(0, getBuckets(i))
                        .mapToObj(b -> columnName + "_B" + b)
                        .toList();
                boolean copiedColumns = false;
                // If there is only one bucket, then this is the same as not bucketing
                if (expectedBucketColumns.size() > 1 && new HashSet<>(headers).containsAll(expectedBucketColumns)) {
                    // then we can pull over the RANGE columns from the starting features without any recalculation
                    int finalI = i;
                    List<ColumnDetails> original = startingFeatures.stream()
                            .filter(r -> r.type == featureType.RANGE && r.underlyingIndex == finalI)
                            .toList();
                    if (original.size() == 1 + getBuckets(i)) {
                        for (ColumnDetails columnDetail : original) {
                            newColumnDetails.add(columnDetail);
                            // we need to find the range field in the data
                            int bucketIndex = Integer.parseInt(columnDetail.name.substring(columnDetail.name.indexOf("_B") + 2));
                            newDataColumns.add(dataColumns.get(headers.indexOf(expectedBucketColumns.get(bucketIndex))));
                            if (debug) System.out.println("Adding Range column " + columnName);
                        }
                        copiedColumns = true;
                    }
                }
                if (buckets[i] > 1 && !copiedColumns) {
                    // We did not find the data in the file, so we calculate it (mostly the buckets)
                    List<Pair<ColumnDetails, List<?>>> missingColumns = handleMissingRangeFeatures(i, dataColumns.get(columnIndex));
                    for (Pair<ColumnDetails, List<?>> missingColumn : missingColumns) {
                        newColumnDetails.add(missingColumn.a);
                        newDataColumns.add(missingColumn.b);
                        if (debug) System.out.println("Adding Range column " + columnName);
                    }
                }
            } else if (columnType.isEnum()) {
                // Enum column: Check for one column per enum value, as well as one with the RAW value
                Class<? extends Enum> enumClass = (Class<? extends Enum>) underlyingTypes[i];
                Object[] enumValues = enumClass.getEnumConstants();

                List<String> expectedEnums = Arrays.stream(enumValues)
                        .map(e -> ((Enum<?>) e).name())
                        .toList();
                int finalI1 = i;
                List<String> actualEnumColumns = startingFeatures.stream().filter(r -> r.underlyingIndex == finalI1)
                        .map(r -> r.name)
                        .toList();
                if (expectedEnums.size() + 1 == actualEnumColumns.size()) {
                    // we just copy over
                    newColumnDetails.add(new ColumnDetails(
                            columnName, featureType.TARGET, null, null, i, columnType, null
                    ));
                    newDataColumns.add(dataColumns.get(columnIndex));
                    for (String expectedEnum : expectedEnums) {
                        String expectedEnumColumn = columnName + "_" + expectedEnum;
                        int enumIndex = headers.indexOf(expectedEnumColumn);
                        if (enumIndex == -1) {
                            throw new IllegalArgumentException("Missing column: " + expectedEnumColumn);
                        }
                        newColumnDetails.add(new ColumnDetails(
                                expectedEnumColumn, featureType.ENUM, Enum.valueOf(enumClass, expectedEnum), null, i, Boolean.class, null
                        ));
                        newDataColumns.add(dataColumns.get(enumIndex));
                        if (debug) System.out.println("Adding Enum column " + columnName);

                    }
                } else {
                    // recalculate
                    List<Pair<ColumnDetails, List<?>>> missingColumns = handleMissingEnumFeature(i, dataColumns.get(columnIndex));
                    for (Pair<ColumnDetails, List<?>> missingColumn : missingColumns) {
                        newColumnDetails.add(missingColumn.a);
                        newDataColumns.add(missingColumn.b);
                        if (debug) System.out.println("Adding Enum column " + columnName);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unsupported column type: " + columnType);
            }
        }

        // We now run through all interactions and check if they are already present in the data (if so, we add them in without further processing)
        // if not, then we need to calculate them
        for (int i = 0; i < featureNames.size(); i++) {
            if (featureTypes.get(i) == featureType.INTERACTION) {
                List<Integer> interaction = interactions.get(i);
                List<String> componentNames = interaction.stream()
                        .map(featureNames::get)
                        .toList();
                String interactionName = String.join(":", componentNames);

                // We now need to find the indices for the interaction in the new data.
                List<Integer> newIndices = new ArrayList<>(interaction);
                for (int loop = 0; loop < newColumnDetails.size(); loop++) {
                    ColumnDetails columnDetails = newColumnDetails.get(loop);
                    // we now check if the columnDetails.name matches any of the interaction component names
                    // if it does, then loop is the corresponding entry in newIndices
                    for (int f = 0; f < interaction.size(); f++) {
                        if (columnDetails.name.equals(componentNames.get(f))) {
                            newIndices.set(f, loop);
                        }
                    }
                }
                newColumnDetails.add(new ColumnDetails(
                        interactionName, featureType.INTERACTION, null, null, -1, Double.class, newIndices
                ));
                if (headers.contains(interactionName)) {
                    // just copy over
                    int interactionIndex = headers.indexOf(interactionName);
                    newDataColumns.add(dataColumns.get(interactionIndex));
                } else {
                    // need to calculate this
                    List<Double> interactionData = new ArrayList<>();
                    for (int j = 0; j < newDataColumns.get(0).size(); j++) {
                        double interactionValue = 1.0;
                        for (int k = 0; k < newIndices.size(); k++) {
                            int index = newIndices.get(k);
                            Object valueObj = newDataColumns.get(index).get(j);
                            double value = valueObj instanceof Number ? ((Number) valueObj).doubleValue() :
                                    valueObj instanceof String ? Double.parseDouble((String) valueObj) : 0;
                            interactionValue *= value;
                        }
                        interactionData.add(interactionValue);
                    }
                    newDataColumns.add(interactionData);
                }
            }
        }

        // Then all other data in the input file
        // we run through all the columns (in header), and any that we have not already included in newColumns, we add in unchanged
        for (int i = 0; i < headers.size(); i++) {
            String columnName = headers.get(i);
            // Skip the raw non-numeric columns
            if (Arrays.asList(underlyingNames).contains(columnName))
                continue;

            if (newColumnDetails.stream().noneMatch(r -> r.name.equals(columnName))) {
                newColumnDetails.add(new ColumnDetails(
                        columnName, featureType.TARGET, null, null, -1, String.class, null
                ));
                newDataColumns.add(dataColumns.get(i));
            }
        }


        // The logic so far has constructed a RAW column for each underlying feature
        // Then a RANGE column per bucket for that underlying feature
        // And similarly a number of ENUM column per underlying feature that is an enum
        // Then INTERACTION columns are created from the existing featureNames

        // The issue now is that we are about to convert all the columns into new features
        // This will be a problem if we have removed features (RAW, RANGE or ENUM) from featureName, as the processing
        // of the underlying features has added them back in.
        for (int i = newColumnDetails.size() - 1; i >= 0; i--) {
            ColumnDetails column = newColumnDetails.get(i);
            if (removedFeatureNames.contains(column.name)) {
                if (column.type == featureType.RAW) {
                    // we still need to write this column, so we switch it to TARGET
                    // (iff it is used to derive another constructed feature)
                    boolean rawDataIsUsed = false;
                    for (int f = 0;  f < featureNames.size(); f++) {
                        if (featureTypes.get(f) != featureType.RAW) {
                            if (featureIndices.get(f) == column.underlyingIndex) {
                                // RANGE and ENUM features
                                rawDataIsUsed = true;
                                break;
                            }
                            if (featureTypes.get(f) == featureType.INTERACTION) {
                                if (interactions.get(f).contains(column.underlyingIndex)) {
                                    rawDataIsUsed = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (rawDataIsUsed) {
                        // convert it to a TARGET column to keep it in the file
                        ColumnDetails oldDetails = newColumnDetails.get(i);
                        ColumnDetails newDetails = oldDetails.updateType(featureType.TARGET);
                        newColumnDetails.remove(i);
                        newColumnDetails.add(i, newDetails);
                    } else {
                        // we remove it completely
                        newColumnDetails.remove(i);
                        newDataColumns.remove(i);
                    }
                } else {
                    // we remove it completely
                    newColumnDetails.remove(i);
                    newDataColumns.remove(i);
                }
            }

        }


        // We now have all the column, to write to file we need to convert this into a set of rows
        List<List<Object>> newDataRows = new ArrayList<>();
        for (int i = 0; i < newDataColumns.size(); i++) {
            List<?> columnData = newDataColumns.get(i);
            for (int j = 0; j < columnData.size(); j++) {
                if (newDataRows.size() <= j) {
                    newDataRows.add(new ArrayList<>());
                }
                newDataRows.get(j).add(columnData.get(j));
            }
        }

        // we also need to set the featureNames and so on from the new column details
        featureNames.clear();
        featureTypes.clear();
        enumValues.clear();
        featureRanges.clear();
        featureIndices.clear();
        interactions.clear();
        for (ColumnDetails column : newColumnDetails) {
            if (column.type == featureType.TARGET)
                continue;
            addFeature(column);
        }
        // update interactions here given changes to indices, as removing the TARGET columns will have changed the indices
        for (int i = 0; i < interactions.size(); i++) {
            List<Integer> interaction = interactions.get(i);
            if (interaction != null) {
                String interactionName = featureNames.get(i);
                // split this by :, then look up each component in featureNames
                String[] components = interactionName.split(":");
                List<Integer> indices = Arrays.stream(components).map(s -> featureNames.indexOf(s)).toList();
                interactions.set(i, indices);
            }
        }

        Utils.writeDataWithHeader("\t", newColumnDetails.stream().map(r -> r.name).toList(),
                newDataRows, outputFile);
        return newDataRows;
    }

    public int addFeature(ColumnDetails column) {
        featureNames.add(column.name);
        featureTypes.add(column.type);
        enumValues.add(column.enumValue);
        featureRanges.add(column.range);
        featureIndices.add(column.underlyingIndex);
        interactions.add(column.interaction);
        return featureNames.size() - 1; // return the index of the newly added feature
    }


    // Stub methods for handling missing features
    private List<Pair<ColumnDetails, List<?>>> handleMissingRangeFeatures(int i, List<String> columnData) {
        List<Pair<ColumnDetails, List<?>>> newColumns = new ArrayList<>();
        String feature = underlyingNames[i];
        Class<?> columnType = underlyingTypes[i];
        Class<?> numericClass = columnType.equals(Integer.class) || columnType.equals(int.class) ?
                Integer.class : Double.class;

        List<Pair<Number, Number>> proposedFeatureRanges = calculateFeatureRanges(columnData, buckets[i], numericClass);
        for (int b = 0; b < proposedFeatureRanges.size(); b++) {
            Pair<Number, Number> range = proposedFeatureRanges.get(b);
            String rangeName = feature + "_B" + b;
            newColumns.add(Pair.of(
                    new ColumnDetails(rangeName, featureType.RANGE, null, range, i, numericClass, null),
                    columnData.stream()
                            .map(Double::parseDouble)
                            .map(value -> {
                                if (value >= range.a.doubleValue() && value < range.b.doubleValue()) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }).toList()
            ));
        }

        return newColumns;
    }

    private List<Pair<ColumnDetails, List<?>>> handleMissingEnumFeature(int i, List<String> columnData) {
        List<Pair<ColumnDetails, List<?>>> newColumns = new ArrayList<>();
        String feature = underlyingNames[i];
        Class<?> columnType = underlyingTypes[i];
        // add column for unchanged value as TARGET
        newColumns.add(Pair.of(
                new ColumnDetails(feature, featureType.TARGET, null, null, i, columnType, null),
                columnData)
        );
        if (columnType.isEnum()) {
            // Add ENUM features for each enum value
            Object[] enumValues = columnType.getEnumConstants();
            for (Object enumValue : enumValues) {
                String enumName = feature + "_" + ((Enum<?>) enumValue).name();
                newColumns.add(Pair.of(
                        new ColumnDetails(enumName, featureType.ENUM, enumValue, null, i, Boolean.class, null),
                        columnData.stream()
                                .map(value -> ((Enum<?>) enumValue).name().equals(value) ? 1 : 0)
                                .toList())
                );
            }
        } else {
            // Handle string features
            Set<String> uniqueValues = new HashSet<>(columnData);
            if (uniqueValues.size() <= 10)
                for (String value : uniqueValues) {
                    String enumName = feature + "_" + value;
                    newColumns.add(Pair.of(
                            new ColumnDetails(enumName, featureType.STRING, value, null, i, Boolean.class, null),
                            columnData.stream()
                                    .map(v -> v.equals(value) ? 1 : 0)
                                    .toList())
                    );
                }
        }
        return newColumns;
    }


    private <T extends Number> List<Pair<Number, Number>> calculateFeatureRanges(List<T> numericValues,
                                                                                 int buckets, List<Number> exclusions) {
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

    private List<Boolean> validateBooleanColumnData(List<String> columnData) {
        List<Boolean> booleanValues = new ArrayList<>();
        for (String value : columnData) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                booleanValues.add(Boolean.parseBoolean(value));
            } else {
                Double number = Double.parseDouble(value);
                if (number == 0.0) {
                    booleanValues.add(false);
                } else if (number == 1.0) {
                    booleanValues.add(true);
                } else {
                    System.err.println("Warning: Skipping non-boolean value: " + value);
                    return Collections.emptyList();
                }
            }
        }
        return booleanValues;
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

    @Override
    public String toString() {
        // Just print feature names
        return String.join(", ", featureNames);
    }

}

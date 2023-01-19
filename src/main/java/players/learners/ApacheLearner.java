package players.learners;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public abstract class ApacheLearner extends AbstractLearner {

    Dataset<Row> apacheData;
    boolean debug = false;
    static SparkSession spark = SparkSession
            .builder()
            .appName("Java Spark SQL basic example")
            //     .config("spark.driver.memory", "1g")
            .master("local").getOrCreate();

    @Override
    public void learnFrom(String... files) {
        loadData(files);
        // first add the target to the data array so that we can convert to an apache dataset (we just add on the target)
        double[][] apacheDataArray = new double[dataArray.length][dataArray[0].length];
        for (int i = 0; i < dataArray.length; i++) {
            // we skip the BIAS at the front here, as we add that in separately
            System.arraycopy(dataArray[i], 1, apacheDataArray[i], 0, descriptions.length);
            apacheDataArray[i][descriptions.length] = target[i][0]; // add target to end
        }
        // convert the raw data into Rows
        List<Row> rowList = Arrays.stream(apacheDataArray)
                .map(doubleArray -> Arrays.stream(doubleArray).boxed().toArray())
                .map(RowFactory::create)
                .collect(toList());
        // use the header to get the names, and all of them are double by design
        String[] apacheHeader = new String[descriptions.length + 1];
        System.arraycopy(descriptions, 0, apacheHeader, 0, descriptions.length);
        apacheHeader[descriptions.length] = "target";
        // set up the column names
        StructType schema = new StructType(Arrays.stream(apacheHeader)
                .map(name -> new StructField(name, DataTypes.DoubleType, true, Metadata.empty()))
                .toArray(StructField[]::new)
        );

        // and convert to an apache Dataset
        apacheData = spark.createDataFrame(rowList, schema);

        if (debug)
            apacheData.show(10);

        learnFromApacheData();
    }

    abstract void learnFromApacheData();

}

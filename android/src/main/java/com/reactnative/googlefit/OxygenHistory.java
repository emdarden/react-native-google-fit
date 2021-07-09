package com.reactnative.googlefit;

import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.data.HealthDataTypes;
import com.google.android.gms.fitness.data.HealthFields;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OxygenHistory {
  private ReactContext mReactContext;
  private GoogleFitManager googleFitManager;
  private DataSet Dataset;
  private DataType dataType;

  private static final String TAG = "Oxygen History";

  public OxygenHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
    this.mReactContext = reactContext;
    this.googleFitManager = googleFitManager;
    this.dataType = HealthDataTypes.TYPE_OXYGEN_SATURATION;
  }

  public boolean save(ReadableMap sample) {

    this.Dataset = createDataForRequest(
            DataSource.TYPE_RAW,
            sample.getDouble("value"),
            (long)sample.getDouble("date")
    );

    new InsertAndVerifyDataTask(this.Dataset).execute();

    return true;
  }


  private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {

    private DataSet Dataset;

    InsertAndVerifyDataTask(DataSet dataset) {
        this.Dataset = dataset;
    }

    protected Void doInBackground(Void... params) {
        // Create a new dataset and insertion request.
        DataSet dataSet = this.Dataset;

        // [START insert_dataset]
        // Then, invoke the History API to insert the data and await the result, which is
        // possible here because of the {@link AsyncTask}. Always include a timeout when calling
        // await() to prevent hanging that can occur from the service being shutdown because
        // of low memory or other conditions.
        //Log.i(TAG, "Inserting the dataset in the History API.");
        com.google.android.gms.common.api.Status insertStatus =
                Fitness.HistoryApi.insertData(googleFitManager.getGoogleApiClient(), dataSet)
                        .await(1, TimeUnit.MINUTES);

        // Before querying the data, check to see if the insertion succeeded.
        if (!insertStatus.isSuccess()) {
            //Log.i(TAG, "There was a problem inserting the dataset.");
            return null;
        }

        //Log.i(TAG, "Data insert was successful!");

        return null;
    }
  }

  private DataSet createDataForRequest(int dataSourceType, Double value,
                                         long time) {
    DataSource dataSource = new DataSource.Builder()
            .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
            .setDataType(this.dataType)
            .setType(dataSourceType)
            .build();

    DataSet dataSet = DataSet.create(dataSource);
    // DataPoint dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, timeUnit);

    float f1 = Float.valueOf(value.toString());

    DataPoint dataPoint =
      DataPoint.builder(dataSource)
          .setTimestamp(time, TimeUnit.MILLISECONDS)
          .setField(HealthFields.FIELD_OXYGEN_SATURATION, f1)
          .setField(HealthFields.FIELD_SUPPLEMENTAL_OXYGEN_FLOW_RATE, 0.0f)
          .build();

    // float f1 = Float.valueOf(value.toString());
    // dataPoint = dataPoint.setFloatValues(f1);

    dataSet.add(dataPoint);

    return dataSet;
  }
}

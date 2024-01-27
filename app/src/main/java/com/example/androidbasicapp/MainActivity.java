package com.example.androidbasicapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

class WeatherResponse {
    Daily daily;
}

class Daily {
    List<Double> temperature_2m_max;
    List<Double> temperature_2m_min;
    List<Double> precipitation_sum;
    List<Double> wind_speed_10m_max;
    List<Double> winddirection_10m_dominant;
}

class PredictTemperature {
    private static Instances data;
    private static RandomForest cls;

//    public static void main(String[] args) throws Exception
//    {
//        trainModel();
//        double predictedTemp = predictTemperature(30, -97, 60);
//        System.out.println("Predicted temp: " + predictedTemp);
//    }

    public void trainModel(InputStream inputStream) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setSource(inputStream);

//        loader.setFieldSeparator(";");
        data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);
// configure classifier
        cls = new RandomForest();

        cls.buildClassifier(data);

// cross-validate (10-fold) classifier
        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(cls, data, 10, new Random(1));
// output collected predictions
        System.out.println(eval.toSummaryString("\nResults\n\n", false));
    }

    public double predictTemperature(double lat, double lon, double temp) {
        try {
            // Create instance and predict temperature
            // Return the predicted value

            Instance newInstance = new DenseInstance(data.numAttributes());
            newInstance.setDataset(data);

            newInstance.setValue(0, lat);
            newInstance.setValue(1, lon);
            newInstance.setValue(2, temp);

            newInstance.setValue(3, 0);
            data.add(newInstance);
            double pred_temp = cls.classifyInstance(newInstance);
            return pred_temp;
        } catch(Exception e) {
            return -1;
        }
    }
}

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;

    private Button buttonTwo;

    private TextView textViewOne;

    private LineChart lineChart;
//    private EditText editTextOne;
    LatLng location = new LatLng(30.2849, -97.7341);

    double predictedTemp;

    PredictTemperature model;

    void fetchData() {
        TextView textViewTwo = findViewById(R.id.scrollableTextView);
        // from volley
        //final TextView textView = (TextView) findViewById(R.id.text);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + location.latitude + "&longitude=" + location.longitude + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,winddirection_10m_dominant&timezone=auto&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch";
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        // Display the first 500 characters of the response string.
//                        textViewTwo.setText("Response is: " + response.substring(0, 100));
                        // Use Gson to parse the response
                        Gson gson = new Gson();
                        WeatherResponse weatherResponse = gson.fromJson(response, WeatherResponse.class);

                        Log.i("Information", "Weather: "+response);

                        // Extract data and update UI
                        updateWeatherUI(weatherResponse);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textViewTwo.setText("API Error");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);


    }

    private void updateWeatherUI(WeatherResponse weatherResponse) {
        if (weatherResponse != null && weatherResponse.daily != null) {
            StringBuilder weatherInfo = new StringBuilder();
//            List<Double> avg_temp = new ArrayList<>();

            List<Entry> minTempEntries = new ArrayList<>();
            List<Entry> maxTempEntries = new ArrayList<>();
            List<Entry> avgTempEntries = new ArrayList<>();

            // call ML code
            predictedTemp = model.predictTemperature(location.latitude, location.longitude, (weatherResponse.daily.temperature_2m_min.get(0) + weatherResponse.daily.temperature_2m_max.get(0)) / 2);

            for (int i = 0; i < weatherResponse.daily.temperature_2m_max.size(); i++) {
                double avgTemp = (weatherResponse.daily.temperature_2m_min.get(i) + weatherResponse.daily.temperature_2m_max.get(i)) / 2;
                double minTemp = weatherResponse.daily.temperature_2m_min.get(i);
                double maxTemp = weatherResponse.daily.temperature_2m_max.get(i);


//                avg_temp.add(avgTemp);
                minTempEntries.add(new Entry(i+1, (float) minTemp));
                maxTempEntries.add(new Entry(i+1, (float) maxTemp));
                avgTempEntries.add(new Entry(i+1, (float) avgTemp));

                weatherInfo.append("day ")
                        .append(i + 1)
                        .append("\n")
                        .append("min temp = ")
                        .append(weatherResponse.daily.temperature_2m_min.get(i))
                        .append(" °f\n")
                        .append("max temp = ")
                        .append(weatherResponse.daily.temperature_2m_max.get(i))
                        .append(" °f\n")
                        .append("avg temp = ")
                        .append(String.format("%.2f °f", avgTemp))
                        .append("\n")
                        .append("rain = ")
                        .append(weatherResponse.daily.precipitation_sum.get(i))
                        .append(" in\n")
                        .append("wind speed = ")
                        .append(weatherResponse.daily.wind_speed_10m_max.get(i))
                        .append(" mph\n")
                        .append("wind direction = ")
                        .append(weatherResponse.daily.winddirection_10m_dominant.get(i))
                        .append("°\n\n");
            }

//            TextView textViewTwo = findViewById(R.id.textView2);
            TextView scrollViewOne = findViewById(R.id.scrollableTextView);
            scrollViewOne.setText(weatherInfo.toString());
            //textViewTwo.setText(weatherInfo.toString());

            LineDataSet minTempDataSet = new LineDataSet(minTempEntries, "min temp");
            minTempDataSet.setColor(Color.BLUE);

            LineDataSet maxTempDataSet = new LineDataSet(maxTempEntries, "max temp");
            maxTempDataSet.setColor(Color.RED);
//            maxTempDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

            LineDataSet avgTempDataSet = new LineDataSet(avgTempEntries, "avg temp");
            avgTempDataSet.setColor(Color.GREEN);

            LineData lineData = new LineData(minTempDataSet, avgTempDataSet, maxTempDataSet);
            lineChart.setData(lineData);
            lineChart.getDescription().setEnabled(false);
            lineChart.getAxisRight().setEnabled(true);
            lineChart.getXAxis().setTextColor(Color.WHITE);
            lineChart.getAxisLeft().setTextColor(Color.WHITE);
            lineChart.getAxisRight().setTextColor(Color.WHITE);
            maxTempDataSet.setDrawValues(false);
            minTempDataSet.setDrawValues(false);
            avgTempDataSet.setDrawValues(false);

            lineChart.invalidate(); // refresh the chart

            // update ML data
            TextView textViewTwo = findViewById(R.id.textView2);
            textViewTwo.setText(String.format("%.2f °f", predictedTemp));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//         for map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // for chart
        lineChart = findViewById(R.id.lineChart);

        // train ML model
        new Thread(new Runnable() {
            @Override
            public void run() {
                AssetManager assetManager = getAssets();
                try {
                    InputStream inputStream = assetManager.open("temperature_data.csv");
                    model = new PredictTemperature();
                    model.trainModel(inputStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

//        AssetManager assetManager = getAssets();
//        try {
//            InputStream inputStream = assetManager.open("temperature_data.csv");
//            model = new PredictTemperature();
//            model.trainModel(inputStream);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }


        // another way of creating/attaching listener
        // to create a dynamic UI programatically
//        buttonTwo = findViewById(R.id.button2);
//        buttonTwo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i("Information", "The internet button has been pressed.");
//                fetchData();
//            }
//        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


//    public void topButtonTap(View view) {
//        // Legacy
////        textViewOne = findViewById(R.id.textView1);
////        //textViewOne.setText("Changed text");
////        editTextOne = findViewById(R.id.editText1);
////        textViewOne.setText(editTextOne.getText());
//
//
//        Log.i("Information", "Top button pressed.");
//    }




    @Override
    public void onMapReady(GoogleMap googleMap) {

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
//        markerOptions.title("Marker"); // Optional: you can also set a title for the marker
//        markerOptions.snippet("Marker"); // Optional: set a snippet or description

        // Add the marker to the map
        googleMap.addMarker(markerOptions);

        // move the camera to the marker position
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10)); // 10 here is the zoom level
        fetchData();


        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng l) {
                location = l;
                // clear the old marker
                googleMap.clear();
                // Add a marker at the clicked location
                googleMap.addMarker(new MarkerOptions().position(location));
                fetchData();

            }
        });
    }


}


package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * <p>
 * Created by Angad on 08-09-2016.
 * </p>
 */

public class DetailsActivity extends AppCompatActivity {
    private LineChartView lineChartView;
    private int maxClose, minClose;
    private long minDate, maxDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);
        lineChartView = (LineChartView) findViewById(R.id.linechart);
        String label = getIntent().getStringExtra("symbol");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(label);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        new GraphDataTask().execute(label);
    }

    private class GraphDataTask extends AsyncTask<String, Void, LineSet> {
        @Override
        protected LineSet doInBackground(String... params) {
            LineSet lines = new LineSet();
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + params[0] + "/chartdata;type=quote;range=6m/json")
                    .build();
            Response response;
            try {
                response = okHttpClient.newCall(request).execute();
                String responseStr = response.body().string();
                String jsonStr = responseStr.split("finance_charts_json_callback\\(")[1].split("\\)")[0];
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(jsonStr);
                    JSONArray array = jsonObject.getJSONArray("series");
                    if (array.length() > 0) {
                        minClose = array.getJSONObject(0).getInt("close");
                        maxClose = minClose;
                        minDate = array.getJSONObject(0).getLong("Date");
                        maxDate = minDate;
                    }
                    for (int i = 0; i < array.length(); i++) {
                        int close = array.getJSONObject(i).getInt("close");
                        long date = array.getJSONObject(i).getLong("Date");
                        lines.addPoint("", close);
                        if (close > maxClose) {
                            maxClose = close;
                        }
                        if (close < minClose) {
                            minClose = close;
                        }
                        if (date > maxDate) {
                            maxDate = date;
                        }
                        if (date < minDate) {
                            minDate = date;
                        }
                    }
                } catch (JSONException e) {
                    Log.d("TAG", e.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return lines;
        }

        @Override
        protected void onPostExecute(LineSet lineSet) {
            super.onPostExecute(lineSet);
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(Tools.fromDpToPx(1f));
            lineChartView.setBorderSpacing(1)
                    .setAxisBorderValues(minClose, maxClose)
                    .setXLabels(AxisController.LabelPosition.OUTSIDE)
                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
                    .setLabelsColor(Color.RED)
                    .setXAxis(false)
                    .setYAxis(false)
                    .setBorderSpacing(Tools.fromDpToPx(5))
                    .setGrid(ChartView.GridType.HORIZONTAL, paint);
            TextView range = (TextView) findViewById(R.id.range);
            String sRange = Utils.formatDate(String.valueOf(minDate)) + " - " + Utils.formatDate(String.valueOf(maxDate));
            range.setText(sRange);
            if (lineSet.size() > 0) {
                lineChartView.addData(lineSet);
                lineChartView.show();
            }
        }
    }
}

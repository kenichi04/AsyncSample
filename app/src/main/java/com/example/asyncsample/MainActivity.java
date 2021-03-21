package com.example.asyncsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvCityList = findViewById(R.id.lvCityList);
        List<Map<String, String>> cityList = new ArrayList<>();

        Map<String, String> city = new HashMap<>();
        city.put("name", "大阪");
        city.put("id", "270000");
        cityList.add(city);

        city = new HashMap<>();
        city.put("name", "神戸");
        city.put("id", "280010");
        cityList.add(city);

        String[] from = {"name"};
        int [] to = {android.R.id.text1};

        SimpleAdapter adapter =
                new SimpleAdapter(MainActivity.this, cityList, android.R.layout.simple_expandable_list_item_1, from, to);

        lvCityList.setAdapter(adapter);
        lvCityList.setOnItemClickListener(new ListItemClickListener());
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Map<String, String> item = (Map<String, String>) parent.getItemAtPosition(position);
            String cityName = item.get("name");
            String cityId = item.get("id");

            TextView tvCityName = findViewById(R.id.tvWCityName);
            tvCityName.setText(cityName + "の天気: ");

            // 天気情報・詳細情報のTextView
            TextView tvWeatherTelop = findViewById(R.id.tvWeatherTelop);
            TextView tvWeatherDesc = findViewById(R.id.tvWeatherDesc);

            WeatherInfoReceiver receiver = new WeatherInfoReceiver(tvWeatherTelop, tvWeatherDesc);
            // WeatherInfoReceiverを実行, execute()の引数が、そのままdoInBackground()の引数として渡される
            receiver.execute(cityId);
        }
    }

    /*
     * 引数1つ目: execute(), doInBackground()の引数の型
     * 引数2つ目: publishProgress(), onProgressUpdate()の引数の型
     * 引数3つ目: doInBackground()の戻り値の型、および、onPostExecute(), onCancelled()の引数の型
     * 　　　　　　doInbackground()の戻り値が、そのままonPostExecute(), onCancelled()の引数として渡される
     */
    private class WeatherInfoReceiver extends AsyncTask<String, String, String> {
        /* 現在の天気を表示 */
        private TextView _tvWeatherTelop;
        /* 天気の詳細を表示 */
        private TextView _tvWeatherDesc;

        public WeatherInfoReceiver(TextView tvWeatherTelop, TextView tvWeatherDesc) {
            _tvWeatherTelop = tvWeatherTelop;
            _tvWeatherDesc = tvWeatherDesc;
        }

        // 非同期で行う処理（バックグラウンドスレッドで実行される）
        @Override
        public String doInBackground(String... params) {
            // 可変長引数の1つ目:都市ID
            String id = params[0];
//            String urlStr = "http://weather.livedoor.com/forecast/webservice/json/v1?city=" + id;
            String urlStr = "https://weather.tsukumijima.net/api/forecast?city=" + id;

            // 天気情報サービスから取得するJSON文字列
            String result = "";

            // 上記URLに接続してJSON文字列を取得する処理
            // HTTP接続を行うHttpURLConnectionオブジェクト取得
            HttpURLConnection con = null;
            // HTTP接続のレスポンスデータとして取得するInputStreamオブジェクト
            InputStream is = null;
            try {
                // URLオブジェクト
                URL url = new URL(urlStr);
                // URLオブジェクトからHttpURLConnectionオブジェクト取得
                con = (HttpURLConnection) url.openConnection();
                // HTTP接続メソッドを設定
                con.setRequestMethod("GET");
                // 接続
                con.connect();
                // HttpURLConnectionオブジェクトからレスポンスデータ取得
                is = con.getInputStream();
                // 文字列に変換
                result = is2String(is);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // HttpURLConnectionオブジェクト解放
                if(con != null) {
                    con.disconnect();
                }
                // InputStreamオブジェクト解放
                if(is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // JSON文字列を返す
            return result;
        }

        // doInBackground()の後にUIスレッドで実行される（画面表示はUIスレッドで処理する必要がある）
        // 引数にはdoInBackgroundの戻り値がそのまま渡される
        @Override
        public void onPostExecute(String result) {
            // 天気情報用文字列
            String telop = "";
            String desc = "";

            try {
                // JSON文字列からJSONObjectオブジェクトを生成
                JSONObject rootJSON = new JSONObject(result);
                // ルートJSON直下の「description」JSONオブジェクトを取得
                JSONObject descriptionJSON = rootJSON.getJSONObject("description");
                //「description」プロパティ直下の「text」文字列（天気概要文）取得
                desc = descriptionJSON.getString("text");
                // ルートJSON直下の「forecast」JSON配列を取得
                JSONArray forecasts = rootJSON.getJSONArray("forecast");
                //「forecasts」JSON配列1つ目（インデックス0）のJSONオブジェクト取得
                JSONObject forecastNow = forecasts.getJSONObject(0);
                //「forecasts」1つ目のJSONオブジェクトから「telop」文字列を取得
                telop = forecastNow.getString("telop");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            _tvWeatherTelop.setText(telop);
            _tvWeatherDesc.setText(desc);
        }

        // InputStreamを文字列に変換する
        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while (0 <= (line = reader.read(b))) {
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }
}
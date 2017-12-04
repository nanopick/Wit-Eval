package ai.wit.eval.wit_eval;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.TreeMap;


import javax.net.ssl.HttpsURLConnection;

public class HttpRequest extends AsyncTask<String, Void, String> {
    /* Variable Declaration */
    private MainActivity mainActivity;

    public static Token token = new Token();
    public static final String TOKEN = token.getLIFX_TOKEN();
    public static final String SELECTOR = token.getSelector();

    /*
  {
    "_text" : "turn on the red light",
    "outcomes" : [ {
        "confidence" : 1,
		"intent" : "bulb_turnon",
		"_text" : "turn on the red light",
		"entities" : {
		    "color" : [ {
			    "confidence" : 0.98760171345235,
				"value" : "red",
				"type" : "value"
				} ]
			}
		} ],
    "WARNING" : "DEPRECATED",
    "msg_id" : "0byrbFLxvEqTkoSTK"
  }
     */

    /* Constructor */
    public HttpRequest(MainActivity activity){
        mainActivity = activity;
    }

    /* Methods */
    // Understand Intent
    public String understandIntent( JSONObject json ) throws JSONException {
        TreeMap<String,String> colormap = ColorMap.colormap;
        String result = "power=on";
        String intent = null;

        String power = "";
        String color = "";
        String brightness = "";

        if (json.has("intent"))
            intent = json.getString("intent");
        if (json.has("entities")) {
            JSONObject entities = json.getJSONObject("entities");
            if (entities.has("color")) {
                entities = entities.getJSONArray("color").getJSONObject(0);
                if (entities.has("value"))
                    color = entities.getString("value");
            }
        }
        if (json.has("brightness"))
            brightness = json.getString("brightness");


        // Handling intent
        if (intent == null) {
        } else switch (intent.split("_")[0]) {
            case "bulb":
                switch (intent.split("_")[1]) {
                    case "turnoff":
                        power = "power=off";
                        break;
                    case "turnon":
                        power = "power=on";
                        break;
                    case "toggle":
                        power = "power=toggle";
                        break;
                }
                ;
            case "color":
                Log.d("JSON", "Color matches from map?: " + colormap.containsKey(color));
                if(colormap.containsKey(color))
                    color = "color=" + colormap.get(color);
                else
                    color = "color=" + color;
                break;
            case "light":
                switch (intent.split("_")[1]) {
                    case "bright":
                        brightness = "brightness=1";
                    case "dim":
                        brightness = "brightness=0.1";
                    case "set":
                        try {
                            if (!brightness.contains("."))
                                brightness = "." + brightness;
                            BigDecimal dblBrightness = new BigDecimal(brightness);
                            brightness = "brightness=" + dblBrightness.toString();
                        } catch (NumberFormatException e) {
                            Log.d("JSON", "Error understanding brightness");
                            Log.d("JSON", e.getMessage());
                            brightness = "";
                        }
                }
                break;
            default:
                break;
        }

        // Join all the result if one of them are not empty
        result = ( ( power + color + brightness ).length() == 0 ) ? result : (
                ( (power.equalsIgnoreCase("") ) ? "" : ( power + "&" ) ) +
                ( (color.equalsIgnoreCase("") ) ? "" : ( color + "&" ) ) +
                ( (brightness.equalsIgnoreCase("") ) ? "" : ( brightness )) );
        Log.d("JSON", "Intent to be sent: " + result);
        return result;
    }

    // Asynchronized Process
    @Override
    protected String doInBackground(String... param) {
        JSONObject json = null;

        // Accessing Url
        String urlSt = "https://api.lifx.com/v1/lights/" + SELECTOR + "/state";
        // Sccessing method
        String method = "PUT";

        HttpURLConnection con = null;
        BufferedReader br;
        StringBuilder sb;

        String result = "";
        String pass = "power=on"; //power=on&color=green&brightness=1.0";


        Log.d("JSON", "Json to be parsed: \n" + param[0]);
        // Parse JSON and compose pass
        try {
            json = new JSONArray(param[0]).getJSONObject(0);
            Log.d("JSON", "Json parsed.");
            Log.d("JSON Result", json.toString(4));
            // Obtain "outcomes" component
            pass = understandIntent( json );
            Log.d("JSON Result", pass);
        } catch (JSONException e) {
            Log.e("JSON Result", "Error get information from JSON");
            Log.e("JSON Result", e.toString());
        }

        if (pass.contains("toggle")) {
            urlSt = "https://api.lifx.com/v1/lights/" + SELECTOR + "/toggle";
            method = "POST";
        }
        // power, color, duration, brightness

        try {
            // Set URL
            URL url = new URL(urlSt);

            // HttpURLConnection
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Authorization", "Bearer " + TOKEN);

            // request POST
            con.setRequestMethod(method);
            con.setDoOutput(true);

            con.setInstanceFollowRedirects(false); // no Redirects
            con.setReadTimeout(10000); // Set Timeout
            con.setConnectTimeout(20000); // Set Timeout

            // Connect
            Log.d("Debugging", "Connecting to " + urlSt);
            con.connect();

            // Values to be sent
            OutputStream out = null;
            try {
                out = con.getOutputStream();
                out.write( pass.getBytes("UTF-8") );
                out.flush();
                Log.d("debug","flush");
            } catch (IOException e) {
                e.printStackTrace();
                result="Error Sending Request!";
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            final int status = con.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK || status == 207) {
                // レスポンスを受け取る処理等
                result = "HTTP_OK";
                br = new BufferedReader(new InputStreamReader((con.getInputStream())));
                sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null)
                    sb.append(output);
                String responseResult = sb.toString();
                responseResult.replaceFirst("^[\\s\\[]*", "");
                responseResult.replaceFirst("[\\s\\]]*$", "");
                Log.i("Received", sb.toString());
                try {
                    json = new JSONObject(responseResult);
                    Log.d("JSON Result", json.toString(4));
                    result = json.getJSONArray("results").getJSONObject(0).getString("status");
                    Log.d("JSON Result", result);
                } catch (JSONException e) {
                    Log.e("JSON Result", "unable to parse JSON");
                }
            } else {
                    result = "status=" + String.valueOf(status);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result + " " + pass;
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        mainActivity.setTextView(result);
    }
}

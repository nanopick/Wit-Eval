package ai.wit.eval.wit_eval;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import ai.wit.sdk.IWitListener;
//import ai.wit.sdk.Wit;
import ai.wit.sdk.Wit_local;
import ai.wit.sdk.model.WitOutcome;


public class MainActivity extends ActionBarActivity implements IWitListener {

    Wit_local _wit;
    private TextView requestResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Token token = new Token();
        String accessToken = token.getWIT_TOKEN();
        _wit = new Wit_local(accessToken, this);
        _wit.enableContextLocation(getApplicationContext());
        //String jsonoutput = "{ \"_text\" : \"turn on the red light\", \"outcomes\" : [ { \"confidence\" : 1, \"intent\" : \"bulb_turnon\", \"_text\" : \"turn on the red light\", \"entities\" : { \"color\" : [ { \"confidence\" : 0.98760171345235, \"value\" : \"red\", \"type\" : \"value\" } ] } } ], \"WARNING\" : \"DEPRECATED\", \"msg_id\" : \"0byrbFLxvEqTkoSTK\" }";
        //startAsync(jsonoutput);
    }

    private void startAsync(String str) {
        HttpRequest testTask = new HttpRequest(this);
        // executeを呼んでAsyncTaskを実行する、パラメータは１番目
        testTask.execute(str);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggle(View v) {
        try {
            _wit.toggleListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void witDidGraspIntent(ArrayList<WitOutcome> witOutcomes, String messageId, Error error) {
        Log.d("Debugging", "Finish processing @ witDidGraspIntent");
        TextView jsonView = (TextView) findViewById(R.id.jsonView);
        jsonView.setMovementMethod(new ScrollingMovementMethod());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (error != null) {
            jsonView.setText(error.getLocalizedMessage());
            return ;
        }
        String jsonOutput = gson.toJson(witOutcomes);
        jsonView.setText(jsonOutput);

        Log.d("Debugging", witOutcomes.get(0).get_text());
        for (WitOutcome outcome : witOutcomes) {
            Log.d("Debugging", outcome.get_text());
        }
        String text = witOutcomes.get(0).get_text();
        ((TextView) findViewById(R.id.txtResult)).setText(text);

        //Log.d(witOutcomes.toString(), "Debugging");
        //Log.d(error.toString(), "Debugging");
        //Log.d(messageId, "Debugging");
        //Log.i("HOGEHOGEHOGEHOGEHOGEHOGEHOGE", "Testing out my logging.");


        startAsync(jsonOutput);
        ((TextView) findViewById(R.id.txtText)).setText(R.string.done);

    }

    @Override
    public void witDidStartListening() {
        Log.d("Debugging", "Wit Start Listening @ witDidStartListening (i.e. witting)");
        //Log.d("Debugging", "intent: "_wit. ;
        ((TextView) findViewById(R.id.txtText)).setText(R.string.witting);
    }

    @Override
    public void witDidStopListening() {
        Log.d("Debugging", "Wit Stop Listening @ witDidStopListening");
        ((TextView) findViewById(R.id.txtText)).setText(R.string.processing);
    }

    @Override
    public void witActivityDetectorStarted() {
        Log.d("Debugging", "Set to listening... @ witActivityDetectorStarted");
        ((TextView) findViewById(R.id.txtText)).setText(R.string.listening);
    }

    @Override
    public String witGenerateMessageId() {
        return null;
    }

    public static class PlaceholderFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.wit_button, container, false);
        }
    }

    void setTextView(String str) {
        Log.d("Status", "I'm Alive!!!!!!!!!!!" + str);
        try {
            Log.i("MainActivity.setTextView()", str);
            ((TextView) findViewById(R.id.requestResult)).setText(str);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e("ERROR!!", "Something happened");
            ((TextView) findViewById(R.id.requestResult)).setText("Error!!");

        }
    }
}

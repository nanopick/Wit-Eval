//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ai.wit.sdk;

import ai.wit.sdk.model.WitOutcome;
import ai.wit.sdk.model.WitResponse;
import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Wit_local implements IWitCoordinator {
    private static final String TAG = "Wit";
    private String _accessToken;
    private IWitListener _witListener;
    private WitMic _witMic;
    private PipedInputStream _in;
    private JsonObject _context = new JsonObject();
    private Context _androidContext;
    public Wit.vadConfig vad;

    public Wit_local(String accessToken, IWitListener witListener) {
        Log.d("Wit", "Using local wit alias!! (wit_local)");
        this.vad = Wit.vadConfig.detectSpeechStop;
        this._accessToken = accessToken;
        this._witListener = witListener;
    }

    public void startListening() throws IOException {
        new WitContextSetter(this._context, this._androidContext);
        this._witMic = new WitMic(this, this.vad);
        this._witMic.startRecording();
        this._in = this._witMic.getInputStream();
        if(this.vad != Wit.vadConfig.full) {
            this.voiceActivityStarted();
        } else {
            this._witListener.witActivityDetectorStarted();
        }

    }

    public void stopListening() {
        this._witMic.stopRecording();
        this._witListener.witDidStopListening();
    }

    public void voiceActivityStarted() {
        this.streamRawAudio(this._in, "signed-integer", 16, 16000, ByteOrder.LITTLE_ENDIAN);
        this._witListener.witDidStartListening();
    }

    public void toggleListening() throws IOException {
        if(this._witMic != null && this._witMic.isRecording()) {
            this.stopListening();
        } else {
            this.startListening();
        }

    }

    public void streamRawAudio(InputStream audio, String encoding, int bits, int rate, ByteOrder order) {
        if(audio == null) {
            this._witListener.witDidGraspIntent((ArrayList)null, (String)null, new Error("InputStream null"));
        } else {
            Log.d(this.getClass().getName(), "streamRawAudio started.");
            String contentType = String.format("audio/raw;encoding=%s;bits=%s;rate=%s;endian=%s", new Object[]{encoding, String.valueOf(bits), String.valueOf(rate), order == ByteOrder.LITTLE_ENDIAN?"little":"big"});
            WitSpeechRequestTask request = new WitSpeechRequestTask(this._accessToken, contentType, this._context, this._witListener) {
                protected void onPostExecute(String result) {
                    Wit_local.this.processWitResponse(result);
                }
            };
            request.execute(new InputStream[]{audio});
        }

    }

    public void captureTextIntent(String text) {
        if(text == null) {
            this._witListener.witDidGraspIntent((ArrayList)null, (String)null, new Error("Input Text null"));
        }

        new WitContextSetter(this._context, this._androidContext);
        WitMessageRequestTask request = new WitMessageRequestTask(this._accessToken, this._context, this._witListener) {
            protected void onPostExecute(String result) {
                Wit_local.this.processWitResponse(result);
            }
        };
        request.execute(new String[]{text});
    }

    private void processWitResponse(String result) {
        WitResponse response = null;
        Error errorDuringRecognition = null;
        Log.d("Wit", "Wit : Response " + result);

        try {
            Gson gson = new Gson();
            response = (WitResponse)gson.fromJson(result, WitResponse.class);
            Log.d("Wit", "Gson : Response " + gson.toJson(response));
        } catch (Exception var5) {
            Log.e("Wit", "Wit : Error " + var5.getMessage());
            errorDuringRecognition = new Error(var5.getMessage());
        }
        if(errorDuringRecognition != null) {
            this._witListener.witDidGraspIntent((ArrayList)null, (String)null, errorDuringRecognition);
        } else if (response.getOutcomes() == null ) {
            this._witListener.witDidGraspIntent((ArrayList) null, (String)null, new Error("Response null"));
        } else if(response == null) {
            this._witListener.witDidGraspIntent((ArrayList)null, (String)null, new Error("Response null"));
        } else if(response.getOutcomes().size() == 0) {
            this._witListener.witDidGraspIntent((ArrayList)null, (String)null, new Error("No outcome"));
        } else {
            Log.d("Wit", "Wit did grasp " + response.getOutcomes().size() + " outcome(s)");
            this._witListener.witDidGraspIntent(response.getOutcomes(), response.getMsgId(), (Error)null);
        }

    }

    public void setContext(JsonObject context) {
        this._context = context;
    }

    public void enableContextLocation(Context androidContext) {
        this._androidContext = androidContext;
    }

    public static enum vadConfig {
        disabled,
        detectSpeechStop,
        full;

        private vadConfig() {
        }
    }
}
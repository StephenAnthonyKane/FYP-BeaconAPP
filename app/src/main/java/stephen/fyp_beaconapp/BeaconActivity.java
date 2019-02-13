package stephen.fyp_beaconapp;

import android.Manifest;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;

import okhttp3.*;

public class BeaconActivity extends AppCompatActivity implements BeaconConsumer {

    protected static final String TAG = "MonitoringActivity";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static OkHttpClient client;
    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);


        requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);

        client = new OkHttpClient();


        beaconManager = BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers()
                .add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.bind( this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    protected void runfunc(View view) {

        JSONObject objectToSend = new JSONObject();
        JSONArray jArray = new JSONArray();
        //Log.i(TAG, "Beacon seen UID: "+ beacon.getId1()+" RSS: "+ beacon.getRssi());
        for (int i = 0; i < 6; i++){
            try {
                JSONObject beaconObject = new JSONObject();
                beaconObject.put("UID", i);
                beaconObject.put("RSS", i);
                jArray.put(beaconObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            objectToSend.put("Beacons", jArray);

            post(objectToSend.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    void post(String json) throws IOException {

        RequestBody body = RequestBody.create(JSON, json);

        String url = "http://sbsrv1.cs.nuim.ie/fyp/skane/RequestHandler.py";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.i(TAG, response.message());
                    throw new IOException("Unexpected code " + response);
                } else {
                    // do something wih the result
                }
            }
        });
        //Log.i(TAG, response.message());
    }

    @Override
    public void onBeaconServiceConnect() {

        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                final TextView TxtView = (TextView) findViewById(R.id.textViewId);
                Switch s = (Switch) findViewById(R.id.SwitchID);

                if (s.isChecked()) {

                    if (beacons.size() > 0) {
                        TxtView.setText("");
                        JSONObject objectToSend = new JSONObject();
                        JSONArray jArray = new JSONArray();

                        for (Beacon beacon : beacons) {
                            Log.i(TAG, "Beacon seen UID: " + beacon.getId1() + " RSS: " + beacon.getRssi());
                            try {
                                JSONObject beaconObject = new JSONObject();
                                beaconObject.put("UID", beacon.getId1());
                                beaconObject.put("RSS", beacon.getRssi());
                                jArray.put(beaconObject);

                                TxtView.append(beaconObject.toString());


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                objectToSend.put("Beacons", jArray);

                                post(objectToSend.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}

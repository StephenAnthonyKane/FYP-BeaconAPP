package stephen.fyp_beaconapp;

import android.Manifest;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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


    void post(String json) throws IOException {

        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url("http://192.168.1.7:8080/")
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

                JSONArray jArray = new JSONArray();

                if (beacons.size()>0){
                    TxtView.setText("");
                }

                for (Beacon beacon: beacons){
                    //Log.i(TAG, "Beacon seen UID: "+ beacon.getId1()+" RSS: "+ beacon.getRssi());

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("UID", beacon.getId1());
                        jsonObject.put("RSS", beacon.getRssi());

                        TxtView.append(jsonObject.toString());

                        post(jsonObject.toString());

                        jArray.put(jsonObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    /*
                    catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    */
                }


                //Log.i(TAG, "Start of Array");
                for (int i=0; i < jArray.length(); i++){
                    try{
                        JSONObject testObj = jArray.getJSONObject(i);

                        Log.i(TAG, "Beacon seen UID: "+ testObj.get("UID") +" RSS: "+ testObj.get("RSS"));
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //Log.i(TAG, "End of Array");

                /*
                for (Beacon beacon : beacons) {

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("UID", beacon.getId1().toInt());
                        jsonObject.put("RSS", beacon.getRssi());

                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
                */

            }
            
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}

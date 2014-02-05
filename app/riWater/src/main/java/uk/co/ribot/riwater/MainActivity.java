package uk.co.ribot.riwater;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.NameValuePair;

import java.util.ArrayList;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void gotToPreviousFragment(){
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void showWatering(){
        replaceFragment(new WateringFragment());
    }

    private void hideWatering(){
        gotToPreviousFragment();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment implements View.OnClickListener {

        EditText totalWaterEditView;
        EditText potCapacityEditView;
        Button waterButton;
        final static String PI_ROOT_URL = "http://192.168.0.5:8000/GPIO";
        final static int GPIO_NUMBER = 17;
        final static float PUMP_SPEED = 4000; //4 Liters per minute
        NetworkAsyncTask asyncTask;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            totalWaterEditView = (EditText) rootView.findViewById(R.id.totalWaterEditText);
            potCapacityEditView = (EditText) rootView.findViewById(R.id.maxContainerEditText);
            waterButton = (Button) rootView.findViewById(R.id.buttonWater);
            waterButton.setOnClickListener(this);
            doRequestSetOut();
            return rootView;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.buttonWater:
                    try{
                        float totalWaterFloat = Float.valueOf(totalWaterEditView.getText().toString());
                        float potCapacityFloat = Float.valueOf(potCapacityEditView.getText().toString());
                        if(!isValidTotalWater(totalWaterFloat)){
                            Toast.makeText(getActivity().getApplicationContext(), "Total water must be between 0.25 and 5 liters", Toast.LENGTH_LONG).show();
                            return;
                        }else if(!isValidPotCapacity(potCapacityFloat)){
                            Toast.makeText(getActivity().getApplicationContext(), "Pot capacity must be between 10 and 1000 milliliters", Toast.LENGTH_LONG).show();
                            return;
                        }
                        doWaterRequest(totalWaterFloat * 1000, potCapacityFloat);
                    }catch (NumberFormatException e){
                        Toast.makeText(getActivity().getApplicationContext(), "Oops, some of the values are wrong. Watering aborted!", Toast.LENGTH_LONG).show();
                    }
                    break;
                default:

            }
        }

        @Override
        public void onDestroy() {
            if(asyncTask != null){
                asyncTask.cancel(true);
            }
            super.onDestroy();
        }

        private boolean isValidTotalWater(float totalWater){
            if(totalWater >= 0.25 && totalWater <= 5){
                return true;
            }
            return false;
        }

        private boolean isValidPotCapacity(float potCapacity){
            if(potCapacity >= 10 && potCapacity <= 1000){
                return true;
            }
            return false;
        }

        private String getWateringUrlRequest(float totalWaterMl, float potCapacityMl){
            float totalWaterMil = totalWaterMl;
            float timeOnNeeded = (totalWaterMil * 60 / PUMP_SPEED) * 1000; //mil
            float potCapacityMil = potCapacityMl;
            int numTimesPumpOn = (int)  Math.ceil(totalWaterMl / potCapacityMil);
            int intervalOnMil = (int) Math.floor(timeOnNeeded / (float) numTimesPumpOn);
            android.util.Log.d("YOYO", "totalWaterMil "+totalWaterMil+" timeOnNeeded "+timeOnNeeded+" potCapacityMil "+potCapacityMil+" numTimesPumpOn "+numTimesPumpOn+" intervalOnMil "+intervalOnMil);
            StringBuilder sb = new StringBuilder(intervalOnMil+",");
            for(int i=0; i < numTimesPumpOn; i++){
                sb.append("100");
            }
            return PI_ROOT_URL+ "/"+GPIO_NUMBER+"/sequence/"+sb.toString();
        }

        private void doWaterRequest(float totalWaterMl, float potCapacityMl){
            String url = getWateringUrlRequest(totalWaterMl, potCapacityMl);
            android.util.Log.d("YOYO", "Request url "+url);
            asyncTask = new NetworkAsyncTask(url, NetworkAsyncTask.HttpMethod.POST, new ArrayList<NameValuePair>(), new AsyncTaskCompleteListener<String>() {
                @Override
                public void onTaskComplete(String result) {
                    Toast.makeText(getActivity().getApplicationContext(), "Watering finished! result: "+result, Toast.LENGTH_LONG).show();
                    MainActivity.this.hideWatering();
                }

                @Override
                public void onTaskFailed() {
                    Toast.makeText(getActivity().getApplicationContext(), "Oops, error watering your plants ", Toast.LENGTH_LONG).show();
                    MainActivity.this.hideWatering();
                }
            });
            MainActivity.this.showWatering();
            asyncTask.execute();
        }

        private void doRequestSetOut(){
            String url = PI_ROOT_URL+"/"+GPIO_NUMBER+"/function/out";
            asyncTask = new NetworkAsyncTask(url, NetworkAsyncTask.HttpMethod.POST, new ArrayList<NameValuePair>(), new AsyncTaskCompleteListener<String>() {
                @Override
                public void onTaskComplete(String result) {

                }

                @Override
                public void onTaskFailed() {
                    Toast.makeText(getActivity().getApplicationContext(), "Oops, error on setting up request ", Toast.LENGTH_LONG).show();
                }
            });
            asyncTask.execute();
        }
    }

    public static class WateringFragment extends Fragment{

        public WateringFragment(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_watering, container, false);
            return rootView;
        }
    }

}

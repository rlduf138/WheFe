package com.example.chun.whefe.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chun.whefe.CafeInfo;
import com.example.chun.whefe.MainActivity;
import com.example.chun.whefe.NavigationActivity;
import com.example.chun.whefe.R;
import com.example.chun.whefe.dbhelper.MyCafeInfoHelper;
import com.example.chun.whefe.nmap.NMapFragment;
import com.example.chun.whefe.nmap.NMapPOIflagType;
import com.example.chun.whefe.nmap.NMapViewerResourceProvider;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class NaverMapFragment extends NMapFragment implements NMapView.OnMapStateChangeListener {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "NMapViewer";
    private static final String NAVER_CLIENT_ID = "fpin69EB6CPM5ATQLpgI";

    MyCafeInfoHelper helper;
    SQLiteDatabase db;


    NMapView nMapView;
    NMapController nMapController;
    NMapLocationManager mMapLocationManager;
    NMapCompassManager mMapCompassManager;
    NMapMyLocationOverlay mMyLocationOverlay;
    private MapContainerView mMapContainerView;

    private List<CafeInfo> cafeInfos;

    NMapPOIdata poIdata;

    View baseView;

    NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = null;
    private NMapViewerResourceProvider mMapViewerResourceProvider;
    private NMapOverlayManager mOverlayManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Whefe");

        helper = new MyCafeInfoHelper(getContext());
        db = helper.getWritableDatabase();

        String categoryUrl = MainActivity.ip + "/whefe/android/cafeinfo";
        new DownloadCategoryTask().execute(categoryUrl);


        baseView = inflater.inflate(R.layout.fragment_naver_map, container, false);
        nMapView = (NMapView) baseView.findViewById(R.id.nv_mapView);

        nMapView.setClientId(NAVER_CLIENT_ID);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Whefe");

        return baseView;

    }
    @Override
    public void onStart() {
        super.onStart();
        nMapView.setBuiltInZoomControls(true, null);
        nMapView.setOnMapStateChangeListener(this);
        nMapView.setFocusable(true);
        nMapView.setFocusableInTouchMode(true);
        nMapView.setClickable(true);
        nMapView.requestFocus();
        nMapController = nMapView.getMapController();

        mMapViewerResourceProvider = new NMapViewerResourceProvider(getContext());
        mOverlayManager = new NMapOverlayManager(getContext(), nMapView,mMapViewerResourceProvider);
    }

    private class DownloadCategoryTask extends AsyncTask<String, Void, String> {                     // 카테고리 출력 Connection

        @Override
        protected String doInBackground(String... urls) {
            try {
                return (String) downloadUrl((String) urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
                return "다운로드 실패";
            }
        }

        protected void onPostExecute(String result) {
            try {


                cafeInfos = new ArrayList<CafeInfo>();


                db.execSQL("delete from cafeinfo");

                JSONArray ja = new JSONArray(result);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject order = ja.getJSONObject(i);
                    String cafe_id = (String)order.get("cafe_id");
                    String cafeName = (String) order.get("cafe_name");
                    String cafeAddress = (String)order.get("cafe_address");
                    String cafePhone = (String)order.get("cafe_tel");
                    String cafeOpen = (String)order.get("cafe_open");
                    String cafeClose = (String)order.get("cafe_end");
                    String cafePerson = (String)order.get("cafe_curr");
                    String cafeMax = (String)order.get("cafe_max");
                    String cafe_intro = (String)order.get("cafe_intro");
                    String cafe_image1 = (String)order.get("imageFilename1");
                    String cafe_image2 = (String)order.get("imageFilename2");
                    String cafe_image3 = (String)order.get("imageFilename3");

                    CafeInfo cafeInfo = new CafeInfo(cafe_id,cafeName,cafeAddress,cafePhone,cafeOpen,cafeClose,cafePerson,cafeMax,cafe_intro,cafe_image1, cafe_image2,cafe_image3);
                    db.execSQL("insert into cafeinfo(cafe_id, cafe_name) " +
                            "values('"+ cafe_id + "','" + cafeName  + "');");

                    cafeInfos.add(cafeInfo);

                    String info = cafeInfo.getCafeName();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            poIdata= new NMapPOIdata(2, mMapViewerResourceProvider);

            setPOIData(poIdata,0);

            NMapPOIdataOverlay poidataOverlay = mOverlayManager.createPOIdataOverlay(poIdata,null);

            poidataOverlay.setOnStateChangeListener(new NMapPOIdataOverlay.OnStateChangeListener() {
                @Override
                public void onFocusChanged(NMapPOIdataOverlay nMapPOIdataOverlay, NMapPOIitem nMapPOIitem) {
                    if (nMapPOIitem != null) {
                        Log.i(LOG_TAG, "onFocusChanged: " + nMapPOIitem.toString());
                    } else {
                        Log.i(LOG_TAG, "onFocusChanged: ");
                    }
                }

                @Override
                public void onCalloutClick(NMapPOIdataOverlay nMapPOIdataOverlay, NMapPOIitem nMapPOIitem) {
                    RelativeLayout relativeLayout = (RelativeLayout)getView().findViewById(R.id.nmap_info);
                    relativeLayout.setVisibility(View.VISIBLE);

                    int i = 0;
                    for(i = 0; i<cafeInfos.size();i++){
                        if(nMapPOIitem.getTitle().equals(cafeInfos.get(i).getCafeName())){
                            break;
                        }
                    }

                    TextView nameView = (TextView)getView().findViewById(R.id.cafeInfoNameView);
                    TextView addressView = (TextView)getView().findViewById(R.id.cafeInfoAddressView);
                    TextView phoneView = (TextView)getView().findViewById(R.id.cafeInfoPhoneView);
                    TextView timeView = (TextView)getView().findViewById(R.id.cafeInfoTimeView);
                    TextView personView = (TextView)getView().findViewById(R.id.cafeInfoPersonView);
                    Button button = (Button)getView().findViewById(R.id.cafeInfoButton);

                    nameView.setText(cafeInfos.get(i).getCafeName());
                    addressView.setText(cafeInfos.get(i).getCafeAddress());
                    phoneView.setText("전화번호 : " + cafeInfos.get(i).getCafePhone());
                    timeView.setText("영업시간 : " + cafeInfos.get(i).getCafeOpen() + " ~ " + cafeInfos.get(i).getCafeClose());

                    double person = Double.parseDouble(cafeInfos.get(i).getCafePerson());
                    double max = Double.parseDouble(cafeInfos.get(i).getCafeMaximum());

                    double maxPerPerson = (person/max*100);

                    personView.setText("혼잡도 : " + (int)person + "/" + (int)max + " ( " + (int)maxPerPerson + "% )");

                    if(maxPerPerson > 65){
                        personView.setBackgroundColor(Color.RED);
                    }else if(maxPerPerson > 40){
                        //personView.setTextColor(Color.YELLOW);
                        personView.setBackgroundColor(Color.YELLOW);
                    }else if(maxPerPerson >=0){
                        personView.setBackgroundColor(Color.GREEN);
                    }

                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            NavigationActivity activity = (NavigationActivity)getActivity();
                            stopMyLocation();
                            activity.onFragmentChanged(1);
                        }
                    });

                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences("INFO_PREFERENCE", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    editor.putString("cafe_id",cafeInfos.get(i).getCafe_id());
                    editor.putString("name", cafeInfos.get(i).getCafeName());
                    editor.putString("address", cafeInfos.get(i).getCafeAddress());
                    editor.putString("phone", cafeInfos.get(i).getCafePhone());
                    editor.putString("open", cafeInfos.get(i).getCafeOpen());
                    editor.putString("close",cafeInfos.get(i).getCafeClose());
                    editor.putString("person", cafeInfos.get(i).getCafePerson());
                    editor.putString("max", cafeInfos.get(i).getCafeMaximum());
                    editor.putString("intro", cafeInfos.get(i).getCafe_intro());
                    editor.putString("image1",cafeInfos.get(i).getCafe_image1());
                    editor.putString("image2",cafeInfos.get(i).getCafe_image2());
                    editor.putString("image3",cafeInfos.get(i).getCafe_image3());

                    editor.commit();
                }
            });
            mMapLocationManager = new NMapLocationManager(getContext());
            mMapLocationManager.setOnLocationChangeListener(onMyLocationChangeListener);
            mMapLocationManager.enableMyLocation(false);
            mMapCompassManager = new NMapCompassManager(getActivity());
            //poIdataOverlay.showAllPOIdata(0);
            mMyLocationOverlay =  mOverlayManager.createMyLocationOverlay(mMapLocationManager, mMapCompassManager);
        }

        private String downloadUrl(String myurl) throws IOException {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(myurl);
                conn = (HttpURLConnection) url.openConnection();
                System.out.println("status code : " + conn.getResponseCode() + "!!!!!!!!!!!!!!");
                Log.e("status code", conn.getResponseMessage());

                BufferedReader bufreader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line = null;
                String page = "";
                while ((line = bufreader.readLine()) != null) {
                    page += line;
                }
                return page;
            } finally {
            }
        }
    }

    public NMapPOIdata setPOIData(NMapPOIdata poiData, int markerId){


        for(int i = 0; i<cafeInfos.size();i++){
            String info = cafeInfos.get(i).getCafeName();

            Geocoder geocoder = new Geocoder(getContext());
            List<Address> list = null;

            double latitude = 0;
            double longitude = 0;

            try{
                list = geocoder.getFromLocationName(cafeInfos.get(i).getCafeAddress(),10);
                Log.e("geocoder", cafeInfos.get(i).getCafeAddress());
                Log.e("geocoder",list.toString());
            }catch (IOException e){
                e.printStackTrace();
                Log.e("geocoder", "주소 변환 오류");
            }
            if(list!=null){
                if(list.size()==0){
                    Log.e("geocoder", "주소 없음");
                    longitude = 127.010010;
                    latitude = 37.582562;
                }else{
                    latitude = list.get(0).getLatitude();
                    longitude = list.get(0).getLongitude();
                }
            }
          //  poiData.addPOIitem(latitude,longitude,info,NMapPOIflagType.RED,0);
            double person = Double.parseDouble(cafeInfos.get(i).getCafePerson());
            double max = Double.parseDouble(cafeInfos.get(i).getCafeMaximum());
            double maxPerPerson = (double)(person/max*100);

            if(maxPerPerson > 65){
                poiData.addPOIitem(longitude, latitude, info, NMapPOIflagType.RED, 0);
            }else if(maxPerPerson > 40){
                poiData.addPOIitem(longitude, latitude, info, NMapPOIflagType.YELLOW, 0);
            }else if(maxPerPerson >=0){
                poiData.addPOIitem(longitude, latitude, info, NMapPOIflagType.GREEN, 0);
            }


            Log.e("geocoder","최종 위도 경도 : " +  latitude + " , " + longitude);
        }
        poiData.endPOIdata();


        return poiData;
    }
    /* MyLocation Listener */
    private final NMapLocationManager.OnLocationChangeListener onMyLocationChangeListener = new NMapLocationManager.OnLocationChangeListener() {

        @Override
        public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {

            if (nMapController != null) {
                nMapController.animateTo(myLocation);
            }

            return true;
        }

        @Override
        public void onLocationUpdateTimeout(NMapLocationManager locationManager) {}

        @Override
        public void onLocationUnavailableArea(NMapLocationManager locationManager, NGeoPoint myLocation) {}

    };
    private void startMyLocation() {

        if (mMyLocationOverlay != null) {
            if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
                mOverlayManager.addOverlay(mMyLocationOverlay);
            }

            if (mMapLocationManager.isMyLocationEnabled()) {

                if (!nMapView.isAutoRotateEnabled()) {
                    mMyLocationOverlay.setCompassHeadingVisible(true);

                  //  mMapCompassManager.enableCompass();

                    nMapView.setAutoRotateEnabled(true, false);

             //       mMapContainerView.requestLayout();
                } else {
                    stopMyLocation();
                }

                nMapView.postInvalidate();
            } else {
                boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(true);
                if (!isMyLocationEnabled) {
                    Toast.makeText(getContext(), "Please enable a My Location source in system settings",
                            Toast.LENGTH_LONG).show();

                    Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(goToSettings);

                    return;
                }
            }
        }
    }
    private void stopMyLocation() {
        if (mMyLocationOverlay != null) {
            mMapLocationManager.disableMyLocation();

            if (nMapView.isAutoRotateEnabled()) {
                mMyLocationOverlay.setCompassHeadingVisible(false);

            //    mMapCompassManager.disableCompass();

                nMapView.setAutoRotateEnabled(false, false);

           //     mMapContainerView.requestLayout();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("CGY","stopMyLocation");
        stopMyLocation();
    }

    @Override
    public void onMapInitHandler(NMapView nMapView, NMapError nMapError) {
        if(nMapError == null){
            nMapController.setZoomLevel(20);
            //startLocationService();
            //NGeoPoint point = new NGeoPoint(126.97362, 37.57570);
           // nMapController.setMapCenter(point,20);
        }

    }

    @Override
    public void onMapCenterChange(NMapView nMapView, NGeoPoint nGeoPoint) {

    }

    @Override
    public void onMapCenterChangeFine(NMapView nMapView) {

    }

    @Override
    public void onZoomLevelChange(NMapView nMapView, int i) {

    }

    @Override
    public void onAnimationStateChange(NMapView nMapView, int i, int i1) {

    }
    Double newLatitude;
    Double newLongitude;
    LocationManager manager;

    private void startLocationService() {
        manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        // 위치 정보를 받을 리스너 생성
        GPSListener gpsListener = new GPSListener();
        long minTime = 10000;
        float minDistance = 0;

        try {
            // GPS를 이용한 위치 요청
            manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    gpsListener);

            // 네트워크를 이용한 위치 요청
            manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0,
                    0,
                    gpsListener);
            /*manager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
                    minTime,
                    minDistance,
                    gpsListener);*/


            // 위치 확인이 안되는 경우에도 최근에 확인된 위치 정보 먼저 확인
            Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                newLatitude = lastLocation.getLatitude();
                newLongitude = lastLocation.getLongitude();
            }
            //nMapController.setMapCenter(new NGeoPoint(lastLocation.getLongitude(),lastLocation.getLatitude()),11);


        } catch(SecurityException ex) {
            ex.printStackTrace();
        }

        // Toast.makeText(getContext(), "위치 확인이 시작되었습니다. 로그를 확인하세요.", Toast.LENGTH_SHORT).show();
    }

    class GPSListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            //if(count ==1)

            newLatitude = location.getLatitude();
            newLongitude = location.getLongitude();

            // 맵 중앙 설정 및 애니메이션 효과
            nMapController.animateTo(new NGeoPoint(newLongitude,newLatitude));
            //mMapViewerResourceProvider.getLocationDot();
            String msg = "Latitude : " + newLatitude + "\nLongitude : " + newLongitude;
            Log.i("GPSListener", msg);
            Toast.makeText(getContext(), msg , Toast.LENGTH_SHORT).show();

            manager.removeUpdates(this);    // 업데이트 중지
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private class MapContainerView extends ViewGroup {

        public MapContainerView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int width = getWidth();
            final int height = getHeight();
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);
                final int childWidth = view.getMeasuredWidth();
                final int childHeight = view.getMeasuredHeight();
                final int childLeft = (width - childWidth) / 2;
                final int childTop = (height - childHeight) / 2;
                view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }

            if (changed) {
                mOverlayManager.onSizeChanged(width, height);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            int sizeSpecWidth = widthMeasureSpec;
            int sizeSpecHeight = heightMeasureSpec;

            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);

                if (view instanceof NMapView) {
                    if (nMapView.isAutoRotateEnabled()) {
                        int diag = (((int)(Math.sqrt(w * w + h * h)) + 1) / 2 * 2);
                        sizeSpecWidth = MeasureSpec.makeMeasureSpec(diag, MeasureSpec.EXACTLY);
                        sizeSpecHeight = sizeSpecWidth;
                    }
                }

                view.measure(sizeSpecWidth, sizeSpecHeight);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}

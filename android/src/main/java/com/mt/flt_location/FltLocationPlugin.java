package com.mt.flt_location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * FltLocationPlugin
 */
public class FltLocationPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private static String googlePlaceKey;
    private static Activity mActivity;
    public static Location LOC = null;

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flt_location/method");
        channel.setMethodCallHandler(new FltLocationPlugin());

    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flt_location/method");
        channel.setMethodCallHandler(new FltLocationPlugin());

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("getCurLocations")) {
            getCurLocations(result);
        } else if (call.method.equals("getLocation")) {
            getLocation(result);
        } else if (call.method.equals("searchLocation")) {
            Map arg = call.arguments();
            String keyWord = (String) arg.get("key");
            searchLocation(keyWord, result);
        } else if (call.method.equals("getplacedetail")) {
            Map arg = call.arguments();
            String placeId = (String) arg.get("placeId");
            getPlaceDetail(placeId, result);
        } else {
            result.notImplemented();
        }
    }

    private void getPlaceDetail(String placeId, Result result) {

        Flowable.create((FlowableOnSubscribe<Map>) emitter -> {
            Configuration config = mActivity.getResources().getConfiguration();
            Locale locale = new Locale("en"); // <---- your target language
            Locale.setDefault(locale);
            config.locale = locale;
            mActivity.getResources().updateConfiguration(config, mActivity.getResources().getDisplayMetrics());
            // Create a new Places client instance.
            PlacesClient placesClient = Places.createClient(mActivity);
            List<Place.Field> placeFields = Arrays.asList(Place.Field.ADDRESS, Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME);
            FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
            placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                Place responseplace = response.getPlace();
                PlaceRes place = new PlaceRes();
                place.setName(responseplace.getName());
                place.setThoroughfare(responseplace.getAddress());
                double[] latlng = {responseplace.getLatLng().longitude, responseplace.getLatLng().latitude};
                place.setCoordinate(latlng);
                emitter.onNext(getItem(place, null));
            }).addOnFailureListener((exception) -> {
                emitter.onError(exception);
            });
        }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(comSubscriber(result));
    }

    private void searchLocation(String keyWord, Result result) {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 100000);
            result.success(null);
            return;
        }

        Flowable.create((FlowableOnSubscribe<List<PlaceRes>>) emitter -> {
            RectangularBounds bounds = RectangularBounds.newInstance(new LatLng(-43.577054, 112.372762), //dummy lat/lng
                    new LatLng(-11.914561, 154.239446));
            Configuration config = mActivity.getResources().getConfiguration();
            Locale locale = new Locale("en"); // <---- your target language
            Locale.setDefault(locale);
            config.locale = locale;
            mActivity.getResources().updateConfiguration(config, mActivity.getResources().getDisplayMetrics());
            // Create a new Places client instance.
            PlacesClient placesClient = Places.createClient(mActivity);
            AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder().setLocationRestriction(bounds)
                    //.setCountry("us")
                    .setTypeFilter(TypeFilter.ADDRESS).setSessionToken(token).setQuery(keyWord).build();
            placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
                List<PlaceRes> placeResList = new ArrayList<>();
                for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                    PlaceRes place = new PlaceRes();
                    place.setName(prediction.getPrimaryText(null).toString());
                    place.setThoroughfare(prediction.getSecondaryText(null).toString());
                    place.setPlaceId(prediction.getPlaceId());
                    placeResList.add(place);
                }
                emitter.onNext(placeResList);
                emitter.onComplete();
                //mSearchResult.setText(String.valueOf(mResult));
            }).addOnFailureListener((exception) -> {
                emitter.onError(exception);
            });
        }, BackpressureStrategy.LATEST).map((Function<List<PlaceRes>, Map>) placeRes -> {
            Map<String, Object> resMap = new HashMap<>();
            Map<String, Object> valuesMap = new HashMap<>();
            LocationRes locationRes = null;
            if (placeRes.size() > 0) {
                List<Map<String, Object>> locations = new ArrayList<>();
                for (PlaceRes place : placeRes) {
                    locations.add(getItem(place, locationRes));
                }
                valuesMap.put("locations", locations);
            }
            resMap.put("value", valuesMap);
            return resMap;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(comSubscriber(result));
    }


    private LocationRes transformationLocation(Context context, double[] latlng) throws IOException {
        Geocoder geoCoder = new Geocoder(context, Locale.ENGLISH);
        List<Address> addresses = geoCoder.getFromLocation(latlng[0], latlng[1], 1);
        LocationRes location = null;
        if (null != addresses && addresses.size() > 0) {
            Address address = addresses.get(0);
            location = new LocationRes();
            location.setCountry(address.getCountryName());
            location.setCountryCode(address.getCountryCode());
            location.setProvince(address.getAdminArea());
            location.setLocality(address.getLocality());
            location.setSubLocality(address.getSubLocality());
            location.setSubThoroughfare(address.getFeatureName());
            location.setPostalCode(address.getPostalCode());
        }
        return location;
    }

    private void getLocation(Result result) {
        if (null == mActivity) {
            result.success(null);
            return;
        }
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 100000);
            result.success(null);
            return;
        }
        getLocation(mActivity).timeout(3, TimeUnit.SECONDS).map(location -> {
            if (null != LOC) {
                double latitude = LOC.getLatitude();
                double longitude = LOC.getLongitude();
                double[] latlng = {longitude, latitude};
                LocationRes locationRes = transformationLocation(mActivity, latlng);
                Map<String, Object> item = new HashMap<>();
                item.put("coordinate", latlng);
                if (null != locationRes) {
                    item.put("locality", locationRes.getLocality());
                    item.put("country", locationRes.getCountry());
                    item.put("subLocality", locationRes.getSubLocality());
                    item.put("subThoroughfare", locationRes.getSubThoroughfare());
                    item.put("countryCode", locationRes.getCountryCode());
                    item.put("province", locationRes.getProvince());
                    item.put("postalCode", locationRes.getPostalCode());
                }
                Map<String, Object> resMap = new HashMap<>();
                Map<String, Object> valuesMap = new HashMap<>();
                valuesMap.put("locations", item);
                resMap.put("value", valuesMap);
                return resMap;
            } else {
                return null;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(comSubscriber(result));
    }

    public static Flowable<Location> getLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); //定位精度: 最高
        criteria.setAltitudeRequired(false); //海拔信息：不需要
        criteria.setBearingRequired(false); //方位信息: 不需要
        criteria.setCostAllowed(true);  //是否允许付费
        criteria.setPowerRequirement(Criteria.POWER_LOW); //耗电量: 低功耗

        String provider = locationManager.getBestProvider(criteria, true); //获取定位器类别
        if (provider == null) {

            return Flowable.just(null == LOC ? new Location("") : LOC);
        }
        LOC = locationManager.getLastKnownLocation(provider);
        Flowable<Location> locationFlowable = Flowable.create(emitter -> {
            LocationListener ll = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        LOC = location;
                    }
                    emitter.onNext(location);
                    emitter.onComplete();
                }

                @Override
                public void onStatusChanged(String provider1, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider1) {

                }

                @Override
                public void onProviderDisabled(String provider1) {

                }
            };
            locationManager.requestSingleUpdate(provider, ll, context.getMainLooper());
        }, BackpressureStrategy.LATEST);
        return locationFlowable.subscribeOn(Schedulers.io());

    }

    private void getCurLocations(Result result) {
        if (null == mActivity) {
            result.success(null);
            return;
        }
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 100000);
            result.success(null);
            return;
        }
        Flowable.create((FlowableOnSubscribe<List<PlaceRes>>) emitter -> {
            List<Place.Field> placeFields = Arrays.asList(Place.Field.ADDRESS, Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME);
            Configuration config = mActivity.getResources().getConfiguration();
            Locale locale = new Locale("en"); // <---- your target language
            Locale.setDefault(locale);
            config.locale = locale;
            mActivity.getResources().updateConfiguration(config, mActivity.getResources().getDisplayMetrics());
            PlacesClient placesClient = Places.createClient(mActivity);
            FindCurrentPlaceRequest findCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields);
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(findCurrentPlaceRequest);
            placeResponse.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FindCurrentPlaceResponse response = task.getResult();
                    List<PlaceRes> placeResList = new ArrayList<>();
                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        PlaceRes place = new PlaceRes();
                        place.setName(placeLikelihood.getPlace().getName());
                        place.setThoroughfare(placeLikelihood.getPlace().getAddress());
                        double[] latlng = {placeLikelihood.getPlace().getLatLng().longitude, placeLikelihood.getPlace().getLatLng().latitude};
                        place.setCoordinate(latlng);
                        placeResList.add(place);
                    }
                    emitter.onNext(placeResList);
                    emitter.onComplete();
                } else {
                    Exception exception = task.getException();
                    emitter.onError(exception);
                }
            });
        }, BackpressureStrategy.LATEST).map((Function<List<PlaceRes>, Map>) placeRes -> {
            Map<String, Object> resMap = new HashMap<>();
            Map<String, Object> valuesMap = new HashMap<>();
            LocationRes locationRes = null;
            if (placeRes.size() > 0) {
                locationRes = transformationLocation(mActivity, placeRes.get(0).getCoordinate());
                PlaceRes current = placeRes.get(0);
                Map<String, Object> item = getItem(current, locationRes);
                valuesMap.put("curLocation", item);
                placeRes.remove(0);
            }
            if (placeRes.size() > 0) {
                List<Map<String, Object>> locations = new ArrayList<>();
                for (PlaceRes place : placeRes) {
                    locations.add(getItem(place, locationRes));
                }
                valuesMap.put("locations", locations);
            }
            resMap.put("value", valuesMap);
            return resMap;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(comSubscriber(result));
    }

    private Subscriber<Map> comSubscriber(Result result) {
        return new Subscriber<Map>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Map map) {
                result.success(map);
            }

            @Override
            public void onError(Throwable t) {
                result.success(null);
            }

            @Override
            public void onComplete() {

            }
        };
    }


    private Map<String, Object> getItem(PlaceRes place, LocationRes locationRes) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", place.getName());
        item.put("placeid", place.getPlaceId());
        item.put("thoroughfare", place.getThoroughfare());
        item.put("coordinate", place.getCoordinate());
        if (null != locationRes) {
            item.put("locality", locationRes.getLocality());
            item.put("country", locationRes.getCountry());
            item.put("subLocality", locationRes.getSubLocality());
            item.put("subThoroughfare", locationRes.getSubThoroughfare());
            item.put("countryCode", locationRes.getCountryCode());
            item.put("province", locationRes.getProvince());
            item.put("postalCode", locationRes.getPostalCode());
        }
        return item;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
        ApplicationInfo appInfo = null;
        try {
            appInfo = mActivity.getPackageManager().getApplicationInfo(mActivity.getPackageName(), PackageManager.GET_META_DATA);
            googlePlaceKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (!Places.isInitialized() && !TextUtils.isEmpty(googlePlaceKey)) {
            Places.initialize(mActivity, googlePlaceKey);
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}

package com.devfd.RNGeocoder;

import android.location.Address;
import android.location.Geocoder;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import android.content.Context;

class AddressFormatUtils {
    public static WritableArray transform(List<Address> addresses) {
        WritableArray results = new WritableNativeArray();

        for (Address address: addresses) {
            WritableMap result = new WritableNativeMap();

            WritableMap position = new WritableNativeMap();
            position.putDouble("lat", address.getLatitude());
            position.putDouble("lng", address.getLongitude());
            result.putMap("position", position);

            final String feature_name = address.getFeatureName();
            if (feature_name != null && !feature_name.equals(address.getSubThoroughfare()) &&
                    !feature_name.equals(address.getThoroughfare()) &&
                    !feature_name.equals(address.getLocality())) {

                result.putString("feature", feature_name);
            }
            else {
                result.putString("feature", null);
            }

            result.putString("locality", address.getLocality());
            result.putString("adminArea", address.getAdminArea());
            result.putString("country", address.getCountryName());
            result.putString("countryCode", address.getCountryCode());
            result.putString("locale", address.getLocale().toString());
            result.putString("postalCode", address.getPostalCode());
            result.putString("subAdminArea", address.getSubAdminArea());
            result.putString("subLocality", address.getSubLocality());
            result.putString("streetNumber", address.getSubThoroughfare());
            result.putString("streetName", address.getThoroughfare());

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(address.getAddressLine(i));
            }

            result.putString("formattedAddress", sb.toString());

            results.pushMap(result);
        }

        return results;
    }

    public static WritableArray transformAutoComplete(List<Address> addresses) {
        WritableArray results = new WritableNativeArray();

        for (Address address: addresses) {
            WritableMap result = new WritableNativeMap();

            if (address.getFeatureName() != null) {
                result.putString("title",address.getFeatureName());
            } else if (address.getLocality() != null) {
                result.putString("title",address.getLocality());
            } else if (address.getAdminArea() != null) {
                result.putString("title",address.getAdminArea());
            } else if (address.getSubLocality() != null) {
                result.putString("title",address.getSubLocality());
            } else if (address.getSubAdminArea() != null) {
                result.putString("title",address.getSubAdminArea());
            } else  if (address.getThoroughfare() != null) {
                result.putString("title",address.getThoroughfare());
            } else {
                // Cannot extract useful geo data for places auto completion.
                continue;
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(address.getAddressLine(i));
            }

            result.putString("subTitle",sb.toString());

            results.pushMap(result);
        }

        return results;
    }
}

class GeocodingThread extends Thread {
    private Promise promise;
    private Geocoder geocoder;
    private String addressName;
    private boolean autoCompleting;

    public GeocodingThread(Geocoder geocoder, String addressName, boolean autoCompleting, Promise promise) {
        this.promise = promise;
        this.geocoder = geocoder;
        this.addressName = addressName;
        this.autoCompleting = autoCompleting;
    }

    @Override
    public void run() {
        try {
            List<Address> addresses = geocoder.getFromLocationName(this.addressName, 10);
            if (this.autoCompleting) {
                promise.resolve(AddressFormatUtils.transformAutoComplete(addresses));
            } else {
                if (addresses.size() > 0) {
                    promise.resolve(AddressFormatUtils.transform(addresses));
                } else {
                    promise.reject("Cannot geocode address using native geocoder","Fallback to AMap or Google");
                }    
            }
        }
        catch (IOException e) {
            promise.reject(e);
        }    
    }

}

class ReverseGeocodingThread extends Thread {
    private Promise promise;
    private Geocoder geocoder;
    private ReadableMap position;

    public ReverseGeocodingThread(Geocoder geocoder, ReadableMap position, Promise promise) {
        this.promise = promise;
        this.geocoder = geocoder;
        this.position = position;
    }

    @Override
    public void run() {
        try {
            List<Address> addresses = geocoder.getFromLocation(this.position.getDouble("lat"), this.position.getDouble("lng"), 1);
            if (addresses.size() > 0) {
                promise.resolve(AddressFormatUtils.transform(addresses));
            } else {
                promise.reject("Cannot reverse geocode address using native geocoder","Fallback to AMap or Google");
            }
        }
        catch (IOException e) {
            promise.reject(e);
        }
    }
}

public class RNGeocoderModule extends ReactContextBaseJavaModule {
    private ReactApplicationContext reactContext;

    private Geocoder geocoder;

    public RNGeocoderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        geocoder = new Geocoder(reactContext.getApplicationContext());
    }

    @Override
    public String getName() {
        return "RNGeocoder";
    }

    @ReactMethod
    public void geocodeAddress(String addressName, Promise promise) {
        if (!geocoder.isPresent()) {
          promise.reject("NOT_AVAILABLE", "Geocoder not available for this platform");
          return;
        }

        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(reactContext.getApplicationContext().getApplicationContext());
        if (result == ConnectionResult.SUCCESS) {
            GeocodingThread geocodingThread = new GeocodingThread(geocoder, addressName, false, promise);
            geocodingThread.start();
        } else {
            promise.reject("Google Play Service unavailable", "Fallback to AMap or Google");
        }
    }

    @ReactMethod
    public void geocodeAutoComplete(String addressName, Promise promise) {
        if (!geocoder.isPresent()) {
            promise.reject("NOT_AVAILABLE", "Geocoder not available for this platform");
            return;
        }

        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(reactContext.getApplicationContext().getApplicationContext());
        if (result == ConnectionResult.SUCCESS) {
            GeocodingThread geocodingThread = new GeocodingThread(geocoder, addressName, true, promise);
            geocodingThread.start();
        } else {
            promise.reject("Google Play Service unavailable", "Fallback to AMap or Google");
        }
    }

    @ReactMethod
    public void geocodePosition(ReadableMap position, Promise promise) {
        if (!geocoder.isPresent()) {
            promise.reject("NOT_AVAILABLE", "Geocoder not available for this platform");
            return;
        }

        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(reactContext.getApplicationContext().getApplicationContext());
        if (result == ConnectionResult.SUCCESS) {
            ReverseGeocodingThread rGeocodingThread = new ReverseGeocodingThread(geocoder, position, promise);
            rGeocodingThread.start();
        } else {
            promise.reject("Google Play Service unavailable", "Fallback to AMap or Google");
        }
    }

    @ReactMethod
    public void cancelAutoComplete() {
    }
    

}

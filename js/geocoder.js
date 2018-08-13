import { NativeModules } from 'react-native';
import GoogleApi from './googleApi.js';
import AmapApi from './amapApi.js'

const { RNGeocoder } = NativeModules;

export default {
  googleApiKey: null,
  amapApiKey: null,

  fallbackToGoogle(key) {
    this.googleApiKey = key;
  },
  
  fallbackToAmap(key) {
    this.amapApiKey = key;
  },

  geocodePosition(position) {
    if (!position || !position.lat || !position.lng) {
      return Promise.reject(new Error("invalid position: {lat, lng} required"));
    }

    return RNGeocoder.geocodePosition(position).catch(err => {
      if (!this.googleApiKey) { throw err; }
      return GoogleApi.geocodePosition(this.googleApiKey, position);
    });
  },

  geocodeAddress(address) {
    if (!address) {
      return Promise.reject(new Error("address is null"));
    }

    return RNGeocoder.geocodeAddress(address).catch(err => {
      if (this.googleApiKey) {
        return GoogleApi.geocodeAddress(this.googleApiKey, address);
      } else if (this.amapApiKey) {
        return AmapApi.geocodeAddress(this.amapApiKey, address);
      } else { 
        throw err; 
      }
    });
  },

  geocodeAutoComplete(queryFragment) {
    if (!queryFragment) {
      return Promise.reject(new Error("queryFragment param is null"));
    }
    return RNGeocoder.geocodeAutoComplete(queryFragment).catch(err => {
      if (this.googleApiKey) {
        return GoogleApi.geocodeAutoComplete(this.googleApiKey, queryFragment);
      } else if (this.amapApiKey) {
        return AmapApi.geocodeAutoComplete(this.amapApiKey, queryFragment);
      } else {
        throw err;
      }
    });
  },

  cancelAutoComplete() {
    return RNGeocoder.cancelAutoComplete();
  }
}

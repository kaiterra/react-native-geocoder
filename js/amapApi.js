const amapAutoCompleteURL = 'https://restapi.amap.com/v3/assistant/inputtips';
const amapGeocodeURl = 'https://restapi.amap.com/v3/geocode/geo';
const amapReverseGeocodingURL = 'https://restapi.amap.com/v3/geocode/regeo'

function mappedAmapLanguage(lang) {
  switch(lang) {
    case 'en-US':
      return 'en'
    case 'zh-CN':
    case 'zh-TW':
    default:
      return 'cn'
  }
}
export default {
  geocodePosition(apiKey, position, lang) {
    lang = mappedAmapLanguage(lang)
    
    if (!apiKey || !position || !position.lat || !position.lng) {
      return Promise.reject(new Error("invalid apiKey / position"));
    }

    return this.regeocodeRequest(`${amapReverseGeocodingURL}?key=${apiKey}&location=${position.lng},${position.lat}&language=${lang}`, position);
  },

  geocodeAddress(apiKey, address, lang) {
    lang = mappedAmapLanguage(lang)

    if (!apiKey || !address) {
      return Promise.reject(new Error("invalid apiKey / address"));
    }

    return this.geocodeRequest(`${amapGeocodeURl}?key=${apiKey}&address=${encodeURI(address)}&language=${lang}`);
  },

  // Amap API, see doc from https://lbs.amap.com/api/webservice/guide/api/inputtips
  geocodeAutoComplete(apiKey, query, lang) {
    lang = mappedAmapLanguage(lang)

    if (!apiKey || !query) {
      return Promise.reject(new Error("invalid apiKey / address"));
    }
    
    return this.autoCompleteRequest(`${amapAutoCompleteURL}?key=${apiKey}&keywords=${encodeURI(query)}&type=190100|190103|190104|190105&datatype=poi&language=${lang}`);
  },

  async autoCompleteRequest(url) {
    const res = await fetch(url);
    const json = await res.json();
  
    const tips = json.tips.map(tip => {
      let newTip = {}
      newTip.title = tip.name ? tip.name : null;
      newTip.subTitle = tip.district.length ? tip.district : (tip.address.length ? tip.address : null)
      return newTip;
    })
    if (!tips || json.info !== 'OK') {
      return Promise.reject(new Error(`geocoding error ${json.status}, ${json.info}`));
    } else {
      return tips;
    }
  },

  async geocodeRequest(url) {
    const res = await fetch(url);
    const json = await res.json();

    if (!json.geocodes || json.info !== 'OK') {
      return Promise.reject(new Error(`geocoding error ${json.status}, ${json.error_message}`));
    }
    const geocodes = json.geocodes.map(geocode => {
      let newGeocode = {};
      newGeocode.formattedAddress = geocode.formatted_address ? geocode.formatted_address : null;
      newGeocode.position = geocode.location.length ? {lng: parseFloat(geocode.location.split(',')[0]) , lat: parseFloat(geocode.location.split(',')[1]) } : {}
      newGeocode.feature = null;
      newGeocode.streetNumber = geocode.number.length ? geocode.number : null;
      newGeocode.streetName = geocode.street.length ? geocode.street : null;
      newGeocode.postalCode = null;
      newGeocode.locality = geocode.city.length ? geocode.city : null;
      newGeocode.country = null;
      newGeocode.countryCode = null;
      newGeocode.adminArea = geocode.province.length ? geocode.province : null;
      newGeocode.subAdminArea = null;
      newGeocode.subLocality = geocode.district.length ? geocode.district : null;
      return newGeocode;
    })
    return geocodes;
  },

  async regeocodeRequest(url, position) {
    console.log(url)

    const res = await fetch(url);
    const json = await res.json();

    if (!json.regeocode || json.info !== 'OK') {
      return Promise.reject(new Error(`geocoding error ${json.status}, ${json.info}`));
    }

    const regeocode = json.regeocode;
    const addressComponent = regeocode.addressComponent;
    const addresses = [{
      formattedAddress : regeocode.formatted_address ? regeocode.formatted_address : null,
      position : {lng: position.lng, lat: position.lat },
      feature : null,
      locality : addressComponent.city.length ? addressComponent.city : null,
      country : addressComponent.country.length ? addressComponent.country.length : null,
      countryCode : null,
      adminArea : addressComponent.province.length ? addressComponent.province : null,
      subAdminArea : null,
      subLocality : addressComponent.district.length ? addressComponent.district : null
    }]
    return addresses;
  }
}

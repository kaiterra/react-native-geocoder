#import "RNGeocoder.h"

#import <CoreLocation/CoreLocation.h>

#import <React/RCTConvert.h>

@implementation RCTConvert (CoreLocation)

+ (CLLocation *)CLLocation:(id)json
{
  json = [self NSDictionary:json];

  double lat = [RCTConvert double:json[@"lat"]];
  double lng = [RCTConvert double:json[@"lng"]];
  return [[CLLocation alloc] initWithLatitude:lat longitude:lng];
}

@end


@implementation RNGeocoder

RCT_EXPORT_MODULE();

- (void)completerDidUpdateResults:(MKLocalSearchCompleter *)completer
{
    if (self.completeResolve) {
        self.completeResolve([self searchCompletionsForDictionary:completer.results]);
        self.completeReject = NULL;
        self.completeResolve = NULL;
    }
    
    self.completer.delegate = nil;
    self.completer = nil;
}

- (void)completer:(MKLocalSearchCompleter *)completer didFailWithError:(NSError *)error
{
    if (self.completeReject) {
        self.completeReject(@"Error", @"Completer failed", error);
        self.completeReject = NULL;
        self.completeResolve = NULL;
    }
    
    self.completer.delegate = nil;
    self.completer = nil;
}

RCT_EXPORT_METHOD(geocodePosition:(CLLocation *)location
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  if (!self.geocoder) {
    self.geocoder = [[CLGeocoder alloc] init];
  }

  if (self.geocoder.geocoding) {
    [self.geocoder cancelGeocode];
  }

  [self.geocoder reverseGeocodeLocation:location completionHandler:^(NSArray *placemarks, NSError *error) {

    if (error) {
      if (placemarks.count == 0) {
          return reject(@"NOT_FOUND", @"geocodePosition failed", error);
      }

      return reject(@"ERROR", @"geocodePosition failed", error);
    }

    resolve([self placemarksToDictionary:placemarks]);

  }];
}

RCT_EXPORT_METHOD(geocodeAddress:(NSString *)address
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.geocoder) {
        self.geocoder = [[CLGeocoder alloc] init];
    }

    if (self.geocoder.geocoding) {
      [self.geocoder cancelGeocode];
    }

    if (self.localSearch) {
        [self.localSearch cancel];
        self.localSearch = nil;
    }
    MKLocalSearchRequest *request = [[MKLocalSearchRequest alloc] init];
    request.naturalLanguageQuery = address;
    self.localSearch = [[MKLocalSearch alloc] initWithRequest:request];
    [self.localSearch startWithCompletionHandler:^(MKLocalSearchResponse * _Nullable response, NSError * _Nullable error) {
        if (!error) {
            if (response.mapItems.count) {
                return resolve([self placemarksToDictionary:@[response.mapItems[0].placemark]]);
            } else {
                return reject(@"NOT_FOUND", @"geocodeAddress failed", error);
            }
        } else {
            return reject(@"ERROR", @"geocodeAddress failed", error);
        }
    }];

    /*
    [self.geocoder geocodeAddressString:address completionHandler:^(NSArray *placemarks, NSError *error) {

        if (error) {
            if (placemarks.count == 0) {
              return reject(@"NOT_FOUND", @"geocodeAddress failed", error);
            }

            return reject(@"ERROR", @"geocodeAddress failed", error);
        }

        resolve([self placemarksToDictionary:placemarks]);
  }];
     */
}

RCT_EXPORT_METHOD(cancelAutoComplete)
{
    if (self.completer) {
        self.completeReject = NULL;
        self.completeResolve = NULL;
        [self.completer cancel];
        self.completer.delegate = nil;
        self.completer = nil;
    }
}

RCT_EXPORT_METHOD(geocodeAutoComplete:(NSString *)queryFragment
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_sync(dispatch_get_main_queue(), ^{
        if (self.completer) {
            self.completeReject = NULL;
            self.completeResolve = NULL;
            [self.completer cancel];
            self.completer.delegate = nil;
            self.completer = nil;
        }
        self.completer = [[MKLocalSearchCompleter alloc] init];
        self.completer.delegate = (id)self;
        self.completer.filterType = MKSearchCompletionFilterTypeLocationsOnly;
        self.completeReject = reject;
        self.completeResolve = resolve;
        self.completer.queryFragment = queryFragment;
    });
}

- (NSArray *)placemarksToDictionary:(NSArray *)placemarks {

  NSMutableArray *results = [[NSMutableArray alloc] init];

  for (int i = 0; i < placemarks.count; i++) {
    CLPlacemark* placemark = [placemarks objectAtIndex:i];

    id name = [NSNull null];

    if (![placemark.name isEqualToString:placemark.locality] &&
        ![placemark.name isEqualToString:placemark.thoroughfare] &&
        ![placemark.name isEqualToString:placemark.subThoroughfare])
    {

        name = placemark.name;
    }

    NSArray *lines = placemark.addressDictionary[@"FormattedAddressLines"];

    NSDictionary *result = @{
     @"feature": name,
     @"position": @{
         @"lat": [NSNumber numberWithDouble:placemark.location.coordinate.latitude],
         @"lng": [NSNumber numberWithDouble:placemark.location.coordinate.longitude],
         },
     @"country": placemark.country ?: [NSNull null],
     @"countryCode": placemark.ISOcountryCode ?: [NSNull null],
     @"locality": placemark.locality ?: [NSNull null],
     @"subLocality": placemark.subLocality ?: [NSNull null],
     @"streetName": placemark.thoroughfare ?: [NSNull null],
     @"streetNumber": placemark.subThoroughfare ?: [NSNull null],
     @"postalCode": placemark.postalCode ?: [NSNull null],
     @"adminArea": placemark.administrativeArea ?: [NSNull null],
     @"subAdminArea": placemark.subAdministrativeArea ?: [NSNull null],
     @"formattedAddress": [lines componentsJoinedByString:@", "] ?: [NSNull null]
   };

    [results addObject:result];
  }

  return results;
}

- (NSArray *)searchCompletionsForDictionary:(NSArray *)completions {
    NSMutableArray *results = [[NSMutableArray alloc] init];
    
    for (int i = 0; i < completions.count; i++) {
        MKLocalSearchCompletion* completion = [completions objectAtIndex:i];
        
        NSDictionary *result = @{@"title": completion.title.length ? completion.title : [NSNull null],
                                 @"subTitle": completion.subtitle.length ? completion.subtitle : [NSNull null]
                                 };
        
        [results addObject:result];
    }
    
    return results;
}
@end

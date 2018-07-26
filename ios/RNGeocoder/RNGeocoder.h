#import <React/RCTBridgeModule.h>
#import <React/RCTConvert.h>

#import <CoreLocation/CoreLocation.h>
#import <MapKit/MapKit.h>

@interface RCTConvert (CoreLocation) <MKLocalSearchCompleterDelegate>
+ (CLLocation *)CLLocation:(id)json;
@end

@interface RNGeocoder : NSObject<RCTBridgeModule>
@property (nonatomic, strong) CLGeocoder *geocoder;
@property (nonatomic, copy) RCTPromiseResolveBlock completeResolve;
@property (nonatomic, copy) RCTPromiseRejectBlock completeReject;
@property (nonatomic, strong) MKLocalSearchCompleter *completer;
@end

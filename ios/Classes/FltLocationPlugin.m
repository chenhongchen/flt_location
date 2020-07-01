#import "FltLocationPlugin.h"
#import <CoreLocation/CoreLocation.h>
#import <MapKit/MapKit.h>

#define DEFAULTSPAN 500

@interface FltLocationPlugin ()<CLLocationManagerDelegate>
@property (nonatomic, strong) NSArray *placeItems;
@property (strong, nonatomic) CLLocationManager *locManager;
@property (nonatomic, strong) CLGeocoder *geocoder;
@property (nonatomic, strong) CLLocation *location;
@property (nonatomic, copy) FlutterResult result;
@property (nonatomic, copy) NSString *curMethdName;
@property (nonatomic, copy) NSString *searchKey;
@property (nonatomic, strong) NSArray<NSString*> *curCoordinate;
@property (nonatomic, strong) NSDictionary *curLocation;
@property (nonatomic, strong) NSArray *userDefaultLanguages; // 系统默认的语言
@end

@implementation FltLocationPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"flt_location/method"
            binaryMessenger:[registrar messenger]];
    FltLocationPlugin* instance = [[FltLocationPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSDictionary *argsMap = call.arguments;
    self.curMethdName = call.method;
    self.userDefaultLanguages = [[NSUserDefaults standardUserDefaults] objectForKey:@"AppleLanguages"];
    if ([@"getCurLocations" isEqualToString:call.method]) {
        [self getCurLocations:result];
    }
    else if ([@"searchLocation" isEqualToString:call.method]) {
        [self searchLocation:argsMap result:result];
    }
    else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)getCurLocations:(FlutterResult)result {
    CLAuthorizationStatus status = [CLLocationManager authorizationStatus];
    if (kCLAuthorizationStatusDenied == status || kCLAuthorizationStatusRestricted == status) {
        self.result = nil;
        if (result) {
            result(@{@"err":@"没有授权"});
        }
    }
    else {
        [self.locManager startUpdatingLocation];
        self.result = result;
    }
}

- (void)searchLocation:(NSDictionary *)argsMap result:(FlutterResult)result {
    CLAuthorizationStatus status = [CLLocationManager authorizationStatus];
    if (kCLAuthorizationStatusDenied == status || kCLAuthorizationStatusRestricted == status) {
        self.result = nil;
        if (result) {
            result(@{@"err":@"没有授权"});
        }
    }
    else {
        self.searchKey = argsMap[@"key"];
        [self.locManager startUpdatingLocation];
        self.result = result;
    }
}

#pragma mark - 初始化
- (instancetype)init
{
    self = [super init];
    if (self) {
        _placeItems = @[];
        _searchKey = @"";
        _curMethdName = @"";
        if ([self.locManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
            [self.locManager requestWhenInUseAuthorization];
        }
    }
    return self;
}

#pragma mark - 懒加载
- (CLLocationManager *)locManager
{
    if (_locManager == nil) {
        _locManager = [[CLLocationManager alloc] init];
        _locManager.delegate = self;
    }
    return _locManager;
}

#pragma mark - LLocationManagerDelegate
- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    [manager stopUpdatingLocation];
    // 1.获取用户位置的对象
    _location = manager.location;
    //获取坐标
    CLLocationCoordinate2D coordinate = _location.coordinate;
    //打印地址
    NSLog(@"latitude = %lf longtude = %lf",coordinate.latitude,coordinate.longitude);
    
    if (CLLocationCoordinate2DIsValid(coordinate)) {
        NSString *longi = [NSString stringWithFormat:@"%@", @(coordinate.longitude)];
        NSString *lati = [NSString stringWithFormat:@"%@", @(coordinate.latitude)];
        _curCoordinate = @[longi, lati];
    }
    
    if ([self.curMethdName isEqualToString:@"getCurLocations"]) {
        // 地址的编码通过经纬度得到具体的地址
        CLGeocoder *gecoder = [[CLGeocoder alloc] init];
        __weak typeof(self) weakSelf = self;
        [gecoder reverseGeocodeLocation:_location completionHandler:^(NSArray *placemarks, NSError *error) {
            weakSelf.placeItems = [weakSelf getCurrentPlaces:placemarks];
            weakSelf.curLocation = weakSelf.placeItems.firstObject;
            [weakSelf getAroundInfoMationWithCoordinate:coordinate withKey:@"food"];
            [[NSUserDefaults standardUserDefaults] setObject: weakSelf.userDefaultLanguages forKey:@"AppleLanguages"];
        }];
    }
    else if ([self.curMethdName isEqualToString:@"searchLocation"]){
        [self getAroundInfoMationWithCoordinate:coordinate withKey:self.searchKey];
    }
    else {
        self.curMethdName = @"";
        self.searchKey = @"";
        self.placeItems = @[];
        if (self.result) {
            self.result(@{});
        }
    }
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
    [[NSUserDefaults standardUserDefaults] setObject: self.userDefaultLanguages forKey:@"AppleLanguages"];
    if (self.result) {
        self.result(@{});
    }
}

- (void)getAroundInfoMationWithCoordinate:(CLLocationCoordinate2D)coordinate withKey:(NSString *)key
{
    MKCoordinateRegion region = MKCoordinateRegionMakeWithDistance(coordinate, DEFAULTSPAN, DEFAULTSPAN);
    MKLocalSearchRequest *request = [[MKLocalSearchRequest alloc] init];
    request.region = region;
    request.naturalLanguageQuery = key;
    MKLocalSearch *localSearch = [[MKLocalSearch alloc] initWithRequest:request];
    __weak typeof(self) weakSelf = self;
    [localSearch startWithCompletionHandler:^(MKLocalSearchResponse *response, NSError *error){
        [[NSUserDefaults standardUserDefaults] setObject: weakSelf.userDefaultLanguages forKey:@"AppleLanguages"];
        if (!error) {
            [weakSelf getAroundPlaces:response.mapItems];
        }else{
            if (self.result) {
                self.result(@{});
            }
            NSLog(@"Quest around Error:%@",error.localizedDescription);
        }
        self.curMethdName = @"";
        self.searchKey = @"";
        self.placeItems = @[];
    }];
}

- (NSArray *)getCurrentPlaces:(NSArray <CLPlacemark *> *)places
{
    NSMutableArray *placeItemsM = [NSMutableArray array];
    for (CLPlacemark *placemark in places) {
        NSMutableDictionary *dictM = [NSMutableDictionary dictionary];
        dictM[@"name"] = placemark.name; // 地点
        dictM[@"countryCode"] = placemark.ISOcountryCode.uppercaseString; // 国家码
        dictM[@"country"] = placemark.country; // 国家
        dictM[@"province"] = placemark.administrativeArea; // 省
        dictM[@"locality"] = placemark.locality; // 城市
        dictM[@"subLocality"] = placemark.subLocality; // 区
        dictM[@"thoroughfare"] = placemark.thoroughfare; // 街道
        dictM[@"subThoroughfare"] = placemark.subThoroughfare; // 门牌号
        if (CLLocationCoordinate2DIsValid(placemark.location.coordinate)) {
            NSString *longi = [NSString stringWithFormat:@"%@", @(placemark.location.coordinate.longitude)];
            NSString *lati = [NSString stringWithFormat:@"%@", @(placemark.location.coordinate.latitude)];
            dictM[@"coordinate"] = @[longi, lati];
        }
        [placeItemsM addObject:dictM];
    }
    return placeItemsM;
}

//{
//    "locality" : "Nanchang",
//    "country" : "China",
//    "subLocality" : "Qingyunpu",
//    "subThoroughfare" : "No.135",
//    "countryCode" : "CN",
//    "thoroughfare" : "Hongdu South Avenue",
//    "name" : "No.135 Hongdu South Avenue",
//    "province" : "Jiangxi"
//}

- (void)getAroundPlaces:(NSArray <MKMapItem *> *)places
{
    NSMutableArray *placeItemsM = [NSMutableArray arrayWithArray:self.placeItems];
    for (MKMapItem *item in places) {
        MKPlacemark * placemark = item.placemark;
        NSMutableDictionary *dictM = [NSMutableDictionary dictionary];
        dictM[@"name"] = placemark.name; // 地点
        dictM[@"countryCode"] = placemark.ISOcountryCode.uppercaseString; // 国家码
        dictM[@"country"] = placemark.country; // 国家
        dictM[@"province"] = placemark.administrativeArea; // 省
        dictM[@"locality"] = placemark.locality; // 城市
        dictM[@"subLocality"] = placemark.subLocality; // 区
        dictM[@"thoroughfare"] = placemark.thoroughfare; // 街道
        dictM[@"subThoroughfare"] = placemark.subThoroughfare; // 门牌号
        if (CLLocationCoordinate2DIsValid(placemark.location.coordinate)) {
            NSString *longi = [NSString stringWithFormat:@"%@", @(placemark.location.coordinate.longitude)];
            NSString *lati = [NSString stringWithFormat:@"%@", @(placemark.location.coordinate.latitude)];
            dictM[@"coordinate"] = @[longi, lati];
        }
        BOOL isRepeat = NO;
        for (NSDictionary *dict in self.placeItems) {
            if ([dict[@"name"] isEqualToString:dictM[@"name"]]) {
                isRepeat = YES;
                break;
            }
        }
        if (isRepeat) {
            continue;
        }
        [placeItemsM addObject:dictM];
    }
    self.placeItems = placeItemsM;
    
    if (self.result) {
        NSMutableDictionary *dictM = [NSMutableDictionary dictionary];
        if ([_placeItems isKindOfClass:[NSArray class]]) {
            dictM[@"locations"] = _placeItems;
        }
        if ([_curLocation isKindOfClass:[NSDictionary class]]) {
            dictM[@"curLocation"] = _curLocation;
        }
        self.result(@{@"value" : dictM});
    }
}

@end

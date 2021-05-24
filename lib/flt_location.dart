import 'dart:async';

import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class FltLocation {
  static const MethodChannel _channel =
      const MethodChannel('flt_location/method');

  /*
  * 当前位置及周边附近
  * return:
  * 成功： {'value' : {'locations':[location], 'curLocation':'{location}'}}
  * 'location':{'name':'xx','countryCode':'xx','country':'xx',
  *             'province':'xx','locality':'xx','subLocality':'xx','thoroughfare':'xx',
  *             'subThoroughfare':'xx','coordinate':['longitude','latitude']}
  * 失败：{'err':'xx'}
  * */
  static Future<Map> get curLocations async {
    var locationAlways = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.locationAlways);
    var locationWhenInUse = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.locationWhenInUse);
    if (locationAlways != PermissionStatus.granted &&
        locationWhenInUse != PermissionStatus.granted) {
      return null;
    }

    Map locations = await _channel.invokeMethod('getCurLocations');
    return locations;
  }

/*
  * 获取当前位置
  * return:
  * 成功： {'value' : {'locations':location}
  * 'location':{'name':'xx','countryCode':'xx','country':'xx',
  *             'province':'xx','locality':'xx','subLocality':'xx','thoroughfare':'xx',
  *             'subThoroughfare':'xx','coordinate':['longitude','latitude']}
  * 失败：{'err':'xx'}
  * */
  static Future<Map> get getLocation async {
    var locationAlways = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.locationAlways);
    var locationWhenInUse = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.locationWhenInUse);
    if (locationAlways != PermissionStatus.granted &&
        locationWhenInUse != PermissionStatus.granted) {
      return null;
    }

    Map locations = await _channel.invokeMethod('getLocation');
    return locations;
  }
  /*
  * 搜索周边位置
  * argsMap: 要搜索的字符串，格式{'key':'xxxx'}
  * return:
  * 成功： {'value' : {'locations':[location]}}
  * 'location':{'name':'xx','countryCode':'xx','country':'xx',
  *             'province':'xx','locality':'xx','subLocality':'xx','thoroughfare':'xx',
  *             'subThoroughfare':'xx','coordinate':['longitude','latitude']}
  * 失败：{'err':'xx'}
  * */
  static Future<Map> searchLocation(Map argsMap) async {
    var locationAlways = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.locationAlways);
    var locationWhenInUse = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.locationWhenInUse);
    if (locationAlways != PermissionStatus.granted &&
        locationWhenInUse != PermissionStatus.granted) {
      return null;
    }

    Map locations = await _channel.invokeMethod('searchLocation', argsMap);
    return locations;
  }

  static Future<Map> getPlaceDetail(Map argsMap) async {
    Map locations = await _channel.invokeMethod('getplacedetail', argsMap);
    return locations;
  }
}

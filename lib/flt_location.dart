import 'dart:async';

import 'package:flutter/services.dart';

class FltLocation {
  static const MethodChannel _channel =
      const MethodChannel('flt_location/method');

  /*
  * 当前位置及周边附近
  * return:
  * 成功： {'value' : {'locations':[{}], 'coordinate':['latitude','longitude']}}
  * 'locations':{'name':'xx','thoroughfare':'xx','subThoroughfare':'xx','locality':'xx','coordinate':['latitude','longitude']}
  * 失败：{}
  * */
  static Future<Map> get curLocations async {
    Map locations = await _channel.invokeMethod('getCurLocations');
    return locations;
  }

  /*
  * 搜索周边位置
  * argsMap: 要搜索的字符串，格式{'key':'xxxx'}
  * return:
  * 成功： {'value' : {'locations':[{}], 'coordinate':['latitude','longitude']}}
  * 'locations':{'name':'xx','thoroughfare':'xx','subThoroughfare':'xx','locality':'xx','coordinate':['latitude','longitude']}
  * 失败：{}
  * */
  static Future<Map> searchLocation(Map argsMap) async {
    Map locations = await _channel.invokeMethod('searchLocation', argsMap);
    return locations;
  }
}

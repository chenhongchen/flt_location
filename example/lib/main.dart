import 'package:flutter/material.dart';
import 'package:flt_location/flt_location.dart';
import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    _requestLocationAlways();
  }

  _requestLocationAlways() async {
    try {
      await Permission.locationAlways.request();
    } catch (e) {}
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: <Widget>[
            GestureDetector(
              child: Center(
                child: Text('获取当前位置及周边位置列表'),
              ),
              onTap: () {
                _getCurPositions();
              },
            ),
            GestureDetector(
              child: Center(
                child: Text('获取当前位置'),
              ),
              onTap: () {
                _getLocation();
              },
            ),
            GestureDetector(
              child: Center(
                child: Text('搜索 key == 大厦'),
              ),
              onTap: () {
                _searchPosition();
              },
            ),
            GestureDetector(
              child: Center(
                child: Text('获取定位'),
              ),
              onTap: () {
                _getLocation();
              },
            )
          ],
        ),
      ),
    );
  }

  _getCurPositions() async {
    var res = await FltLocation.curLocations;
    debugPrint('_getCurPositions -- $res');
  }

  _getLocation() async {
    var res = await FltLocation.getLocation;
    debugPrint('_getLocation -- $res');
  }

  _searchPosition() async {
    var res = await FltLocation.searchLocation({'key': '大厦'});
    debugPrint('_getCurPositions -- $res');
  }
}

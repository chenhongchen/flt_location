import 'package:flutter/material.dart';
import 'package:flt_location/flt_location.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
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
                child: Text('获取当前位置列表'),
              ),
              onTap: () {
                _getCurPositions();
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
          ],
        ),
      ),
    );
  }

  _getCurPositions() async {
    var res = await FltLocation.curLocations;
    debugPrint('_getCurPositions -- $res');
  }

  _searchPosition() async {
    var res = await FltLocation.searchLocation({'key': '大厦'});
    debugPrint('_getCurPositions -- $res');
  }
}

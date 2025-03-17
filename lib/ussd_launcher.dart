import 'package:flutter/services.dart';
import 'dart:developer' as developer;

class UssdLauncher {

  static const MethodChannel _channel = MethodChannel('ussd_launcher');


  static Future<String?> sendUssdRequest({
    required String ussdCode,
    required int subscriptionId,
  }) async {
    try {
      final String? response = await _channel.invokeMethod('sendUssdRequest', {
        'ussdCode': ussdCode,
        'subscriptionId': subscriptionId,
      });
      return response;
    } on PlatformException catch (e) {
      print("Erreur lors de l'envoi de la requête USSD : ${e.message}");
      return null;
    }
  }

  static Future<bool> requestAccessibilityPermission() async {
     try {
      final bool isGranted = await _channel.invokeMethod('requestAccessibilityPermission');
       return isGranted;
    } on PlatformException catch (e) {
      print("Failed to request accessibility permission : ${e.message}");
      return false;
    }
  }

  
  static Future<String?> multisessionUssd(
      {String? code, int? slotIndex, List<String>? options}) async {
    try {
      print("................................... code : $code");
      developer.log("................. slotIndex : $slotIndex");
      developer.log("................. options : $options");

      final String? result = await _channel.invokeMethod('multisessionUssd', {
        'ussdCode': code,
        'slotIndex': slotIndex,
        'options': options,
      });

      developer.log("............................. result : $result");
      return result;
    } on PlatformException catch (e) {
      print("Échec du lancement de la session USSD multi-étapes : '${e.message}'.");
      rethrow;
    }
  }

 
  static void setUssdMessageListener(Function(String) listener) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onUssdMessageReceived') {
        final String ussdMessage = call.arguments;
        listener(ussdMessage);
      }
    });
  }

 
}

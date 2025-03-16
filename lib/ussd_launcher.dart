import 'package:flutter/services.dart';
import 'dart:developer' as developer;

class UssdLauncher {
  // Canal de méthode pour communiquer avec le code natif
  static const MethodChannel _channel = MethodChannel('ussd_launcher');

// Lance une requête USSD en session unique 
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

  // Lance une requête USSD en session unique 
  // static Future<String> launchUssd(String ussdCode, {int subscriptionId = -1}) async {
  // static Future<String> launchUssd(String ussdCode, int? subscriptionId) async {
  //   try {
  //     final String response = await _channel.invokeMethod('sendUssdRequest', {
  //       'ussdCode': ussdCode,
  //       'subscriptionId': subscriptionId,
  //     });

  //     return response;
  //   } on PlatformException catch (e) {
  //     throw Exception('Failed to send USSD request: ${e.message}');
  //   }
  // }

  // Lance une session USSD multi-étapes avec des options de menu
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

  // Envoie un message/une commande dans une session USSD multi-étapes
  // static Future<String?> sendMessage(String message) async {
  //   try {
  //     final String? result =
  //         await _channel.invokeMethod('sendMessage', {'message': message});
  //     developer.log("............................. message : $message");
  //     developer.log("............................. result : $result");
  //     return result;
  //   } on PlatformException catch (e) {
  //     print("Failed to send USSD message: '${e.message}'.");
  //     rethrow;
  //   }
  // }

  // Annule la session USSD en cours
  // static Future<void> cancelSession() async {
  //   try {
  //     await _channel.invokeMethod('cancelSession');
  //   } on PlatformException catch (e) {
  //     print("Failed to cancel USSD session: '${e.message}'.");
  //     rethrow;
  //   }
  // }

  /// Définit un listener pour les messages USSD reçus depuis le backend.
  static void setUssdMessageListener(Function(String) listener) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onUssdMessageReceived') {
        final String ussdMessage = call.arguments;
        listener(ussdMessage);
      }
    });
  }

  // Vérifie si l'autorisation d'accessibilité est activée
  // static Future<bool> isAccessibilityPermissionEnabled() async {
  //   try {
  //     final bool isEnabled =
  //         await _channel.invokeMethod('isAccessibilityPermissionEnabled');
  //     return isEnabled;
  //   } on PlatformException catch (e) {
  //     print("Failed to check accessibility permission: '${e.message}'.");
  //     return false;
  //   }
  // }

  // Ouvre les paramètres d'accessibilité
  // static Future<void> openAccessibilitySettings() async {
  //   try {
  //     await _channel.invokeMethod('openAccessibilitySettings');
  //   } on PlatformException catch (e) {
  //     print("Failed to open accessibility settings: '${e.message}'.");
  //     rethrow;
  //   }
  // }

  static Future<List<Map<String, dynamic>>> getSimCards() async {
    try {
      final List<dynamic> result = await _channel.invokeMethod('getSimCards');
      return result.map((item) => Map<String, dynamic>.from(item)).toList();
    } on PlatformException catch (e) {
      print("Failed to get SIM cards info: '${e.message}'.");
      return [];
    }
  }
}

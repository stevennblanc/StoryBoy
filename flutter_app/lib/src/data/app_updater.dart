import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';

const updateManifestUrl =
    'https://github.com/stevennblanc/StoryBoy/releases/latest/download/update.json';

class UpdateManifest {
  const UpdateManifest({
    required this.versionCode,
    required this.versionName,
    required this.apkUrl,
    required this.releaseNotes,
  });

  final int versionCode;
  final String versionName;
  final String apkUrl;
  final String releaseNotes;

  factory UpdateManifest.fromJson(Map<String, dynamic> json) {
    return UpdateManifest(
      versionCode: (json['versionCode'] as num?)?.toInt() ?? 0,
      versionName: (json['versionName'] as String?) ?? '',
      apkUrl: (json['apkUrl'] as String?) ?? '',
      releaseNotes: (json['releaseNotes'] as String?) ?? '',
    );
  }
}

/// In-app updater: reads update.json from the latest GitHub release, and
/// installs a newer APK through the platform package installer.
class AppUpdater {
  static const _channel = MethodChannel('storyboy/updater');

  /// Returns a manifest when a newer build is available, otherwise null.
  Future<UpdateManifest?> checkForUpdate() async {
    final response = await http.get(Uri.parse(updateManifestUrl));
    if (response.statusCode != 200) {
      throw HttpException('Update check failed with ${response.statusCode}');
    }
    final manifest = UpdateManifest.fromJson(
      (jsonDecode(response.body) as Map).cast<String, dynamic>(),
    );
    final info = await PackageInfo.fromPlatform();
    final currentCode = int.tryParse(info.buildNumber) ?? 0;
    return manifest.versionCode > currentCode ? manifest : null;
  }

  Future<File> downloadUpdate(UpdateManifest manifest) async {
    final response = await http.get(Uri.parse(manifest.apkUrl));
    if (response.statusCode != 200) {
      throw HttpException('Update download failed with ${response.statusCode}');
    }
    final cacheDir = await getTemporaryDirectory();
    final apkFile = File('${cacheDir.path}/updates/storyboy-update.apk');
    await apkFile.create(recursive: true);
    await apkFile.writeAsBytes(response.bodyBytes);
    return apkFile;
  }

  Future<void> installUpdate(File apkFile) async {
    await _channel.invokeMethod('installApk', {'path': apkFile.path});
  }
}

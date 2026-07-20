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

  /// Streams the APK download, reporting progress in [0, 1] when the server
  /// sends a content length (null-progress ticks otherwise). GitHub release
  /// asset URLs redirect to a CDN, which http follows automatically.
  Future<File> downloadUpdate(
    UpdateManifest manifest, {
    void Function(double? progress)? onProgress,
  }) async {
    final client = http.Client();
    try {
      final request = http.Request('GET', Uri.parse(manifest.apkUrl));
      final response = await client.send(request);
      if (response.statusCode != 200) {
        throw HttpException('Update download failed with ${response.statusCode}');
      }

      final cacheDir = await getTemporaryDirectory();
      final apkFile = File('${cacheDir.path}/updates/storyboy-update.apk');
      await apkFile.create(recursive: true);
      final sink = apkFile.openWrite();

      final total = response.contentLength;
      var received = 0;
      onProgress?.call(total != null && total > 0 ? 0 : null);
      try {
        await for (final chunk in response.stream) {
          sink.add(chunk);
          received += chunk.length;
          if (total != null && total > 0) {
            onProgress?.call(received / total);
          } else {
            onProgress?.call(null);
          }
        }
      } finally {
        await sink.close();
      }
      onProgress?.call(1);
      return apkFile;
    } finally {
      client.close();
    }
  }

  Future<void> installUpdate(File apkFile) async {
    await _channel.invokeMethod('installApk', {'path': apkFile.path});
  }
}

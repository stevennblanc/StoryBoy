import 'package:shared_preferences/shared_preferences.dart';

/// Reading progress and app preferences, stored on-device.
class ProgressStore {
  ProgressStore(this._prefs);

  final SharedPreferences _prefs;

  static Future<ProgressStore> load() async {
    return ProgressStore(await SharedPreferences.getInstance());
  }

  String? currentNode(String bookId) => _prefs.getString('progress.$bookId.node');

  Future<void> saveCurrentNode(String bookId, String nodeId) async {
    await _prefs.setString('progress.$bookId.node', nodeId);
  }

  Set<String> collectedIds(String bookId, String collection) {
    return (_prefs.getStringList('progress.$bookId.$collection') ?? const []).toSet();
  }

  Future<void> saveCollectedIds(String bookId, String collection, Set<String> ids) async {
    await _prefs.setStringList('progress.$bookId.$collection', ids.toList());
  }

  bool hasProgress(String bookId) => currentNode(bookId) != null;

  Future<void> reset(String bookId) async {
    await _prefs.remove('progress.$bookId.node');
    await _prefs.remove('progress.$bookId.inventory');
    await _prefs.remove('progress.$bookId.evidence');
  }

  double get fontScale => _prefs.getDouble('settings.fontScale') ?? 1.0;

  Future<void> saveFontScale(double scale) async {
    await _prefs.setDouble('settings.fontScale', scale);
  }
}

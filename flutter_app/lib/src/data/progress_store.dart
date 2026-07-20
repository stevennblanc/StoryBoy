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

  /// Per-playthrough stat values, stored as "id=value" pairs. Empty when the
  /// playthrough has no saved stats yet (caller seeds from stat defaults).
  Map<String, int> stats(String bookId) {
    final raw = _prefs.getStringList('progress.$bookId.stats') ?? const [];
    final result = <String, int>{};
    for (final pair in raw) {
      final index = pair.indexOf('=');
      if (index <= 0) continue;
      final value = int.tryParse(pair.substring(index + 1));
      if (value != null) result[pair.substring(0, index)] = value;
    }
    return result;
  }

  Future<void> saveStats(String bookId, Map<String, int> values) async {
    await _prefs.setStringList(
      'progress.$bookId.stats',
      [for (final entry in values.entries) '${entry.key}=${entry.value}'],
    );
  }

  bool hasProgress(String bookId) => currentNode(bookId) != null;

  Future<void> reset(String bookId) async {
    await _prefs.remove('progress.$bookId.node');
    await _prefs.remove('progress.$bookId.inventory');
    await _prefs.remove('progress.$bookId.evidence');
    await _prefs.remove('progress.$bookId.stats');
  }

  double get fontScale => _prefs.getDouble('settings.fontScale') ?? 1.0;

  Future<void> saveFontScale(double scale) async {
    await _prefs.setDouble('settings.fontScale', scale);
  }

  String get displayMode => _prefs.getString('settings.displayMode') ?? 'grid';

  Future<void> saveDisplayMode(String mode) async {
    await _prefs.setString('settings.displayMode', mode);
  }
}

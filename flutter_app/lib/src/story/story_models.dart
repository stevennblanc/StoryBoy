/// Runtime story models, mirroring docs/GAMEBOOK_FORMAT.md.
library;

class StoryGamebook {
  const StoryGamebook({
    required this.id,
    required this.title,
    required this.startNodeId,
    required this.nodes,
    required this.inventoryCatalog,
    required this.evidenceCatalog,
    required this.inventoryConfig,
    required this.evidenceConfig,
  });

  final String id;
  final String title;
  final String startNodeId;
  final Map<String, StoryNode> nodes;
  final Map<String, CollectionItem> inventoryCatalog;
  final Map<String, CollectionItem> evidenceCatalog;
  final CollectionConfig inventoryConfig;
  final CollectionConfig evidenceConfig;

  StoryNode node(String id) {
    final node = nodes[id];
    if (node == null) throw StateError('Story node not found: $id');
    return node;
  }
}

class StoryNode {
  const StoryNode({
    required this.id,
    required this.type,
    required this.text,
    this.images = const [],
    this.choices = const [],
    this.inventoryGained = const [],
    this.evidenceGained = const [],
    this.mapLocations = const [],
    this.battle,
    this.acceptedAnswers = const [],
    this.correctTargetId,
    this.incorrectTargetId,
  });

  final String id;
  final String type;
  final String text;
  final List<StoryImage> images;
  final List<StoryChoice> choices;
  final List<CollectionItem> inventoryGained;
  final List<CollectionItem> evidenceGained;
  final List<MapLocation> mapLocations;
  final BattleConfig? battle;
  final List<String> acceptedAnswers;
  final String? correctTargetId;
  final String? incorrectTargetId;

  bool get isEnding =>
      type != 'puzzle' && type != 'map' && type != 'battle' && choices.isEmpty;
}

class StoryChoice {
  const StoryChoice({required this.text, required this.targetId});

  final String text;
  final String targetId;
}

class StoryImage {
  const StoryImage({required this.path, this.caption = ''});

  final String path;
  final String caption;
}

class MapLocation {
  const MapLocation({required this.title, required this.description, required this.targetId});

  final String title;
  final String description;
  final String targetId;
}

/// Shared shape for inventory items and evidence entries.
class CollectionItem {
  const CollectionItem({
    required this.id,
    required this.title,
    this.description = '',
    this.detail = '',
    this.image,
  });

  final String id;
  final String title;
  final String description;
  final String detail;
  final String? image;

  bool get hasMore => detail.isNotEmpty || image != null;
}

/// Book-defined presentation for a collection system (label, count, enabled).
class CollectionConfig {
  const CollectionConfig({
    required this.label,
    this.showCount = true,
    this.enabled = false,
  });

  final String label;
  final bool showCount;
  final bool enabled;

  String buttonLabel(int count) => showCount ? '$label $count' : label;
}

class BattleConfig {
  const BattleConfig({
    required this.playerDice,
    required this.opponentDice,
    required this.playerBonus,
    required this.opponentBonus,
    required this.winTargetId,
    required this.loseTargetId,
    this.drawTargetId,
    this.itemModifiers = const [],
  });

  final String playerDice;
  final String opponentDice;
  final int playerBonus;
  final int opponentBonus;
  final String winTargetId;
  final String loseTargetId;
  final String? drawTargetId;
  final List<BattleModifier> itemModifiers;
}

class BattleModifier {
  const BattleModifier({required this.itemId, required this.bonus, required this.description});

  final String itemId;
  final int bonus;
  final String description;
}

class BattleResult {
  const BattleResult({
    required this.playerRolls,
    required this.playerBonus,
    required this.opponentRolls,
    required this.opponentBonus,
    required this.appliedModifiers,
    required this.outcome,
    required this.targetId,
  });

  final List<int> playerRolls;
  final int playerBonus;
  final List<int> opponentRolls;
  final int opponentBonus;
  final List<BattleModifier> appliedModifiers;
  final String outcome;
  final String targetId;

  int get playerTotal => playerRolls.fold(0, (a, b) => a + b) + playerBonus;
  int get opponentTotal => opponentRolls.fold(0, (a, b) => a + b) + opponentBonus;
}

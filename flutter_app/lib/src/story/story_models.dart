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
    this.stats = const [],
  });

  final String id;
  final String title;
  final String startNodeId;
  final Map<String, StoryNode> nodes;
  final Map<String, CollectionItem> inventoryCatalog;
  final Map<String, CollectionItem> evidenceCatalog;
  final CollectionConfig inventoryConfig;
  final CollectionConfig evidenceConfig;

  /// Book-defined numeric stats (HP, Armor Class, gold, or any renamed
  /// equivalent). Empty when the book uses no stat systems.
  final List<StatDef> stats;

  StatDef? get healthStat {
    for (final stat in stats) {
      if (stat.role == StatRole.health) return stat;
    }
    return null;
  }

  StoryNode node(String id) {
    final node = nodes[id];
    if (node == null) throw StateError('Story node not found: $id');
    return node;
  }
}

enum StatRole { normal, health }

/// A named numeric stat with a default label the book may override, a starting
/// value, an optional cap, and an optional [StatRole.health] flag that makes
/// hitting zero end the run.
class StatDef {
  const StatDef({
    required this.id,
    required this.label,
    required this.start,
    this.max,
    this.role = StatRole.normal,
    this.hidden = false,
  });

  final String id;
  final String label;
  final int start;
  final int? max;
  final StatRole role;
  final bool hidden;

  String display(int value) {
    if (max != null) return '$label $value/$max';
    return '$label $value';
  }
}

/// A change applied to a stat when a node is entered.
class StatChange {
  const StatChange({required this.statId, required this.amount, this.set = false});

  final String statId;
  final int amount;
  final bool set;
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
    this.check,
    this.combat,
    this.statChanges = const [],
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
  final CheckConfig? check;
  final CombatConfig? combat;
  final List<StatChange> statChanges;
  final List<String> acceptedAnswers;
  final String? correctTargetId;
  final String? incorrectTargetId;

  bool get isEnding =>
      type != 'puzzle' &&
      type != 'map' &&
      type != 'battle' &&
      type != 'check' &&
      type != 'combat' &&
      choices.isEmpty;
}

/// A single-roll test (saving throw / luck roll): roll [dice] + [modifier],
/// meet or beat [target] to route to [successTargetId], else [failureTargetId].
class CheckConfig {
  const CheckConfig({
    required this.dice,
    required this.modifier,
    required this.target,
    required this.successTargetId,
    required this.failureTargetId,
    this.statModifier,
    this.successLabel = 'Success',
    this.failureLabel = 'Failure',
  });

  final String dice;
  final int modifier;

  /// Optional stat whose value is added to the roll (e.g. a skill or bonus).
  final String? statModifier;
  final int target;
  final String successTargetId;
  final String failureTargetId;
  final String successLabel;
  final String failureLabel;
}

/// Round-based combat. The player rolls to hit the enemy; the enemy rolls to
/// hit the player's health stat. Reaching zero enemy HP routes to
/// [winTargetId]; the player's health stat reaching zero routes to
/// [loseTargetId].
class CombatConfig {
  const CombatConfig({
    required this.enemyLabel,
    required this.enemyHp,
    required this.enemyHitTarget,
    required this.enemyDamage,
    required this.playerDamage,
    required this.playerDamageBonus,
    required this.playerHitBonus,
    required this.monsterHitsOn,
    required this.winTargetId,
    required this.loseTargetId,
    required this.healthStatId,
    this.fleeTargetId,
    this.talkTargetId,
    this.talkLabel = 'Talk',
  });

  final String enemyLabel;
  final int enemyHp;

  /// Number the player must roll (1d20 + [playerHitBonus]) to hit the enemy.
  final int enemyHitTarget;
  final String enemyDamage;
  final String playerDamage;
  final int playerDamageBonus;
  final int playerHitBonus;

  /// Number the enemy must roll (1d20) to hit the player.
  final int monsterHitsOn;
  final String winTargetId;
  final String loseTargetId;
  final String healthStatId;
  final String? fleeTargetId;
  final String? talkTargetId;
  final String talkLabel;
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

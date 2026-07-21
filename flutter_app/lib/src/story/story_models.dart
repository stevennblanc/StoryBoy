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
    this.equipmentCatalog = const {},
    this.equipmentConfig = const CollectionConfig(label: 'Equipment'),
    this.mapCatalog = const {},
    this.mapConfig = const CollectionConfig(label: 'Map'),
    this.stats = const [],
    this.characters = const [],
  });

  final String id;
  final String title;
  final String startNodeId;
  final Map<String, StoryNode> nodes;
  final Map<String, CollectionItem> inventoryCatalog;
  final Map<String, CollectionItem> evidenceCatalog;
  final Map<String, CollectionItem> equipmentCatalog;

  /// Ordered catalog of map fragments (id/title/image). Revealed as the player
  /// explores; the reader assembles the revealed pieces in this order.
  final Map<String, CollectionItem> mapCatalog;
  final CollectionConfig inventoryConfig;
  final CollectionConfig evidenceConfig;
  final CollectionConfig equipmentConfig;
  final CollectionConfig mapConfig;

  /// Book-defined numeric stats (HP, Armor Class, gold, or any renamed
  /// equivalent). Empty when the book uses no stat systems.
  final List<StatDef> stats;

  /// Pre-made protagonists offered at the start. Empty = no character choice.
  final List<CharacterOption> characters;

  StatDef? statById(String id) {
    for (final stat in stats) {
      if (stat.id == id) return stat;
    }
    return null;
  }

  StatDef? get healthStat => _statByRole(StatRole.health);
  StatDef? get armorStat => _statByRole(StatRole.armor);

  StatDef? _statByRole(StatRole role) {
    for (final stat in stats) {
      if (stat.role == role) return stat;
    }
    return null;
  }

  StoryNode node(String id) {
    final node = nodes[id];
    if (node == null) throw StateError('Story node not found: $id');
    return node;
  }
}

enum StatRole { normal, health, armor }

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
    this.abilityScore = false,
    this.modifierTable = const [],
  });

  final String id;
  final String label;
  final int start;
  final int? max;
  final StatRole role;
  final bool hidden;

  /// When true this stat is an ability score: its raw value (typically 1-18)
  /// maps to a small modifier via [modifierTable] (or StoryBoy's default
  /// tiers). When false the stat's own value is its modifier.
  final bool abilityScore;

  /// Optional custom score->modifier bands ([min, max, mod]); overrides the
  /// default table.
  final List<ModifierBand> modifierTable;

  /// The bonus this stat contributes when referenced by a check or combat.
  int modifier(int value) {
    if (!abilityScore) return value;
    for (final band in modifierTable) {
      if (value >= band.min && value <= band.max) return band.mod;
    }
    // StoryBoy default tiers (our own numbers).
    if (value <= 3) return -3;
    if (value <= 5) return -2;
    if (value <= 8) return -1;
    if (value <= 12) return 0;
    if (value <= 15) return 1;
    if (value <= 17) return 2;
    return 3;
  }

  String display(int value) {
    if (abilityScore) {
      final mod = modifier(value);
      return '$label $value (${mod >= 0 ? '+$mod' : '$mod'})';
    }
    if (max != null) return '$label $value/$max';
    return '$label $value';
  }
}

class ModifierBand {
  const ModifierBand({required this.min, required this.max, required this.mod});
  final int min;
  final int max;
  final int mod;
}

/// A pre-made protagonist a book may offer at the start. Choosing one seeds
/// starting stats and gear; the story then speaks to the player as that
/// character.
class CharacterOption {
  const CharacterOption({
    required this.id,
    required this.name,
    this.description = '',
    this.image,
    this.stats = const {},
    this.equipmentIds = const [],
    this.equippedBySlot = const {},
    this.startNodeId,
  });

  final String id;
  final String name;
  final String description;
  final String? image;
  final Map<String, int> stats;
  final List<String> equipmentIds;
  final Map<String, String> equippedBySlot;
  final String? startNodeId;
}

/// A change applied to a stat when a node is entered.
class StatChange {
  const StatChange({required this.statId, required this.amount, this.set = false});

  final String statId;
  final int amount;
  final bool set;
}

/// One titled section of a `lore` node. Rendered as its own headed block so a
/// multi-part page (a journal, a "how to play" section) stays readable.
class LoreEntry {
  const LoreEntry({required this.title, required this.text});

  final String title;
  final String text;
}

class StoryNode {
  const StoryNode({
    required this.id,
    required this.type,
    required this.text,
    this.loreEntries = const [],
    this.images = const [],
    this.choices = const [],
    this.inventoryGained = const [],
    this.evidenceGained = const [],
    this.equipmentGained = const [],
    this.mapRevealIds = const [],
    this.mapLocations = const [],
    this.battle,
    this.check,
    this.combat,
    this.shop,
    this.statChanges = const [],
    this.setFlags = const [],
    this.clearFlags = const [],
    this.acceptedAnswers = const [],
    this.correctTargetId,
    this.incorrectTargetId,
  });

  final String id;
  final String type;
  final String text;

  /// Titled sections for `lore` nodes; empty for every other type, in which
  /// case [text] carries the whole body.
  final List<LoreEntry> loreEntries;
  final List<StoryImage> images;
  final List<StoryChoice> choices;
  final List<CollectionItem> inventoryGained;
  final List<CollectionItem> evidenceGained;
  final List<CollectionItem> equipmentGained;

  /// Map fragment ids revealed when this node is entered.
  final List<String> mapRevealIds;
  final List<MapLocation> mapLocations;
  final BattleConfig? battle;
  final CheckConfig? check;
  final CombatConfig? combat;
  final ShopConfig? shop;
  final List<StatChange> statChanges;

  /// Story flags raised / cleared when this node is entered. Flags are free-form
  /// (no declaration needed) and drive conditional choices.
  final List<String> setFlags;
  final List<String> clearFlags;
  final List<String> acceptedAnswers;
  final String? correctTargetId;
  final String? incorrectTargetId;

  bool get isEnding =>
      type != 'puzzle' &&
      type != 'map' &&
      type != 'battle' &&
      type != 'check' &&
      type != 'combat' &&
      type != 'shop' &&
      choices.isEmpty;
}

/// A shop: buy catalog items with a currency stat, then leave.
class ShopConfig {
  const ShopConfig({
    required this.currencyStatId,
    required this.items,
    this.returnTargetId,
    this.leaveLabel = 'Leave',
  });

  final String currencyStatId;
  final List<ShopItem> items;
  final String? returnTargetId;
  final String leaveLabel;
}

class ShopItem {
  const ShopItem({
    required this.itemId,
    required this.collection,
    required this.price,
  });

  final String itemId;

  /// Which catalog the item comes from: 'equipment' or 'inventory'.
  final String collection;
  final int price;
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
    this.hitStatId,
    this.damageStatId,
    required this.monsterHitsOn,
    required this.winTargetId,
    required this.loseTargetId,
    required this.healthStatId,
    this.armorStatId,
    this.enemyAttackBonus = 0,
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

  /// Optional stats whose modifier is added to the player's to-hit / damage,
  /// so a character's ability scores matter in combat.
  final String? hitStatId;
  final String? damageStatId;

  /// Number the enemy must roll (1d20) to hit the player.
  final int monsterHitsOn;
  final String winTargetId;
  final String loseTargetId;
  final String healthStatId;

  /// When set, the enemy must roll (1d20 + [enemyAttackBonus]) >= the player's
  /// effective value of this stat to hit, so equipped armor matters. Falls back
  /// to [monsterHitsOn] when null.
  final String? armorStatId;
  final int enemyAttackBonus;
  final String? fleeTargetId;
  final String? talkTargetId;
  final String talkLabel;
}

class StoryChoice {
  const StoryChoice({
    required this.text,
    required this.targetId,
    this.requires,
    this.lockedText,
  });

  final String text;
  final String targetId;

  /// When set, the choice only appears once these conditions are met.
  final ChoiceRequirement? requires;

  /// When set, an unmet choice is shown disabled with this hint instead of
  /// being hidden entirely.
  final String? lockedText;
}

/// A numeric comparison against a stat's effective value.
class StatCondition {
  const StatCondition({this.min, this.max, this.equals});

  final int? min;
  final int? max;
  final int? equals;

  bool test(int value) {
    if (min != null && value < min!) return false;
    if (max != null && value > max!) return false;
    if (equals != null && value != equals!) return false;
    return true;
  }
}

/// Conditions gating a choice or map location. All listed predicates must pass.
class ChoiceRequirement {
  const ChoiceRequirement({
    this.items = const [],
    this.notItems = const [],
    this.equipment = const [],
    this.equipped = const [],
    this.evidence = const [],
    this.characters = const [],
    this.notCharacters = const [],
    this.flags = const [],
    this.notFlags = const [],
    this.stats = const {},
  });

  final List<String> items;
  final List<String> notItems;
  final List<String> equipment;
  final List<String> equipped;
  final List<String> evidence;
  final List<String> characters;
  final List<String> notCharacters;
  final List<String> flags;
  final List<String> notFlags;
  final Map<String, StatCondition> stats;

  bool get isEmpty =>
      items.isEmpty &&
      notItems.isEmpty &&
      equipment.isEmpty &&
      equipped.isEmpty &&
      evidence.isEmpty &&
      characters.isEmpty &&
      notCharacters.isEmpty &&
      flags.isEmpty &&
      notFlags.isEmpty &&
      stats.isEmpty;
}

class StoryImage {
  const StoryImage({required this.path, this.caption = ''});

  final String path;
  final String caption;
}

class MapLocation {
  const MapLocation({
    required this.title,
    required this.description,
    required this.targetId,
    this.requires,
    this.lockedText,
  });

  final String title;
  final String description;
  final String targetId;
  final ChoiceRequirement? requires;
  final String? lockedText;
}

/// Shared shape for inventory items, evidence entries, and equipment. The
/// equipment fields are only meaningful for items in the equipment collection.
class CollectionItem {
  const CollectionItem({
    required this.id,
    required this.title,
    this.description = '',
    this.detail = '',
    this.image,
    this.slot,
    this.equipEffects = const {},
    this.damage,
    this.damageBonus = 0,
    this.hitBonus = 0,
  });

  final String id;
  final String title;
  final String description;
  final String detail;
  final String? image;

  /// Equipment: the slot this item occupies (e.g. weapon, armor, shield).
  /// Only one item per slot is active at a time.
  final String? slot;

  /// Stat deltas applied while this item is equipped (e.g. {"ac": 2}).
  final Map<String, int> equipEffects;

  /// Weapon damage dice used in combat while equipped (overrides the node's
  /// player damage when set).
  final String? damage;
  final int damageBonus;
  final int hitBonus;

  bool get hasMore => detail.isNotEmpty || image != null;
  bool get isEquippable => slot != null;

  CollectionItem copyWith({String? image}) => CollectionItem(
        id: id,
        title: title,
        description: description,
        detail: detail,
        image: image ?? this.image,
        slot: slot,
        equipEffects: equipEffects,
        damage: damage,
        damageBonus: damageBonus,
        hitBonus: hitBonus,
      );
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

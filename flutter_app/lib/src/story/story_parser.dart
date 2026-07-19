import 'story_models.dart';

/// Dart port of the story.json parser. Accepts the same field aliases as the
/// Kotlin and web implementations (see docs/GAMEBOOK_FORMAT.md).
StoryGamebook parseStoryGamebook(Map<String, dynamic> source) {
  final metadata = (source['metadata'] as Map?)?.cast<String, dynamic>() ?? const {};
  final rawNodes = source['nodes'];
  final nodes = <String, StoryNode>{};

  if (rawNodes is List) {
    for (final raw in rawNodes) {
      final node = _parseNode((raw as Map).cast<String, dynamic>());
      nodes[node.id] = node;
    }
  } else if (rawNodes is Map) {
    rawNodes.forEach((id, raw) {
      final node = _parseNode((raw as Map).cast<String, dynamic>(), fallbackId: id as String);
      nodes[node.id] = node;
    });
  }

  final startNodeId = (metadata['start_node'] as String?) ??
      (metadata['startNode'] as String?) ??
      (nodes.keys.isNotEmpty ? nodes.keys.first : '');
  if (!nodes.containsKey(startNodeId)) {
    throw const FormatException('Start node does not exist.');
  }

  final inventoryCatalog = _parseCatalog(source['inventory']);
  final evidenceCatalog = _parseCatalog(source['evidence']);
  for (final node in nodes.values) {
    for (final item in node.inventoryGained) {
      inventoryCatalog.putIfAbsent(item.id, () => item);
    }
    for (final item in node.evidenceGained) {
      evidenceCatalog.putIfAbsent(item.id, () => item);
    }
  }

  final collections = (source['collections'] as Map?)?.cast<String, dynamic>() ?? const {};
  return StoryGamebook(
    id: (metadata['folder'] as String?) ?? (metadata['id'] as String?) ?? 'gamebook',
    title: (metadata['title'] as String?) ?? 'Gamebook',
    startNodeId: startNodeId,
    nodes: nodes,
    inventoryCatalog: inventoryCatalog,
    evidenceCatalog: evidenceCatalog,
    inventoryConfig: _parseCollectionConfig(
      (collections['inventory'] ?? collections['items']) as Map?,
      defaultLabel: 'Items',
      hasEntries: inventoryCatalog.isNotEmpty,
    ),
    evidenceConfig: _parseCollectionConfig(
      collections['evidence'] as Map?,
      defaultLabel: 'Evidence',
      hasEntries: evidenceCatalog.isNotEmpty,
    ),
  );
}

CollectionConfig _parseCollectionConfig(
  Map? raw, {
  required String defaultLabel,
  required bool hasEntries,
}) {
  final config = raw?.cast<String, dynamic>() ?? const <String, dynamic>{};
  final label = [config['label'], config['title'], config['name']]
          .whereType<String>()
          .map((value) => value.trim())
          .firstWhere((value) => value.isNotEmpty, orElse: () => '')
          .trim();
  final enabled = config.containsKey('enabled')
      ? config['enabled'] == true
      : config.containsKey('include')
          ? config['include'] == true
          : hasEntries;
  final showCount = config.containsKey('show_count')
      ? config['show_count'] == true
      : config.containsKey('showCount')
          ? config['showCount'] == true
          : true;
  return CollectionConfig(
    label: label.isEmpty ? defaultLabel : label,
    showCount: showCount,
    enabled: enabled,
  );
}

StoryNode _parseNode(Map<String, dynamic> json, {String? fallbackId}) {
  final id = ((json['id'] as String?) ?? fallbackId ?? '').trim();
  if (id.isEmpty) throw const FormatException('Story node is missing an id.');
  final type = (json['type'] as String?) ?? 'text';

  switch (type) {
    case 'lore':
      return _parseLoreNode(json, id);
    case 'puzzle':
      return _parsePuzzleNode(json, id);
    case 'inventory':
    case 'evidence':
      return _parseCollectionNode(json, id, type);
    case 'map':
      return _parseMapNode(json, id);
    case 'battle':
      return _parseBattleNode(json, id);
    default:
      return StoryNode(
        id: id,
        type: type,
        text: (json['text'] as String?) ?? (json['body'] as String?) ?? '',
        images: _parseImages(json),
        choices: _parseChoices(json['choices']),
        inventoryGained: _parseGained(json, _inventoryKeys),
        evidenceGained: _parseGained(json, _evidenceKeys),
      );
  }
}

StoryNode _parseLoreNode(Map<String, dynamic> json, String id) {
  final entries = json['entries'];
  final buffer = StringBuffer();
  if (entries is List) {
    for (final raw in entries) {
      final entry = (raw as Map).cast<String, dynamic>();
      if (buffer.isNotEmpty) buffer.write('\n\n');
      buffer.write((entry['title'] as String?) ?? '');
      buffer.write('\n');
      buffer.write((entry['text'] as String?) ?? '');
    }
  } else {
    buffer.write((json['text'] as String?) ?? '');
  }
  final returnTo = (json['return_to'] as String?) ?? (json['returnTo'] as String?) ?? '';

  return StoryNode(
    id: id,
    type: 'lore',
    text: buffer.toString(),
    images: _parseImages(json),
    choices: returnTo.isEmpty ? const [] : [StoryChoice(text: 'Continue', targetId: returnTo)],
    inventoryGained: _parseGained(json, _inventoryKeys),
    evidenceGained: _parseGained(json, _evidenceKeys),
  );
}

StoryNode _parsePuzzleNode(Map<String, dynamic> json, String id) {
  final answers = ((json['answers'] as List?) ?? const [])
      .map((answer) => normalizeAnswer(answer.toString()))
      .toList();
  return StoryNode(
    id: id,
    type: 'puzzle',
    text: (json['question'] as String?) ?? (json['text'] as String?) ?? '',
    images: _parseImages(json),
    inventoryGained: _parseGained(json, _inventoryKeys),
    evidenceGained: _parseGained(json, _evidenceKeys),
    acceptedAnswers: answers,
    correctTargetId: _firstString(json, ['correct_target', 'correctTarget']),
    incorrectTargetId: _firstString(json, [
      'incorrect_target',
      'incorrectTarget',
      'default_target',
      'defaultTarget',
    ]),
  );
}

StoryNode _parseCollectionNode(Map<String, dynamic> json, String id, String type) {
  final returnTo = (json['return_to'] as String?) ?? (json['returnTo'] as String?) ?? '';
  var choices = _parseChoices(json['choices']);
  if (choices.isEmpty && returnTo.isNotEmpty) {
    choices = [StoryChoice(text: 'Continue', targetId: returnTo)];
  }
  return StoryNode(
    id: id,
    type: type,
    text: (json['text'] as String?) ?? '',
    images: _parseImages(json),
    choices: choices,
    inventoryGained: _parseGained(json, _inventoryKeys),
    evidenceGained: _parseGained(json, _evidenceKeys),
  );
}

StoryNode _parseMapNode(Map<String, dynamic> json, String id) {
  final locations = ((json['locations'] as List?) ?? const []).map((raw) {
    final location = (raw as Map).cast<String, dynamic>();
    return MapLocation(
      title: (location['title'] as String?) ?? '',
      description: (location['description'] as String?) ?? '',
      targetId: (location['target'] as String?) ?? '',
    );
  }).toList();

  return StoryNode(
    id: id,
    type: 'map',
    text: (json['text'] as String?) ?? (json['title'] as String?) ?? 'Choose a location.',
    images: _parseImages(json),
    choices: _parseChoices(json['choices']),
    inventoryGained: _parseGained(json, _inventoryKeys),
    evidenceGained: _parseGained(json, _evidenceKeys),
    mapLocations: locations,
  );
}

StoryNode _parseBattleNode(Map<String, dynamic> json, String id) {
  final battleJson = ((json['battle'] as Map?) ?? json).cast<String, dynamic>();
  final modifiersRaw = (battleJson['item_modifiers'] ?? battleJson['inventory_modifiers']) as List?;
  final modifiers = (modifiersRaw ?? const []).map((raw) {
    final modifier = (raw as Map).cast<String, dynamic>();
    final itemId = _firstString(modifier, ['item', 'item_id', 'inventory_id', 'requires_item', 'id']) ?? '';
    return BattleModifier(
      itemId: itemId,
      bonus: _firstInt(modifier, ['bonus', 'player_bonus']) ?? 0,
      description: (modifier['description'] as String?) ?? itemId,
    );
  }).toList();

  return StoryNode(
    id: id,
    type: 'battle',
    text: (json['text'] as String?) ?? (json['title'] as String?) ?? 'Roll for the outcome.',
    images: _parseImages(json),
    inventoryGained: _parseGained(json, _inventoryKeys),
    evidenceGained: _parseGained(json, _evidenceKeys),
    battle: BattleConfig(
      playerDice: _firstString(battleJson, ['player_dice', 'playerDice']) ?? '1d6',
      opponentDice:
          _firstString(battleJson, ['opponent_dice', 'opponentDice', 'enemy_dice', 'enemyDice']) ?? '1d6',
      playerBonus: _firstInt(battleJson, ['player_bonus', 'playerBonus']) ?? 0,
      opponentBonus:
          _firstInt(battleJson, ['opponent_bonus', 'opponentBonus', 'enemy_bonus', 'enemyBonus']) ?? 0,
      winTargetId: _firstString(battleJson, ['win_target', 'on_win', 'success_target']) ?? '',
      loseTargetId: _firstString(battleJson, ['lose_target', 'on_lose', 'failure_target']) ?? '',
      drawTargetId: _firstString(battleJson, ['draw_target', 'tie_target', 'on_draw']),
      itemModifiers: modifiers,
    ),
  );
}

List<StoryChoice> _parseChoices(dynamic raw) {
  if (raw is! List) return const [];
  return raw.map((entry) {
    final choice = (entry as Map).cast<String, dynamic>();
    return StoryChoice(
      text: (choice['text'] as String?) ?? (choice['title'] as String?) ?? 'Continue',
      targetId: (choice['target'] as String?) ?? '',
    );
  }).toList();
}

List<StoryImage> _parseImages(Map<String, dynamic> json) {
  final images = <StoryImage>[];
  final single = json['image'] as String?;
  if (single != null && single.isNotEmpty) {
    images.add(StoryImage(
      path: single,
      caption: (json['image_caption'] as String?) ?? (json['imageCaption'] as String?) ?? '',
    ));
  }
  final list = json['images'];
  if (list is List) {
    for (final raw in list) {
      if (raw is String) {
        images.add(StoryImage(path: raw));
      } else if (raw is Map) {
        final image = raw.cast<String, dynamic>();
        final path = image['path'] as String?;
        if (path != null && path.isNotEmpty) {
          images.add(StoryImage(path: path, caption: (image['caption'] as String?) ?? ''));
        }
      }
    }
  }
  return images;
}

const _inventoryKeys = ['items', 'inventory', 'gain_inventory', 'gains_inventory'];
const _evidenceKeys = ['evidence', 'gain_evidence', 'gains_evidence'];

List<CollectionItem> _parseGained(Map<String, dynamic> json, List<String> keys) {
  for (final key in keys) {
    final raw = json[key];
    if (raw is List) {
      return raw.map(_parseCollectionItem).toList();
    }
  }
  return const [];
}

Map<String, CollectionItem> _parseCatalog(dynamic raw) {
  final catalog = <String, CollectionItem>{};
  if (raw is List) {
    for (final entry in raw) {
      final item = _parseCollectionItem(entry);
      catalog[item.id] = item;
    }
  }
  return catalog;
}

CollectionItem _parseCollectionItem(dynamic raw) {
  if (raw is Map) {
    final json = raw.cast<String, dynamic>();
    final id = (json['id'] as String?) ?? '';
    return CollectionItem(
      id: id,
      title: ((json['title'] as String?) ?? '').trim().isEmpty ? _displayTitle(id) : json['title'] as String,
      description: (json['description'] as String?) ?? '',
      detail: (json['detail'] as String?) ?? '',
      image: ((json['image'] as String?) ?? '').isEmpty ? null : json['image'] as String,
    );
  }
  final id = raw.toString();
  return CollectionItem(id: id, title: _displayTitle(id));
}

String _displayTitle(String id) {
  return id
      .replaceAll(RegExp(r'[_\-]+'), ' ')
      .split(' ')
      .where((word) => word.isNotEmpty)
      .map((word) => word[0].toUpperCase() + word.substring(1))
      .join(' ');
}

String? _firstString(Map<String, dynamic> json, List<String> keys) {
  for (final key in keys) {
    final value = json[key];
    if (value is String && value.trim().isNotEmpty) return value;
  }
  return null;
}

int? _firstInt(Map<String, dynamic> json, List<String> keys) {
  for (final key in keys) {
    final value = json[key];
    if (value is num) return value.toInt();
  }
  return null;
}

String normalizeAnswer(String value) {
  return value.trim().toLowerCase().replaceAll(RegExp(r'\s+'), ' ');
}

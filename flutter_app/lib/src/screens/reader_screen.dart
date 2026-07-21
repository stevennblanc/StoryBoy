import 'dart:io';
import 'dart:math';

import 'package:flutter/material.dart';

import '../app_model.dart';
import '../data/gbk_package.dart';
import '../models.dart';
import '../story/story_models.dart';
import '../story/story_parser.dart';
import '../theme.dart';

class ReaderScreen extends StatefulWidget {
  const ReaderScreen({super.key, required this.model, required this.book});

  final AppModel model;
  final LocalBook book;

  @override
  State<ReaderScreen> createState() => _ReaderScreenState();
}

class _ReaderScreenState extends State<ReaderScreen> {
  GbkPackage? _package;
  StoryGamebook? _story;
  StoryNode? _node;
  Set<String> _inventoryIds = {};
  Set<String> _evidenceIds = {};
  Set<String> _equipmentIds = {};
  Set<String> _mapIds = {};
  Set<String> _flags = {};
  String? _chosenCharacterId;
  final Map<String, int> _stats = {};
  final Map<String, String> _equippedBySlot = {};
  BattleResult? _battleResult;
  _CheckOutcome? _checkOutcome;
  int? _enemyHp;
  final List<String> _combatLog = [];
  String? _combatEndTarget;
  String _shopMessage = '';
  bool _showInventory = false;
  bool _showEvidence = false;
  bool _showEquipment = false;
  bool _showMap = false;
  bool _selectingCharacter = false;
  String? _expandedItemId;
  String? _error;
  final _random = Random();
  final _answerController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _answerController.dispose();
    widget.model.markProgressChanged();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final package = await GbkPackage.open(widget.book.filePath, widget.book.id);
      final story = package.readStory();
      final progress = widget.model.progress;
      final savedNode = progress.currentNode(story.id);
      final savedStats = progress.stats(story.id);
      setState(() {
        _package = package;
        _story = story;
        _inventoryIds = progress.collectedIds(story.id, 'inventory');
        _evidenceIds = progress.collectedIds(story.id, 'evidence');
        _equipmentIds = progress.collectedIds(story.id, 'equipment');
        _mapIds = progress.collectedIds(story.id, 'map');
        _flags = progress.flags(story.id);
        _chosenCharacterId = progress.chosenCharacter(story.id);
        _stats.clear();
        for (final stat in story.stats) {
          _stats[stat.id] = savedStats[stat.id] ?? stat.start;
        }
        _equippedBySlot
          ..clear()
          ..addAll(progress.equipped(story.id));
      });
      if (story.characters.isNotEmpty && savedNode == null) {
        setState(() => _selectingCharacter = true);
      } else {
        _enterNode(
          savedNode != null && story.nodes.containsKey(savedNode) ? savedNode : story.startNodeId,
        );
      }
    } catch (error) {
      setState(() => _error = 'Could not open this gamebook. $error');
    }
  }

  void _enterNode(String nodeId) {
    final story = _story;
    if (story == null) return;
    final node = story.nodes[nodeId];
    if (node == null) return;

    final inventoryIds = {..._inventoryIds, ...node.inventoryGained.map((item) => item.id)};
    final evidenceIds = {..._evidenceIds, ...node.evidenceGained.map((item) => item.id)};
    final equipmentIds = {..._equipmentIds, ...node.equipmentGained.map((item) => item.id)};
    final mapIds = {..._mapIds, ...node.mapRevealIds};
    final flags = {..._flags, ...node.setFlags}..removeAll(node.clearFlags);
    _applyStatChanges(story, node.statChanges);
    final progress = widget.model.progress;
    progress.saveFlags(story.id, flags);
    progress.saveCurrentNode(story.id, nodeId);
    progress.saveCollectedIds(story.id, 'inventory', inventoryIds);
    progress.saveCollectedIds(story.id, 'evidence', evidenceIds);
    progress.saveCollectedIds(story.id, 'equipment', equipmentIds);
    progress.saveCollectedIds(story.id, 'map', mapIds);
    progress.saveStats(story.id, _stats);

    setState(() {
      _node = node;
      _inventoryIds = inventoryIds;
      _evidenceIds = evidenceIds;
      _equipmentIds = equipmentIds;
      _mapIds = mapIds;
      _flags = flags;
      _battleResult = null;
      _checkOutcome = null;
      _enemyHp = node.combat?.enemyHp;
      _combatLog.clear();
      _combatEndTarget = null;
      _shopMessage = '';
      _answerController.clear();
    });
  }

  /// A stat's value including the effects of everything currently equipped.
  int _effectiveStat(String statId) {
    final story = _story;
    var value = _stats[statId] ?? 0;
    if (story == null) return value;
    for (final itemId in _equippedBySlot.values) {
      final item = story.equipmentCatalog[itemId];
      final delta = item?.equipEffects[statId];
      if (delta != null) value += delta;
    }
    return value;
  }

  CollectionItem? get _equippedWeapon {
    final story = _story;
    if (story == null) return null;
    for (final itemId in _equippedBySlot.values) {
      final item = story.equipmentCatalog[itemId];
      if (item?.damage != null) return item;
    }
    return null;
  }

  void _toggleEquip(CollectionItem item) {
    final story = _story;
    if (story == null || item.slot == null) return;
    setState(() {
      if (_equippedBySlot[item.slot] == item.id) {
        _equippedBySlot.remove(item.slot);
      } else {
        _equippedBySlot[item.slot!] = item.id;
      }
    });
    widget.model.progress.saveEquipped(story.id, _equippedBySlot);
  }

  void _applyStatChanges(StoryGamebook story, List<StatChange> changes) {
    if (changes.isEmpty) return;
    final byId = {for (final stat in story.stats) stat.id: stat};
    for (final change in changes) {
      final def = byId[change.statId];
      final current = _stats[change.statId] ?? def?.start ?? 0;
      var next = change.set ? change.amount : current + change.amount;
      if (def?.max != null && next > def!.max!) next = def.max!;
      if (next < 0) next = 0;
      _stats[change.statId] = next;
    }
  }

  void _restart() {
    final story = _story;
    if (story == null) return;
    widget.model.progress.reset(story.id);
    _inventoryIds = {};
    _evidenceIds = {};
    _equipmentIds = {};
    _mapIds = {};
    _flags = {};
    _chosenCharacterId = null;
    _equippedBySlot.clear();
    _stats.clear();
    for (final stat in story.stats) {
      _stats[stat.id] = stat.start;
    }
    if (story.characters.isNotEmpty) {
      setState(() {
        _selectingCharacter = true;
        _node = null;
      });
    } else {
      _enterNode(story.startNodeId);
    }
  }

  void _chooseCharacter(CharacterOption character) {
    final story = _story;
    if (story == null) return;
    for (final stat in story.stats) {
      _stats[stat.id] = character.stats[stat.id] ?? stat.start;
    }
    _equipmentIds = {...character.equipmentIds};
    _equippedBySlot
      ..clear()
      ..addAll(character.equippedBySlot);
    final progress = widget.model.progress;
    _chosenCharacterId = character.id;
    progress.saveChosenCharacter(story.id, character.id);
    progress.saveStats(story.id, _stats);
    progress.saveCollectedIds(story.id, 'equipment', _equipmentIds);
    progress.saveEquipped(story.id, _equippedBySlot);
    setState(() => _selectingCharacter = false);
    _enterNode(character.startNodeId ?? story.startNodeId);
  }

  /// True when every predicate on [requirement] currently holds.
  bool _meets(ChoiceRequirement? requirement) {
    if (requirement == null || requirement.isEmpty) return true;
    for (final id in requirement.items) {
      if (!_inventoryIds.contains(id)) return false;
    }
    for (final id in requirement.notItems) {
      if (_inventoryIds.contains(id)) return false;
    }
    for (final id in requirement.equipment) {
      if (!_equipmentIds.contains(id)) return false;
    }
    for (final id in requirement.equipped) {
      if (!_equippedBySlot.containsValue(id)) return false;
    }
    for (final id in requirement.evidence) {
      if (!_evidenceIds.contains(id)) return false;
    }
    for (final flag in requirement.flags) {
      if (!_flags.contains(flag)) return false;
    }
    for (final flag in requirement.notFlags) {
      if (_flags.contains(flag)) return false;
    }
    if (requirement.characters.isNotEmpty &&
        !requirement.characters.contains(_chosenCharacterId)) {
      return false;
    }
    for (final entry in requirement.stats.entries) {
      if (!entry.value.test(_effectiveStat(entry.key))) return false;
    }
    return true;
  }

  /// The bonus a stat contributes: an ability score's tiered modifier, or a
  /// plain stat's effective value.
  int _statModifier(StoryGamebook story, String statId) {
    final value = _effectiveStat(statId);
    return story.statById(statId)?.modifier(value) ?? value;
  }

  String _resolveHealthStatId(StoryGamebook story, CombatConfig combat) {
    if (_stats.containsKey(combat.healthStatId)) return combat.healthStatId;
    return story.healthStat?.id ?? combat.healthStatId;
  }

  void _submitAnswer() {
    final node = _node;
    if (node == null) return;
    final answer = normalizeAnswer(_answerController.text);
    final target = node.acceptedAnswers.contains(answer)
        ? node.correctTargetId
        : node.incorrectTargetId;
    if (target != null) _enterNode(target);
  }

  List<int> _rollDiceList(String expression) {
    final match = RegExp(r'^(\d*)d(\d+)$', caseSensitive: false).firstMatch(expression.trim());
    final count = max(1, int.tryParse(match?.group(1) ?? '') ?? 1);
    final sides = max(2, int.tryParse(match?.group(2) ?? '') ?? 6);
    return List.generate(count, (_) => _random.nextInt(sides) + 1);
  }

  void _resolveBattle() {
    final battle = _node?.battle;
    if (battle == null) return;
    final applied =
        battle.itemModifiers.where((modifier) => _inventoryIds.contains(modifier.itemId)).toList();
    final playerBonus = battle.playerBonus + applied.fold<int>(0, (sum, m) => sum + m.bonus);
    final playerRolls = _rollDiceList(battle.playerDice);
    final opponentRolls = _rollDiceList(battle.opponentDice);
    final playerTotal = playerRolls.fold(0, (a, b) => a + b) + playerBonus;
    final opponentTotal = opponentRolls.fold(0, (a, b) => a + b) + battle.opponentBonus;
    final String outcome;
    final String targetId;
    if (playerTotal > opponentTotal) {
      outcome = 'Success';
      targetId = battle.winTargetId;
    } else if (playerTotal < opponentTotal) {
      outcome = 'Setback';
      targetId = battle.loseTargetId;
    } else {
      outcome = 'Draw';
      targetId = battle.drawTargetId ?? battle.winTargetId;
    }
    setState(() {
      _battleResult = BattleResult(
        playerRolls: playerRolls,
        playerBonus: playerBonus,
        opponentRolls: opponentRolls,
        opponentBonus: battle.opponentBonus,
        appliedModifiers: applied,
        outcome: outcome,
        targetId: targetId,
      );
    });
  }

  void _rollCheck() {
    final node = _node;
    final story = _story;
    final check = node?.check;
    if (check == null || story == null) return;
    final rolls = _rollDiceList(check.dice);
    final statBonus = check.statModifier != null ? _statModifier(story, check.statModifier!) : 0;
    final total = rolls.fold(0, (a, b) => a + b) + check.modifier + statBonus;
    final success = total >= check.target;
    setState(() {
      _checkOutcome = _CheckOutcome(
        rolls: rolls,
        bonus: check.modifier + statBonus,
        total: total,
        target: check.target,
        success: success,
        targetId: success ? check.successTargetId : check.failureTargetId,
        label: success ? check.successLabel : check.failureLabel,
      );
    });
  }

  void _combatRound(CombatConfig combat) {
    final story = _story;
    if (story == null) return;
    final healthId = _resolveHealthStatId(story, combat);
    var enemyHp = _enemyHp ?? combat.enemyHp;
    var playerHp = _stats[healthId] ?? 0;
    final log = <String>[];

    final weapon = _equippedWeapon;
    final hitMod = combat.hitStatId != null ? _statModifier(story, combat.hitStatId!) : 0;
    final damageMod = combat.damageStatId != null ? _statModifier(story, combat.damageStatId!) : 0;
    final playerDamageDice = weapon?.damage ?? combat.playerDamage;
    final playerDamageBonus = (weapon?.damageBonus ?? 0) + combat.playerDamageBonus + damageMod;
    final playerHitBonus = combat.playerHitBonus + (weapon?.hitBonus ?? 0) + hitMod;
    final playerRoll = _random.nextInt(20) + 1;
    if (playerRoll + playerHitBonus >= combat.enemyHitTarget) {
      final dmg = _rollDiceList(playerDamageDice).fold(0, (a, b) => a + b) + playerDamageBonus;
      enemyHp -= dmg;
      log.add('You hit (rolled $playerRoll) for $dmg damage.');
    } else {
      log.add('You miss (rolled $playerRoll).');
    }

    if (enemyHp > 0) {
      final hitsOn = combat.armorStatId != null ? _effectiveStat(combat.armorStatId!) : combat.monsterHitsOn;
      final enemyRoll = _random.nextInt(20) + 1;
      if (enemyRoll + combat.enemyAttackBonus >= hitsOn) {
        final dmg = _rollDiceList(combat.enemyDamage).fold(0, (a, b) => a + b);
        playerHp -= dmg;
        log.add('${combat.enemyLabel} hits (rolled $enemyRoll) for $dmg damage.');
      } else {
        log.add('${combat.enemyLabel} misses (rolled $enemyRoll).');
      }
    }

    if (enemyHp < 0) enemyHp = 0;
    if (playerHp < 0) playerHp = 0;
    setState(() {
      _enemyHp = enemyHp;
      _stats[healthId] = playerHp;
      _combatLog
        ..clear()
        ..addAll(log);
      if (enemyHp <= 0) {
        _combatEndTarget = combat.winTargetId;
      } else if (playerHp <= 0) {
        _combatEndTarget = combat.loseTargetId;
      }
    });
    widget.model.progress.saveStats(story.id, _stats);
  }

  void _flee(CombatConfig combat) {
    final story = _story;
    if (story == null || combat.fleeTargetId == null) return;
    final healthId = _resolveHealthStatId(story, combat);
    var playerHp = _stats[healthId] ?? 0;
    final hitsOn = combat.armorStatId != null ? _effectiveStat(combat.armorStatId!) : combat.monsterHitsOn;
    final enemyRoll = _random.nextInt(20) + 1;
    final log = <String>[];
    if (enemyRoll + combat.enemyAttackBonus >= hitsOn) {
      final dmg = _rollDiceList(combat.enemyDamage).fold(0, (a, b) => a + b);
      playerHp -= dmg;
      log.add('${combat.enemyLabel} strikes as you flee for $dmg damage.');
    } else {
      log.add('You slip away cleanly.');
    }
    if (playerHp < 0) playerHp = 0;
    _stats[healthId] = playerHp;
    widget.model.progress.saveStats(story.id, _stats);
    if (playerHp <= 0) {
      setState(() {
        _combatLog
          ..clear()
          ..addAll(log);
        _combatEndTarget = combat.loseTargetId;
      });
    } else {
      _enterNode(combat.fleeTargetId!);
    }
  }

  List<CollectionItem> get _collectedInventory {
    final story = _story;
    if (story == null) return const [];
    return [
      for (final id in _inventoryIds)
        if (story.inventoryCatalog[id] != null) story.inventoryCatalog[id]!,
    ];
  }

  List<CollectionItem> get _collectedEvidence {
    final story = _story;
    if (story == null) return const [];
    return [
      for (final id in _evidenceIds)
        if (story.evidenceCatalog[id] != null) story.evidenceCatalog[id]!,
    ];
  }

  List<CollectionItem> get _collectedEquipment {
    final story = _story;
    if (story == null) return const [];
    return [
      for (final id in _equipmentIds)
        if (story.equipmentCatalog[id] != null) story.equipmentCatalog[id]!,
    ];
  }

  /// Revealed map fragments, in the catalog's defined order.
  List<CollectionItem> get _revealedMap {
    final story = _story;
    if (story == null) return const [];
    return [
      for (final entry in story.mapCatalog.entries)
        if (_mapIds.contains(entry.key)) entry.value,
    ];
  }

  @override
  Widget build(BuildContext context) {
    final story = _story;
    final node = _node;
    final fontScale = widget.model.progress.fontScale;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.book.title, style: mutedStyle.copyWith(fontSize: 15)),
        actions: [
          TextButton(onPressed: _restart, child: const Text('Restart')),
        ],
      ),
      body: SafeArea(
        child: _error != null
            ? Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(_error!)))
            : _selectingCharacter && story != null
                ? _characterSelect(story)
                : story == null || node == null
                    ? const Center(child: CircularProgressIndicator())
                    : ListView(
                    padding: const EdgeInsets.fromLTRB(20, 4, 20, 32),
                    children: [
                      _statBar(story),
                      _collectionBar(story),
                      if (_showInventory && story.inventoryConfig.enabled)
                        _collectionPanel(story.inventoryConfig.label, _collectedInventory),
                      if (_showEquipment && story.equipmentConfig.enabled)
                        _equipmentPanel(story.equipmentConfig.label, _collectedEquipment),
                      if (_showMap && story.mapConfig.enabled)
                        _mapPanel(story.mapConfig.label, _revealedMap),
                      if (_showEvidence && story.evidenceConfig.enabled)
                        _collectionPanel(story.evidenceConfig.label, _collectedEvidence),
                      for (final image in node.images) _storyImage(image),
                      const SizedBox(height: 8),
                      Text(
                        node.text,
                        style: readerTextStyle.copyWith(fontSize: readerTextStyle.fontSize! * fontScale),
                      ),
                      const SizedBox(height: 20),
                      ..._nodeActions(node),
                    ],
                  ),
      ),
    );
  }

  Widget _characterSelect(StoryGamebook story) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
      children: [
        const Text('Choose your character', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w700)),
        const SizedBox(height: 12),
        for (final character in story.characters)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: InkWell(
              onTap: () => _chooseCharacter(character),
              borderRadius: BorderRadius.circular(8),
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: SbColors.surface,
                  border: Border.all(color: SbColors.line),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (character.image != null)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: _assetImage(character.image!),
                      ),
                    Text(character.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                    if (character.description.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Text(character.description, style: readerTextStyle.copyWith(fontSize: 15)),
                      ),
                    if (character.stats.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Wrap(
                          spacing: 8,
                          runSpacing: 6,
                          children: [
                            for (final entry in character.stats.entries)
                              _statChip(story, entry.key, entry.value),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _statChip(StoryGamebook story, String statId, int value) {
    final def = story.statById(statId);
    final label = def?.display(value) ?? '$statId $value';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: SbColors.surface2,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: SbColors.line),
      ),
      child: Text(label, style: const TextStyle(fontSize: 13)),
    );
  }

  Widget _statBar(StoryGamebook story) {
    final visible = story.stats.where((stat) => !stat.hidden).toList();
    if (visible.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Wrap(
        spacing: 8,
        runSpacing: 6,
        children: [
          for (final stat in visible)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              decoration: BoxDecoration(
                color: SbColors.surface2,
                borderRadius: BorderRadius.circular(999),
                border: Border.all(color: SbColors.line),
              ),
              child: Text(
                stat.display(_effectiveStat(stat.id)),
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
              ),
            ),
        ],
      ),
    );
  }

  Widget _collectionBar(StoryGamebook story) {
    final buttons = <Widget>[];
    if (story.inventoryConfig.enabled) {
      buttons.add(TextButton(
        onPressed: _collectedInventory.isEmpty
            ? null
            : () => setState(() => _showInventory = !_showInventory),
        child: Text(story.inventoryConfig.buttonLabel(_collectedInventory.length)),
      ));
    }
    if (story.equipmentConfig.enabled) {
      buttons.add(TextButton(
        onPressed: _collectedEquipment.isEmpty
            ? null
            : () => setState(() => _showEquipment = !_showEquipment),
        child: Text(story.equipmentConfig.buttonLabel(_collectedEquipment.length)),
      ));
    }
    if (story.mapConfig.enabled) {
      buttons.add(TextButton(
        onPressed: _revealedMap.isEmpty ? null : () => setState(() => _showMap = !_showMap),
        child: Text(story.mapConfig.buttonLabel(_revealedMap.length)),
      ));
    }
    if (story.evidenceConfig.enabled) {
      buttons.add(TextButton(
        onPressed:
            _collectedEvidence.isEmpty ? null : () => setState(() => _showEvidence = !_showEvidence),
        child: Text(story.evidenceConfig.buttonLabel(_collectedEvidence.length)),
      ));
    }
    if (buttons.isEmpty) return const SizedBox.shrink();
    return Row(mainAxisAlignment: MainAxisAlignment.end, children: buttons);
  }

  Widget _collectionPanel(String title, List<CollectionItem> items) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: SbColors.surface,
        border: Border.all(color: SbColors.line),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          for (final item in items) _collectionEntry(item),
        ],
      ),
    );
  }

  Widget _mapPanel(String title, List<CollectionItem> fragments) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: SbColors.surface,
        border: Border.all(color: SbColors.line),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          for (final fragment in fragments) ...[
            if (fragment.title.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 6, bottom: 4),
                child: Text(fragment.title, style: const TextStyle(fontWeight: FontWeight.w600)),
              ),
            if (fragment.image != null) _assetImage(fragment.image!),
            if (fragment.description.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(fragment.description, style: mutedStyle),
              ),
          ],
        ],
      ),
    );
  }

  Widget _equipmentPanel(String title, List<CollectionItem> items) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: SbColors.surface,
        border: Border.all(color: SbColors.line),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          for (final item in items) _equipmentEntry(item),
        ],
      ),
    );
  }

  Widget _equipmentEntry(CollectionItem item) {
    final equipped = item.slot != null && _equippedBySlot[item.slot] == item.id;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  equipped ? '${item.title} (equipped)' : item.title,
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                if (item.description.isNotEmpty) Text(item.description, style: mutedStyle),
                if (item.hasMore && item.detail.isNotEmpty) Text(item.detail, style: mutedStyle),
              ],
            ),
          ),
          if (item.isEquippable)
            TextButton(
              onPressed: () => _toggleEquip(item),
              child: Text(equipped ? 'Unequip' : 'Equip'),
            ),
        ],
      ),
    );
  }

  Widget _collectionEntry(CollectionItem item) {
    final expanded = _expandedItemId == item.id;
    return InkWell(
      onTap: item.hasMore
          ? () => setState(() => _expandedItemId = expanded ? null : item.id)
          : null,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(item.title, style: const TextStyle(fontWeight: FontWeight.w600)),
                ),
                if (item.hasMore) Text(expanded ? 'Close' : 'View', style: mutedStyle),
              ],
            ),
            if (item.description.isNotEmpty) Text(item.description, style: mutedStyle),
            if (expanded) ...[
              if (item.image != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: _assetImage(item.image!),
                ),
              if (item.detail.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(item.detail, style: readerTextStyle.copyWith(fontSize: 15)),
                ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _storyImage(StoryImage image) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _assetImage(image.path),
          if (image.caption.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(image.caption, style: mutedStyle),
            ),
        ],
      ),
    );
  }

  Widget _assetImage(String assetPath) {
    final package = _package;
    if (package == null) return const SizedBox.shrink();
    return FutureBuilder<File?>(
      future: package.extractAsset(assetPath),
      builder: (context, snapshot) {
        final file = snapshot.data;
        if (file == null) return const SizedBox.shrink();
        return ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: Image.file(file, width: double.infinity, fit: BoxFit.fitWidth),
        );
      },
    );
  }

  List<Widget> _nodeActions(StoryNode node) {
    if (node.type == 'puzzle') {
      return [
        TextField(
          controller: _answerController,
          decoration: const InputDecoration(labelText: 'Answer'),
          onSubmitted: (_) => _submitAnswer(),
        ),
        const SizedBox(height: 10),
        FilledButton(onPressed: _submitAnswer, child: const Text('Submit')),
      ];
    }

    if (node.type == 'map') {
      return [
        for (final location in node.mapLocations)
          if (_meets(location.requires))
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: FilledButton(
              onPressed: () => _enterNode(location.targetId),
              child: Column(
                children: [
                  Text(location.title),
                  if (location.description.isNotEmpty)
                    Text(location.description, style: mutedStyle.copyWith(fontSize: 13)),
                ],
              ),
            ),
          ),
      ];
    }

    if (node.type == 'battle' && node.battle != null) {
      return _battleActions(node.battle!);
    }

    if (node.type == 'check' && node.check != null) {
      return _checkActions(node.check!);
    }

    if (node.type == 'combat' && node.combat != null) {
      return _combatActions(node.combat!);
    }

    if (node.type == 'shop' && node.shop != null) {
      return _shopActions(node.shop!);
    }

    if (node.choices.isEmpty) {
      return [
        const Center(child: Text('The End', style: TextStyle(fontSize: 24))),
        const SizedBox(height: 12),
        FilledButton(onPressed: _restart, child: const Text('Start over')),
      ];
    }

    return [
      for (final choice in node.choices)
        if (_meets(choice.requires))
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: FilledButton(
              onPressed: () => _enterNode(choice.targetId),
              child: Text(choice.text, textAlign: TextAlign.center),
            ),
          )
        else if (choice.lockedText != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: FilledButton(
              onPressed: null,
              child: Text(choice.lockedText!, textAlign: TextAlign.center),
            ),
          ),
    ];
  }

  List<Widget> _battleActions(BattleConfig battle) {
    final result = _battleResult;
    final line = TextStyle(color: SbColors.muted, fontSize: 14);
    return [
      Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: SbColors.surface,
          border: Border.all(color: SbColors.line),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Battle', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
            const SizedBox(height: 8),
            Text('Your roll: ${battle.playerDice}${_bonusText(battle.playerBonus)}', style: line),
            Text('Opponent: ${battle.opponentDice}${_bonusText(battle.opponentBonus)}', style: line),
            for (final modifier in battle.itemModifiers)
              Text(
                '${_inventoryIds.contains(modifier.itemId) ? 'Prepared' : 'Missing item'}: '
                '${modifier.description} (${_bonusText(modifier.bonus, always: true)})',
                style: line,
              ),
            if (result != null) ...[
              const Divider(),
              Text(result.outcome, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
              Text(
                'You: ${result.playerRolls.join(' + ')}'
                '${_bonusText(result.playerBonus)} = ${result.playerTotal}',
                style: line,
              ),
              Text(
                'Opponent: ${result.opponentRolls.join(' + ')}'
                '${_bonusText(result.opponentBonus)} = ${result.opponentTotal}',
                style: line,
              ),
            ],
          ],
        ),
      ),
      const SizedBox(height: 12),
      if (result == null)
        FilledButton(onPressed: _resolveBattle, child: const Text('Roll'))
      else
        FilledButton(
          onPressed: () => _enterNode(result.targetId),
          child: const Text('Continue'),
        ),
    ];
  }

  String _bonusText(int bonus, {bool always = false}) {
    if (bonus == 0 && !always) return '';
    return bonus >= 0 ? ' +$bonus' : ' $bonus';
  }

  void _buy(ShopConfig shop, ShopItem entry, CollectionItem item) {
    final story = _story;
    if (story == null) return;
    final funds = _stats[shop.currencyStatId] ?? 0;
    if (funds < entry.price) {
      setState(() => _shopMessage = 'Not enough to buy ${item.title}.');
      return;
    }
    setState(() {
      _stats[shop.currencyStatId] = funds - entry.price;
      if (entry.collection == 'equipment') {
        _equipmentIds = {..._equipmentIds, item.id};
      } else {
        _inventoryIds = {..._inventoryIds, item.id};
      }
      _shopMessage = 'Bought ${item.title}.';
    });
    final progress = widget.model.progress;
    progress.saveStats(story.id, _stats);
    progress.saveCollectedIds(story.id, 'equipment', _equipmentIds);
    progress.saveCollectedIds(story.id, 'inventory', _inventoryIds);
  }

  List<Widget> _shopActions(ShopConfig shop) {
    final story = _story;
    if (story == null) return const [];
    final funds = _stats[shop.currencyStatId] ?? 0;
    var currencyLabel = 'Gold';
    for (final stat in story.stats) {
      if (stat.id == shop.currencyStatId) {
        currencyLabel = stat.label;
        break;
      }
    }

    final rows = <Widget>[];
    for (final entry in shop.items) {
      final catalog = entry.collection == 'equipment' ? story.equipmentCatalog : story.inventoryCatalog;
      final item = catalog[entry.itemId];
      if (item == null) continue;
      final owned = entry.collection == 'equipment'
          ? _equipmentIds.contains(item.id)
          : _inventoryIds.contains(item.id);
      rows.add(Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(item.title, style: const TextStyle(fontWeight: FontWeight.w600)),
                  if (item.description.isNotEmpty) Text(item.description, style: mutedStyle),
                ],
              ),
            ),
            const SizedBox(width: 12),
            if (owned)
              const Text('Owned', style: mutedStyle)
            else
              FilledButton(
                // Local width override: the theme's Size.fromHeight(48) makes
                // buttons infinitely wide, which collapses the name Expanded in
                // this Row. Constrain the buy button to its content.
                style: FilledButton.styleFrom(
                  minimumSize: const Size(0, 40),
                  padding: const EdgeInsets.symmetric(horizontal: 14),
                ),
                onPressed: funds >= entry.price ? () => _buy(shop, entry, item) : null,
                child: Text('${entry.price} $currencyLabel'),
              ),
          ],
        ),
      ));
    }

    return [
      Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: SbColors.surface,
          border: Border.all(color: SbColors.line),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('$currencyLabel: $funds', style: const TextStyle(fontWeight: FontWeight.w600)),
            ...rows,
            if (_shopMessage.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(_shopMessage, style: mutedStyle),
              ),
          ],
        ),
      ),
      const SizedBox(height: 12),
      if (shop.returnTargetId != null)
        FilledButton(
          onPressed: () => _enterNode(shop.returnTargetId!),
          child: Text(shop.leaveLabel),
        ),
    ];
  }

  List<Widget> _checkActions(CheckConfig check) {
    final outcome = _checkOutcome;
    final line = const TextStyle(color: SbColors.muted, fontSize: 14);
    return [
      Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: SbColors.surface,
          border: Border.all(color: SbColors.line),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Roll ${check.dice}${_bonusText(check.modifier)} — need ${check.target}+', style: line),
            if (outcome != null) ...[
              const Divider(),
              Text(
                outcome.label,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
              Text(
                'Rolled ${outcome.rolls.join(' + ')}${_bonusText(outcome.bonus)} = '
                '${outcome.total} vs ${outcome.target}',
                style: line,
              ),
            ],
          ],
        ),
      ),
      const SizedBox(height: 12),
      if (outcome == null)
        FilledButton(onPressed: _rollCheck, child: const Text('Roll'))
      else
        FilledButton(
          onPressed: () => _enterNode(outcome.targetId),
          child: const Text('Continue'),
        ),
    ];
  }

  List<Widget> _combatActions(CombatConfig combat) {
    final line = const TextStyle(color: SbColors.muted, fontSize: 14);
    final enemyHp = _enemyHp ?? combat.enemyHp;
    final ended = _combatEndTarget != null;
    return [
      Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: SbColors.surface,
          border: Border.all(color: SbColors.line),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(combat.enemyLabel,
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                Text('HP $enemyHp', style: line),
              ],
            ),
            for (final entry in _combatLog)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(entry, style: line),
              ),
          ],
        ),
      ),
      const SizedBox(height: 12),
      if (ended)
        FilledButton(
          onPressed: () => _enterNode(_combatEndTarget!),
          child: const Text('Continue'),
        )
      else ...[
        FilledButton(onPressed: () => _combatRound(combat), child: const Text('Attack')),
        if (combat.talkTargetId != null) ...[
          const SizedBox(height: 10),
          FilledButton(
            onPressed: () => _enterNode(combat.talkTargetId!),
            child: Text(combat.talkLabel),
          ),
        ],
        if (combat.fleeTargetId != null) ...[
          const SizedBox(height: 10),
          FilledButton(onPressed: () => _flee(combat), child: const Text('Run away')),
        ],
      ],
    ];
  }
}

class _CheckOutcome {
  const _CheckOutcome({
    required this.rolls,
    required this.bonus,
    required this.total,
    required this.target,
    required this.success,
    required this.targetId,
    required this.label,
  });

  final List<int> rolls;
  final int bonus;
  final int total;
  final int target;
  final bool success;
  final String targetId;
  final String label;
}

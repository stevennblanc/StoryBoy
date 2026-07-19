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
  BattleResult? _battleResult;
  bool _showInventory = false;
  bool _showEvidence = false;
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
      setState(() {
        _package = package;
        _story = story;
        _inventoryIds = progress.collectedIds(story.id, 'inventory');
        _evidenceIds = progress.collectedIds(story.id, 'evidence');
      });
      _enterNode(
        savedNode != null && story.nodes.containsKey(savedNode) ? savedNode : story.startNodeId,
      );
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
    final progress = widget.model.progress;
    progress.saveCurrentNode(story.id, nodeId);
    progress.saveCollectedIds(story.id, 'inventory', inventoryIds);
    progress.saveCollectedIds(story.id, 'evidence', evidenceIds);

    setState(() {
      _node = node;
      _inventoryIds = inventoryIds;
      _evidenceIds = evidenceIds;
      _battleResult = null;
      _answerController.clear();
    });
  }

  void _restart() {
    final story = _story;
    if (story == null) return;
    widget.model.progress.reset(story.id);
    _inventoryIds = {};
    _evidenceIds = {};
    _enterNode(story.startNodeId);
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
            : story == null || node == null
                ? const Center(child: CircularProgressIndicator())
                : ListView(
                    padding: const EdgeInsets.fromLTRB(20, 4, 20, 32),
                    children: [
                      _collectionBar(story),
                      if (_showInventory && story.inventoryConfig.enabled)
                        _collectionPanel(story.inventoryConfig.label, _collectedInventory),
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

    if (node.choices.isEmpty) {
      return [
        const Center(child: Text('The End', style: TextStyle(fontSize: 24))),
        const SizedBox(height: 12),
        FilledButton(onPressed: _restart, child: const Text('Start over')),
      ];
    }

    return [
      for (final choice in node.choices)
        Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: FilledButton(
            onPressed: () => _enterNode(choice.targetId),
            child: Text(choice.text, textAlign: TextAlign.center),
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
}

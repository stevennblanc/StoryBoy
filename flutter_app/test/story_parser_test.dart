import 'package:flutter_test/flutter_test.dart';
import 'package:storyboy/src/story/story_models.dart';
import 'package:storyboy/src/story/story_parser.dart';

void main() {
  test('parses a minimal gamebook with collections config', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'Test', 'folder': 'test_book', 'start_node': 'start'},
      'collections': {
        'evidence': {'label': 'Memories', 'show_count': false},
      },
      'inventory': [
        {
          'id': 'key',
          'title': 'Key',
          'description': 'A small key.',
          'detail': 'Stamped 317.',
        },
      ],
      'nodes': [
        {
          'id': 'start',
          'type': 'text',
          'text': 'Begin.',
          'items': ['key'],
          'evidence': [
            {'id': 'clue', 'title': 'Clue'},
          ],
          'choices': [
            {'text': 'End', 'target': 'end'},
          ],
        },
        {'id': 'end', 'type': 'text', 'text': 'Done.'},
      ],
    });

    expect(story.startNodeId, 'start');
    expect(story.evidenceConfig.label, 'Memories');
    expect(story.evidenceConfig.showCount, false);
    expect(story.evidenceConfig.enabled, true);
    expect(story.inventoryConfig.label, 'Items');
    expect(story.inventoryCatalog['key']!.detail, 'Stamped 317.');
    expect(story.node('end').isEnding, true);
  });

  test('string-id grants do not overwrite rich catalog entries', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'evidence': [
        {'id': 'ledger', 'title': 'Ledger', 'description': 'Payments.', 'detail': 'Three payments.'},
      ],
      'nodes': [
        {
          'id': 'a',
          'type': 'text',
          'text': 'x',
          'evidence': ['ledger'],
        },
      ],
    });

    expect(story.evidenceCatalog['ledger']!.detail, 'Three payments.');
  });

  test('battle node parses dice, targets, and modifiers', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'fight'},
      'nodes': [
        {
          'id': 'fight',
          'type': 'battle',
          'text': 'Fight!',
          'player_dice': '2d6',
          'win_target': 'won',
          'lose_target': 'lost',
          'item_modifiers': [
            {'item': 'knuckles', 'bonus': 2, 'description': 'Brass Knuckles'},
          ],
        },
        {'id': 'won', 'type': 'text', 'text': 'w'},
        {'id': 'lost', 'type': 'text', 'text': 'l'},
      ],
    });

    final battle = story.node('fight').battle!;
    expect(battle.playerDice, '2d6');
    expect(battle.winTargetId, 'won');
    expect(battle.itemModifiers.single.itemId, 'knuckles');
  });

  test('stats parse with renamable labels, health role, and defaults', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'stats': [
        {'id': 'hp', 'label': 'Shields', 'start': 8, 'max': 8, 'role': 'health'},
        {'id': 'ac'},
      ],
      'nodes': [
        {'id': 'a', 'type': 'text', 'text': 'x'},
      ],
    });

    expect(story.stats, hasLength(2));
    final hp = story.stats.first;
    expect(hp.label, 'Shields');
    expect(hp.max, 8);
    expect(hp.role, StatRole.health);
    expect(story.healthStat!.id, 'hp');
    // Undeclared label falls back to a title-cased id.
    expect(story.stats[1].label, 'Ac');
    expect(story.stats[1].start, 0);
  });

  test('stat_changes and set_stats parse on any node', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'nodes': [
        {
          'id': 'a',
          'type': 'text',
          'text': 'x',
          'stat_changes': {'gold': 100, 'hp': -2},
          'set_stats': {'hp': 8},
        },
      ],
    });

    final changes = story.node('a').statChanges;
    expect(changes, hasLength(3));
    expect(changes.any((c) => c.statId == 'gold' && c.amount == 100 && !c.set), true);
    expect(changes.any((c) => c.statId == 'hp' && c.amount == 8 && c.set), true);
  });

  test('check node parses dice, target, and routing', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'trap'},
      'nodes': [
        {
          'id': 'trap',
          'type': 'check',
          'text': 'A blade springs out!',
          'dice': '1d20',
          'target': 13,
          'success_target': 'safe',
          'failure_target': 'hurt',
        },
        {'id': 'safe', 'type': 'text', 'text': 's'},
        {'id': 'hurt', 'type': 'text', 'text': 'h'},
      ],
    });

    final check = story.node('trap').check!;
    expect(check.dice, '1d20');
    expect(check.target, 13);
    expect(check.successTargetId, 'safe');
    expect(check.failureTargetId, 'hurt');
  });

  test('combat node parses enemy, player, and death routing', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'fight'},
      'nodes': [
        {
          'id': 'fight',
          'type': 'combat',
          'text': 'A creature lunges!',
          'enemy': {'label': 'Cave Lurker', 'hp': 6, 'hit_target': 9, 'damage': '1d6', 'hits_on': 12},
          'player': {'damage': '1d8', 'damage_bonus': 2, 'hit_bonus': 1},
          'health_stat': 'hp',
          'win_target': 'won',
          'lose_target': 'died',
          'flee_target': 'fled',
        },
        {'id': 'won', 'type': 'text', 'text': 'w'},
        {'id': 'died', 'type': 'text', 'text': 'd'},
        {'id': 'fled', 'type': 'text', 'text': 'f'},
      ],
    });

    final combat = story.node('fight').combat!;
    expect(combat.enemyLabel, 'Cave Lurker');
    expect(combat.enemyHp, 6);
    expect(combat.enemyHitTarget, 9);
    expect(combat.monsterHitsOn, 12);
    expect(combat.playerDamage, '1d8');
    expect(combat.playerDamageBonus, 2);
    expect(combat.winTargetId, 'won');
    expect(combat.loseTargetId, 'died');
    expect(combat.fleeTargetId, 'fled');
  });
}

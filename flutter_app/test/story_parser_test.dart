import 'package:flutter_test/flutter_test.dart';
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
}

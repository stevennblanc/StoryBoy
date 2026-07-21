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

  test('equipment parses slot, effects, and weapon stats with renamable label', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'collections': {
        'equipment': {'label': 'Gear'},
      },
      'equipment': [
        {'id': 'leather', 'title': 'Leather', 'slot': 'armor', 'equip_effects': {'ac': 2}},
        {'id': 'sword', 'title': 'Sword', 'slot': 'weapon', 'damage': '1d6', 'damage_bonus': 1, 'hit_bonus': 1},
      ],
      'nodes': [
        {'id': 'a', 'type': 'text', 'text': 'x'},
      ],
    });

    expect(story.equipmentConfig.label, 'Gear');
    expect(story.equipmentConfig.enabled, true);
    final leather = story.equipmentCatalog['leather']!;
    expect(leather.slot, 'armor');
    expect(leather.equipEffects['ac'], 2);
    expect(leather.isEquippable, true);
    final sword = story.equipmentCatalog['sword']!;
    expect(sword.damage, '1d6');
    expect(sword.damageBonus, 1);
    expect(sword.hitBonus, 1);
  });

  test('armor stat role and combat armor_stat parse', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'fight'},
      'stats': [
        {'id': 'ac', 'label': 'Shields', 'start': 9, 'role': 'armor'},
      ],
      'nodes': [
        {
          'id': 'fight',
          'type': 'combat',
          'text': 'x',
          'enemy': {'label': 'Drone', 'hp': 4, 'hit_target': 8, 'damage': '1d4', 'attack_bonus': 2},
          'armor_stat': 'ac',
          'win_target': 'won',
          'lose_target': 'died',
        },
        {'id': 'won', 'type': 'text', 'text': 'w'},
        {'id': 'died', 'type': 'text', 'text': 'd'},
      ],
    });

    expect(story.armorStat!.id, 'ac');
    expect(story.armorStat!.label, 'Shields');
    final combat = story.node('fight').combat!;
    expect(combat.armorStatId, 'ac');
    expect(combat.enemyAttackBonus, 2);
  });

  test('shop node parses currency, items, and return target', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'shop'},
      'equipment': [
        {'id': 'sword', 'title': 'Sword', 'slot': 'weapon', 'damage': '1d6'},
      ],
      'inventory': [
        {'id': 'torch', 'title': 'Torch'},
      ],
      'nodes': [
        {
          'id': 'shop',
          'type': 'shop',
          'text': 'Buy something.',
          'currency_stat': 'gold',
          'items': [
            {'equipment': 'sword', 'price': 10},
            {'inventory': 'torch', 'price': 1},
          ],
          'return_target': 'town',
        },
        {'id': 'town', 'type': 'text', 'text': 't'},
      ],
    });

    final shop = story.node('shop').shop!;
    expect(shop.currencyStatId, 'gold');
    expect(shop.returnTargetId, 'town');
    expect(shop.items, hasLength(2));
    expect(shop.items[0].itemId, 'sword');
    expect(shop.items[0].collection, 'equipment');
    expect(shop.items[0].price, 10);
    expect(shop.items[1].collection, 'inventory');
  });

  test('map fragments parse with renamable label and node reveals', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'collections': {
        'map': {'label': 'Star Chart'},
      },
      'map': [
        {'id': 'entry_hall', 'title': 'Entry Hall', 'image': 'images/hall.jpg'},
        {'id': 'gallery', 'title': 'Gallery', 'image': 'images/gallery.jpg'},
      ],
      'nodes': [
        {
          'id': 'a',
          'type': 'text',
          'text': 'x',
          'reveal_map': 'entry_hall',
          'choices': [
            {'text': 'On', 'target': 'b'},
          ],
        },
        {
          'id': 'b',
          'type': 'text',
          'text': 'y',
          'reveal_map': ['gallery'],
        },
      ],
    });

    expect(story.mapConfig.label, 'Star Chart');
    expect(story.mapConfig.enabled, true);
    expect(story.mapCatalog.keys.toList(), ['entry_hall', 'gallery']);
    expect(story.mapCatalog['entry_hall']!.image, 'images/hall.jpg');
    expect(story.node('a').mapRevealIds, ['entry_hall']);
    expect(story.node('b').mapRevealIds, ['gallery']);
  });

  test('map system stays off when a book never uses it', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'nodes': [
        {'id': 'a', 'type': 'text', 'text': 'x'},
      ],
    });
    expect(story.mapConfig.enabled, false);
    expect(story.equipmentConfig.enabled, false);
  });

  test('ability scores map to default tier modifiers; plain stats are their value', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'stats': [
        {'id': 'str', 'label': 'Strength', 'start': 16, 'ability': true},
        {'id': 'wits', 'label': 'Wits', 'start': 4},
      ],
      'nodes': [
        {'id': 'a', 'type': 'text', 'text': 'x'},
      ],
    });
    final str = story.statById('str')!;
    expect(str.abilityScore, true);
    expect(str.modifier(16), 2);
    expect(str.modifier(9), 0);
    expect(str.modifier(3), -3);
    expect(str.modifier(18), 3);
    // plain stat: value is its own modifier
    expect(story.statById('wits')!.modifier(4), 4);
  });

  test('custom modifier_table overrides default tiers', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'stats': [
        {
          'id': 'luck',
          'start': 10,
          'modifier_table': [
            {'min': 1, 'max': 9, 'mod': 0},
            {'min': 10, 'max': 20, 'mod': 5},
          ],
        },
      ],
      'nodes': [
        {'id': 'a', 'type': 'text', 'text': 'x'},
      ],
    });
    final luck = story.statById('luck')!;
    expect(luck.abilityScore, true);
    expect(luck.modifier(10), 5);
    expect(luck.modifier(9), 0);
  });

  test('choice requirements parse: items, stats, flags, character', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'a'},
      'nodes': [
        {
          'id': 'a',
          'type': 'text',
          'text': 'x',
          'set_flags': ['met_goblin'],
          'clear_flags': ['door_shut'],
          'choices': [
            {'text': 'Unlock', 'target': 'b', 'requires': {'item': 'iron_key'}},
            {
              'text': 'Bribe',
              'target': 'b',
              'requires': {'stat': {'gold': {'gt': 26}}},
              'locked_text': 'Bribe (need 27 gold)',
            },
            {'text': 'Gloat', 'target': 'b', 'requires': {'flag': 'killed_worm'}},
            {'text': 'Ward', 'target': 'b', 'requires': {'character': 'witch'}},
            {'text': 'Plain', 'target': 'b'},
          ],
        },
        {'id': 'b', 'type': 'text', 'text': 'y'},
      ],
    });

    final node = story.node('a');
    expect(node.setFlags, ['met_goblin']);
    expect(node.clearFlags, ['door_shut']);
    expect(node.choices[0].requires!.items, ['iron_key']);
    // gt: 26 becomes min 27
    expect(node.choices[1].requires!.stats['gold']!.min, 27);
    expect(node.choices[1].requires!.stats['gold']!.test(27), true);
    expect(node.choices[1].requires!.stats['gold']!.test(26), false);
    expect(node.choices[1].lockedText, 'Bribe (need 27 gold)');
    expect(node.choices[2].requires!.flags, ['killed_worm']);
    expect(node.choices[3].requires!.characters, ['witch']);
    expect(node.choices[4].requires, isNull);
  });

  test('map locations can carry requirements', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'hub'},
      'nodes': [
        {
          'id': 'hub',
          'type': 'map',
          'text': 'Where to?',
          'locations': [
            {'title': 'Locked wing', 'target': 'wing', 'requires': {'not_flag': 'wing_sealed'}},
          ],
        },
        {'id': 'wing', 'type': 'text', 'text': 'w'},
      ],
    });
    expect(story.node('hub').mapLocations.single.requires!.notFlags, ['wing_sealed']);
  });

  test('usable items parse their effects, charges, and verb', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'start'},
      'stats': [
        {'id': 'hp', 'role': 'health', 'start': 10, 'max': 12},
      ],
      'inventory': [
        {
          'id': 'tonic',
          'title': 'Brine Tonic',
          'use': {'hp': 6},
          'uses': 2,
          'use_label': 'Drink',
          'use_text': 'It burns going down.',
        },
        {'id': 'rope', 'title': 'Rope'},
      ],
      'nodes': [
        {'id': 'start', 'type': 'text', 'text': 's'},
      ],
    });
    final tonic = story.inventoryCatalog['tonic']!;
    expect(tonic.isUsable, isTrue);
    expect(tonic.useEffects, {'hp': 6});
    expect(tonic.uses, 2);
    expect(tonic.useLabel, 'Drink');
    expect(tonic.useText, 'It burns going down.');

    // An item with no use block is not usable, and defaults stay sane.
    final rope = story.inventoryCatalog['rope']!;
    expect(rope.isUsable, isFalse);
    expect(rope.uses, 1);
    expect(rope.useLabel, 'Use');
  });

  test('not_character excludes the chosen class', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'start'},
      'nodes': [
        {
          'id': 'start',
          'type': 'text',
          'text': 's',
          'choices': [
            {'text': 'The hard way', 'target': 'end', 'requires': {'not_character': 'witch'}},
          ],
        },
        {'id': 'end', 'type': 'text', 'text': 'e'},
      ],
    });
    expect(story.node('start').choices.single.requires!.notCharacters, ['witch']);
  });

  test('lore nodes keep their entries as titled sections', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'start'},
      'nodes': [
        {
          'id': 'start',
          'type': 'lore',
          'return_to': 'after',
          'entries': [
            {'title': 'Gear', 'text': 'Equip what you carry.'},
            {'title': 'Fighting', 'text': 'A fight runs in rounds.'},
          ],
        },
        {'id': 'after', 'type': 'text', 'text': 'a'},
      ],
    });
    final node = story.node('start');
    expect(node.loreEntries.map((e) => e.title), ['Gear', 'Fighting']);
    expect(node.loreEntries.last.text, 'A fight runs in rounds.');
    expect(node.choices.single.targetId, 'after');
  });

  test('characters parse with stats, gear, and combat hit/damage stats', () {
    final story = parseStoryGamebook({
      'metadata': {'title': 'T', 'folder': 't', 'start_node': 'start'},
      'stats': [
        {'id': 'str', 'ability': true, 'start': 10},
      ],
      'equipment': [
        {'id': 'sword', 'title': 'Sword', 'slot': 'weapon', 'damage': '1d6'},
      ],
      'characters': [
        {
          'id': 'warrior',
          'name': 'Bram the Warrior',
          'description': 'Strong.',
          'stats': {'str': 16},
          'equipment': ['sword'],
          'equipped': {'weapon': 'sword'},
          'start_node': 'start',
        },
      ],
      'nodes': [
        {
          'id': 'start',
          'type': 'combat',
          'text': 'x',
          'enemy': {'label': 'Brute', 'hp': 5, 'hit_target': 10, 'damage': '1d6'},
          'player': {'damage': '1d4', 'hit_stat': 'str', 'damage_stat': 'str'},
          'win_target': 'won',
          'lose_target': 'died',
        },
        {'id': 'won', 'type': 'text', 'text': 'w'},
        {'id': 'died', 'type': 'text', 'text': 'd'},
      ],
    });
    expect(story.characters, hasLength(1));
    final warrior = story.characters.first;
    expect(warrior.name, 'Bram the Warrior');
    expect(warrior.stats['str'], 16);
    expect(warrior.equipmentIds, ['sword']);
    expect(warrior.equippedBySlot['weapon'], 'sword');
    final combat = story.node('start').combat!;
    expect(combat.hitStatId, 'str');
    expect(combat.damageStatId, 'str');
  });
}

import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'src/app_model.dart';
import 'src/data/progress_store.dart';
import 'src/screens/library_screen.dart';
import 'src/screens/settings_screen.dart';
import 'src/screens/store_screen.dart';
import 'src/theme.dart';

const supabaseUrl = 'https://ndgguqbrhatvcqetgeks.supabase.co';
const supabaseKey = 'sb_publishable_H46jhfVAHcILTZQyu7BleA_I2YohwoI';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Supabase.initialize(url: supabaseUrl, publishableKey: supabaseKey);
  final progress = await ProgressStore.load();
  runApp(StoryBoyApp(model: AppModel(progress)));
}

class StoryBoyApp extends StatelessWidget {
  const StoryBoyApp({super.key, required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'StoryBoy',
      theme: storyBoyTheme(),
      debugShowCheckedModeBanner: false,
      home: HomeShell(model: model),
    );
  }
}

class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.model});

  final AppModel model;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _tab = 0;

  @override
  void initState() {
    super.initState();
    widget.model.refreshAll();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.model,
      builder: (context, _) {
        return Scaffold(
          body: IndexedStack(
            index: _tab,
            children: [
              LibraryScreen(model: widget.model),
              StoreScreen(model: widget.model),
              SettingsScreen(model: widget.model),
            ],
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: _tab,
            onDestinationSelected: (index) => setState(() => _tab = index),
            destinations: const [
              NavigationDestination(icon: Icon(Icons.menu_book_outlined), label: 'Library'),
              NavigationDestination(icon: Icon(Icons.shopping_bag_outlined), label: 'Store'),
              NavigationDestination(icon: Icon(Icons.tune), label: 'Settings'),
            ],
          ),
        );
      },
    );
  }
}

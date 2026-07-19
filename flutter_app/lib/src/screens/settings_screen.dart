import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import '../app_model.dart';
import '../theme.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key, required this.model});

  final AppModel model;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _displayNameController = TextEditingController();
  bool _busy = false;
  String _message = '';
  double _fontScale = 1.0;

  SupabaseClient get _supabase => Supabase.instance.client;

  @override
  void initState() {
    super.initState();
    _fontScale = widget.model.progress.fontScale;
    _displayNameController.text = widget.model.displayName;
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _displayNameController.dispose();
    super.dispose();
  }

  Future<void> _run(Future<String> Function() action) async {
    setState(() {
      _busy = true;
      _message = '';
    });
    try {
      final message = await action();
      setState(() => _message = message);
    } on AuthException catch (error) {
      setState(() => _message = error.message);
    } catch (error) {
      setState(() => _message = 'Something went wrong. $error');
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _signIn() => _run(() async {
        await _supabase.auth.signInWithPassword(
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
        _displayNameController.text = widget.model.displayName;
        return '';
      });

  Future<void> _signUp() => _run(() async {
        final displayName = _displayNameController.text.trim();
        final response = await _supabase.auth.signUp(
          email: _emailController.text.trim(),
          password: _passwordController.text,
          data: displayName.isEmpty ? null : {'display_name': displayName},
        );
        if (response.session == null) {
          return 'Account created. Check your email for a confirmation link, then sign in.';
        }
        return '';
      });

  Future<void> _saveProfile() => _run(() async {
        final displayName = _displayNameController.text.trim();
        await _supabase.auth.updateUser(UserAttributes(data: {'display_name': displayName}));
        final userId = _supabase.auth.currentUser?.id;
        if (userId != null) {
          await _supabase
              .from('profiles')
              .upsert({'id': userId, 'display_name': displayName});
        }
        return 'Profile saved.';
      });

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.model,
      builder: (context, _) {
        return SafeArea(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
            children: [
              const Text('Settings', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w600)),
              const SizedBox(height: 16),
              _accountPanel(),
              const SizedBox(height: 16),
              _panel(
                title: 'Reader text size',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Slider(
                      value: _fontScale,
                      min: 0.8,
                      max: 1.4,
                      divisions: 6,
                      onChanged: (value) {
                        setState(() => _fontScale = value);
                        widget.model.progress.saveFontScale(value);
                      },
                    ),
                    Text('${(_fontScale * 100).round()}%', style: mutedStyle),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              _panel(
                title: 'About',
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('StoryBoy 0.20.0 (Flutter)', style: TextStyle(fontSize: 16)),
                    SizedBox(height: 6),
                    Text(
                      'Interactive gamebooks with a shared library across the app and story-boy.vercel.app.',
                      style: mutedStyle,
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _accountPanel() {
    final model = widget.model;
    if (model.isSignedIn) {
      final ownedCount = model.ownedBookIds.length;
      return _panel(
        title: 'Account',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(model.user?.email ?? '', style: const TextStyle(fontSize: 16)),
            const SizedBox(height: 4),
            Text(
              '$ownedCount book${ownedCount == 1 ? '' : 's'} in your StoryBoy library',
              style: mutedStyle,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _displayNameController,
              decoration: const InputDecoration(labelText: 'Display name'),
            ),
            const SizedBox(height: 10),
            FilledButton(
              onPressed: _busy ? null : _saveProfile,
              child: const Text('Save profile'),
            ),
            if (_message.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(_message, style: mutedStyle),
              ),
            TextButton(
              onPressed: _busy
                  ? null
                  : () => _run(() async {
                        await _supabase.auth.signOut();
                        return '';
                      }),
              child: const Text('Sign out'),
            ),
          ],
        ),
      );
    }

    return _panel(
      title: 'Account',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Sign in to keep your StoryBoy library and profile across devices.',
            style: mutedStyle,
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _emailController,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _passwordController,
            obscureText: true,
            decoration: const InputDecoration(labelText: 'Password'),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _displayNameController,
            decoration: const InputDecoration(labelText: 'Display name (new accounts)'),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: FilledButton(
                  onPressed: _busy ? null : _signIn,
                  child: const Text('Sign in'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: FilledButton(
                  onPressed: _busy ? null : _signUp,
                  child: const Text('Create account'),
                ),
              ),
            ],
          ),
          if (_message.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Text(_message, style: mutedStyle),
            ),
        ],
      ),
    );
  }

  Widget _panel({required String title, required Widget child}) {
    return Container(
      width: double.infinity,
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
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';

/// StoryBoy's monochrome-first palette, mirroring the web reader.
abstract final class SbColors {
  static const background = Color(0xFF050505);
  static const surface = Color(0xFF111111);
  static const surface2 = Color(0xFF1C1C1C);
  static const text = Color(0xFFF5F5F5);
  static const muted = Color(0xFFA9A9A9);
  static const line = Color(0xFF333333);
  static const accent = Color(0xFF8BD8E8);
  static const accentText = Color(0xFF050505);
  static const danger = Color(0xFFF1A7A7);
}

ThemeData storyBoyTheme() {
  const scheme = ColorScheme.dark(
    surface: SbColors.background,
    onSurface: SbColors.text,
    primary: SbColors.accent,
    onPrimary: SbColors.accentText,
    secondary: SbColors.surface2,
    onSecondary: SbColors.text,
    outline: SbColors.line,
    error: SbColors.danger,
  );

  final base = ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: SbColors.background,
  );

  return base.copyWith(
    textTheme: base.textTheme.apply(
      bodyColor: SbColors.text,
      displayColor: SbColors.text,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: SbColors.background,
      foregroundColor: SbColors.text,
      elevation: 0,
      centerTitle: false,
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: SbColors.surface,
      indicatorColor: SbColors.surface2,
      iconTheme: WidgetStateProperty.resolveWith((states) {
        return IconThemeData(
          color: states.contains(WidgetState.selected) ? SbColors.accent : SbColors.muted,
        );
      }),
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        return TextStyle(
          fontSize: 12,
          color: states.contains(WidgetState.selected) ? SbColors.accent : SbColors.muted,
        );
      }),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: SbColors.surface2,
        foregroundColor: SbColors.text,
        minimumSize: const Size.fromHeight(48),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(foregroundColor: SbColors.accent),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: SbColors.surface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: SbColors.line),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: SbColors.line),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: SbColors.accent),
      ),
      labelStyle: const TextStyle(color: SbColors.muted),
      hintStyle: const TextStyle(color: SbColors.muted),
    ),
    dividerTheme: const DividerThemeData(color: SbColors.line, thickness: 1),
    sliderTheme: const SliderThemeData(
      activeTrackColor: SbColors.accent,
      thumbColor: SbColors.accent,
      inactiveTrackColor: SbColors.surface2,
    ),
  );
}

/// Serif style for story prose, matching the web reader's Georgia look.
const readerTextStyle = TextStyle(
  fontFamily: 'serif',
  fontSize: 19,
  height: 1.58,
  color: SbColors.text,
);

const mutedStyle = TextStyle(color: SbColors.muted, fontSize: 14);

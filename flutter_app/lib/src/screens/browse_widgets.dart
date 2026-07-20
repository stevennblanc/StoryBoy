import 'package:flutter/material.dart';

import '../app_model.dart';
import '../theme.dart';

/// Screen title row with trailing action buttons.
class BrowseHeader extends StatelessWidget {
  const BrowseHeader({super.key, required this.title, required this.actions, this.subtitle});

  final String title;
  final String? subtitle;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w600)),
                if (subtitle != null && subtitle!.isNotEmpty) Text(subtitle!, style: mutedStyle),
              ],
            ),
          ),
          ...actions,
        ],
      ),
    );
  }
}

class BrowseSearchField extends StatelessWidget {
  const BrowseSearchField({super.key, required this.value, required this.onChanged, required this.hint});

  final String value;
  final ValueChanged<String> onChanged;
  final String hint;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      initialValue: value,
      onChanged: onChanged,
      decoration: InputDecoration(
        hintText: hint,
        prefixIcon: const Icon(Icons.search, color: SbColors.muted, size: 20),
        isDense: true,
      ),
    );
  }
}

/// Grid/list toggle plus optional sort options in one menu.
class ViewMenuButton extends StatelessWidget {
  const ViewMenuButton({super.key, required this.model, this.sortOptions = false});

  final AppModel model;
  final bool sortOptions;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      icon: Icon(
        model.displayMode == DisplayMode.grid ? Icons.grid_view : Icons.view_list,
        color: SbColors.text,
      ),
      color: SbColors.surface2,
      onSelected: (value) {
        switch (value) {
          case 'grid':
            model.setDisplayMode(DisplayMode.grid);
          case 'list':
            model.setDisplayMode(DisplayMode.list);
          case 'sort_title':
            model.setLibrarySort(LibrarySort.title);
          case 'sort_author':
            model.setLibrarySort(LibrarySort.author);
        }
      },
      itemBuilder: (context) => [
        _item('grid', 'Grid view', checked: model.displayMode == DisplayMode.grid),
        _item('list', 'List view', checked: model.displayMode == DisplayMode.list),
        if (sortOptions) ...[
          const PopupMenuDivider(),
          _item('sort_title', 'Sort by title', checked: model.librarySort == LibrarySort.title),
          _item('sort_author', 'Sort by author', checked: model.librarySort == LibrarySort.author),
        ],
      ],
    );
  }

  PopupMenuItem<String> _item(String value, String label, {required bool checked}) {
    return PopupMenuItem<String>(
      value: value,
      child: Row(
        children: [
          SizedBox(
            width: 24,
            child: checked ? const Icon(Icons.check, size: 16, color: SbColors.accent) : null,
          ),
          Text(label),
        ],
      ),
    );
  }
}

/// Genre filter (plus optional progress filter) menu.
class FilterMenuButton extends StatelessWidget {
  const FilterMenuButton({
    super.key,
    required this.genres,
    required this.selectedGenre,
    required this.onGenre,
    this.progressFilter,
    this.onProgressFilter,
  });

  final List<String> genres;
  final String? selectedGenre;
  final ValueChanged<String?> onGenre;
  final ProgressFilter? progressFilter;
  final ValueChanged<ProgressFilter>? onProgressFilter;

  bool get _active =>
      selectedGenre != null || (progressFilter != null && progressFilter != ProgressFilter.all);

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      icon: Icon(Icons.filter_list, color: _active ? SbColors.accent : SbColors.text),
      color: SbColors.surface2,
      onSelected: (value) {
        if (value == 'all') {
          onGenre(null);
          onProgressFilter?.call(ProgressFilter.all);
        } else if (value == 'in_progress') {
          onProgressFilter?.call(ProgressFilter.inProgress);
        } else if (value == 'not_started') {
          onProgressFilter?.call(ProgressFilter.notStarted);
        } else if (value.startsWith('genre:')) {
          onGenre(value.substring(6));
        }
      },
      itemBuilder: (context) => [
        const PopupMenuItem(value: 'all', child: Text('All')),
        if (onProgressFilter != null) ...[
          PopupMenuItem(
            value: 'in_progress',
            child: Text(
              'In progress',
              style: TextStyle(
                color: progressFilter == ProgressFilter.inProgress ? SbColors.accent : SbColors.text,
              ),
            ),
          ),
          PopupMenuItem(
            value: 'not_started',
            child: Text(
              'Not started',
              style: TextStyle(
                color: progressFilter == ProgressFilter.notStarted ? SbColors.accent : SbColors.text,
              ),
            ),
          ),
        ],
        if (genres.isNotEmpty) const PopupMenuDivider(),
        for (final genre in genres)
          PopupMenuItem(
            value: 'genre:$genre',
            child: Text(
              genre,
              style: TextStyle(color: genre == selectedGenre ? SbColors.accent : SbColors.text),
            ),
          ),
      ],
    );
  }
}

bool matchesQuery(String query, List<String> fields) {
  final normalized = query.trim().toLowerCase();
  if (normalized.isEmpty) return true;
  return fields.any((field) => field.toLowerCase().contains(normalized));
}

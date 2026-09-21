# VEX Files — Feature Specification

## Core promise

A phone file manager should feel like a tool, not a content feed.

### Navigation

- Internal storage root
- Folder drilling
- Parent navigation
- Search in current directory
- Recent paths
- Favorite folders

### File operations

| Feature | v0.1 |
|---|---|
| Open | Yes |
| Share | Yes |
| Copy | Yes |
| Move | Yes |
| Delete | Yes |
| Create folder | Yes |
| Multi-select | Yes |
| Sort | Yes |
| Search | Yes |
| ZIP create | Yes |
| ZIP extract | Yes |
| Rename | Single-item rename |
| Recycle bin | Planned |

### Archive workflow

Selecting files and pressing `ZIP` creates an archive in the current directory. Opening a `.zip` file offers direct extraction into a unique sibling folder.

### Sharing workflow

A single selected regular file is exposed through Android's `FileProvider` and passed to the native sharing surface with a read grant.

### Home workflow

The bottom strip intentionally stays tiny:

```text
HOME     BACK     NEW
```

Selection replaces this with contextual actions:

```text
2 SEL     COPY     MOVE     ZIP     SHARE     DEL
```

## Interaction principles

- Tap folder → open
- Tap file → open with native app
- Long press → enter selection
- Tap more selected rows → add/remove selection
- One action bar → all bulk operations
- Search field appears only when requested
- Sort is a menu action, not a permanent control

## Motion

Target durations:

- row feedback: 90–120 ms
- navigation fade/translation: 120 ms
- dialog: default platform motion

Avoid chained, looping, spring-heavy, or decorative motion.

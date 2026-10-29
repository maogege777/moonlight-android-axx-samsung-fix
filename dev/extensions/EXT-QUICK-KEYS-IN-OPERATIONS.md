# EXT-QUICK-KEYS-IN-OPERATIONS

- Change type: extension development
- Status: implemented (v5), pending on-device verification
- Added: 2026-10-15 · v4 revision: 2026-10-16 · v5 revision: 2026-10-29
- Purpose: fold the stream menu's "Quick keys" (「快捷键」) feature into the sidebar
  "Operations" (「操作」) panel as a "Custom quick keys" (「自定义快捷键」) grid group, and add
  export/import of the user's custom quick keys in Settings › Interface & General.

## v4 changes (user-confirmed, 2026-10-16)

1. **Default keys are seeded data, not injected built-ins.** On the first run after install the
   app writes one default group into the normal user store, once (guarded by a `seeded` flag in
   the meta file `quick_axi_keyAssemble_meta`): the original 11 built-in Windows combos from the
   old "Quick keys" list, with names, icons and VK combinations kept verbatim (e.g.
   "ESC (退出/菜单)"; fixed ids with the `quick_assemble_key_seed_` prefix). Afterwards they are
   ordinary entries: editable, deletable, exported and imported like anything the user adds.
   Import no longer "resets" anything - it replaces everything, and a successful import also
   marks the store as seeded so the default group is never appended again.
   `seed_version` version-guards the definition: the short-lived name-less v1 definition (one
   build on 2026-10-16) is upgraded once by `migrateSeedItems`, which restores names and VK
   combinations only for seed items whose name is still empty - user-renamed and user-deleted
   entries are left alone.
2. **The editor's reset button is "清空按键" (clear keys only)** - it clears the captured
   combination but keeps the entered name.
3. **Names may be empty.** Saving a quick key without a name is allowed (virtual/gamepad
   button combos still require one). A grid button with an empty name shows only the icon plus
   the combination text (desc); the editor no longer auto-overwrites the name field when keys
   are tapped (keyFrom==1 only).
4. **Shared dialog panels are opaque.** `bg_update_dialog_panel.xml` (shared by 8 dialogs incl.
   the import/export confirmations) changed from `#61262626` to `#FF262626`.
5. **Tapping a quick key closes the whole menu stack**, not just the Operations panel:
   `BaseGameMenuFragmentDialog.dismissAllDialogs()` dismisses every live menu dialog.

## User-visible behavior (current)

- The main menu has no "Quick keys" entry; the sidebar "Operations" panel starts with a
  "自定义快捷键" group holding the seeded default group (the old 11 built-in combos, same names
  and icons) plus the user's own combos.
- Grid buttons show an icon, the name when one was entered, and the combination text below;
  tapping sends the combination and closes all menus, long-pressing opens Edit / Delete.
- The panel's top-right "Add" button opens the combination editor with the optional SVG icon
  picker (fixed 19 icons; no selection keeps the default keyboard icon).
- Settings › Interface & General › Custom quick keys exports every entry (icons and order
  included) to a JSON file, and imports a file replacing all entries after a confirmation.
- "禁用默认的特殊按键指令" hides seeded entries (they remain in the store and in exports) and
  prevents seeding on a fresh device.

## Files

- Added: `QuickKeyStore.java`, `item_game_menu_quick_key.xml`, `ic_axi_qk_*.xml` (19 vectors),
  `dev/gen_quick_icons.ps1`, `dev/check_resources.ps1`, `dev/check_java_balance.ps1`
- Modified: `GameMenuFragment.java`, `GameFunctionFragment.java`, `GameListQuickFragment.java`,
  `GameKeyboardUpdateFragment.java`, `bean/GameMenuQuickBean.java`, `StreamSettings.java`,
  `SettingsCatalog.java`, `Game.java` (seed hook in onCreate),
  `BaseFragmentDialog/BaseGameMenuFragmentDialog.java` (dismissAllDialogs),
  `dialog_game_menu.xml`, `dialog_game_menu_function.xml`, `dialog_game_menu_keyboard_group_add.xml`,
  `bg_update_dialog_panel.xml`, `preferences.xml`

## Storage

- User entries incl. the seeded group: SharedPreferences `quick_axi_keyAssemble`, key prefix
  `quick_assemble_key_`, values `GameMenuQuickBean` JSON (Gson tolerates `icon`/`sort`).
  Seeded ids use the fixed `quick_assemble_key_seed_` prefix.
- `quick_axi_keyAssemble_meta` holds the one-shot `seeded` flag and the `seed_version` of the
  default-group definition (see item 1 for the v1 → v2 name-restoration migration).
- v3 leftovers no longer read or written: the `builtin_` override records (skipped on load, ids
  rewritten on import) and `quick_builtin_hidden`. Old installs keep their user entries; the old
  injected built-ins simply disappear and the seeded group appears instead (only when the store
  has no user entries and seeding has not happened).

## Sending constraint

- User entries added through the editor carry `codes` (Android key codes, same source as the
  editor's on-screen keyboard tags); `GameFunctionFragment.sendQuickKey` sends them through
  `Game.keyboardEvent`, which translates to VK codes via `KeyboardTranslator`.
- The seeded default group carries VK-code `datas` exactly like the old built-in list, so
  `sendQuickKey` routes it through `GameMenuFragment.sendKeys` - identical behavior to the old
  "Quick keys" panel. Entries imported from old files that only carry `datas` take the same path.

## Import/export format

- v2 export: `{type, version:2, exportedAt, data:[...]}` only - what you export is what you
  import, no separate built-in state. `name` may be empty; entries without any keys are skipped.
- v1 files still import: their `data` array is read, their `builtin` section is ignored.

## Verification so far

`dev/check_resources.ps1` (19 icons + 6 layouts/drawables parse) and `dev/check_java_balance.ps1`
(9 touched Java files) both pass. A real compile requires GitHub Actions `assembleNonRootDebug`;
this machine has no JDK or Android SDK. On-device checks still needed: first-run seeding on a
fresh install and on an old install with existing user keys, empty-name buttons, 清空按键 keeping
the name, opaque dialogs over the game画面, tap closing the full menu stack, export → fresh
device → import round-trip.

---

## v5 changes (user-confirmed, 2026-10-29)

### 1. Icon picker is scene-driven, not `keyFrom`-driven

`keyFrom == 1` means "quick-key semantics" (empty name allowed, quick-key id prefix, mouse/function
tabs hidden) and is shared by **two** live entry points: the Operations panel's custom quick keys
and the virtual keyboard's combo bar. Commit `426374a4` used `keyFrom == 1` as a proxy for "combo
bar" to hide the icon row, which also removed the icon picker from the Operations panel - the only
place whose buttons actually render an icon.

- `GameKeyboardUpdateFragment` now has `showIconPicker` (default `false`) and `hintText`
  (default empty = hidden); the icon row and the orange hint are driven by those two fields.
- `GameFunctionFragment.openQuickKeyEditor` sets `showIconPicker(true)` and
  `"保存后长按按钮可以进行编辑和删除"`.
- `KeyBoardLayoutController.openComboEditor` leaves the icon picker off and sets
  `"保存后长按组合键气泡可以编辑和删除"`.
- `KeyBoardController` (virtual-key combo editor) sets neither, so both are hidden.
- `tx_save_hint` got an id and defaults to `gone`, so the hint no longer shows in the virtual-key
  editor where "组合键气泡" does not exist.

### 2. 全键盘快捷键 has its own store (no longer shares QuickKeyStore)

`FullKeyboardKeyStore` (new; v6 renamed from `ComboKeyStore`) stores the 全键盘快捷键 entries in
SharedPreferences `combo_axi_keyAssemble` (prefix `combo_assemble_key_`; both the pref name and the
`axi_combo_keys` export type are kept verbatim so data written by earlier builds still loads).
One-shot migration copies the QuickKeyStore **user** entries
(seed group excluded, semantics unchanged) at first run, guarded by
`combo_axi_keyAssemble_meta.migrated_from_quick_keys`, so an upgraded device shows exactly the
same chips as before and the two lists are independent afterwards.
`KeyBoardLayoutController.rebuildComboBar / openComboEditor / confirmDeleteCombo` now read and
write `FullKeyboardKeyStore`.

### 3. One backup file for all three key stores, selective restore

`AxiKeyBackup` (new) exports a single file:

```json
{ "type": "axi_key_backup", "version": 1, "exportedAt": "...",
  "quickKeys": { "type": "axi_quick_keys", "version": 2, "data": [ ... ] },
  "comboKeys": { "type": "axi_combo_keys", "version": 1, "data": [ ... ] },
  "keyboardLayouts": { "OSC_Keyboard": [ ... ], "OSC_Keyboard_2": [ ... ] },
  "keyboardScheme": "OSC_Keyboard",
  "gamepadLayouts": { "gamePad": [ ... ], "gamePad_2": [ ... ] },
  "gamepadScheme": "gamePad" }
```

- All **five** virtual-keyboard schemes and all **five** virtual-gamepad schemes are exported (the
  old Settings entry only exported the currently selected keyboard scheme). Schemes with no saved
  file are omitted (they are the default layout).
- Import shows a checklist dialog (自定义快捷键 / 全键盘快捷键 / 虚拟按键布局 / 虚拟手柄布局); only the
  checked sections are restored, and restoring is an overwrite of that section. Unchecked sections
  keep their current content. The checklist is a hand-built `ScrollView` + `CheckBox` view, **not**
  `AlertDialog.setMultiChoiceItems`: combining `setMessage` with multi-choice items on some ROMs
  (Samsung One UI in particular) renders only the message and silently drops the list, which looks
  like "tapping import does nothing". Parse failures now also report the first 40 chars of the file
  and log `axi->import key backup sections=` so a wrong file is diagnosable from the log.
- Old files still import: `type: axi_quick_keys` → quick keys only; a bare JSON array (old
  "虚拟按键配置" export) → the currently selected keyboard scheme only.
- Settings entry `export_quick_keys_file` / `import_quick_keys_file` now live at
  **设置 › 备份与恢复 › 按键配置** (Section added to the existing 备份与恢复 group in
  `SettingsCatalog`; the preference keys are unchanged). The separate
  「导入/导出虚拟按键配置」 and 「导入/导出虚拟手柄配置」 entries stay, as the fine-grained
  "replace one scheme" operations.

### 4. Copy a scheme slot onto another (keyboard and gamepad)

In the long-press scheme popup of both 虚拟手柄 and 虚拟按键, a "复制方案…" row opens a two-step
picker: source scheme → target scheme → overwrite confirmation. `AxiKeyBackup.copyLayout` copies
`files/axi_<scheme>.txt` (same naming for both controller kinds, see
`KeyBoardController`'s `fileName`). If the source scheme has no saved file (i.e. it is the default
layout), the target file is deleted so the target also returns to the default layout - the
confirmation text states this explicitly. If the target is the currently active scheme, the layout
is hot-reloaded through `game.updateVirtualView()`; the active scheme is never switched.

### 5. Dialog dim follows the caller again

`GameKeyboardUpdateFragment` overrode `getDimAmount()` with a hard-coded `0.9f`, which silently
discarded every caller's `setDimAmount(0.8f)` (the base class reads `mDimAmount` in
`BaseGameMenuFragmentDialog.onStart`). The override is removed, so all entry points use 0.8.

### Files added in v5

- `FullKeyboardKeyStore.java`, `AxiKeyBackup.java`

### Files modified in v5

- `GameKeyboardUpdateFragment.java` (scene flags, hint, dim), `GameFunctionFragment.java`
  (`showIconPicker` + hint), `KeyBoardLayoutController.java` (FullKeyboardKeyStore + hint),
  `GameMenuFragment.java` (scheme copy), `QuickKeyStore.java` (`itemJson`/`parseItems` made
  reusable), `SettingsCatalog.java` (new 备份与恢复 › 按键配置 section),
  `StreamSettings.java` (unified export / selective import), `preferences.xml`,
  `dialog_game_menu_keyboard_group_add.xml` (`tx_save_hint` id)

### Storage (v5)

- Combo bar: SharedPreferences `combo_axi_keyAssemble` (prefix `combo_assemble_key_`), meta
  `combo_axi_keyAssemble_meta` with the one-shot `migrated_from_quick_keys` flag.
- Virtual keyboard/gamepad layouts: unchanged, `files/axi_<scheme>.txt` (5 schemes each,
  `keyboard_axi_values` / `gamepad_axi_values`).
- Quick keys: unchanged (`quick_axi_keyAssemble`).

### Still open

- The two orphan fragments `GameListQuickFragment` / `GameListKeyBoardFragment` are unreachable
  (no `new` call anywhere) but still provide `PREF_KEYBOARD_LIST_*` constants used by three files.
  Deleting them should be its own change (move the constants first).
- v5 on-device checks: icon row only in the Operations editor; combo bar unaffected by quick-key
  edits and vice versa; migration keeps existing chips; combined export includes all saved
  keyboard **and** gamepad schemes; selective import leaves unchecked sections alone; copying a
  scheme between slots (both kinds), including the default-layout source case; old export files
  still import.

---

## v6 changes (terminology + simplified keyboard, 2026-10-29)

### 1. Terminology normalized around the three editors

The three entry points into `GameKeyboardUpdateFragment` now share one vocabulary, in UI strings
and in code comments:

| Entry point | Editor title |
|---|---|
| 操作 › 自定义快捷键 | 添加自定义快捷键 / 编辑自定义快捷键 |
| 全键盘顶栏 | 添加全键盘快捷键 / 编辑全键盘快捷键 |
| 虚拟按键布局编辑 | 添加虚拟按键 |

- "组合键条" is gone from UI, comments and docs; the feature is called **全键盘快捷键**.
  `ComboKeyStore` was renamed to `FullKeyboardKeyStore` (new constant names
  `PREF_FULL_KEYBOARD_*`), while every persisted identifier keeps its old value on purpose:
  SharedPreferences `combo_axi_keyAssemble` / prefix `combo_assemble_key_`, and the export
  `type: "axi_combo_keys"` plus the outer JSON key `"comboKeys"` (the parser also accepts
  `"fullKeyboardKeys"` for files written during the transition).
- The confirm dialog for deleting an entry reads 删除全键盘快捷键, and the editor hint is now
  "保存后长按气泡可以编辑和删除" (the 组合键条 wording no longer appears).

### 2. Simplified keyboard: the right-hand Enter is now one real key

`item_layout_axixi_keyboard_simple.xml` used to draw two Enter keys - row 3's (weight 150) and
the one added in `7ddcf14a` at the end of row 4 (weight 100). Note they were never a "tall Enter in
a shared column": the row-4 `↑` has to stay on the `↓` column of row 5, and that is what fixes the
geometry:

- row 4 (without Enter) sums to 1340 and `↑` occupies 1240..1340;
- row 5 sums to 1440, its last three keys are `← ↓ →` at 1140..1240 / 1240..1340 / 1340..1440.

So row 4's Enter can only be 100 units wide, otherwise `↑` slides off `↓`.

**Two rejected attempts** (both looked wrong on device):

1. Wrap rows 3+4 in a horizontal container with one tall Enter of `weight=150`. The left column of
   both rows became 1290, which scaled row 4 by 1290/1340 - `↑` moved left and got narrower.
2. Keep the 150/100 widths but draw them as one shape with two backgrounds
   (`bg_ax_key_enter_top/bottom`, square joint, no inset at the seam). It looked joined, but it was
   still two views: two tap targets, and the inner corner plus the 2dp insets never matched.

**Final layout** (user-confirmed): keep the two widths - row 3 Enter 150, row 4 Enter 100 - and
make them read as one down-facing L:

- `bg_ax_key_enter_top.xml`: 2dp inset on left/right/top, **no bottom inset**, only the top two
  corners rounded. 150 wide, extends 50 units further left than the leg.
- `bg_ax_key_enter_bottom.xml`: 2dp inset on left/right/bottom, **no top inset**, only the bottom
  two corners rounded. 100 wide, flush with the far right edge, so `↑` stays exactly above `↓`.
- The label `Enter` is only on the wide top half.

Because they are still two views, `KeyBoardLayoutController.keyDown / keyUp` now also updates every
other view tagged `66` inside `lv_keyboard_simple` (`setSplitEnterPressed` + `collectTagged`), so a
press highlights both halves at once - otherwise each half would flash on its own and still feel
like two keys.

A single 100-wide TextView spanning both rows was also tried: it is one real view, but the user
found the key too narrow to hit comfortably, which is why the L shape (wide top, narrow leg) is
the accepted trade-off. Extending to the right is not an option - it would eat the touchpad width.

### 3. Other layout tweaks in the same pass

- `Enter` is the label everywhere (simplified, digitpad, digitpad add dialog); 回车 is gone from
  layouts and comments.
- The wheel keys are labelled 滚轮⬆ / 滚轮⬇ instead of 滚动⬆ / 滚动⬇ (both keyboards).
- Full keyboard F-row: `ESC` had `marginRight=5dp` and `Ins` had `marginLeft=5dp`, which made the
  gaps around F1 / F12 wider than between the F keys. Both margins are removed, so every key in
  that row now has the standard 4dp spacing (2dp inset on each side).
- Digitpad right column: `ScrLk / Home / PgUp / Del / End / PgDn` were 9.5sp and the wheel keys
  10sp; all are now 11sp, matching `Shift`.





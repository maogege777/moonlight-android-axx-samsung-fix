# EXT-KB-VARIANTS：全键盘布局启停

## 目的

全键盘自带四种布局（电脑全键盘 / 简化键盘 / 简化v2 / 数字键盘）。有些场景只想要其中一两种，
需要一个能启停布局、并决定顶栏「布局 ▾」按钮切换顺序的入口。

- 入口位置：串流中菜单 › **虚拟手柄与虚拟按键** › 虚拟按键组下面新增的 **全键盘** 组
- 交互：横排四个 chip，**点亮 = 启用**；右上角小角标是 **切换顺序**
- 顶栏「布局 ▾」只在已启用的布局之间按角标顺序循环

简化v2 是本分支新增的第四种布局（左右分离 + 中间鼠标/滚轮/触摸板），
默认**不启用**，需要在「全键盘」组里点一下才进入循环，不影响原有三种布局的任何行为。

## 规则

| 规则 | 说明 |
|---|---|
| 点亮 / 熄灭 | 点一次切换。熄灭的布局不参与循环，布局本身不删除 |
| 顺序 | 点亮先后即顺序；新点亮的追加到末尾，关闭后后面的角标前移 |
| 至少保留 1 个 | 只剩 1 个点亮时再点它 → 拒绝并 Toast「至少要保留一种键盘布局」，状态不变 |
| 当前布局被关掉 | 点「更新配置」或下次显示键盘时，自动切到第 1 个已启用布局 |
| 只启用 1 种 | 点顶栏「布局 ▾」提示「只启用了一种布局」，不做循环 |

## 存储

- key：`keyboard_axi_variants`（默认 SharedPreferences）
- 值：有序逗号串，例如 `simple,digitpad`；顺序 = 切换顺序
- 取值：`full`（电脑全键盘）/ `simple`（简化键盘）/ `simplev2`（简化v2）/ `digitpad`（数字键盘）

### 初始化

| 场景 | 行为 |
|---|---|
| 全新安装 | 写入 `simple,digitpad`（只启用 简化 + 数字） |
| 覆盖安装 | **不写入**；读取时回落到旧版写死的顺序 `simple,full,digitpad`，老用户行为不变 |
| 无记录且非全新安装 | 同上回落，直到用户在面板里改一次才落盘 |
| 数据被写坏 / 为空 | 读取时兜底成 `simple`，保证顶栏永远不会切到空布局 |

「全新安装」判定：`PackageManager.getPackageInfo().firstInstallTime == lastUpdateTime`。
覆盖安装后 `lastUpdateTime` 一定晚于 `firstInstallTime`，比「prefs 里有没有别的键」更可靠
（老用户可能从没改过任何设置）。

初始化调用点：`Game.onCreate()`（与 `QuickKeyStore.ensureSeeded` 同一位置），方法幂等。

## 文件清单

| 文件 | 改动 |
|---|---|
| `binding/.../keyboard/KeyboardVariantStore.java` | 新增：读写、toggle、初始化、兜底；`ALL` 增加 `simplev2` |
| `ui/gamemenu/GameMenuVirtualViewFragment.java` | 绑定四个 chip、点亮态渲染、点击启停、Toast |
| `res/layout/dialog_game_menu_virtual_gamepad.xml` | 虚拟按键组下方新增「全键盘」组（标题 + 4 chip + 说明） |
| `res/drawable/bg_kb_variant_badge.xml` | 新增：角标底（半透明白圆，弱化，不外溢） |
| `binding/.../keyboard/KeyBoardLayoutController.java` | `cycleMode()` 改为按启用列表循环；新增 `ensureEnabledMode()`，在初始化 / `show()` / `refreshLayout()` 校正当前布局；模式表与可见性补 `simplev2` |
| `res/layout/layout_axixi_keyboard.xml` | 新增 `rbt_keyboard_simple_v2` + `lv_keyboard_simple_v2` 的 include |
| `res/layout/item_layout_axixi_keyboard_simple_v2.xml` | 新增：简化v2 布局（左右分离 + 中间操作区） |
| `Game.java` | 启动时调用 `ensureInstalledDefault()` |

## 布局四：简化v2（EXT-KB-SPLIT-V2）

以简化键盘为基础左右分离，鼠标三键 / 滚轮上下 / 触摸板按原排版搬到中间。

### 结构

```
[ 左半区 645u ][ 操作区 56+92dp ][ 右半区 760u ]
```

- 切分：`5 / T / G / V / 空格` 归左半区，`6 / Y / H / B` 归右半区；**右半区没有空格**。
- 两个半区都是「五行权重恰好用满」，所以中间两道截断线笔直（内缘齐平），操作区与两侧零间隙。
- 左半区为了让出宽度只动三个键：Tab 130→95、Caps 160→95、Shift 240→193；
  首行 `Esc / 反引号 / 1-5` 保持原样，合计正好 645u，就是左半区的宽度基准。
- 左半区底行：Ctrl / Win / Alt + 空格（345u，吃掉整行）。
- 右半区为对齐调整的非字母键：⌫ 150→115、`;` `'` 70→85、`,` `.` `/` 70/70/69→74/74/73、
  Del/Ins/Home/End 100→115；字母（110/113）与数字（100）权重与原版完全一致。
- 方向键：↑ 与 ↓ 同为 100u 且上下严格对齐（右半区内 560–660u），`↵` 与 `→` 对齐（660–760u）。

### 中间操作区（原排版不变）

| 列 | 宽度 | 内容 |
|---|---|---|
| `ll_v2_mouse` | 56dp 起 | 中键 / 左键 / 右键，各占 1/3 高 |
| `ll_v2_pad` | 92dp 起 | 滚轮⬆ : 滚轮⬇ : 触摸板 = 1 : 1 : 3 |

操作区宽度随屏幕变宽而放大（与简化键盘同一套 `excess` 规则，只是基数不同）：

```java
// 简化键盘：mouseW = 64 + excess*0.14；padW = 42 + excess*0.19
int v2MouseW = dip2px(context, 56f + excess * 0.10f);
int v2PadW   = dip2px(context, 92f + excess * 0.19f);
```

| 屏幕宽 | 简化：鼠标列 / 触摸板宽 | 简化v2：鼠标列 / 触摸板宽 |
|---|---|---|
| ≤ 800dp | 64 / 42dp | 56 / 92dp |
| 851dp | 71 / 52dp | 61 / 102dp |
| 1024dp | 95 / 85dp | 78 / 135dp |
| ≥ 1280dp | 131 / 133dp | 104 / 183dp |

### 复用要点

- 新布局的按键块直接从 `item_layout_axixi_keyboard_simple.xml` 生成/复制（tag、背景、角标文字全部一致），
  只改 `layout_weight`；tag 一致才能共用同一个触摸监听与按键映射。
- 操作区 id 必须另起一套（`v2_scroll_up` / `v2_scroll_down` / `fv_keyboard_touch_v2`），
  否则 `findViewById` 会命中另一套布局的同名控件，表现为「按了这个键却动了另一个键」。
- `keyboardContainerOf(v)` 把「按 tag 同步状态」限定在按键所在的那套布局里（L 形回车、组合键修饰键都走它），
  以后再加布局不需要重复改这些同步逻辑。
- 布局默认不点亮：`FRESH_ORDER` 与 `LEGACY_ORDER` 都不含 `simplev2`，用户想用再去面板点亮。

## 视觉约定

- chip：`FrameLayout` 包一层，底 `TextView` + 角标 `TextView`
- 点亮：`ic_game_menu_btn_green_selector` + 白字粗体 + 角标显示序号
- 熄灭：`ic_game_menu_btn_selector` + `#9AA0A6` 常规字重 + 无角标
- 角标：12dp 圆形、`#59000000`（35% 黑，白字可辨但不抢视线）、7dp 白色粗体字，
  位于 chip 内部右上角（`top:1dp right:1dp`），chip 设 `overflow:hidden` 语义上保证不出框

## 文案

- 标题右侧斜体（9dp / #FFC107）：`点亮=启用 · 角标顺序=切换顺序`
- 列表下方灰字（8dp）：`点一次「布局」切到下一个，最后回到 1；至少留 1 个，关掉正在用的会自动切到 1。`
- 拒绝 Toast：`至少要保留一种键盘布局`
- 单布局时点顶栏：`只启用了一种布局`

## 待真机验证

1. 全新安装（清数据重装）→ 面板里只有 简化①、数字② 点亮；顶栏「布局」只在两者间切换。
2. 覆盖安装 → 三个 chip 全亮，角标 简化1 / 全键盘2 / 数字3；切换顺序与旧版完全一致。
3. 关掉「简化键盘」→ 角标重排为 全键盘1 / 数字2；点「布局」顺序随之变化。
4. 只留一个点亮 → 再点它弹 Toast，状态不变。
5. 正在用「数字键盘」时把数字关掉 → 点「更新配置」后自动回到第 1 个已启用布局，顶栏文案同步。
6. 重启 App / 重进串流后，点亮状态与顺序保持。

### 简化v2 专项

7. 面板里点「简化v2」→ 角标出现在末尾，顶栏「布局 ▾」能切到它，文案显示「布局 · 简化v2 ▾」。
8. 简化v2 下：左右半区内缘上下齐平，操作区与两侧没有多余空隙；五行的键宽关系与原版简化一致。
9. 左右两半的按键输入正确：左半区 `5 T G V`、右半区 `6 Y H B` 都在，空格只有左半区一个。
10. 中间操作区：左键/中键/右键、滚轮⬆/⬇、触摸板全部可用；触摸板明显比简化键盘宽。
11. 按住 Enter（L 形）上下两段同时变色；组合键模式下 Ctrl/Win/Alt/Shift 的高亮与释放正常。
12. 关闭简化v2 后再点亮、切换几轮布局，键盘显示/隐藏与高度都正常（简化v2 高度与简化键盘一致）。

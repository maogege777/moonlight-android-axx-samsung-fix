# EXT-KB-VARIANTS：全键盘布局启停

## 目的

全键盘自带三种布局（电脑全键盘 / 简化键盘 / 数字键盘）。有些场景只想要其中一两种，
需要一个能启停布局、并决定顶栏「布局 ▾」按钮切换顺序的入口。

- 入口位置：串流中菜单 › **虚拟手柄与虚拟按键** › 虚拟按键组下面新增的 **全键盘** 组
- 交互：横排三个 chip，**点亮 = 启用**；右上角小角标是 **切换顺序**
- 顶栏「布局 ▾」只在已启用的布局之间按角标顺序循环

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
- 取值：`full`（电脑全键盘）/ `simple`（简化键盘）/ `digitpad`（数字键盘）

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
| `binding/.../keyboard/KeyboardVariantStore.java` | 新增：读写、toggle、初始化、兜底 |
| `ui/gamemenu/GameMenuVirtualViewFragment.java` | 绑定三个 chip、点亮态渲染、点击启停、Toast |
| `res/layout/dialog_game_menu_virtual_gamepad.xml` | 虚拟按键组下方新增「全键盘」组（标题 + 3 chip + 说明） |
| `res/drawable/bg_kb_variant_badge.xml` | 新增：角标底（半透明白圆，弱化，不外溢） |
| `binding/.../keyboard/KeyBoardLayoutController.java` | `cycleMode()` 改为按启用列表循环；新增 `ensureEnabledMode()`，在初始化 / `show()` / `refreshLayout()` 校正当前布局 |
| `Game.java` | 启动时调用 `ensureInstalledDefault()` |

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

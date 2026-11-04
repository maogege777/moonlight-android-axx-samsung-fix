# EXT-OSC-TAP-LONGPRESS-SWAP：点按互换

## 目的

主菜单「功能与操作」里的 **虚拟手柄 / 虚拟按键** 两个按钮，原本固定为：

- 点按 → 切换显示/隐藏（按钮绿色高亮同步）
- 长按（200ms）→ 就近弹出布局方案列表

本扩展加一个开关，把这两个动作**互换**：

- 开关关闭（默认）＝ 上面原行为，完全不变
- 开关开启 → **点按弹出布局方案，长按切换显示/隐藏**

- 开关位置：串流中菜单 › **虚拟手柄与虚拟按键** › **通用** 组第二行，「震感强度」右边（1/3 宽，本行第三格留空）
- 按钮文案：两行——第一行「点按互换」（名称），第二行显示当前生效值（与「震感强度」显示 `15ms/系统默认` 同一模式）：
  关闭时「点按切显隐」、开启时「点按弹方案」，始终描述「点按现在做什么」，由 `initViewData()` 动态设置
- 作用范围：**仅**主菜单的虚拟手柄、虚拟按键两个按钮。「鼠标与触控」按钮的长按弹鼠标菜单、二级面板里的方案 RadioGroup 都不受影响

## 规则

| 项 | 说明 |
|---|---|
| 默认 | 关闭（= 旧行为）。全新安装与覆盖安装都不写 pref |
| 图标 | `ic_axi_tap_swap`：外环由两条反向箭头拼成（长按进度环 + 互换双重含义），中心实心点＝点按。宽视口 32×18，实际渲染 28×16dp |
| 点亮语言 | 与其它开关一致：绿底 `ic_game_menu_btn_green_selector` = 开启 |
| 立即生效 | 二级面板里切换后，回到仍开着的主菜单即刻生效：长按与点按都在**触发时刻**读当前状态，不需要重建监听 |
| 长按骨架 | 200ms 阈值、就近弹浮层、`schemeLongPressFired` 吞掉松手补发的 click、方案选中后的写 pref→角标→热重载→`dismissAllDialogs()` 全部复用，零改动 |

## 存储

- key：`checkbox_osc_menu_tap_swap`（默认 SharedPreferences），boolean，默认 `false`
- 读取：`PreferenceConfiguration.oscMenuTapSwap`（`fromPreferences` 里 `getBoolean(key,false)`）

## 文件清单

| 文件 | 改动 |
|---|---|
| `res/drawable/ic_axi_tap_swap.xml` | 新增图标 |
| `res/layout/dialog_game_menu_virtual_gamepad.xml` | 通用组第二行：震感强度整行宽→1/3（去右箭头、padding 10→6dp），新增 `btn_osc_menu_tap_swap`，第三格空 `View` |
| `preferences/PreferenceConfiguration.java` | 新增 pref 键、字段、读取 |
| `ui/gamemenu/GameMenuVirtualViewFragment.java` | 绑定新按钮：点亮态渲染 + 点击写 pref |
| `ui/gamemenu/GameMenuFragment.java` | `attachSchemePopupLongPress` → `attachOscMenuActions`（长按动作在触发时刻按开关分派）；`onClick` 两个按钮分支按开关决定点按做什么 |

## 实现要点

- **分派时机在触发时刻，不在挂载时刻**：主菜单开着时去二级面板切换开关，回来立即一致。若按挂载时刻固定，就必须重新挂监听或重建主菜单。
- `schemeLongPressFired` 的语义从「长按弹了方案」泛化为「长按动作已触发」——它只负责吞掉松手补发的 click，与长按具体做什么无关，所以互换两种模式共用同一套骨架。
- 1/3 宽的「震感强度」放不下「文字 + 左右两个 drawable」，去掉 `ic_axi_arrow` 右箭头、padding 减半；「系统默认」这类四字值靠收窄后的余量不截断。

## 真机待验证

- 开关默认关闭；切换后返回主菜单，点按/长按行为立即互换。
- 开启后：点按就近弹出方案浮层，点选后写 pref、角标刷新、热重载、关闭全部菜单回串流（与原长按链路完全一致）；长按 200ms 切换显隐、按钮绿色高亮同步、Toast 正常。
- 长按触发后松手，补发的 click 被吞掉：不会既弹方案又切显隐。
- 「鼠标与触控」按钮行为不变（点按/长按都弹鼠标菜单）。
- 二级面板的方案 RadioGroup、角标、与主菜单角标的一致性不受开关影响。
- 「震感强度」1/3 宽下「15ms」「系统默认」都不截断；新按钮图标与两行文字「开关点按/互换」完整显示；第三格留空不影响下一行 DS 触控板行。
- 覆盖升级不重置开关；重启串流后开关状态保持。

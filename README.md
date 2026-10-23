# 墨迹清单 Android（Kotlin）

原生 Android 版待办，与 `todo-android` / `todo-taro` 对齐：

- 增删改、完成切换、上移/下移
- 筛选：全部 / 未完成 / 已完成
- 导出 Markdown 到剪贴板；粘贴 / 选文件导入
- 本地 `SharedPreferences`，键 `todo_taro_todos`

## 环境

- Android Studio，JDK 17+
- Kotlin 2.0 + AGP 8.7

## 运行

```bash
cd todo-kotlin
./gradlew assembleDebug
```

应用 ID：`com.todolist.kotlin`

## 结构

```
app/src/main/kotlin/com/todolist/app/
  MainActivity.kt
  TodoStore.kt
  TodoItem.kt
  TodoAdapter.kt
  FilterKind.kt
```

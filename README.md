# 📱 FreeMe

一款简洁实用的 Android 原生贷款管理应用，帮助您轻松管理多笔贷款、信用卡分期，实时掌握还款计划与财务状况。

## ✨ 功能特性

### 📊 贷款管理
- 支持添加和管理多笔贷款
- 支持**普通贷款**和**信用卡分期**两种类型
- 四种还款方式：
  - **等额本息** - 每月还款金额固定
  - **等额本金** - 每月本金固定，利息递减
  - **先息后本** - 前期只还利息，末期还本金
  - **利随本清** - 到期一次性还本付息

### 📅 还款计划
- 自动生成详细还款计划表
- 显示每期本金、利息、剩余本金
- 计算还款日期与到期提醒

### 📈 数据统计
- 实时统计贷款总额、剩余本金、已还金额
- 本月应还金额概览
- 每日应还金额估算
- 月度还款汇总

### 💾 数据管理
- **自动备份** - 数据变更后自动备份到本地
- **手动导出** - 导出数据到 Excel 文件
- **数据导入** - 从 Excel 文件恢复数据
- **清空数据** - 支持双确认清空所有数据

### 🔔 智能提醒
- 信用卡还款日当天自动弹窗提醒
- 到期账单详情展示

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 开发语言 | Java 17 |
| 架构模式 | MVVM |
| 数据库 | Room (SQLite ORM) |
| 异步处理 | ExecutorService + LiveData |
| UI 框架 | Material Design + AndroidX |
| Excel 处理 | Apache POI |
| 构建工具 | Gradle 8.6.0 |

## 📦 项目结构

```
app/src/main/java/com/finance/loanmanager/
├── data/                    # 数据层
│   ├── AppDatabase.java     # Room 数据库配置
│   ├── dao/                 # 数据访问对象
│   └── entity/              # 实体类
├── repository/              # 数据仓库
├── service/                 # 业务服务
│   └── LoanCalculator.java  # 贷款计算
├── ui/                      # UI 层
│   ├── main/                # 主界面
│   ├── loan/                # 贷款管理
│   ├── monthly/             # 月度统计
│   ├── schedule/            # 还款计划
│   └── data/                # 数据管理
└── util/                    # 工具类
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 36 (Android 16)
- Gradle 8.x

### 构建运行

```bash
# 克隆项目
git clone https://github.com/your-username/Personal-financial-planning.git

# 进入项目目录
cd Personal-financial-planning

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

### 安装到设备

```bash
# 通过 ADB 安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📋 版本信息

- **当前版本**: v2.3.1
- **最低支持**: Android 8.0 (API 26)
- **目标版本**: Android 16 (API 36)

## 🔄 CI/CD

项目使用 GitHub Actions 自动构建：

- 触发条件：推送到 `main` 或 `master` 分支
- 构建产物：Debug APK 和 Release APK
- 自动版本号：基于 GitHub run_number

## 📱 界面预览

应用主要包含以下界面：

1. **主界面** - 显示本月应还、贷款概览、快速入口
2. **贷款列表** - 查看所有贷款及状态
3. **贷款详情** - 还款进度、记录还款、查看计划
4. **还款计划表** - 详细分期还款计划
5. **月度汇总** - 每月还款总额统计
6. **数据管理** - 导入导出、清空数据

## 📄 许可证

本项目仅供个人学习和使用。

---

如有问题或建议，欢迎反馈！

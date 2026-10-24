# DGLab-Bingo (郊狼 Bingo 联动模组)

这是一个基于 Minecraft **Fabric 1.20.1** 的整活模组。它能将 `Yet Another Bingo` 和 `Coyote-Game-Hub` 连接起来。当任意队伍完成 Bingo 目标时，会触发郊狼电击，并在全服聊天栏发送惨叫：“我不会告诉你任何事...饿啊啊啊啊啊！”

## ⚠️ 安全警告 (务必阅读)
- 郊狼设备涉及强电流，**请务必从极低的强度开始测试（如10）**，确认安全后再慢慢调高！
- 本项目仅用于学习与交流，因使用不当造成的身体伤害或设备损坏，开发者概不负责。
- 作者亲测：代码里默认的强度 `40` 已经挺麻了，改成 `80` 可能会当场去世。

## 📦 依赖项 (前置模组/软件)
1. Minecraft 1.20.1
2. [Fabric Loader](https://fabricmc.net/) & [Fabric API](https://modrinth.com/mod/fabric-api)
3. [Yet Another Bingo (YAB)](https://modrinth.com/mod/yet-another-minecraft-bingo)
4. [Coyote-Game-Hub](https://github.com/NoobEric0746/coyote-game-hub) (运行在电脑后台的中转服务)

## 🛠️ 从源码构建
本项目未包含 `libs` 文件夹中的前置 JAR。如果你需要自行编译：
1. 克隆仓库到本地。
2. 前往 [Yet Another Bingo (YAB)](链接)的发布页，下载对应版本的 JAR。
3. 将下载的 JAR 放入项目根目录的 `libs/` 文件夹中。
4. 在终端执行 `./gradlew build`（Windows 下为 `gradlew.bat build`）。
5. 编译好的模组将生成在 `build/libs/` 目录下。

## 🚀 如何使用
1. 下载并运行 `Coyote-Game-Hub` 的 `start.bat`，在网页端用手机 APP 扫码连接。
2. **重要配置**：网页端左侧保持“暂停输出”（圆环不动），基础强度设为 `0`，一键开火强度限制设为你的耐受值（如 `60`）。
3. 将本模组与 YAB 放入 `mods` 文件夹。
4. 启动游戏，开一局 Bingo。完成目标即可享受物理反馈！

## 🛠️ 自定义修改
目前电击的参数硬编码在 `YabObjectiveMixin.java` 中，你可以直接修改源码来调整手感：
```java
String jsonBody = "{" +
        "\"strength\": 40," +    // 强度 (0-200)
        "\"time\": 1000," +      // 持续时间(毫秒)
        "\"channel\": \"A\"," +  // 通道 (A, B 或 AB)
        "\"pulseId\": \"呼吸\"" + // 波形名称 (需与 Hub 网页一致)
        "}";

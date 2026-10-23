package com.lan.dglabbingo.mixin;

import com.lan.dglabbingo.DGLABBINGO;
import me.jfenn.bingo.common.card.objective.BingoObjective;
import me.jfenn.bingo.common.team.BingoTeamKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * YabObjectiveMixin 是连接 Yet Another Bingo (YAB) 和郊狼 (DG-LAB) 的核心桥梁。
 * 它通过 Mixin 技术向 YAB 的 ObjectiveService.update 方法注入代码，
 * 从而实现“完成任意 Bingo 目标 -> 触发游戏内播报 -> 发送网络请求触发电击”。
 */
@Pseudo
@Mixin(targets = "me.jfenn.bingo.common.card.objective.ObjectiveService", remap = false)
public class YabObjectiveMixin {

    // 记录已经触发电击的目标（格式：目标ID_队伍ID）。
    // 因为 update 方法在服务端每 tick 都会执行，如果不加记录，目标完成的瞬间会连续触发电击几百次，导致设备过载或玩家被狂电。
    private static final Set<String> TRIGGERED_OBJECTIVES = new HashSet<>();

    /**
     * 注入到 YAB 的 update 方法头部（HEAD）。
     * 每当服务端检查并更新 Bingo 目标状态时，都会先经过这里。
     */
    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void onObjectiveUpdate(
            BingoObjective objective,         // 当前正在检查的 Bingo 目标对象（包含目标ID、名称等）
            Map<?, ?> playersHolding,         // 玩家持有进度相关的 Map
            Map<?, ?> players,                // 玩家完成状态相关的 Map
            Map<?, ?> teamsProgress,          // 队伍进度相关的 Map
            Set<BingoTeamKey> teams,          // 当前传入的需要检查/更新的队伍集合
            CallbackInfo ci
    ) {
        // 如果传入的队伍集合为空，说明不是队伍状态更新，直接返回
        if (teams == null || teams.isEmpty()) return;

        // 遍历每一个可能刚刚完成目标的队伍
        for (BingoTeamKey teamKey : teams) {

            // 【核心判定】如果目标当前的已完成记录里【没有】这支队伍，
            // 意味着这个目标对这支队伍来说是“刚刚”完成的（状态跃迁）。
            if (!objective.getTeamsAchieved().containsKey(teamKey)) {

                // 生成唯一标识（目标ID + 队伍ID），用于防连击
                String triggerKey = objective.getId() + "_" + teamKey;

                // 如果已经电过了，直接跳过本次循环
                if (TRIGGERED_OBJECTIVES.contains(triggerKey)) continue;
                TRIGGERED_OBJECTIVES.add(triggerKey);

                // 控制台日志输出，方便开发者排查问题
                System.out.println("[DG-Bingo] 目标完成！准备发送电击请求: " + objective.getId());

                // ==========================================
                // 1. 游戏内全服广播（节目效果）
                // 从主类中安全获取服务器实例
                // ==========================================
                MinecraftServer server = DGLABBINGO.SERVER;
                if (server != null) {
                    // broadcast 的第二个参数 false 代表在聊天栏显示（如果在头顶显示则填 true）
                    server.getPlayerManager().broadcast(
                            Text.literal("我不会告诉你任何事...饿啊啊啊啊啊！"),
                            false
                    );
                }

                // ==========================================
                // 2. 发送 HTTP 请求控制 Coyote-Game-Hub 触发电击
                // ==========================================
                try {
                    // 强制使用 HTTP/1.1 协议，避免部分环境下的 405 Method Not Allowed 错误
                    HttpClient client = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_1_1)
                            .build();

                    // 【玩家自定义参数区】
                    // strength: 电击强度（0-200）。警告：作者测试的时候电成沙比了（
                    // time:     持续时间，单位毫秒（1000 = 1秒）
                    // channel:  通道选择，可选 "A"、"B" 或 "AB"（双通道）。注意：需确保电极贴在你指定的通道上。
                    // pulseId:  波形名称。必须与 Coyote-Game-Hub 网页端【波形配置】里的名称完全一致（例如 "呼吸"、"潮汐" 等）。
                    String jsonBody = "{" +
                            "\"strength\": 40," +
                            "\"time\": 1000," +
                            "\"channel\": \"A\"," +
                            "\"pulseId\": \"呼吸\"" +
                            "}";

                    // 构建发往本地的网络请求。
                    // 注：使用 "all" 代表向所有已连接的郊狼设备广播。如果是局域网多人，且你想精准打击，可以改为具体的 ClientID。
                    HttpRequest fireRequest = HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:8920/api/v2/game/all/action/fire"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    // 发送请求并接收服务端响应
                    HttpResponse<String> response = client.send(fireRequest, HttpResponse.BodyHandlers.ofString());

                    // 如果状态码为 200，说明 Hub 已经成功接收指令
                    System.out.println("[DG-Bingo] 开火成功，状态码: " + response.statusCode());

                } catch (Throwable e) {
                    // 捕获所有异常，防止因为 Hub 未启动或网络问题导致 Minecraft 服务端直接崩溃
                    System.err.println("[DG-Bingo] 电击请求发送失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
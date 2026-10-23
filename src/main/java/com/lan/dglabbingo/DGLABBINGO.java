package com.lan.dglabbingo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DGLABBINGO implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("dglab-bingo");

	// 【新增】存储服务端实例
	public static MinecraftServer SERVER = null;

	@Override
	public void onInitialize() {
		// 【新增】服务端启动时赋值，关闭时清空
		ServerLifecycleEvents.SERVER_STARTING.register(server -> SERVER = server);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SERVER = null);
	}
}
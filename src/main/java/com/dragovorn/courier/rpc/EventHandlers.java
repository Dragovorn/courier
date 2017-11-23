package com.dragovorn.courier.rpc;

import com.github.psnrigner.DiscordEventHandler;
import com.github.psnrigner.DiscordJoinRequest;
import com.github.psnrigner.ErrorCode;

public class EventHandlers implements DiscordEventHandler {

    @Override
    public void ready() {
//        Courier.getInstance().getLogger().info("RPC Ready!");
    }

    @Override
    public void disconnected(ErrorCode errorCode, String s) {
//        Courier.getInstance().getLogger().info("DISCONNECT " + s);
    }

    @Override
    public void errored(ErrorCode errorCode, String s) {
        // STUB
    }

    @Override
    public void joinGame(String s) {
        // STUB
    }

    @Override
    public void spectateGame(String s) {
        // STUB
    }

    @Override
    public void joinRequest(DiscordJoinRequest discordJoinRequest) {
        // STUB
    }
}

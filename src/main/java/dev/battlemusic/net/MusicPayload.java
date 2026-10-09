package dev.battlemusic.net;

import net.minecraft.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public record MusicPayload(String json) implements class_8710 {
    public static final class_8710.class_9154<MusicPayload> ID=new class_8710.class_9154<>(class_2960.method_60655("battlemusic","state_v1"));
    public static final class_9139<class_2540,MusicPayload> CODEC=class_8710.method_56484((value,buf)->buf.method_10788(value.json(),8192),buf->new MusicPayload(buf.method_10800(8192)));
    @Override public class_8710.class_9154<? extends class_8710> method_56479(){return ID;}
    public static void register(){PayloadTypeRegistry.playS2C().register(ID,CODEC);}
}

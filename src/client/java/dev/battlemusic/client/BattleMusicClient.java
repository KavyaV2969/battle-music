package dev.battlemusic.client;

import dev.battlemusic.BattleMusicMod;
import dev.battlemusic.net.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.*;

public final class BattleMusicClient implements ClientModInitializer {
    @Override public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(MusicPayload.ID,(payload,context)->{
            try{Playback.INSTANCE.accept(MusicState.parse(payload.json()));}catch(RuntimeException e){BattleMusicMod.LOG.error("Invalid music state ignored",e);}
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->Playback.INSTANCE.disconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(!client.method_1493())Playback.INSTANCE.tick();});
        ResourceManagerHelper.get(class_3264.field_14188).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            public class_2960 getFabricId(){return class_2960.method_60655("battlemusic","audio_reload");}
            public java.util.Collection<class_2960> getFabricDependencies(){return java.util.List.of(ResourceReloadListenerKeys.SOUNDS);}
            public void method_14491(class_3300 manager){class_310.method_1551().execute(()->Playback.INSTANCE.resourcesReloaded());}
        });
    }
}

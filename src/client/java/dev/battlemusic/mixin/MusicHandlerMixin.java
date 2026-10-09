package dev.battlemusic.mixin;

import com.cobblemon.mod.common.client.net.battle.BattleMusicHandler;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMusicPacket;
import dev.battlemusic.client.Playback;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BattleMusicHandler.class,remap=false)
public abstract class MusicHandlerMixin {
    @Inject(method="handle(Lcom/cobblemon/mod/common/net/messages/client/battle/BattleMusicPacket;Lnet/minecraft/class_310;)V",at=@At("HEAD"),cancellable=true)
    private void handle(BattleMusicPacket packet,class_310 client,CallbackInfo ci){if(Playback.INSTANCE.owns()){if(packet.getMusic()==null)Playback.INSTANCE.end();ci.cancel();}}
}

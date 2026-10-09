package dev.battlemusic.mixin;

import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMusicPacket;
import dev.battlemusic.server.Sessions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PlayerBattleActor.class,remap=false)
public abstract class PlayerActorMixin {
    @Inject(method="sendUpdate",at=@At("HEAD"))
    private void beforeMusic(NetworkPacket<?> packet,CallbackInfo ci){if(packet instanceof BattleMusicPacket music)Sessions.beforeMusic((PlayerBattleActor)(Object)this,music);}
}

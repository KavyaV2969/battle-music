package dev.battlemusic.fixture.mixin;
import dev.battlemusic.fixture.Shared;

import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.net.messages.client.battle.BattleSwitchPokemonPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PlayerBattleActor.class,priority=2000,remap=false)
public abstract class PacketCueMixin {
    @Inject(method="sendUpdate",at=@At("HEAD"))
    private void send(NetworkPacket<?> packet,CallbackInfo ci){
        if(packet instanceof BattleSwitchPokemonPacket change&&!change.isAlly()){
            String species=change.getNewPokemon().getProperties().getSpecies();
            if("arceus".equals(species)||"cobblemon:arceus".equals(species))Shared.entrySentNanos=System.nanoTime();
        }
    }
}

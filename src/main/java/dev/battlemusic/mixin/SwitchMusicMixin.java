package dev.battlemusic.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import dev.battlemusic.server.Sessions;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Release the outgoing theme when the committed recall animation begins. */
@Mixin(targets="com.cobblemon.mod.common.battles.interpreter.instructions.SwitchInstruction$Companion",remap=false)
public abstract class SwitchMusicMixin {
    @Inject(method="createEntitySwitch",at=@At("HEAD"))
    private void recall(PokemonBattle battle,BattleActor actor,class_1309 entity,String pnx,ActiveBattlePokemon slot,BattlePokemon next,BattlePokemon illusion,boolean imposter,CallbackInfoReturnable<?> ci){
        var current=slot.getBattlePokemon();
        if(current!=null&&!current.getUuid().equals(next.getUuid()))Sessions.recalling(battle,actor,slot);
    }
}

package dev.battlemusic.fixture.mixin;
import dev.battlemusic.fixture.Shared;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.cobblemon.mod.common.battles.interpreter.instructions.SwitchInstruction$Companion",priority=2000,remap=false)
public abstract class CueMixin {
    @Inject(method="createEntitySwitch",at=@At("HEAD"))
    private void recall(PokemonBattle battle,BattleActor actor,class_1309 entity,String pnx,ActiveBattlePokemon slot,BattlePokemon next,BattlePokemon illusion,boolean initial,CallbackInfoReturnable<?> ci){
        var old=slot.getBattlePokemon();
        if(old!=null&&old.getOriginalPokemon().getSpecies().getResourceIdentifier().toString().equals("cobblemon:arceus"))Shared.recallStartedNanos=System.nanoTime();
    }
}

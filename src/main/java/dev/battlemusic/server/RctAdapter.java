package dev.battlemusic.server;

import com.cobblemon.mod.common.api.battles.model.actor.*;
import com.gitlab.srcmc.rctapi.api.RCTApi;

/** Loaded only when the optional RCT API mod is present. Full registry identity prevents ID collisions. */
public final class RctAdapter {
    public static String identify(BattleActor actor){
        if(actor.getType()!=ActorType.NPC)return null;
        for(var entry:RCTApi.getInstances().toList()){
            var registry=entry.getValue().getTrainerRegistry();
            if(actor instanceof EntityBackedBattleActor<?> backed&&backed.getEntity()!=null){
                String id=registry.getId(backed.getEntity());
                if(id!=null&&!id.isBlank())return entry.getKey()+":"+id;
            }
            for(var p:actor.getPokemonList()){
                var trainer=registry.getByOT(p.getOriginalPokemon());
                if(trainer==null)continue;
                for(String id:registry.getIds())if(registry.getById(id)==trainer)return entry.getKey()+":"+id;
            }
        }
        return null;
    }
}

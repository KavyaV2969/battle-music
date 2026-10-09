package dev.battlemusic.server;

import com.cobblemon.mod.common.api.battles.model.actor.*;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import dev.battlemusic.BattleMusicApi;
import dev.battlemusic.rules.Context;
import net.minecraft.*;
import java.util.*;

public final class ContextFactory {
    public record Location(String dimension,String biome,Set<String> tags,double x,double y,double z,String trainer){}
    public static Location location(PlayerBattleActor viewer,BattleActor opponent){
        class_1297 entity=opponent instanceof EntityBackedBattleActor<?> e?e.getEntity():viewer.getEntity();
        if(entity==null)entity=viewer.getEntity();
        var world=entity.method_37908();var biome=world.method_23753(entity.method_24515());
        var tags=new HashSet<String>();biome.method_40228().forEach(t->tags.add(t.comp_327().toString()));
        return new Location(world.method_27983().method_29177().toString(),biome.method_40230().map(k->k.method_29177().toString()).orElse(""),Set.copyOf(tags),entity.method_23317(),entity.method_23318(),entity.method_23321(),BattleMusicApi.trainerIdentity(opponent));
    }
    public static Context create(String type,BattleActor opponent,Pokemon original,Pokemon current,Location l){
        return new Context(type,current.getSpecies().getResourceIdentifier().toString(),current.getForm().getName(),current.getAspects(),
            Context.labels(current.getSpecies().getLabels(),current.getForm().getLabels()),l.trainer(),original.getUuid(),BattleMusicApi.getEncounterKey(original),l.dimension(),l.biome(),l.tags(),l.x(),l.y(),l.z());
    }
    public static String nativeTrainer(BattleActor actor){
        if(actor instanceof EntityBackedBattleActor<?> backed&&backed.getEntity() instanceof NPCEntity npc)return npc.getNpc().getResourceIdentifier().toString();
        return null;
    }
}

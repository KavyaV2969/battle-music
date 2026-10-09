package dev.battlemusic;

import com.google.gson.*;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.Priority;
import dev.battlemusic.net.MusicPayload;
import dev.battlemusic.rules.*;
import dev.battlemusic.server.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.resource.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.*;
import org.slf4j.*;
import java.io.*;
import java.util.*;

public final class BattleMusicMod implements ModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("battlemusic");
    public static volatile RuleSet rules=RuleSet.EMPTY;
    @Override public void onInitialize(){
        MusicPayload.register();
        if(FabricLoader.getInstance().isModLoaded("rctapi"))BattleMusicApi.registerTrainerAdapter(RctAdapter::identify);
        BattleMusicApi.registerTrainerAdapter(ContextFactory::nativeTrainer);
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.LOWEST,(java.util.function.Consumer<BattleStartedEvent.Pre>)e->{if(!e.isCanceled())Sessions.prepare(e.getBattle());});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{Sessions.clear();rules=RuleSet.EMPTY;});
        ResourceManagerHelper.get(class_3264.field_14190).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            public class_2960 getFabricId(){return class_2960.method_60655("battlemusic","rules");}
            public void method_14491(class_3300 manager){reload(manager);}
        });
        Commands.register();
        LOG.info("Battle Music 0.1.0 ready: Cobblemon 1.7.3, optional RCT, private audio pack");
    }
    public static void reload(class_3300 manager){
        try{
            var docs=new TreeMap<String,JsonObject>();
            for(var entry:manager.method_14488("battlemusic/rules",id->id.method_12832().endsWith(".json")).entrySet()){
                try(var reader=entry.getValue().method_43039()){
                    String path=entry.getKey().method_12832();
                    String id=entry.getKey().method_12836()+":"+path.substring("battlemusic/rules/".length(),path.length()-5);
                    try{docs.put(id,JsonParser.parseReader(reader).getAsJsonObject());}catch(RuntimeException e){throw new IllegalArgumentException(id+": "+e.getMessage(),e);}
                }
            }
            RuleSet next=RuleSet.parse(rules.revision()+1,docs); rules=next;
            LOG.info("Published music rule revision {} ({} rules); active battles retain their snapshot",next.revision(),next.rules().size());
        }catch(Exception e){LOG.error("Music datapack reload rejected; retaining revision {}: {}",rules.revision(),e.getMessage());}
    }
}

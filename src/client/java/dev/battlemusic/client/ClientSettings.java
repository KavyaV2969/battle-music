package dev.battlemusic.client;

import com.google.gson.*;
import dev.battlemusic.BattleMusicMod;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

/** Local listening preference; no changes to server rules or trainer teams. */
public final class ClientSettings {
    private static boolean trainerEntrances;
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("battlemusic-client.json");}
    public static boolean trainerEntrances(){return trainerEntrances;}
    public static void load(){
        trainerEntrances=false;
        if(!Files.exists(path()))return;
        try{
            var value=JsonParser.parseString(Files.readString(path())).getAsJsonObject().get("trainer_entrance_music");
            if(value!=null){
                if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("trainer_entrance_music must be boolean");
                trainerEntrances=value.getAsBoolean();
            }
        }catch(Exception e){BattleMusicMod.LOG.warn("Cannot load Battle Music settings; using trainer entrances disabled",e);}
    }
    public static void setTrainerEntrances(boolean enabled){trainerEntrances=enabled;}
    public static void save(){
        try{
            Path target=path();Files.createDirectories(target.getParent());
            Path temporary=target.resolveSibling(target.getFileName()+".tmp");
            Files.writeString(temporary,"{\n  \"trainer_entrance_music\": "+trainerEntrances+"\n}\n");
            try{Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){BattleMusicMod.LOG.error("Cannot save Battle Music settings",e);}
        Playback.INSTANCE.refreshSettings();
    }
}

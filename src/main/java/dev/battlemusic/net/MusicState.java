package dev.battlemusic.net;

import com.google.gson.*;
import dev.battlemusic.rules.Rule;
import java.util.*;

/** Bounded versioned snapshot; no opponent team or future encounter information. */
public record MusicState(int version,UUID battle,long revision,String base,String fallback,float volume,float pitch,
                         String entrance,UUID pokemon,int transitionTicks,boolean ended,boolean trainerBattle) {
    public static final Gson GSON=new Gson();
    /** Older callers describe wild playback; the additive wire field defaults to false. */
    public MusicState(int version,UUID battle,long revision,String base,String fallback,float volume,float pitch,String entrance,UUID pokemon,int transitionTicks,boolean ended){
        this(version,battle,revision,base,fallback,volume,pitch,entrance,pokemon,transitionTicks,ended,false);
    }
    public MusicState {
        if(version!=1||battle==null||revision<0||transitionTicks<1||transitionTicks>1200)throw new IllegalArgumentException("invalid music state");
        if(!Float.isFinite(volume)||volume<0||volume>4||!Float.isFinite(pitch)||pitch<=0||pitch>4)throw new IllegalArgumentException("invalid gain/pitch");
        for(String id:Arrays.asList(base,fallback,entrance))if(id!=null)Rule.identifier(id);
        if(!ended&&base==null)throw new IllegalArgumentException("missing base");
    }
    public String json(){return GSON.toJson(this);}
    public static MusicState parse(String json){JsonObject o=JsonParser.parseString(json).getAsJsonObject();return new MusicState(o.get("version").getAsInt(),UUID.fromString(o.get("battle").getAsString()),o.get("revision").getAsLong(),str(o,"base"),str(o,"fallback"),o.get("volume").getAsFloat(),o.get("pitch").getAsFloat(),str(o,"entrance"),str(o,"pokemon")==null?null:UUID.fromString(str(o,"pokemon")),o.get("transitionTicks").getAsInt(),o.get("ended").getAsBoolean(),o.has("trainerBattle")&&o.get("trainerBattle").getAsBoolean());}
    private static String str(JsonObject o,String key){return !o.has(key)||o.get(key).isJsonNull()?null:o.get(key).getAsString();}
}

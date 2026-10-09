package dev.battlemusic;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.battlemusic.rules.Rule;
import java.util.*;
import java.util.function.Function;

public final class BattleMusicApi {
    public static final String ENCOUNTER_KEY="battlemusic:encounter";
    private static final List<Function<BattleActor,String>> ADAPTERS=new ArrayList<>();
    private BattleMusicApi(){}
    /** Call on the server thread. null removes the persistent key. */
    public static void setEncounterKey(Pokemon pokemon,String key){if(key==null)pokemon.getPersistentData().method_10551(ENCOUNTER_KEY);else{Rule.identifier(key);pokemon.getPersistentData().method_10582(ENCOUNTER_KEY,key);}}
    public static String getEncounterKey(Pokemon pokemon){String s=pokemon.getPersistentData().method_10558(ENCOUNTER_KEY);return s.isBlank()?null:s;}
    /** Register during common initialization. Return null for unsupported actors. First non-null identity wins. */
    public static synchronized void registerTrainerAdapter(Function<BattleActor,String> adapter){ADAPTERS.add(Objects.requireNonNull(adapter));}
    public static synchronized String trainerIdentity(BattleActor actor){for(var adapter:ADAPTERS){String id=adapter.apply(actor);if(id!=null){Rule.identifier(id);return id;}}return null;}
}

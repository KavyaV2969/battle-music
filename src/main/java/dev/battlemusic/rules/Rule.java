package dev.battlemusic.rules;

import com.google.gson.*;
import java.util.*;
import java.util.regex.Pattern;

public record Rule(String id, String kind, int priority, Match match, String sound, int transitionTicks) {
    public static final Pattern IDENTIFIER=Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    public record Box(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        boolean contains(Context c){return c.x()>=minX&&c.x()<=maxX&&c.y()>=minY&&c.y()<=maxY&&c.z()>=minZ&&c.z()<=maxZ;}
    }
    public record Match(String battleType,Set<String> species,Set<String> forms,Set<String> aspects,
                        Set<String> labels,Set<String> trainers,Set<UUID> pokemon,Set<String> encounters,
                        Set<String> dimensions,Set<String> biomes,Box box) {
        boolean matches(Context c) {
            return (battleType==null||battleType.equals(c.battleType())) && has(species,c.species()) && has(forms,c.form())
                && any(aspects,c.aspects()) && any(labels,c.labels()) && has(trainers,c.trainer())
                && (pokemon.isEmpty()||pokemon.contains(c.pokemon())) && has(encounters,c.encounter())
                && has(dimensions,c.dimension()) && (biomes.isEmpty()||biomes.stream().anyMatch(b -> b.startsWith("#")?c.biomeTags().contains(b.substring(1)):b.equals(c.biome())))
                && (box==null||box.contains(c));
        }
        private static boolean has(Set<String> s,String value){return s.isEmpty()||s.contains(value);}
        private static boolean any(Set<String> s,Set<String> values){return s.isEmpty()||s.stream().anyMatch(values::contains);}
    }
    public boolean matches(String requested,Context c){return kind.equals(requested)&&match.matches(c);}
    public static Rule parse(String id,JsonObject obj) {
        try {
            keys(obj,Set.of("schema_version","kind","priority","match","sound","transition_ticks"));
            if(integer(obj,"schema_version",-1)!=1)throw new IllegalArgumentException("unsupported schema_version");
            String kind=string(obj,"kind",null); if(!Set.of("base","entrance").contains(kind))throw new IllegalArgumentException("kind must be base or entrance");
            if(!obj.has("sound"))throw new IllegalArgumentException("sound is required (null is allowed)");
            String sound=obj.get("sound").isJsonNull()?null:string(obj,"sound",null); if(sound!=null)identifier(sound);
            int ticks=integer(obj,"transition_ticks",4); if(ticks<1||ticks>1200)throw new IllegalArgumentException("transition_ticks must be 1..1200");
            JsonObject m=obj.getAsJsonObject("match"); if(m==null)throw new IllegalArgumentException("match is required");
            keys(m,Set.of("battle_type","species","forms","aspects","labels","trainers","pokemon_uuids","encounters","dimensions","biomes","box"));
            String type=string(m,"battle_type",null); if(type!=null&&!Set.of("wild","trainer").contains(type))throw new IllegalArgumentException("invalid battle_type");
            Set<String> species=list(m,"species",true),forms=list(m,"forms",false),aspects=list(m,"aspects",false),labels=list(m,"labels",false),trainers=list(m,"trainers",true),encounters=list(m,"encounters",true),dims=list(m,"dimensions",true),biomes=list(m,"biomes",false);
            biomes.forEach(b -> identifier(b.startsWith("#")?b.substring(1):b));
            var uuids=new HashSet<UUID>();list(m,"pokemon_uuids",false).forEach(s -> uuids.add(UUID.fromString(s)));
            Box box=null;
            if(m.has("box")) {
                JsonObject b=m.getAsJsonObject("box");keys(b,Set.of("min","max"));
                double[] min=point(b,"min"),max=point(b,"max");
                for(int i=0;i<3;i++)if(min[i]>max[i])throw new IllegalArgumentException("box min exceeds max");
                box=new Box(min[0],min[1],min[2],max[0],max[1],max[2]);
            }
            return new Rule(id,kind,integer(obj,"priority",500),new Match(type,species,forms,aspects,labels,trainers,Set.copyOf(uuids),encounters,dims,biomes,box),sound,ticks);
        } catch(RuntimeException e){throw new IllegalArgumentException(id+": "+e.getMessage(),e);}
    }
    private static double[] point(JsonObject obj,String key){JsonArray a=obj.getAsJsonArray(key);if(a==null||a.size()!=3)throw new IllegalArgumentException("box points require three coordinates");double[] r=new double[3];for(int i=0;i<3;i++){r[i]=a.get(i).getAsDouble();if(!Double.isFinite(r[i]))throw new IllegalArgumentException("nonfinite coordinate");}return r;}
    private static void keys(JsonObject obj,Set<String> valid){for(String key:obj.keySet())if(!valid.contains(key))throw new IllegalArgumentException("unknown field "+key);}
    private static String string(JsonObject obj,String key,String fallback){if(!obj.has(key))return fallback;JsonElement e=obj.get(key);if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString()||e.getAsString().isBlank())throw new IllegalArgumentException(key+" must be a nonempty string");return e.getAsString();}
    private static int integer(JsonObject obj,String key,int fallback){if(!obj.has(key))return fallback;JsonElement e=obj.get(key);if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber()||!e.getAsString().matches("-?[0-9]+"))throw new IllegalArgumentException(key+" must be an integer");return Integer.parseInt(e.getAsString());}
    private static Set<String> list(JsonObject obj,String key,boolean ids){if(!obj.has(key))return Set.of();JsonArray a=obj.getAsJsonArray(key);if(a.size()==0||a.size()>1024)throw new IllegalArgumentException(key+" must contain 1..1024 values");var result=new HashSet<String>();for(JsonElement e:a){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString()||e.getAsString().isBlank())throw new IllegalArgumentException(key+" requires strings");String s=e.getAsString();if(ids)identifier(s);if(!result.add(s))throw new IllegalArgumentException("duplicate "+key+" value "+s);}return Set.copyOf(result);}
    public static void identifier(String value){if(!IDENTIFIER.matcher(value).matches())throw new IllegalArgumentException("invalid identifier "+value);}
}

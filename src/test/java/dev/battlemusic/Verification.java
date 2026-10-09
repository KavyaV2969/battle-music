package dev.battlemusic;

import com.google.gson.*;
import dev.battlemusic.rules.*;
import dev.battlemusic.net.MusicState;
import dev.battlemusic.client.Envelope;
import org.objectweb.asm.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

public final class Verification {
    public static final class RaidFixture {public boolean crd_isRaidBoss(){return true;}}
    public static final class NormalFixture {public boolean crd_isRaidBoss(){return false;}}
    static int checks;static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);checks++;}
    static Context context(String species,String form,Set<String> aspects,Set<String> labels){return new Context("wild","cobblemon:"+species,form,aspects,labels,null,UUID.fromString("00000000-0000-0000-0000-000000000001"),"test:encounter","minecraft:overworld","minecraft:plains",Set.of("minecraft:is_overworld"),1,64,1);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);var documents=new TreeMap<String,JsonObject>();
        try(var files=Files.walk(root.resolve("datapack/data/battlemusic/battlemusic/rules"))){for(Path path:files.filter(p->p.toString().endsWith(".json")).toList())documents.put("battlemusic:"+root.resolve("datapack/data/battlemusic/battlemusic/rules").relativize(path).toString().replace('\\','/').replaceFirst("\\.json$",""),JsonParser.parseString(Files.readString(path)).getAsJsonObject());}
        RuleSet rules=RuleSet.parse(1,documents);
        for(int gen=1;gen<=9;gen++){var c=context("rattata","Normal",Set.of(),Set.of("gen"+gen));check(rules.select("base",c).orElseThrow().sound().equals("battlemusic:wild.gen"+gen),"generation "+gen);}
        check(Context.labels(Set.of("gen1"),Set.of("gen7","alolan_form")).equals(Set.of("gen7","alolan_form")),"form generation overrides species");
        check(Context.labels(Set.of("gen7b","mythical"),Set.of("gen8","gmax")).containsAll(Set.of("gen8","mythical","gmax")),"form keeps mythical category");
        check(Context.labels(Set.of("gen8a"),Set.of()).contains("gen8"),"Hisui normalization");
        check(Context.labels(Set.of("gen7b"),Set.of()).contains("gen7"),"Lets Go normalization");
        check(rules.select("base",context("articuno","Galar",Set.of("galarian"),Set.of("gen8","legendary"))).orElseThrow().sound().endsWith("galarian_birds"),"Galar bird precedence");
        check(rules.select("base",context("victini","Normal",Set.of(),Set.of("gen5","mythical"))).orElseThrow().sound().equals("battlemusic:legendary.victini"),"Victini Undertale assignment");
        check(rules.select("entrance",context("pikachu","Normal",Set.of(),Set.of("gen1"))).isEmpty(),"ordinary Pokemon no entrance");
        var cat=JsonParser.parseString(Files.readString(root.resolve("catalog/music.json"))).getAsJsonObject();
        for(var e:cat.getAsJsonArray("assignments")){var a=e.getAsJsonObject();for(var s:a.getAsJsonArray("species")){
            var aspects=new HashSet<String>();if(a.has("aspects"))a.getAsJsonArray("aspects").forEach(v->aspects.add(v.getAsString()));
            var c=context(s.getAsString().substring(10),"Normal",aspects,Set.of("gen9","legendary"));
            check(rules.select("base",c).orElseThrow().sound().equals(a.get("sound").getAsString()),"special base "+s);
            check(rules.select("entrance",c).orElseThrow().sound().equals(a.get("sound").getAsString()),"special entrance "+s);
        }}
        var custom=JsonParser.parseString("{\"schema_version\":1,\"kind\":\"base\",\"priority\":800,\"sound\":null,\"match\":{\"species\":[\"cobblemon:arceus\"],\"encounters\":[\"test:encounter\"],\"dimensions\":[\"minecraft:overworld\"],\"biomes\":[\"#minecraft:is_overworld\"],\"pokemon_uuids\":[\"00000000-0000-0000-0000-000000000001\"],\"box\":{\"min\":[0,60,0],\"max\":[2,70,2]}}}").getAsJsonObject();
        var rule=Rule.parse("test:custom",custom);check(rule.matches("base",context("arceus","Normal",Set.of(),Set.of("gen4"))),"combined UUID encounter biome tag box");
        check(!rule.matches("base",context("mew","Normal",Set.of(),Set.of("gen1"))),"AND selectors");
        var extras=new HashMap<>(documents);extras.put("test:custom",custom);var updated=RuleSet.parse(2,extras);
        check(updated.select("base",context("arceus","Normal",Set.of(),Set.of("gen4"))).orElseThrow().sound()==null,"null wins and preserves native");
        check(rules.revision()==1&&rules.select("base",context("arceus","Normal",Set.of(),Set.of("gen4"))).orElseThrow().sound()!=null,"immutable battle snapshot");
        for(String invalid:List.of("{\"schema_version\":2}","{\"schema_version\":1,\"kind\":\"base\",\"sound\":\"BAD\",\"match\":{}}","{\"schema_version\":1,\"kind\":\"base\",\"sound\":null,\"match\":{\"typo\":true}}")){
            boolean rejected=false;try{Rule.parse("test:invalid",JsonParser.parseString(invalid).getAsJsonObject());}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid rule rejected");
        }
        var envelope=new Envelope(1);envelope.to(0,30);for(int i=0;i<15;i++)envelope.tick();check(Math.abs(envelope.value()-.5)<.001,"smooth midpoint");float before=envelope.value();envelope.to(1,30);check(envelope.value()==before,"retarget keeps current gain");for(int i=0;i<30;i++)envelope.tick();check(envelope.settled()&&envelope.value()==1,"fade completes");
        var state=new MusicState(1,UUID.randomUUID(),3,"battlemusic:wild.gen1","cobblemon:battle.pvw.default",1,1,"battlemusic:legendary.arceus",UUID.randomUUID(),30,false);
        check(MusicState.parse(state.json()).equals(state),"payload roundtrip");
        var trainerState=new MusicState(1,state.battle(),4,state.base(),state.fallback(),1,1,state.entrance(),state.pokemon(),30,false,true);
        check(MusicState.parse(trainerState.json()).trainerBattle(),"trainer context survives networking");
        check(!TrainerMusicPolicy.entranceAllowed(true,false),"trainer interruptions disabled by default");
        check(TrainerMusicPolicy.entranceAllowed(true,true),"trainer interruptions can be enabled");
        check(TrainerMusicPolicy.entranceAllowed(false,false),"wild interruptions unaffected by trainer preference");
        for(String id:List.of("rctmod:kanto_champion_blue","rctmod:johto_champion_lance","test:champion","test:league/champion"))check(TrainerMusicPolicy.isChampion(id),"champion protected "+id);
        for(String id:List.of("rctmod:kanto_league_lance","rctmod:kanto_brock","test:championship_qualifier"))check(!TrainerMusicPolicy.isChampion(id),"nonchampion eligible "+id);
        check(!TrainerMusicPolicy.isChampion(null),"unknown trainer identity does not crash");
        for(String trainer:List.of("lorelei","bruno","agatha","lance")){
            Context c=new Context("trainer","cobblemon:geodude","Normal",Set.of(),Set.of("gen1"),"rctmod:kanto_league_"+trainer,null,null,"minecraft:overworld","minecraft:plains",Set.of(),0,64,0);
            check(rules.select("base",c).orElseThrow().sound().equals("battlemusic:trainer.kanto."+trainer),"individual Elite Four theme "+trainer);
        }
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        Rule entranceRule=rules.select("entrance",context("arceus","Normal",Set.of(),Set.of("legendary"))).orElseThrow();
        var left=new EntranceSelection.Candidate(entranceRule,first,0);var right=new EntranceSelection.Candidate(entranceRule,second,1);
        check(EntranceSelection.choose(List.of(right,left),null)==left,"doubles stable slot order independent of iteration");
        check(EntranceSelection.choose(List.of(left,right),second)==right,"doubles retains current theme on priority tie");
        var higher=new EntranceSelection.Candidate(new Rule("test:strong","entrance",800,entranceRule.match(),entranceRule.sound(),30),first,0);
        check(EntranceSelection.choose(List.of(right,higher),second)==higher,"higher priority interrupts sticky incumbent");
        check(EntranceSelection.choose(List.of(left),second)==left,"switch/faint removes prior candidate");
        check(EntranceSelection.choose(List.of(),first)==null,"no living active opponent returns base");
        check(dev.battlemusic.server.RaidGuard.excluded(new RaidFixture()),"raid boss ownership excludes management regardless of native sound namespace");
        check(!dev.battlemusic.server.RaidGuard.excluded(new NormalFixture()),"ordinary Pokemon with raid mixins remains eligible");
        check(!dev.battlemusic.server.RaidGuard.excluded(new Object()),"raid integration optional without raid mod");
        abi(root,"Cobblemon-fabric-1.7.3+1.21.1.jar","com/cobblemon/mod/common/battles/actor/PlayerBattleActor","sendUpdate","(Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V");
        abi(root,"Cobblemon-fabric-1.7.3+1.21.1.jar","com/cobblemon/mod/common/api/battles/model/PokemonBattle","tick","()V");
        abi(root,"client-intermediary.jar","net/minecraft/class_1142","method_18669","()V");
        abi(root,"client-intermediary.jar","net/minecraft/class_1144","method_4873","(Lnet/minecraft/class_1113;)V");
        Files.createDirectories(root.resolve("reports"));Files.writeString(root.resolve("reports/unit-tests.json"),"{\"success\":true,\"checks\":"+checks+"}\n");
        System.out.println("PASS: "+checks+" rule, envelope, protocol and installed ABI checks");
    }
    static void abi(Path root,String jar,String clazz,String method,String descriptor)throws Exception{try(var z=new JarFile(root.resolve("build/deps/"+jar).toFile())){var found=new boolean[1];new ClassReader(z.getInputStream(z.getJarEntry(clazz+".class"))).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){if(n.equals(method)&&d.equals(descriptor))found[0]=true;return null;}},ClassReader.SKIP_CODE);check(found[0],"installed ABI "+clazz+"."+method);}}
}

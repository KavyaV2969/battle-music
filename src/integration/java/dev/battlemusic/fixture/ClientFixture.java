package dev.battlemusic.fixture;

import dev.battlemusic.client.*;
import dev.battlemusic.mixin.*;
import dev.battlemusic.net.MusicState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.*;
import java.util.*;
import java.util.concurrent.*;

/** Audio assertions run on the actual sound executor, including OpenAL state and stream offset. */
public final class ClientFixture implements ClientModInitializer {
    private ManagedSound retained;private class_4235.class_4236 retainedChannel;private float pausedOffset;
    private final Object[] sources=new Object[4],streams=new Object[4];private ManagedSound previousEntrance;
    private BackgroundSound background;private UUID entranceUuid;private float entrancePausedOffset;
    private int stable,togglePhase,outroTicks;private boolean defaultVerified,resumeProbed;private String last="";
    private CompletableFuture<float[]> probe,voiceProbe;
    private float backgroundFullGain,lastBackgroundGain;private int backgroundFadeTicks;
    private boolean trainerOutro,sawBackgroundFade,sawQuietBackground,sawMidBackground,lateTestDone;
    private BackgroundSound lateBackground;private CompletableFuture<float[]> lateProbe;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(client->{
        if(Shared.error!=null||Shared.expect.isBlank()||Shared.expect.equals(Shared.ack))return;
        try{tick();}catch(Throwable e){Shared.error=e.toString();e.printStackTrace();}
    });}
    private class_4235.class_4236 channel(class_1113 sound){return ((SoundSystemAccessor)((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine()).battlemusic$channels().get(sound);}
    private CompletableFuture<float[]> probe(class_1113 sound){return probe(sound,0);}
    private CompletableFuture<float[]> probe(class_1113 sound,int role){
        var future=new CompletableFuture<float[]>();var channel=channel(sound);if(channel==null){future.completeExceptionally(new IllegalStateException("No audio channel"));return future;}
        channel.method_19735(source->{try{
            var field=class_4224.class.getDeclaredField("field_18893");field.setAccessible(true);int id=field.getInt(source);
            var streamField=class_4224.class.getDeclaredField("field_18896");streamField.setAccessible(true);Object stream=streamField.get(source);
            if(sources[role]==null)sources[role]=source;
            if(streams[role]==null&&stream!=null)streams[role]=stream; // Initial asynchronous decoder attachment is expected.
            if(source!=sources[role]||stream!=streams[role])throw new AssertionError("OpenAL source or decoder stream replaced for role "+role);
            var al=Class.forName("org.lwjgl.openal.AL10");
            float offset=(float)al.getMethod("alGetSourcef",int.class,int.class).invoke(null,id,0x1024);
            int state=(int)al.getMethod("alGetSourcei",int.class,int.class).invoke(null,id,0x1010);
            float gain=(float)al.getMethod("alGetSourcef",int.class,int.class).invoke(null,id,0x100A);
            future.complete(new float[]{offset,state,gain});
        }catch(Throwable e){future.completeExceptionally(e);}});return future;
    }
    private void tick()throws Exception{
        String expect=Shared.expect;var p=Playback.INSTANCE;
        if(!expect.equals(last)){
            last=expect;stable=outroTicks=backgroundFadeTicks=0;probe=voiceProbe=null;resumeProbed=false;
            sawBackgroundFade=sawQuietBackground=sawMidBackground=false;lastBackgroundGain=-1;
        }
        if(p.state()!=null&&!p.state().ended())trainerOutro=p.state().trainerBattle();
        if(expect.equals("background")){
            if(background==null){background=new BackgroundSound();class_310.method_1551().method_1483().method_4873(background);return;}
            if(channel(background)==null)return;
            if(probe==null){probe=probe(background,2);return;}if(!probe.isDone())return;
            if(probe.join()[1]==0x1011){probe=null;return;}
            if(probe.join()[1]!=0x1012)throw new AssertionError("Background probe not playing");
            backgroundFullGain=probe.join()[2];if(backgroundFullGain<=0)throw new AssertionError("Background probe has no gain");
            ack("Independent background audio probe starts before battle");return;
        }
        if(expect.equals("ended")){checkOutro(p);return;}
        if(p.state()==null||p.base()==null)return;
        String base=p.base().method_4775().toString();
        switch(expect){
            case "wild": if(base.equals("battlemusic:wild.gen1")&&p.entrance()==null&&channel(p.base())!=null&&++stable>35){retained=p.base();ack("Real wild Rattata battle selects Gen 1; own Mew does not interrupt");}break;
            case "special": if(base.equals("battlemusic:legendary.arceus")&&p.entrance()==null&&channel(p.base())!=null&&++stable>35)ack("Real wild Arceus battle selects its special base without duplicate entrance playback");break;
            case "trainer":
                if(!base.equals("battlemusic:trainer.kanto.gym")||p.entrance()!=null||channel(p.base())==null)return;
                if(++stable<45)return;retained=p.base();retainedChannel=channel(retained);
                if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] result=probe.join();if(result[1]!=0x1012||result[2]<=0)throw new AssertionError("Base audio not audibly playing: "+Arrays.toString(result));
                if(ClientSettings.trainerEntrances())throw new AssertionError("Trainer entrances not disabled by default");
                var factory=new MusicModMenu().getModConfigScreenFactory();
                if(factory==null||factory.create(null)==null)throw new AssertionError("Mod Menu settings screen unavailable");
                Shared.checks.add("Mod Menu/Cloth Config settings screen builds with trainer entrance music disabled");
                ack("Real RCT Brock battle selects updated Desktop gym audio through native packets");break;
            case "overlay": case "reentered":
                if(expect.equals("overlay")&&!defaultVerified){
                    if(p.state().entrance()==null)return;
                    if(p.entrance()!=null||p.base().paused||p.base().gain.value()<.99)throw new AssertionError("Default-off trainer entrance interrupted base");
                    if(++stable<2)return;
                    Shared.checks.add("Trainer Arceus sends an entrance decision but default-off preference keeps gym audio playing");
                    ClientSettings.setTrainerEntrances(true);ClientSettings.save();ClientSettings.load();
                    if(!ClientSettings.trainerEntrances())throw new AssertionError("Preference did not persist");
                    defaultVerified=true;stable=0;
                }
                if(p.entrance()==null||!p.entrance().method_4775().toString().equals("battlemusic:legendary.arceus")||!p.base().paused)return;
                if(++stable<2)return;if(p.base()!=retained||channel(p.base())!=retainedChannel)throw new AssertionError("Base/channel replaced on interruption");
                if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] sample=probe.join();if(sample[1]!=0x1013)throw new AssertionError("Base source was not paused: "+Arrays.toString(sample));
                pausedOffset=sample[0];Shared.measurements.addProperty("pausedSourceOffsetSeconds",pausedOffset);
                if(expect.equals("reentered")&&p.entrance()!=previousEntrance)throw new AssertionError("Re-entry replaced retained entrance voice");
                if(voiceProbe==null){voiceProbe=probe(p.entrance(),1);return;}if(!voiceProbe.isDone())return;
                if(voiceProbe.join()[1]!=0x1012||voiceProbe.join()[2]<=0)throw new AssertionError("Entrance is not audibly playing");
                previousEntrance=p.entrance();entranceUuid=p.state().pokemon();
                if(expect.equals("reentered")){
                    long millis=(System.nanoTime()-Shared.entrySentNanos)/1_000_000;
                    if(Shared.entrySentNanos==0||millis>600)throw new AssertionError("Entrance response too late: "+millis+" ms");
                    Shared.measurements.addProperty("reentryAudibleAfterSendoutPacketMs",millis);
                    Shared.measurements.addProperty("sameEntranceOpenALSourceAndDecoder",true);
                }
                ack(expect.equals("reentered")?"Opponent re-entry resumes its retained entrance voice/source/decoder within 600 ms of send-out":"Actual trainer switch to Arceus crossfades and pauses the original OpenAL channel");break;
            case "pause-retained":
                if(++stable<40)return;if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] paused=probe.join();if(paused[1]!=0x1013||Math.abs(paused[0]-pausedOffset)>.03)throw new AssertionError("Paused music advanced: "+Arrays.toString(paused)+" from "+pausedOffset);
                ack("Paused base stream offset remains fixed while entrance music plays");break;
            case "entrance-pause-retained":
                if(++stable<40)return;if(probe==null){probe=probe(previousEntrance,1);return;}if(!probe.isDone())return;
                float[] benched=probe.join();if(benched[1]!=0x1013||Math.abs(benched[0]-entrancePausedOffset)>.03)throw new AssertionError("Benched entrance music advanced: "+Arrays.toString(benched));
                ack("Benched Arceus retains a fixed stream offset until re-entry");break;
            case "toggle-off":
                if(togglePhase==0){ClientSettings.setTrainerEntrances(false);ClientSettings.save();togglePhase=1;}
                if(togglePhase==1){
                    if(p.entrance()!=null||p.base().paused||p.base().gain.value()<.99)return;
                    if(p.base()!=retained||channel(retained)!=retainedChannel)throw new AssertionError("Toggle off replaced base");
                    Shared.checks.add("Disabling trainer interruptions mid-battle resumes retained gym channel");
                    ClientSettings.setTrainerEntrances(true);ClientSettings.save();togglePhase=2;return;
                }
                if(p.entrance()==null||!p.base().paused)return;
                if(p.base()!=retained||channel(retained)!=retainedChannel)throw new AssertionError("Toggle on replaced base");
                ack("Enabling interruptions mid-battle starts current opponent theme using latest decision");break;
            case "champion":
                if(!Shared.championArceusActive)return;
                if(!base.equals("battlemusic:trainer.kanto.champion")||channel(p.base())==null)return;
                if(!ClientSettings.trainerEntrances()||!p.state().trainerBattle())throw new AssertionError("Champion test requires enabled trainer interruptions");
                if(p.state().entrance()!=null||p.entrance()!=null||p.base().paused)throw new AssertionError("Champion theme interrupted by active Arceus");
                if(++stable<45)return;
                ack("Actual RCT Champion Blue with active Arceus keeps updated Champion theme even with interruptions enabled");break;
            case "leading":
                if(!base.equals("battlemusic:trainer.kanto.gym")||p.entrance()==null)return;
                if(p.state().revision()!=1||p.entrance().gain.value()!=1||p.base().gain.value()!=0)throw new AssertionError("Leading Arceus did not start immediately at full gain: "+p.state().json()+" entrance="+p.entrance().gain.value()+" base="+p.base().gain.value());
                if(channel(p.entrance())==null)return;
                if(++stable<4)return;
                ack("Leading trainer Arceus starts with full entrance gain and no trainer-theme blip or fade-in");break;
            case "elite-lorelei": case "elite-bruno": case "elite-agatha": case "elite-lance":
                String member=expect.substring(6);
                if(!base.equals("battlemusic:trainer.kanto."+member)||channel(p.base())==null)return;
                if(++stable<45)return;
                if(probe==null){sources[0]=streams[0]=null;probe=probe(p.base());return;}if(!probe.isDone())return;
                float[] elite=probe.join();if(elite[1]!=0x1012||elite[2]<=0)throw new AssertionError("Elite Four theme is not audible");
                ack("Actual RCT "+member+" battle plays its individual imported track with positive OpenAL gain");break;
            case "resumed": case "switch-resumed":
                if(p.entrance()!=null||p.base().paused||p.base().gain.value()<.99)return;
                if(p.base()!=retained||channel(p.base())!=retainedChannel)throw new AssertionError("Resume recreated original sound/channel");
                if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] resumed=probe.join();if(resumed[1]!=0x1012||!Float.isFinite(resumed[0])||resumed[0]<0||resumed[2]<=0)throw new AssertionError("Retained source did not resume audibly: "+Arrays.toString(resumed));
                // AL_SEC_OFFSET is relative to the currently queued streaming buffers and wraps
                // when processed buffers are unqueued. Source and decoder identities prove retention.
                Shared.measurements.addProperty("resumedQueueOffsetSeconds",resumed[0]);
                Shared.measurements.addProperty("sameOpenALSourceAndDecoder",true);
                if(expect.equals("switch-resumed")){
                    if(voiceProbe==null){voiceProbe=probe(previousEntrance,1);return;}if(!voiceProbe.isDone())return;
                    float[] pausedVoice=voiceProbe.join();
                    if(pausedVoice[1]!=0x1013)throw new AssertionError("Recalled entrance source not paused");
                    entrancePausedOffset=pausedVoice[0];Shared.measurements.addProperty("pausedEntranceOffsetSeconds",entrancePausedOffset);
                    long millis=(System.nanoTime()-Shared.recallStartedNanos)/1_000_000;
                    if(Shared.recallStartedNanos==0||millis>600)throw new AssertionError("Recall response too late: "+millis+" ms");
                    Shared.measurements.addProperty("trainerResumedAfterRecallStartMs",millis);
                }
                ack(expect.equals("switch-resumed")?"Opponent switch-out audibly resumes the original source and decoder":"Arceus faint audibly resumes the same base sound, OpenAL source and decoder stream");break;
            case "rapid":
                if(stable++==0){
                    var latest=p.state();
                    for(int i=1;i<=24;i++)localEntrance(p,new MusicState(1,latest.battle(),latest.revision(),latest.base(),latest.fallback(),1,1,
                        i%2==0?null:"battlemusic:legendary.arceus",i%2==0?null:new UUID(0,100+i),4,false));
                }
                if(stable<40)return;
                if(p.base()!=retained||channel(retained)!=retainedChannel||p.entrance()!=null||retained.paused)throw new AssertionError("Rapid retargeting lost retained base");
                long voices=((SoundSystemAccessor)((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine()).battlemusic$channels().keySet().stream().filter(s->s instanceof ManagedSound).count();
                if(voices!=14||!previousEntrance.paused)throw new AssertionError("Retained voice capacity/cleanup mismatch: "+voices);
                ack("Twenty-four rapid changes retain base and thirteen paused entrances beyond the reserved streaming-pool limit");break;
            case "stale":
                MusicState before=p.state();p.accept(new MusicState(1,before.battle(),0,before.base(),before.fallback(),1,1,null,null,30,false));
                if(p.state()!=before)throw new AssertionError("Stale revision replaced current state");ack("Stale payload ignored");break;
            case "missing":
                MusicState previous=p.state();
                localEntrance(p,new MusicState(1,previous.battle(),previous.revision(),previous.base(),previous.fallback(),1,1,"battlemusic:missing",UUID.randomUUID(),30,false));
                if(p.entrance()!=null||p.base()!=retained)throw new AssertionError("Missing audio destroyed base");ack("Missing entrance audio retains original base");break;
        }
    }
    private void checkOutro(Playback p){
        if(p.owns()){
            if(!p.state().ended())return;outroTicks++;
            com.cobblemon.mod.common.util.SoundExtensionsKt.resumeSounds(class_310.method_1551().method_1483(),null,class_3419.field_15253);
            ((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine().method_19762();
            if(probe==null){probe=probe(background,2);return;}if(!probe.isDone())return;
            if(probe.join()[1]!=0x1013)throw new AssertionError("Background resumed before battle tail finished");
            probe=null;return;
        }
        if(outroTicks<65)throw new AssertionError("Outro/background gap too short: "+outroTicks);
        if(p.backgroundFading()){
            if(!trainerOutro)throw new AssertionError("Wild outro unexpectedly changed background fade policy");
            sawBackgroundFade=true;backgroundFadeTicks++;
            float envelope=p.backgroundGain();
            if(envelope>.2f&&envelope<.4f&&!lateTestDone&&lateBackground==null){
                // Exercise an ordinary non-tickable BGM source starting during the fade,
                // and Minecraft's category-volume update path at the same time.
                lateBackground=new BackgroundSound();class_310.method_1551().method_1483().method_4873(lateBackground);
                ((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine().method_4844(class_3419.field_15253,.65f);
            }
            if(lateBackground!=null&&!lateTestDone&&channel(lateBackground)!=null){
                if(lateProbe==null)lateProbe=probe(lateBackground,3);
                if(lateProbe.isDone()){
                    float[] started=lateProbe.join();
                    if(started[2]<=0||started[2]>=backgroundFullGain*.8f)throw new AssertionError("New BGM bypassed fade: "+Arrays.toString(started));
                    if(started[1]==0x1011)lateProbe=null; // Channel allocated; streaming decoder still starts asynchronously.
                    else{
                        if(started[1]!=0x1012)throw new AssertionError("New BGM failed to start: "+Arrays.toString(started));
                        Shared.measurements.addProperty("newBackgroundRelativeGainDuringFade",started[2]/backgroundFullGain);
                        lateTestDone=true;Shared.checks.add("New non-tickable BGM and category-volume updates respect the in-progress fade");
                    }
                }
            }
            if(probe==null){probe=probe(background,2);return;}if(!probe.isDone())return;
            float[] sample=probe.join();probe=null;
            if(sample[1]!=0x1012)throw new AssertionError("Background failed to play during fade");
            if(lastBackgroundGain>=0&&sample[2]+backgroundFullGain*.015f<lastBackgroundGain)throw new AssertionError("Background gain went backwards during fade");
            if(envelope<.2f){if(sample[2]>=backgroundFullGain*.3f)throw new AssertionError("Background resumed at full gain");sawQuietBackground=true;}
            if(envelope>.35f&&envelope<.65f){if(sample[2]<=backgroundFullGain*.2f||sample[2]>=backgroundFullGain*.8f)throw new AssertionError("Background midpoint gain incorrect");sawMidBackground=true;Shared.measurements.addProperty("backgroundMidFadeRelativeGain",sample[2]/backgroundFullGain);}
            lastBackgroundGain=sample[2];return;
        }
        if(trainerOutro&&(!sawBackgroundFade||!sawQuietBackground||!sawMidBackground||backgroundFadeTicks<55))throw new AssertionError("Trainer background fade missing or too short: "+backgroundFadeTicks);
        if(!resumeProbed){probe=null;resumeProbed=true;}
        if(probe==null){probe=probe(background,2);return;}if(!probe.isDone())return;
        if(probe.join()[1]!=0x1012)throw new AssertionError("Background did not resume after outro");
        if(Math.abs(probe.join()[2]-backgroundFullGain)>backgroundFullGain*.05f)throw new AssertionError("Background did not return to its original volume");
        if(trainerOutro){Shared.measurements.addProperty("trainerBackgroundFadeTicks",backgroundFadeTicks);Shared.measurements.addProperty("backgroundFinalRelativeGain",probe.join()[2]/backgroundFullGain);}
        if(lateBackground!=null){if(!lateTestDone)throw new AssertionError("Late BGM test incomplete");class_310.method_1551().method_1483().method_4870(lateBackground);lateBackground=null;}
        long voices=((SoundSystemAccessor)((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine()).battlemusic$channels().keySet().stream().filter(s->s instanceof ManagedSound).count();
        if(voices!=0)return;
        ack(trainerOutro?"Background stays paused through trainer outro, then fades smoothly from near-silence to its original gain over three seconds":"Background stays paused throughout the battle tail and quiet gap, then resumes; retained battle channels are released");
    }
    private void localEntrance(Playback controller,MusicState selection)throws Exception{
        // Exercise playback failures/retargeting without forging authoritative server revisions.
        var method=Playback.class.getDeclaredMethod("chooseEntrance",MusicState.class);method.setAccessible(true);method.invoke(controller,selection);
    }
    private void ack(String check){Shared.checks.add(check);Shared.ack=Shared.expect;System.out.println("BATTLEMUSIC CHECK: "+check);}
}

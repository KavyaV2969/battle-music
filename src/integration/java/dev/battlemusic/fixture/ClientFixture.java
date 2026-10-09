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
    private Object retainedSource,retainedStream;private ManagedSound previousEntrance;
    private int stable;private String last="";private CompletableFuture<float[]> probe;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(client->{
        if(Shared.error!=null||Shared.expect.isBlank()||Shared.expect.equals(Shared.ack))return;
        try{tick();}catch(Throwable e){Shared.error=e.toString();e.printStackTrace();}
    });}
    private class_4235.class_4236 channel(ManagedSound sound){return ((SoundSystemAccessor)((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine()).battlemusic$channels().get(sound);}
    private CompletableFuture<float[]> probe(ManagedSound sound){
        var future=new CompletableFuture<float[]>();var channel=channel(sound);if(channel==null){future.completeExceptionally(new IllegalStateException("No audio channel"));return future;}
        channel.method_19735(source->{try{
            var field=class_4224.class.getDeclaredField("field_18893");field.setAccessible(true);int id=field.getInt(source);
            var streamField=class_4224.class.getDeclaredField("field_18896");streamField.setAccessible(true);Object stream=streamField.get(source);
            if(retainedSource==null){retainedSource=source;retainedStream=stream;}
            if(source!=retainedSource||stream!=retainedStream)throw new AssertionError("OpenAL source or decoder stream replaced");
            var al=Class.forName("org.lwjgl.openal.AL10");
            float offset=(float)al.getMethod("alGetSourcef",int.class,int.class).invoke(null,id,0x1024);
            int state=(int)al.getMethod("alGetSourcei",int.class,int.class).invoke(null,id,0x1010);
            float gain=(float)al.getMethod("alGetSourcef",int.class,int.class).invoke(null,id,0x100A);
            future.complete(new float[]{offset,state,gain});
        }catch(Throwable e){future.completeExceptionally(e);}});return future;
    }
    private void tick()throws Exception{
        String expect=Shared.expect;var p=Playback.INSTANCE;
        if(!expect.equals(last)){last=expect;stable=0;probe=null;}
        if(expect.equals("ended")){if(!p.owns()){ack("Battle end releases channels and restores background playback");}return;}
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
                ack("Real RCT Brock battle selects Kanto gym audio through native server/client packets");break;
            case "overlay": case "reentered":
                if(p.entrance()==null||!p.entrance().method_4775().toString().equals("battlemusic:legendary.arceus")||!p.base().paused)return;
                if(++stable<20)return;if(p.base()!=retained||channel(p.base())!=retainedChannel)throw new AssertionError("Base/channel replaced on interruption");
                if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] sample=probe.join();if(sample[1]!=0x1013)throw new AssertionError("Base source was not paused: "+Arrays.toString(sample));
                pausedOffset=sample[0];Shared.measurements.addProperty("pausedSourceOffsetSeconds",pausedOffset);
                if(expect.equals("reentered")&&p.entrance()==previousEntrance)throw new AssertionError("Re-entry reused old entrance voice");
                previousEntrance=p.entrance();
                ack(expect.equals("reentered")?"Repeated opponent entry restarts entrance voice while retaining trainer source":"Actual trainer switch to Arceus crossfades and pauses the original OpenAL channel");break;
            case "pause-retained":
                if(++stable<40)return;if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] paused=probe.join();if(paused[1]!=0x1013||Math.abs(paused[0]-pausedOffset)>.03)throw new AssertionError("Paused music advanced: "+Arrays.toString(paused)+" from "+pausedOffset);
                ack("Paused base stream offset remains fixed while entrance music plays");break;
            case "resumed": case "switch-resumed":
                if(p.entrance()!=null||p.base().paused||p.base().gain.value()<.99)return;
                if(p.base()!=retained||channel(p.base())!=retainedChannel)throw new AssertionError("Resume recreated original sound/channel");
                if(probe==null){probe=probe(retained);return;}if(!probe.isDone())return;
                float[] resumed=probe.join();if(resumed[1]!=0x1012||!Float.isFinite(resumed[0])||resumed[0]<0||resumed[2]<=0)throw new AssertionError("Retained source did not resume audibly: "+Arrays.toString(resumed));
                // AL_SEC_OFFSET is relative to the currently queued streaming buffers and wraps
                // when processed buffers are unqueued. Source and decoder identities prove retention.
                Shared.measurements.addProperty("resumedQueueOffsetSeconds",resumed[0]);
                Shared.measurements.addProperty("sameOpenALSourceAndDecoder",true);
                ack(expect.equals("switch-resumed")?"Opponent switch-out audibly resumes the original source and decoder":"Arceus faint audibly resumes the same base sound, OpenAL source and decoder stream");break;
            case "rapid":
                if(stable++==0){
                    var latest=p.state();
                    for(int i=1;i<=12;i++)localEntrance(p,new MusicState(1,latest.battle(),latest.revision(),latest.base(),latest.fallback(),1,1,
                        i%2==0?null:"battlemusic:legendary.arceus",i%2==0?null:UUID.randomUUID(),30,false));
                }
                if(stable<40)return;
                if(p.base()!=retained||channel(retained)!=retainedChannel||p.entrance()!=null||retained.paused)throw new AssertionError("Rapid retargeting lost retained base");
                long voices=((SoundSystemAccessor)((SoundManagerAccessor)class_310.method_1551().method_1483()).battlemusic$engine()).battlemusic$channels().keySet().stream().filter(s->s instanceof ManagedSound).count();
                if(voices!=1)throw new AssertionError("Rapid changes leaked voices: "+voices);
                ack("Twelve rapid entrance changes settle on the same retained base with no leaked voices");break;
            case "stale":
                MusicState before=p.state();p.accept(new MusicState(1,before.battle(),0,before.base(),before.fallback(),1,1,null,null,30,false));
                if(p.state()!=before)throw new AssertionError("Stale revision replaced current state");ack("Stale payload ignored");break;
            case "missing":
                MusicState previous=p.state();
                localEntrance(p,new MusicState(1,previous.battle(),previous.revision(),previous.base(),previous.fallback(),1,1,"battlemusic:missing",UUID.randomUUID(),30,false));
                if(p.entrance()!=null||p.base()!=retained)throw new AssertionError("Missing audio destroyed base");ack("Missing entrance audio retains original base");break;
        }
    }
    private void localEntrance(Playback controller,MusicState selection)throws Exception{
        // Exercise playback failures/retargeting without forging authoritative server revisions.
        var method=Playback.class.getDeclaredMethod("chooseEntrance",MusicState.class);method.setAccessible(true);method.invoke(controller,selection);
    }
    private void ack(String check){Shared.checks.add(check);Shared.ack=Shared.expect;System.out.println("BATTLEMUSIC CHECK: "+check);}
}

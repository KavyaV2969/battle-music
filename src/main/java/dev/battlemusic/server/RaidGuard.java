package dev.battlemusic.server;

import java.lang.reflect.Method;
import java.util.*;

/** Optional Raid Dens mixin API; no dependency on its JAR or client classes. */
public final class RaidGuard {
    private static final ClassValue<List<Method>> FLAGS=new ClassValue<>(){
        @Override protected List<Method> computeValue(Class<?> type){
            var methods=new ArrayList<Method>();
            for(String name:List.of("crd_isRaidBattle","crd_isRaidBoss"))try{
                Method method=type.getMethod(name);if(method.getReturnType()==boolean.class)methods.add(method);
            }catch(NoSuchMethodException ignored){}
            return List.copyOf(methods);
        }
    };
    private RaidGuard(){}
    public static boolean excluded(Object object){
        if(object==null)return false;
        for(Method flag:FLAGS.get(object.getClass()))try{if((boolean)flag.invoke(object))return true;}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot inspect raid ownership",e);}
        return false;
    }
}

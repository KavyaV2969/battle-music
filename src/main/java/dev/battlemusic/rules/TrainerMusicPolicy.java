package dev.battlemusic.rules;

/** Champion identities are protected even when trainer entrance music is enabled. */
public final class TrainerMusicPolicy {
    private TrainerMusicPolicy(){}
    public static boolean isChampion(String trainer){
        if(trainer==null)return false;
        String path=trainer.substring(trainer.indexOf(':')+1);
        return java.util.regex.Pattern.compile("(^|[/_.-])champion([/_.-]|$)").matcher(path).find();
    }
    public static boolean entranceAllowed(boolean trainerBattle,boolean trainerEntrancesEnabled){
        return !trainerBattle||trainerEntrancesEnabled;
    }
}

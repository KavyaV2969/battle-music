package dev.battlemusic.rules;

import java.util.*;

/** Stable choice across multiple simultaneously active opponents. */
public final class EntranceSelection {
    public record Candidate(Rule rule,UUID pokemon,int slot){}
    private EntranceSelection(){}
    public static Candidate choose(List<Candidate> candidates,UUID current){
        return candidates.stream().min(Comparator.<Candidate>comparingInt(c->c.rule().priority()).reversed()
            .thenComparingInt(c->c.pokemon().equals(current)?0:1).thenComparingInt(Candidate::slot)).orElse(null);
    }
}

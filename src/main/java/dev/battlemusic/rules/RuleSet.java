package dev.battlemusic.rules;

import com.google.gson.*;
import java.util.*;

public record RuleSet(long revision,List<Rule> rules) {
    public static final RuleSet EMPTY=new RuleSet(0,List.of());
    public RuleSet { rules=rules.stream().sorted(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::id)).toList(); }
    public Optional<Rule> select(String kind,Context context){return rules.stream().filter(r -> r.matches(kind,context)).findFirst();}
    public static RuleSet parse(long revision,Map<String,JsonObject> documents){var rules=new ArrayList<Rule>();documents.forEach((id,obj)->rules.add(Rule.parse(id,obj)));return new RuleSet(revision,rules);}
}

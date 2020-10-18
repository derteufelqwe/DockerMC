package de.derteufelqwe.commons;

import de.derteufelqwe.commons.config.annotations.Comment;
import de.derteufelqwe.commons.config.annotations.Exclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Sub {

    @Comment("Das")
    private String a = "a";
    @Comment("Dass")
    private String b = "b";

}

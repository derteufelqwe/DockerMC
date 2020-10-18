package de.derteufelqwe.commons.containers;

import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pair<A, B> {

    @Comment("Hallo")
    private A a;
    private B b;

}

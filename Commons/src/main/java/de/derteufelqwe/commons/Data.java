package de.derteufelqwe.commons;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.commons.config.annotations.Comment;
import de.derteufelqwe.commons.config.annotations.Exclude;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@lombok.Data
public class Data {

    @Comment("Welt")
    private String name = "Arne";
//    private int age = 22;
//    @Exclude
//    private int ag1 = 22;
//    private int ag2 = 22;
//
    //    private List<Integer> lst = Arrays.asList(1, 2, 3);
//    private Sub sub = new Sub();
    @Comment("Map")
    private Map<String, Sub> map = new HashMap<String, Sub>() {{ put("A", new Sub()); }};
//    @Comment("Test")
//    private Sub2 su2 = new Sub2();

}

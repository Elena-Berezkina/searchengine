package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LuceneTest {

    public static void main(String[] args) throws IOException {

        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);

        LuceneMorphology luceneMorph1 =
                new RussianLuceneMorphology();
        List<String> wordBaseForms1 =
                luceneMorph1.getMorphInfo("леса");
        wordBaseForms1.forEach(System.out::println);








    }
}


















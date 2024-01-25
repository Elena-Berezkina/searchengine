package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;



public class LemmaIndexing {

    public String getHtmlText(String url) throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla").get();
        return doc.html();
    }

    public HashMap<String, Integer> getLemmas(String text) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        String query = text.toLowerCase().replaceAll("[^а-яА-ЯеЁ -]", "").toLowerCase();
        String[] words;
        if (query.trim().contains(" ")) {
            String[] wordsArray = query.split(" ");
            words = wordsArray;
        } else {
            words = new String[]{query};
        }
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        for (String word : words) {
            if (word.matches(".*\\p{InCyrillic}.*")) {
                List<String> forms = luceneMorphology.getMorphInfo(word);
                fillTheMap(lemmaMap, forms);
            }
        }
        return lemmaMap;
    }

    public void fillTheMap(HashMap<String, Integer> someMap, List<String> someList) {
        for (String element : someList) {
            String keyWord = element.substring(0, element.indexOf("|"));
            if (keyWord.length() > 1) {
                if (someMap.containsKey(keyWord) && !element.contains("|l") && !element.contains("|p") && !element.contains("|n")) {
                    someMap.put(keyWord, someMap.get(keyWord) + 1);
                } else if (!someMap.containsKey(keyWord) && !element.contains("|l") && !element.contains("|p") && !element.contains("|n")) {
                    someMap.put(keyWord, 1);
                }
            }
        }
    }


    public String getSnippet(String path, String pattern) throws IOException {
        StringBuilder snippet = new StringBuilder();
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        //List<String> wordBaseForms = luceneMorph.getNormalForms(pattern);
        List<String> wordBaseForms = getFormsFromQuery(pattern, luceneMorph);
        String text = Jsoup.connect(path).userAgent("Mozilla").get().text();
        String[] textWords = text.split(" ");
        for (String textWord : textWords) {
            if (textWord.matches(".*\\p{InCyrillic}.*")) {
                List<String> textWordForms = luceneMorph.getNormalForms(textWord.replaceAll("[^а-яА-ЯеЁ -]", "").toLowerCase());
                for (String textWordForm : textWordForms) {
                    for (String wordBaseForm : wordBaseForms) {
                        if (wordBaseForm.equals(textWordForm)) {                    //
                            int i = text.indexOf(textWord);
                            String wordPart = text.substring(i, i + textWord.length());
                            String firstPart = text.substring(0, i);
                            String lastPart = text.substring(i + textWord.length());
                            snippet.append("...").append(firstPart.substring(Math.max(0, firstPart.length() - 60)))
                                    .append("<b>" + wordPart + "</b>").append(lastPart.substring(0, Math.min(lastPart.length(), 60))).append("...");
                        }
                    }
                }
            }
        }
        return snippet.toString();
    }


    public List<String> getFormsFromQuery(String query, LuceneMorphology luceneMorph) {
        List<String> wordBaseForms = new ArrayList<>();
        if(query.trim().contains(" ")) {
            for (String word : query.split(" ")) {
                wordBaseForms.addAll(luceneMorph.getNormalForms(word.toLowerCase()));
            }
        } else { wordBaseForms.addAll(luceneMorph.getNormalForms(query)); }
        return wordBaseForms;
        }

    }






































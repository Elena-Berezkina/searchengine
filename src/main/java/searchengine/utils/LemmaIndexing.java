package searchengine.utils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import java.io.IOException;
import java.util.*;


public class LemmaIndexing {
    private final LuceneMorphology luceneMorphology;

    {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHtmlText(String url) throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla").get();
        return doc.html();
    }

    public HashMap<String, Integer> getLemmas(String text) {
        String textModified = text.replaceAll("[^а-яА-ЯеЁ -]", "").toLowerCase();
        List<String> lemmaList = new ArrayList<>();
        for (String word : textModified.split(" ")) {
            if (word.matches(".*\\p{InCyrillic}.*")) {
                lemmaList.addAll(luceneMorphology.getMorphInfo(word));
            }
        }
        return fillTheMap(lemmaList);
    }

    public HashMap<String, Integer> fillTheMap(List<String> someList) {
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        for (String element : someList) {
            String keyWord = element.substring(0, element.indexOf("|"));
            if (keyWord.length() > 1 && !element.contains("|l") && !element.contains("|p") && !element.contains("|n")) {
                if (lemmaMap.containsKey(keyWord)) {
                    lemmaMap.put(keyWord, lemmaMap.get(keyWord) + 1);
                } else {
                    lemmaMap.put(keyWord, 1);
                }
            }
        }
        return lemmaMap;
    }


    public String setResultSnippet(String path, String query) throws IOException {
        StringBuilder snippet = new StringBuilder();
        List<String> wordBaseForms = getFormsFromQuery(query);
        String text = Jsoup.connect(path).userAgent("Mozilla").get().text();
        String[] textWords = text.split(" ");
        for (String textWord : textWords) {
            if (textWord.matches(".*\\p{InCyrillic}.*")) {
                List<String> textWordForms = luceneMorphology.getNormalForms(textWord
                        .replaceAll("[^а-яА-ЯеЁ -]", "").toLowerCase());
                buildSnippet(textWordForms, wordBaseForms, text, textWord, snippet);
            }
        }
        return snippet.toString();

    }

    public void buildSnippet(List<String> textWordForms, List<String> queryWordForms, String text, String textWord,
                            StringBuilder snippet) {
        for (String textWordForm : textWordForms) {
            queryWordForms.stream().filter(wordBaseForm -> wordBaseForm.equals(textWordForm))
                    .forEach(wordBaseForm -> {
                        int index = text.indexOf(textWord);
                        String wordPart = text.substring(index, index + textWord.length());
                        String firstPart = text.substring(0, index);
                        String lastPart = text.substring(index + textWord.length());
                        snippet.append("...").append(firstPart.substring(Math.max(0, firstPart.length() - 60))).append("<b>")
                                .append(wordPart).append("</b>").append(lastPart, 0, Math.min(lastPart.length(), 60))
                                .append("...");
                    });
        }
    }



    public List<String> getFormsFromQuery(String query) {
        List<String> wordBaseForms = new ArrayList<>();
        if(query.trim().contains(" ")) {
            getFormsList(query.toLowerCase(), wordBaseForms);
        } else { wordBaseForms.addAll(luceneMorphology.getNormalForms(query)); }
        return wordBaseForms;
        }


    public List<String> getFormsList(String text, List<String> listForForms) {
        for(String word : text.split(" ")) {
            if (word.matches(".*\\p{InCyrillic}.*")) {
                listForForms.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return listForForms;
    }

    public boolean pageContainsLemma(Page page, Lemma lemma) {
        List<String> lemmaTextList = new ArrayList<>();
        for(Index index : page.getIndexes()) {
            lemmaTextList.add(index.getLemmaId().getLemma());
        }
        return lemmaTextList.contains(lemma.getLemma());


    }


    }






































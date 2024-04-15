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

    public String setResultSnippet(Page page, String query) throws IOException {
        StringBuilder snippet = new StringBuilder();
        List<String> queryForms = getFormsFromQuery(query);
        String text = Jsoup.parse(page.getContent()).text();
        List<String> resultsInText = getResultWords(text, queryForms);
        return buildSnippet(snippet, text, resultsInText);
    }

    public String buildSnippet(StringBuilder stringBuilder, String text, List<String> resultsInText) {
        LinkedHashMap<Integer, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i <= resultsInText.size() - 1; i++) {
            int index = text.indexOf(resultsInText.get(i));
            while (index != -1) {
                indexes.put(index, index + resultsInText.get(i).length());
                index = text.indexOf(resultsInText.get(i), index + 1);
            }
        }
        for (Map.Entry<Integer, Integer> entry : indexes.entrySet()) {
            String firstPart = text.substring(0, entry.getKey()).substring(Math.max(0, text.substring(0, entry.getKey()).length() - 60));
            String result = text.substring(entry.getKey(), entry.getValue());
            String lastPart = text.substring(entry.getValue()).substring(0, Math.min(text.substring(entry.getValue()).length(), 60));
            if (isPresentInText(lastPart, text, indexes) && !isPresentInText(firstPart, text, indexes) && !containsSubstring(text, firstPart, stringBuilder.toString())) {
                stringBuilder.append(" ...").append(firstPart).append("<b>" + result + "</b>" + " ").append(cutString(lastPart, text, indexes)).append("... ");
            } else if (!isPresentInText(lastPart, text, indexes) && isPresentInText(firstPart, text, indexes) && !containsSubstring(text, lastPart, stringBuilder.toString())) {
                stringBuilder.append(cutString(firstPart, text, indexes)).append(" " + "<b>" + result + "</b>").append(lastPart).append("... ");
            } else if (!isPresentInText(lastPart, text, indexes) && !isPresentInText(firstPart, text, indexes) && !stringBuilder.toString().contains(firstPart) && !stringBuilder.toString().contains(lastPart)) {
                stringBuilder.append(" ...").append(firstPart).append("<b>" + result + " </b>").append(lastPart).append("... ");
            } else if (isPresentInText(lastPart, text, indexes) && isPresentInText(firstPart, text, indexes) && !containsSubstring(text, firstPart, stringBuilder.toString()) && !containsSubstring(text, lastPart, stringBuilder.toString())) {
                    stringBuilder.append(" ...").append(cutString(firstPart, text, indexes)).append("<b>" + result + " </b>").append(cutString(lastPart, text, indexes)).append("... ");
                }
            }
            return stringBuilder.toString();
        }

    public boolean isPresentInText(String textPart, String text, HashMap<Integer, Integer> indexes) {
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : indexes.entrySet()) {
           String word = " " + text.substring(entry.getKey(), entry.getValue());
           if (textPart.contains(word)) {
               i++;
           }
        }
        return i > 0;
    }

    public String cutString(String textPart, String text, HashMap<Integer, Integer> indexes) {
        for (Map.Entry<Integer, Integer> entry : indexes.entrySet()) {
            String word = " " + text.substring(entry.getKey(), entry.getValue());
            if (textPart.contains(word)) {
                String firstPart = textPart.substring(0, textPart.indexOf(word));
                String centralPart = "<b>" + textPart.substring(textPart.indexOf(word), textPart.indexOf(word) + word.length()) + "</b>";
                String lastPart = textPart.substring(textPart.indexOf(word) + word.length());
                if (firstPart.isEmpty() && !lastPart.isEmpty()) {
                    textPart = centralPart + lastPart;
                } else if (lastPart.isEmpty() && !firstPart.isEmpty()) {
                    textPart = firstPart + centralPart;
                } else if (firstPart.isEmpty() && lastPart.isEmpty()) {
                    textPart = centralPart;
                }
            }
        }
        return textPart;
    }

    public List<String> getResultWords(String text, List<String> queryForms) {
        LinkedList<String> resultsInText = new LinkedList<>();
        String[] textWords = text.split(" ");
        for (String textWord : textWords) {
            if (textWord.matches(".*\\p{InCyrillic}.*")) {
                List<String> textWordForms = luceneMorphology.getNormalForms(textWord
                        .replaceAll("[^а-яА-ЯеЁ -]", "").toLowerCase());
                if (containsForm(queryForms, textWordForms)) {
                    resultsInText.add(textWord);
                }
            }
        }
        return resultsInText;
    }

    public List<String> getFormsFromQuery(String query) {
        List<String> wordBaseForms = new ArrayList<>();
        for (String word : query.toLowerCase().split(" ")) {
            if (word.matches(".*\\p{InCyrillic}.*")) {
                wordBaseForms.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return wordBaseForms;
    }

    public boolean pageContainsLemma(Page page, Lemma lemma) {
        List<String> lemmaTextList = new ArrayList<>();
        for(Index index : page.getIndexes()) {
            lemmaTextList.add(index.getLemmaId().getLemma());
        }
        return lemmaTextList.contains(lemma.getLemma());
    }

    public boolean containsForm(List<String> qForms, List<String> wordForms) {
        int i =0;
        for(String wForm : wordForms) {
            if(qForms.contains(wForm)) {
                i++;
            }
        }
        return i > 0;
    }

    public boolean containsSubstring(String text, String substring, String string) {
        int length = substring.length();
        int index1 = text.indexOf(substring);
        int index2 = text.indexOf(substring) + length;
        int index3 = (index1 + index2)/2;
        int index4 = (index1 + index3)/2;
        int index5 = (index3 + index2)/2;
        return string.contains(text.substring(index1, index2)) || string.contains(text.substring(index1, index4)) ||
                string.contains(text.substring(index1, index3)) || string.contains(text.substring(index1, index5)) ||
                string.contains(substring) || string.contains(text.substring(index4, index3)) ||
                string.contains(text.substring(index4, index5)) || string.contains(text.substring(index4, index2)) ||
                string.contains(text.substring(index3, index5)) || string.contains(text.substring(index3, index2)) ||
                string.contains(text.substring(index5, index2));
    }
}






































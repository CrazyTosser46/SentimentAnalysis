package SentimentAnalysis;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Robert on 09.01.2017.
 */
public class Classifier {
    private ArrayList<String> arrayPrepos = new ArrayList<>();
    private String[] userOpinionWords;
    private static final Logger log = LogManager.getLogger("Connection");
    private String nameGenre = null;
    public Classifier() {
        arrayPrepos.add("а");
        arrayPrepos.add("о");
        arrayPrepos.add("и");
        arrayPrepos.add("но");
        arrayPrepos.add("на");
        arrayPrepos.add("под");
        arrayPrepos.add("за");
        arrayPrepos.add("к");
        arrayPrepos.add("из");
        arrayPrepos.add("по");
        arrayPrepos.add("об");
        arrayPrepos.add("от");
        arrayPrepos.add("в");
        arrayPrepos.add("у");
        arrayPrepos.add("с");
        arrayPrepos.add("над");
        arrayPrepos.add("около");
        arrayPrepos.add("при");
        arrayPrepos.add("перед");
    }

    /**
     * Метод для определения тональности текста.
     * Если отзыв положительный число будет > 0, если отзыв отрицатаельный то число будет < 0
     */
    public double getOpinion(String userOpinion) {
        String[] userWords = null;
        HashMap<String, Integer> listCountWordInUserOpinionUnigramm = new HashMap<>();
        HashMap<String, Integer> listCountWordInUserOpinionBigramm = new HashMap<>();
        userOpinion = userOpinion.toLowerCase();
        userOpinion = userOpinion.replaceAll("[^а-яА-Я]", " ");
        userOpinion = userOpinion.replaceAll(" +", " ");

        userWords = userOpinion.split("\\s+");
        userOpinion = "";

        for (int i = 0; i < userWords.length; i++) {
            if (comparePreposInArray(userWords[i])) {
                if (i == 0) {
                    userOpinion = userOpinion + userWords[i];
                } else {
                    userOpinion = userOpinion + " " + userWords[i];
                }
            }
        }
        userWords = null;
        userWords = userOpinion.split("\\s+");
        this.userOpinionWords = userWords;
        for (int indexWords = 0; indexWords < userOpinionWords.length; indexWords++) {
            String unigramm = userOpinionWords[indexWords];
            if (!listCountWordInUserOpinionUnigramm.containsValue(unigramm)) {
                listCountWordInUserOpinionUnigramm.put(unigramm, 1);
            } else {
                listCountWordInUserOpinionUnigramm.put(unigramm, listCountWordInUserOpinionUnigramm.get(unigramm) + 1);
            }
            if (userOpinionWords.length >= 2) {
                if (indexWords < userOpinionWords.length - 1 || indexWords == 0) {
                    String bigramm = userOpinionWords[indexWords] + " " + userOpinionWords[indexWords + 1];
                    if (!listCountWordInUserOpinionBigramm.containsValue(bigramm)) {
                        listCountWordInUserOpinionBigramm.put(bigramm, 1);
                    } else {
                        listCountWordInUserOpinionBigramm.put(bigramm, listCountWordInUserOpinionBigramm.get(bigramm));
                    }
                }
            }
        }
        ConnectionBase con = ConnectionBase.getInstance();
        HashMap<String, Integer> listPositiv = con.getHashMap(nameGenre, 1);
        HashMap<String, Integer> listNegativ = con.getHashMap(nameGenre, 0);
        int sizePositiv = con.getGenreSize(nameGenre, 1);
        int sizeNegativ = con.getGenreSize(nameGenre, 0);

        double weightAll = 0;
        HashSet<String> listUseWords = new HashSet<>();
        for (int indexWords = 0; indexWords < userOpinionWords.length; indexWords++) {
            String wordUnigramm = userOpinionWords[indexWords];
            if (!listUseWords.contains(wordUnigramm)) {
                double weightWord;
                int countInPositiv = 0;
                int countInNegativ = 0;
                if (listPositiv.containsKey(wordUnigramm)) {
                    countInPositiv = listPositiv.get(wordUnigramm);
                }
                if (listNegativ.containsKey(wordUnigramm)) {
                    countInNegativ = listNegativ.get(wordUnigramm);
                }
                if (countInPositiv == 0) {
                    countInPositiv++;
                    countInNegativ++;
                }
                if (countInNegativ == 0) {
                    countInNegativ++;
                    countInPositiv++;
                }
                double number1 = (double) countInPositiv * sizeNegativ;
                double number2 = (double) countInNegativ * sizePositiv;
                weightWord = (double) listCountWordInUserOpinionUnigramm.get(wordUnigramm) * Math.log(number1 / number2);
                weightAll = weightAll + weightWord;
                listUseWords.add(wordUnigramm);
            }
            if (userOpinionWords.length >= 2) {
                if (indexWords < userOpinionWords.length - 1 || indexWords == 0) {
                    String wordBigramm = userOpinionWords[indexWords] + " " + userOpinionWords[indexWords + 1];
                    if (!listUseWords.contains(wordBigramm)) {
                        double weightWord;
                        int countInPositiv = 0;
                        int countInNegativ = 0;
                        if (listPositiv.containsKey(wordBigramm)) {
                            countInPositiv = listPositiv.get(wordBigramm);
                        }
                        if (listNegativ.containsKey(wordBigramm)) {
                            countInNegativ = listNegativ.get(wordBigramm);
                        }
                        if (countInPositiv == 0) {
                            countInPositiv++;
                            countInNegativ++;
                        }
                        if (countInNegativ == 0) {
                            countInNegativ++;
                            countInPositiv++;
                        }
                        double number1 = (double) countInPositiv * sizePositiv;
                        double number2 = (double) countInNegativ * sizeNegativ;
                        weightWord = (double) listCountWordInUserOpinionBigramm.get(wordBigramm) * Math.log(number1 / number2);
                        weightAll = weightAll + weightWord;
                        listUseWords.add(wordBigramm);
                    }
                }
            }
        }
        System.out.println(weightAll);
        return weightAll;
    }

    /**
     * Метод сравнивает полученное слово с массивом предлогом и союзов.
     * Если полученное слово совпадает с любым элементом массива, то метод вернёт false
     */
    public boolean comparePreposInArray(String value) {

        boolean control = true;

        for (int indexPrepos = 0; indexPrepos < arrayPrepos.size(); indexPrepos++) {
            if (arrayPrepos.get(indexPrepos).compareToIgnoreCase(value) == 0) {
                control = false;
                break;
            }
        }
        return control;
    }

    public void setTrainingSample(int indexName){
        ConnectionBase con = ConnectionBase.getInstance();
        ArrayList<String> listNameGenre = con.getListNameGenreDBWithOutMorf();
        nameGenre = listNameGenre.get(indexName);
    }

    public ArrayList<String> getListNameTrainingSample(){
        ConnectionBase con = ConnectionBase.getInstance();
        return con.getListNameGenreDBWithOutMorf();
    }

    public void downloadPositiv(File file, String name) {
        try {
            ConnectionBase con = ConnectionBase.getInstance();
            HashMap<String, Integer> listWordWithCount = new HashMap<>();
            File[] listPositivFile = file.listFiles();
            con.writeGenreDB(name, 1, listPositivFile.length);
            int id = con.getId(name, 1);
            for (int indexFile = 0; indexFile < listPositivFile.length; indexFile++) {

                HashSet<String> listWordUnigrammInFile = new HashSet<>();
                HashSet<String> listWordBigrammInFile = new HashSet<>();
                StringBuffer sb = new StringBuffer();
                FileInputStream fileInputStream = new FileInputStream(listPositivFile[indexFile]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, "utf-8"));
                String temp;
                while ((temp = reader.readLine()) != null) {
                    sb.append(temp + " ");
                }
                String text = sb.toString();
                text = text.toLowerCase();
                text = text.replaceAll("[^а-яА-Я]", " ");
                text = text.replaceAll(" +", " ");

                String[] words = text.split("\\s+");
                text = "";

                for (int i = 0; i < words.length; i++) {
                    if (comparePreposInArray(words[i])) {
                        if (i == 0) {
                            text = text + words[i];
                        } else {
                            text = text + " " + words[i];
                        }
                    }
                }
                words = null;
                words = text.split("\\s+");
                for (int indexWord = 0; indexWord < words.length; indexWord++) {
                    String word = words[indexWord].toLowerCase();
                    if (!listWordUnigrammInFile.contains(word)) {
                        if (listWordWithCount.containsKey(word)) {
                            listWordWithCount.put(word, listWordWithCount.get(word) + 1);
                        } else {
                            listWordWithCount.put(word, 1);
                        }
                        listWordUnigrammInFile.add(word);
                    }
                    if (indexWord < words.length - 1) {
                        StringBuffer stringBuffer = new StringBuffer();
                        stringBuffer.append(words[indexWord])
                                .append(" ")
                                .append(words[indexWord + 1]);

                        String bigramm = stringBuffer.toString().toLowerCase();
                        if (!listWordBigrammInFile.contains(bigramm)) {
                            if (listWordWithCount.containsKey(bigramm)) {
                                listWordWithCount.put(bigramm, listWordWithCount.get(bigramm) + 1);
                            } else {
                                listWordWithCount.put(bigramm, 1);
                            }
                            listWordBigrammInFile.add(bigramm);
                        }
                    }
                }
                System.out.println("Загруженно " + (indexFile+1) + " из " + listPositivFile.length);
            }
            con.writeWordDB(listWordWithCount,id);

        } catch (FileNotFoundException ex) {
            log.log(Level.ERROR,"Ошибка при поиске файла", ex);
        } catch (UnsupportedEncodingException ex) {
            log.log(Level.ERROR, "Не поддерживаемая кодировка файла", ex);
        } catch (IOException ex) {
            log.log(Level.ERROR, "Ошибка при чтении из файла", ex);
        }
    }

    public void downloadNegativ(File file, String name){
        try {
            ConnectionBase con = ConnectionBase.getInstance();
            HashMap<String, Integer> listWordWithCount = new HashMap<>();
            File[] listNegativFile = file.listFiles();
            con.writeGenreDB(name, 0, listNegativFile.length);
            int id = con.getId(name, 0);
            int count = 0;
            for (int indexFile = 0; indexFile < listNegativFile.length; indexFile++) {

                HashSet<String> listWordUnigrammInFile = new HashSet<>();
                HashSet<String> listWordBigrammInFile = new HashSet<>();
                StringBuffer sb = new StringBuffer();
                FileInputStream fileInputStream = new FileInputStream(listNegativFile[indexFile]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, "utf-8"));
                String temp;
                while ((temp = reader.readLine()) != null) {
                    sb.append(temp + " ");
                }
                String text = sb.toString();
                text = text.toLowerCase();
                text = text.replaceAll("[^а-я^А-Я^0-9]", " ");
                text = text.replaceAll(" +", " ");

                String[] words = text.split("\\s+");
                text = "";

                for (int i = 0; i < words.length; i++) {
                    if (comparePreposInArray(words[i])) {
                        if (i == 0) {
                            text = text + words[i];
                        } else {
                            text = text + " " + words[i];
                        }
                    }
                }
                words = null;
                words = text.split(" +");
                for (int indexWord = 0; indexWord < words.length; indexWord++) {
                    String word = words[indexWord].toLowerCase();
                    if (!listWordUnigrammInFile.contains(word)) {
                        if (listWordWithCount.containsKey(word)) {
                            listWordWithCount.put(word, listWordWithCount.get(word) + 1);
                        } else {
                            listWordWithCount.put(word, 1);
                        }
                        listWordUnigrammInFile.add(word);
                    }
                    if (indexWord < words.length - 1) {
                        StringBuffer stringBuffer = new StringBuffer();
                        stringBuffer.append(words[indexWord])
                                .append(" ")
                                .append(words[indexWord + 1]);
                        String bigramm = stringBuffer.toString().toLowerCase();
                        if (!listWordBigrammInFile.contains(bigramm)) {
                            if (listWordWithCount.containsKey(bigramm)) {
                                listWordWithCount.put(bigramm, listWordWithCount.get(bigramm) + 1);
                            } else {
                                listWordWithCount.put(bigramm, 1);
                            }
                            listWordBigrammInFile.add(bigramm);
                        }
                    }
                }
                System.out.println("Загруженно " + (indexFile+1) + " из " + listNegativFile.length);
            }
            con.writeWordDB(listWordWithCount, id);
        } catch (FileNotFoundException ex) {
            log.log(Level.ERROR,"Ошибка при поиске файла", ex);
        } catch (UnsupportedEncodingException ex) {
            log.log(Level.ERROR, "Не поддерживаемая кодировка файла", ex);
        } catch (IOException ex) {
            log.log(Level.ERROR, "Ошибка при чтении из файла", ex);
        }
    }
}
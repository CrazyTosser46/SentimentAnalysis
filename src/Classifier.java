package sample;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Robert on 09.01.2017.
 */
public class Classifier {
    private ArrayList<String> arrayPrepos = new ArrayList<>();
    private String[] userOpinionWords;

    public Classifier(){
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


    public double getOpinion(String userOpinion, String nameGenre) {
        int useMorf = 0;
        char[] subNameGenre = nameGenre.toCharArray();
        if(subNameGenre[nameGenre.indexOf(':') + 1] == 'Д'){
            useMorf = 1;
        }
        nameGenre = nameGenre.substring(0,nameGenre.indexOf("("));
        String[] userWords = null;
        HashMap<String,Integer> listCountWordInUserOpinionUnigramm = new HashMap<>();
        HashMap<String,Integer> listCountWordInUserOpinionBigramm = new HashMap<>();
        userOpinion = userOpinion.toLowerCase();
        userOpinion = userOpinion.replaceAll("[^а-яА-Я]"," ");
        userOpinion = userOpinion.replaceAll(" +"," ");

        userWords = userOpinion.split("\\s+");
        userOpinion = "";

        for(int i = 0; i < userWords.length; i++){
            if(comparePreposInArray(userWords[i])){
                if(i == 0){
                    userOpinion = userOpinion + userWords[i];
                }else{
                    userOpinion = userOpinion + " " + userWords[i];
                }
            }
        }
        userWords = null;
        userWords = userOpinion.split("\\s+");
        this.userOpinionWords = userWords;
        for(int indexWords = 0 ; indexWords < userOpinionWords.length; indexWords++){
            String unigramm = userOpinionWords[indexWords];
            if(!listCountWordInUserOpinionUnigramm.containsValue(unigramm)){
                listCountWordInUserOpinionUnigramm.put(unigramm,1);
            }else{
                listCountWordInUserOpinionUnigramm.put(unigramm,listCountWordInUserOpinionUnigramm.get(unigramm) + 1);
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
        HashMap<String,Integer> listPositiv = con.getHashMap(nameGenre,1,useMorf);
        HashMap<String,Integer> listNegativ = con.getHashMap(nameGenre,0,useMorf);
        int sizePositiv = con.getGenreSize(nameGenre,1);
        int sizeNegativ = con.getGenreSize(nameGenre,0);

        double weightAll = 0;
        HashSet<String> listUseWords = new HashSet<>();
        for(int indexWords = 0 ; indexWords < userOpinionWords.length; indexWords++) {
            String wordUnigramm = userOpinionWords[indexWords];
            if (!listUseWords.contains(wordUnigramm)) {
                double weightWord;
                int countInPositiv = 0;
                int countInNegativ = 0;
                if(listPositiv.containsKey(wordUnigramm)){
                    countInPositiv = listPositiv.get(wordUnigramm);
                }
                if(listNegativ.containsKey(wordUnigramm)){
                    countInNegativ = listNegativ.get(wordUnigramm);
                }
                if(countInPositiv == 0){
                    countInPositiv++;
                    countInNegativ++;
                }
                if(countInNegativ ==0){
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
                        if(listPositiv.containsKey(wordBigramm)){
                            countInPositiv = listPositiv.get(wordBigramm);
                        }
                        if(listNegativ.containsKey(wordBigramm)){
                            countInNegativ = listNegativ.get(wordBigramm);
                        }
                        if(countInPositiv == 0){
                            countInPositiv++;
                            countInNegativ++;
                        }
                        if(countInNegativ ==0){
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
    //Метод сравнивает полученное слово и массивом предлогом и союзов.
    //Если полученное слово совпадает с любым элементом массива то метод вернёт false
    private boolean comparePreposInArray(String value){

        boolean control = true;

        for(int indexPrepos = 0; indexPrepos < arrayPrepos.size(); indexPrepos++ ){
            if(arrayPrepos.get(indexPrepos).compareToIgnoreCase(value) == 0){
                control = false;
                break;
            }
        }
        return control;
    }
}

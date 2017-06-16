package SentimentAnalysis;

/**
 * Created by Robert on 16.06.2017.
 */
public class Main {
    public static void main (String[] args){
        Classifier classifier = new Classifier();
        String opinion = "Фильм нравиться";

        classifier.setTrainingSample(0);

        double result = classifier.getOpinion(opinion);
        System.out.println(result);
    }
}

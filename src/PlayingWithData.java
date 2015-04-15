import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class PlayingWithData {
  public static HashSet<String> ingredients;
  public static HashMap<String, Ingredient> ingredientInfo;
  
  public static void main(String[] args) throws IOException {
    ingredients = new HashSet<String>();
    ingredientInfo = new HashMap<>();
    load();
    
    BufferedWriter bw = new BufferedWriter(new FileWriter(new File("ingredientProbs.txt")));
    for (String string : ingredients) {
      System.out.print(string + " : " + ingredientInfo.get(string).totalCount);
      bw.write(string);
      for (int i = 0; i < 7; i++) {
        System.out.print(" : " + ingredientInfo.get(string).getProbabilityForCuisine(i + 1)); 
        bw.write(";" + ingredientInfo.get(string).getProbabilityForCuisine(i+1));
      }
      System.out.println();
      bw.write("\n");
    }
    
    System.out.println("Num of ingredients: " + ingredients.size());
  }
  private static void load() throws FileNotFoundException, IOException {
    BufferedReader br = new BufferedReader(new FileReader(new File("training-data.txt")));
    String line;

    while ((line = br.readLine()) != null) {
        if (line.length() == 2) { 
            System.err.println("Found bad data");
            continue;
        }
       int cuisine = Character.getNumericValue(line.charAt(0));
       List<String> ingr = Arrays.asList(line.substring(1).trim().split(" "));
       ingredients.addAll(ingr);
       
       for (String i : ingr) {
        if(ingredientInfo.containsKey(i)){
          ingredientInfo.get(i).increment(cuisine);
        } else {
          ingredientInfo.put(i, new Ingredient(i, cuisine));
        }
      }
    }
    br.close();
  }

}

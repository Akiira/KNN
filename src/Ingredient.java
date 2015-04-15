
public class Ingredient {
  public String name;
  public int totalCount;
  public int[] cuisineCounts;

  public Ingredient(String name, int cuisine) {
    this.name = name;
    totalCount = 0;
    cuisineCounts = new int[7];
    for (int i = 0; i < cuisineCounts.length; i++) {
      cuisineCounts[i] = 0;
    }
    
    cuisineCounts[cuisine - 1]++;
    totalCount++;
  }
  
  public void increment(int cuisine) {
    cuisineCounts[cuisine - 1]++;
    totalCount++;
  }
  
  public double getProbabilityForCuisine(int cuisine) {
    return (double) cuisineCounts[cuisine - 1] / (double) totalCount;
  }
  
  public double getMaxDifferenceInProbabilities() {
    double min = 99999, max = -999999;

    for (int i = 0; i < 7; i++) {
        double current = getProbabilityForCuisine(i + 1);
        min = min < current ? min : current;
        max = max > current ? max : current;
    }
    
    return max - min;
  }
  
  
}

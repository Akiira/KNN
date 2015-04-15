import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

enum DistanceFunction { BAG, SET, WEIGHTED_BAG, WEIGHTED_JACCARD }
enum VoteFunction { EQUAL, DISTANCESQ, SCALEDDIST }

// SET, DISTANCESQ = 89.7% (k = 10, o = 0, s = 100)
// BAG, DISTANCESQ = 88.8% (k = 10, o = 0, s = 100)
// SET, EQUAL = 88.?% (k = 10, o = 0, s = 100)
// BAG, EQUAL = 88.5% (k = 10, o = 0, s = 100)
// SET, SCALEDDIST = 89.3% (k = 10, o = 0, s = 100)
// BAG, SCALEDDIST = 89.3% (k = 10, o = 0, s = 100)
// Using leave one out

public class Main {
  public static HashSet<String> ingredients;
  public static HashMap<String, Ingredient> ingredientInfo;
  
	public static HashSet<String> tabooList;
	static ArrayList<Recipe> trainingData;
	public static int k = 10;
	public static int o = 3; // o <= 1 will not add a word to taboo list
	public static int s = 100; // s >= 100 will not add a word to taboo list
	public static int minCutOff = 1;
	public static double minDist;
	public static String trainingFile = "training-data.txt";
	public static PriorityQueue<Recipe> neighbors;
	public static DistanceFunction df = DistanceFunction.WEIGHTED_JACCARD;
	public static VoteFunction vf = VoteFunction.DISTANCESQ; 
	
	public static void main(String[] args) throws IOException {
		tabooList = new HashSet<String>();
		ingredientInfo = new HashMap<>();
		ingredients = new HashSet<String>();
		buildTabooList();
		
		readTrainingSet();	
		System.out.println("Taboo List: " + tabooList.size() + ", IngrientInfo: " + ingredientInfo.size());
		crossValidate();
		//runOnTestData();
	}
	
	public static void buildTabooList() throws IOException {
        tabooList = new HashSet<String>();
        
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
        
        for (String ingr : ingredients) {
          if(ingredientInfo.get(ingr).totalCount < minCutOff) {
            tabooList.add(ingr);
          }
        }
	}
	   public static void readTrainingSet() {
	        trainingData = new ArrayList<Recipe>();

	        try {
	            BufferedReader br = new BufferedReader(new FileReader(new File(trainingFile)));
	            String line;
	            int recipeNum = 0;
	            while ((line = br.readLine()) != null) {
	                if (line.length() == 2) { 
	                    System.err.println("Found bad data");
	                    continue;
	                }
	                trainingData.add(new Recipe(true, line, recipeNum));
	                recipeNum++;
	            }
	            br.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
//	    public static void buildTabooList() {
//	        try {
//	            tabooList = new HashSet<String>();
//	            String line;
//	            String splitLine[];
//	            String splitProportions[];
//	            BufferedReader br = new BufferedReader(new FileReader(new File("all.txt")));
//	            while ((line = br.readLine()) != null) {
//	                splitLine = line.split(":");
//	                if (splitLine.length != 2) {
//	                    System.out.println("ERROR? " + line);
//	                }
//	                splitProportions = splitLine[1].split(" ");
//	                double min, max;
//	                min = max = Double.parseDouble(splitProportions[0]);
//	                for (int i = 1; i < splitProportions.length; i++) {
//	                    double current = Double.parseDouble(splitProportions[i]);
//	                    min = min < current ? min : current;
//	                    max = max > current ? max : current;
//	                }
//	                if (max - min > s) {
//	                    tabooList.add(splitLine[0]);
//	                } else {
//	                  double weight = ((max - min) / max);
//	                  ingredientWeight.put(splitLine[0], weight);
//	                }
//	            }
//	            br.close();
//	            
//	            br = new BufferedReader(new FileReader(new File("one.txt")));
//	            while ((line = br.readLine()) != null) {
//	                splitLine = line.split(":");
//	                if (splitLine.length != 2) {
//	                    System.out.println("ERROR? " + line);
//	                }
//	                if (Integer.parseInt(splitLine[1]) < o ) {
//	                    tabooList.add(splitLine[0]);
//	                }
//	            }
//	            br.close();
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	        }
//	    }
//	public static void readTrainingSet() {
//		trainingData = new ArrayList<Recipe>();
//
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(new File(trainingFile)));
//			String line;
//			int recipeNum = 0;
//			while ((line = br.readLine()) != null) {
//				if (line.length() == 2) { 
//					System.err.println("Found bad data");
//					continue;
//				}
//				trainingData.add(new Recipe(true, line, recipeNum));
//				recipeNum++;
//			}
//			br.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public static int calculateVotes() {
		int cuisine = 1;
		double[] votes = new double[8];
		double maxVotes;
		double maxDist;
		
		switch (vf){
			case EQUAL:
				while (neighbors.peek() != null) {
					votes[neighbors.poll().cuisine]++;
				}
				
				maxVotes = votes[1];
				for (int i = 2; i < 8; i++) {
					if (votes[i] > maxVotes) {
						maxVotes = votes[i];
						cuisine = i;
					}
				}
				break;
			case DISTANCESQ:
				while (neighbors.peek() != null) {
					Recipe current = neighbors.poll();
					votes[current.cuisine] += (1/Math.pow(current.distance,2));
				}
				
				maxVotes = votes[1];
				for (int i = 2; i < 8; i++) {
					if (votes[i] > maxVotes) {
						maxVotes = votes[i];
						cuisine = i;
					}
				}
				break;
			case SCALEDDIST:
				maxDist = neighbors.peek().distance;
				
				while (neighbors.peek() != null) {
					Recipe current = neighbors.poll();
					votes[current.cuisine] += (maxDist - current.distance)/(maxDist - minDist);
				}
				
				maxVotes = votes[1];
				for (int i = 2; i < 8; i++) {
					if (votes[i] > maxVotes) {
						maxVotes = votes[i];
						cuisine = i;
					}
				}
				break;
		}
		
		return cuisine;
	}
	
	public static void crossValidate() {
		int total = 0;
		int correct = 0;
		for (int i = 0 ; i < trainingData.size(); i++) {
			neighbors = new PriorityQueue<Recipe>(k);
			minDist = 1;
			for (int j = 0; j < trainingData.size(); j++) {
				if (i == j) {
					continue;
				}
				
				double distance = trainingData.get(j).getDistance(trainingData.get(i));
				
				minDist = minDist < distance ? minDist : distance;
				
				if (neighbors.size() < k) {
					neighbors.add(trainingData.get(j));
				} else {
					if (neighbors.peek().distance > distance) {
						neighbors.poll();
						neighbors.add(trainingData.get(j));
					}
				}	
			}
			
			// vote
			int prediction = calculateVotes();
			
			// check accuracy
			if (prediction == (trainingData.get(i).cuisine)) {
				correct++;
			}
			total++;
			
			if (total % 500 == 0) {
				System.err.println(total + " " + (double)correct / total);
			}
		}
		
		System.out.println((double)correct / total);
	}
	
	public static void runOnTestData() {
		Scanner sc = new Scanner(System.in);
		String line;
		while ((line = sc.nextLine()) != null) {
			Recipe current = new Recipe(false, line, 0);
			neighbors = new PriorityQueue<Recipe>(k);
			minDist = 1;
			for (int j = 0; j < trainingData.size(); j++) {
				
				double distance = trainingData.get(j).getDistance(current);
				
				minDist = minDist < distance ? minDist : distance;
				
				if (neighbors.size() < k) {
					neighbors.add(trainingData.get(j));
				} else {
					if (neighbors.peek().distance > distance) {
						neighbors.poll();
						neighbors.add(trainingData.get(j));
					}
				}	
			}
			
			// vote
			System.out.println(calculateVotes());
		}
		sc.close();
	}
}

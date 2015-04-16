import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

enum DistanceFunction { WEIGHTED_BAG, WEIGHTED_JACCARD }
enum VoteFunction { EQUAL, DISTANCESQ, SCALEDDIST }

// WEIGHTED_JACCARD, DISTANCESQ = 91.075% (k = 10, cutoff = 1)
//WEIGHTED_JACCARD, DISTANCESQ = 91.47% (k = 10, cutoff = 2)
// Using leave one out

public class Main {
    public static HashSet<String> ingredients;
    public static HashMap<String, Ingredient> ingredientInfo;
    public static BufferedWriter output;
  
	public static HashSet<String> tabooList;
	static ArrayList<Recipe> trainingData;
	
	//Parameters
	public static int k = 10;
	public static int minCutOff = 2;
	public static double minDist;
	public static DistanceFunction df = DistanceFunction.WEIGHTED_JACCARD;
    public static VoteFunction vf = VoteFunction.DISTANCESQ; 
    
	public static String trainingFile = "training-data.txt";
	public static PriorityQueue<Recipe> neighbors;
	
	public static int[][] confusionMatrix;
	
	public static void main(String[] args) throws IOException {	
	  //output = new BufferedWriter(new FileWriter(new File("parameterData3.txt")));		
	  //runTests();
	  //RunFinalClassifier();
	  PrintConfusionMatrixInfo();
	}
	
	public static void RunFinalClassifier() throws IOException {
	  //Set everything to best parameters found during testing:
	  k = 10;
	  minCutOff = 2;
	  df = DistanceFunction.WEIGHTED_JACCARD;
	  vf = VoteFunction.DISTANCESQ;
	  
	  //Build taboo list based on min cutoff and then get all the training data
	  buildTabooList();
      readTrainingSet(); 
      
      runOnTestData();  
	}
	
	public static void searchForGoodParameters() throws IOException {
	  for(int kVar = 12; kVar < 15; kVar++){
	    k = kVar;
	    for(int cutVar = 0; cutVar < 6; cutVar++){
	      minCutOff = cutVar;
	      buildTabooList();
	      readTrainingSet();   
	      for (DistanceFunction dfVar : DistanceFunction.values()) {
	        df = dfVar;
            for (VoteFunction vfVar : VoteFunction.values()) {
              vf = vfVar;
              
              
              output.write("K: " + k + ", MinCutOff: " + minCutOff + ", df: " + df + ", vf: " + vf + " \n");
              output.flush();
              crossValidate();
            }
          }
	    }
	    
	  }
	}
	
	public static void PrintConfusionMatrixInfo() throws IOException {
	  BufferedWriter confFile = new BufferedWriter(new FileWriter(new File("C:\\Users\\Kitty\\Documents\\My Dropbox\\Comp 597 Data Mining\\ConfusionMatrix.txt")));
	  
	  //Set everything to best parameters found during testing:
      k = 10;
      minCutOff = 1;
      df = DistanceFunction.WEIGHTED_JACCARD;
      vf = VoteFunction.DISTANCESQ;
      
      //Build taboo list based on min cutoff and then get all the training data
      buildTabooList();
      readTrainingSet(); 
      
      crossValidate();  
      
      confFile.write("\\begin{tabular}{|c|c|c|c|c|c|c|c|c|}\n");
      confFile.write("\\hline  & French & Italian & Indian & Chinese & Thai & Greek & Mexican \\\\\n");
      
      for (int i = 1; i < confusionMatrix.length; i++) {
        confFile.write("\\hline ");
        for (int j = 1; j < confusionMatrix[i].length; j++) {
          confFile.write("& " + confusionMatrix[i][j] + " ");
        }
        confFile.write("\\\\\n");
      }
      
      confFile.write("\\hline \n\\end{tabular}");
      confFile.close();
	}
	
	 
	
	public static void buildTabooList() throws IOException {
        tabooList = new HashSet<String>();
        ingredientInfo = new HashMap<>();
        ingredients = new HashSet<String>();
        
        BufferedReader br = new BufferedReader(new FileReader(new File(trainingFile)));
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
				break;
			case DISTANCESQ:
				while (neighbors.peek() != null) {
					Recipe current = neighbors.poll();
					votes[current.cuisine] += (1/Math.pow(current.distance,2));
				}
				break;
			case SCALEDDIST:
				maxDist = neighbors.peek().distance;
				
				while (neighbors.peek() != null) {
					Recipe current = neighbors.poll();
					votes[current.cuisine] += (maxDist - current.distance)/(maxDist - minDist);
				}
				break;
		}
		
        maxVotes = votes[1];
        for (int i = 2; i < 8; i++) {
            if (votes[i] > maxVotes) {
                maxVotes = votes[i];
                cuisine = i;
            }
        }
		
		return cuisine;
	}
	
	public static void crossValidate() throws IOException {
		int total = 0;
		int correct = 0;
		confusionMatrix = new int[8][8];
		
		//Shuffle the data to avoid any possible bias in the order of the data
		Collections.shuffle(trainingData);
		
		for (int i = 0 ; i < trainingData.size(); i++) {
			neighbors = new PriorityQueue<Recipe>(k);
			minDist = 9999;
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
			} else {
			  System.out.println("Predicted : " + prediction + ", for: " + trainingData.get(i));
			}
			total++;
			
			confusionMatrix[trainingData.get(i).cuisine][prediction]++;
			
			if (total % 500 == 0) {
				System.err.println(total + " " + (double)correct / total);
			}
		}
		
		System.out.println((double)correct / total);
		output.write("Accuracy: " + (double)correct / total + "\n\n");
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

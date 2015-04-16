import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;


public class Recipe implements Comparable, Comparator {
	public int cuisine;
	public HashSet<String> ingredientSet;
	public HashMap<String, Integer> ingredientBag;
	public int recipeNum;
	public double distance;
	
	public Recipe(boolean training, String line, int recipeNum) {
		this.recipeNum = recipeNum;
		ingredientSet = new HashSet<String>();
		ingredientBag = new HashMap<String, Integer>();
		String[] lineArray = line.split(" ");
		int startIndex;
		if (training) {
			cuisine = Integer.parseInt(lineArray[0]);
			startIndex = 1;
		}
		else {
			cuisine = -1; 
			startIndex = 0;
		}
		
		// build ingredient set
		for (int i = startIndex; i < lineArray.length; i++) {
			if (!(Main.tabooList.contains(lineArray[i]))) {
				ingredientSet.add(lineArray[i]);
			}
		}
					
		// build ingredient bag
		for (int i = startIndex; i < lineArray.length; i++) {
			if (!(Main.tabooList.contains(lineArray[i]))) {
				if (!(ingredientBag.containsKey(lineArray[i]))) {
					ingredientBag.put(lineArray[i], 0);
				}
						
				ingredientBag.put(lineArray[i], ingredientBag.get(lineArray[i]) + 1);
			}
		}
			
		if (ingredientSet.size() == 0) {
			System.out.println("Empty recipe: " + line + " (" + line.length() + ")");
		}
	}
	
	public double getDistance(Recipe other) {
		double unionSize = 0;
		double intersectSize = 0;
		switch (Main.df) {
//			case BAG:
//				for (String word : this.ingredientBag.keySet()) {
//					if (other.ingredientBag.containsKey(word)) {
//						intersectSize += Math.min(this.ingredientBag.get(word), other.ingredientBag.get(word));
//					}
//				}
//				
//				for (String word : this.ingredientBag.keySet()) {
//					unionSize += this.ingredientBag.get(word);
//				}
//				
//				for (String word : other.ingredientBag.keySet()) {
//					unionSize += other.ingredientBag.get(word);
//				}
//				
//				distance = 1 - intersectSize / unionSize;
//				break;
//			case SET:
//			{
//				HashSet<String> union = new HashSet<String>();
//				union.addAll(this.ingredientSet);
//				union.addAll(other.ingredientSet);
//				unionSize = union.size();
//				
//				for (String ingr : this.ingredientSet) {
//					if (other.ingredientSet.contains(ingr)) {
//						intersectSize++;
//					}
//				}
//				
//				distance =  1 - intersectSize / unionSize;
//		    }
//				break;
			case WEIGHTED_JACCARD:
			{
  			    HashSet<String> union = new HashSet<String>();
                union.addAll(this.ingredientSet);
                union.addAll(other.ingredientSet);
                
                for (String ingr : union) {
                  unionSize += (1 * Main.ingredientInfo.get(ingr).getMaxDifferenceInProbabilities());
                }
                
                for (String ingr : this.ingredientSet) {
                    if (other.ingredientSet.contains(ingr)) {
                        intersectSize += (1 * Main.ingredientInfo.get(ingr).getMaxDifferenceInProbabilities());
                    }
                }
                
                distance =  1 - intersectSize / unionSize;
			  }
			  break;
			case WEIGHTED_BAG:
			{
			  for (String word : this.ingredientBag.keySet()) {
                if (other.ingredientBag.containsKey(word)) {
                    intersectSize += Main.ingredientInfo.get(word).getMaxDifferenceInProbabilities() * Math.min(this.ingredientBag.get(word), other.ingredientBag.get(word));
                }
              }
              
              for (String word : this.ingredientBag.keySet()) {
                  unionSize += Main.ingredientInfo.get(word).getMaxDifferenceInProbabilities() * this.ingredientBag.get(word);
              }
              
              for (String word : other.ingredientBag.keySet()) {
                  unionSize += Main.ingredientInfo.get(word).getMaxDifferenceInProbabilities() * other.ingredientBag.get(word);
              }
              
              distance = 1 - intersectSize / unionSize;
			}
		}
		
		return distance;
	}
	
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		Recipe other = (Recipe)obj;
		boolean equal = true;
		equal &= cuisine != -1 && other.cuisine != -1; // They are both from the training set
		if (equal) equal &= cuisine == other.cuisine;	
		if (equal) equal &= ingredientSet.size() == other.ingredientSet.size();
		if (equal) ingredientSet.equals(other.ingredientSet);
		
		return equal;
	}
	

	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + cuisine;
		hash = 31 * hash + ingredientSet.hashCode();
		hash = 31 * hash + recipeNum;
		return hash;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(cuisine + " ");
		for (String s : ingredientSet) {
			sb.append(s + " ");
		}
		return sb.toString();
	}

	@Override
	public int compare(Object o1, Object o2) {
		Recipe r1 = (Recipe)o1;
		Recipe r2 = (Recipe)o2;
		
		if (r1.distance > r2.distance) {
			return -1;
		} else if (r1.distance < r2.distance) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public int compareTo(Object o) {
		Recipe other = (Recipe)o;
		
		if (this.distance > other.distance) {
			return -1;
		} else if (this.distance < other.distance) {
			return 1;
		} else {
			return 0;
		}
	}
}

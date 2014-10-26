package myrecsys;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;

public class myrecsys {
	static HashMap<Integer, ArrayList<Rating>> testRatings;
	static HashMap<Integer, ArrayList<Rating>> ratingsByUser;
	static HashMap<Integer, ArrayList<Rating>> ratingsByMovie;
	
 	static double err = 0;
 	
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Format: java myrecsys training.data test.data algorithm-name");
			System.exit(1);
		}
		
		testRatings = new HashMap<Integer, ArrayList<Rating>>();
		ratingsByUser = new HashMap<Integer, ArrayList<Rating>>();
		ratingsByMovie = new HashMap<Integer, ArrayList<Rating>>();
		
		switch(args[2].toLowerCase()) {
			case "average":			
				parseRatingsFiles("movie", args[0], args[1]);
				average();				
				err = rmse();
				break;
			case "user-euclidean":
				parseRatingsFiles("user", args[0], args[1]);
				userEuclidean();
				err = rmse();
				break;
			case "user-pearson":
				parseRatingsFiles("user", args[0], args[1]);
				userPearson();
				err = rmse();
				break;
			case "item-cosine":
				parseRatingsFiles("movie", args[0], args[1]);
				itemCosine();
				err = rmse();
				break;
			case "item-adcosine":
				parseRatingsFiles("movie", args[0], args[1]);
				itemAdCosine();
				err = rmse();
				break;
			case "slope-one":
				parseRatingsFiles("user", args[0], args[1]);
				slopeOne();
				err = rmse();
				break;
			default:
				System.out.println("Invalid algorithm: \'%s\' does not exist.");
				System.exit(4);
		}
			
		System.out.printf("MYRESULTS Training \t= %s\n", args[0]);
		System.out.printf("MYRESULTS Testing \t= %s\n", args[1]);
		System.out.printf("MYRESULTS Algorithm \t= %s\n", args[2]);
		System.out.printf("MYRESULTS RMSE \t\t= %.6f\n", err);
	}
	
	static void average() {
		
	}
	
	static void userEuclidean() {
		
	}
	
	static void userPearson() {
		
	}
	
	static void itemCosine() {
		
	}
	
	static void itemAdCosine() {
		
	}

	static void slopeOne() {
		
	}
	
	static int parseIntCheck(String s) {		// parse the strings into integer values, and ensure that it won't crash if there's an issue.
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	
	static void parseRatingsFiles(String groupBy, String trainFile, String testFile) {
		BufferedReader br; 										// buffered reader for each file.
		String line, tmp[];										// line - each line of the files. tmp - each line split by space " ".
		ArrayList<Rating> cur;									// the current Rating arraylist being worked with.
		
		try {
			br = new BufferedReader(new FileReader(trainFile));	// training file buffered reader.
			while ( (line=br.readLine()) != null) {				// while there are still more lines.
				tmp = line.split("\t");							
				
				if (tmp.length!=4) {							// expecting lines in the format "user | movie | rating | timestamp"
					System.out.printf("File formatting error for training data file \'%s\'.\n", trainFile);
					System.exit(3);
				} 
				
				Rating r = new Rating(parseIntCheck(tmp[0]), parseIntCheck(tmp[1]), parseIntCheck(tmp[2]));	// create a new rating for this review
				if(!r.isValid()) {													// ensures the Rating was created correctly.
					System.out.println("There's been a rating formatting error.");
					System.exit(5);
				}
		
				if (!ratingsByUser.containsKey(r.getUser())) ratingsByUser.put(r.getUser(), new ArrayList<Rating>());
				cur = ratingsByUser.get(r.getUser());
				cur.add(r);
				ratingsByUser.put(r.getUser(), cur);
			
				if (!ratingsByMovie.containsKey(r.getMovie())) ratingsByMovie.put(r.getMovie(), new ArrayList<Rating>());
				cur = ratingsByMovie.get(r.getMovie());
				cur.add(r);
				ratingsByMovie.put(r.getMovie(), cur);
			} br.close();
		} catch (java.io.IOException e) {
			System.out.printf("Unable to open training data file \'%s\'.\n", trainFile);
			System.exit(2);
		} 
		
		try {															// repeat this whole process for the test data file.
			br = new BufferedReader(new FileReader(testFile));
			while ( (line=br.readLine())!= null) {
				tmp = line.split("\t");
				
				if (tmp.length!=4) {
					System.out.printf("File formatting error for test data file \'%s\'.\n", testFile);
					System.exit(4);
				} 
				
				Rating r = new Rating(parseIntCheck(tmp[0]), parseIntCheck(tmp[1]), parseIntCheck(tmp[2]));
				if(!r.isValid()) {
					System.out.println("There's been a rating formatting error.");
					System.exit(5);
				}
		
				if (groupBy.equals("user")) {
					if (!testRatings.containsKey(r.getUser())) testRatings.put(r.getUser(), new ArrayList<Rating>());
					cur = testRatings.get(r.getUser());
					cur.add(r);
					testRatings.put(r.getUser(), cur);
				}
				else if (groupBy.equals("movie")) {
					if (!testRatings.containsKey(r.getMovie())) testRatings.put(r.getMovie(), new ArrayList<Rating>());
					cur = testRatings.get(r.getMovie());
					cur.add(r);
					testRatings.put(r.getMovie(), cur);
				}
			} br.close();
		} catch (java.io.IOException e) {
			System.out.printf("Unable to open test data file \'%s\'.\n", testFile);
			System.exit(3);
		}
	}
	
	static double rmse() {											
		double sum = 0;						// sum - sum of differences between predicted ratings and actual ratings, squared.
		int count = 0;						// count - number of ratings in the test data.
		for (int i : testRatings.keySet()) {
			ArrayList<Rating> tmp = testRatings.get(i);
			
			for (Rating r : tmp) {
				if (r.getPredicted()!=-1) {
					count++;
					sum += Math.pow(r.getPredicted() - r.getRating(), 2);
				} 
			}
		} 
		
		return Math.sqrt(sum/count);
	}
	
	static class Rating {
		private int userID;
		private int movieID;
		private double rating;
		private double predicted;
		
		public Rating(int userID, int movieID, double rating) {
			this.userID = userID;
			this.movieID = movieID;
			this.rating = rating;
			this.predicted = -1;
			
			if (this.rating > 5 || this.rating < 1) this.rating=-1;
		}
		
		public Rating(Rating other) {
			userID = other.getUser();
			movieID = other.getMovie();
			rating = other.getRating();
		}
		
		public void setPredicted(double score) {
			this.predicted = score;
		}
		
		public double getPredicted() {
			return predicted;
		}
		
		public int getUser() {
			return userID;
		}
		
		public int getMovie() {
			return movieID;
		}
		
		public double getRating() {
			return rating;
		}
		
		public String toString() {
			return "User: " + userID + " Movie: " + movieID + " Rating: " + rating;
		}
		
		public boolean isValid() {
			return !(userID == -1 || movieID == -1 || rating == -1);
		}
	}
}
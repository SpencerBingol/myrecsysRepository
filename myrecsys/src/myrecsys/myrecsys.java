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
		int count;												// count - Number of ratings about a given movie
		double avg, total;										// total - Cumulative sum of rating values (1-5). avg - total/count 
		for (int movie : testRatings.keySet()) {				// for each movie in the test data
			count = 0;
			total = 0;
			avg = 0;
			if (ratingsByMovie.containsKey(movie)) {			// if reviews for this movie also exist in the training data
				for (Rating r : ratingsByMovie.get(movie)) {	// get all reviews for this movie							
					total += r.getRating();						// add movie rating to total
					count++;
				} avg = total / count;							// calculate average
							
				for (Rating r : testRatings.get(movie)) {		// for every test data review	
					r.setPredicted(avg);						// add a predicted value to compare to the actual.
				}
			}
		}
	}
	
	static void userEuclidean() {
		double sim=0, distTotal=0, weightTotal=0, simTotal=0;						// sim - similarity value. distTotal - total manhattan distance btwn two users.
		double[][] matrix = new double[ratingsByUser.size()][ratingsByUser.size()];	// weightTotal - denominator of predicted value formula. simTotal - numerator of PV formula.
		HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>(); 		// matrix - 2D matrix of user similarity values. labels - hashMap mapping users to indices in matrix.
		boolean[][] setMatrix = new boolean[ratingsByUser.size()][ratingsByUser.size()]; // used to denote if a similarity comparison has been done.
																						 // uses extra memory, but prevents duplicating comparisons where similarity is '0'.
		for (int i=0; i < ratingsByUser.size(); i++) {								
			labels.put((int) ratingsByUser.keySet().toArray()[i], i);				// add entry to labels mapping each user to an index by order of appearance
		}
		
		for (int i : testRatings.keySet()) {										// for each user in the test data
			for (int j : ratingsByUser.keySet()) {									// compare to every user in the training data
				distTotal = 0;
				sim = 0;
				boolean match = false;												// set to 'true' if at least one film is a match.
				
				if ((j!=i) && (setMatrix[labels.get(i)][labels.get(j)] == false)) {	// if they are not the same user, and do not currently have a similarity score
					ArrayList<Rating> tmp1 = ratingsByUser.get(i);					// set each to a temporary arraylist of their ratings to find films reviewed in common
					ArrayList<Rating> tmp2 = ratingsByUser.get(j);
					
					if (tmp1.size() > tmp2.size()) {								// make sure to iterate through the shorter review history (films must appear in both lists regardless).
						tmp1 = ratingsByUser.get(j);								
						tmp2 = ratingsByUser.get(i);								
						
						for (Rating r1 : tmp1) {									// for each rating by user 1
							for (Rating r2 : tmp2) {								// compare to each rating by user 2
								if (r1.getMovie()==r2.getMovie()) {					// if the same film exists in both review histories
									distTotal+=Math.abs(r1.getRating() - r2.getRating());	// add the absolute value of u1 rating - u2 rating
									match = true;
									break;											// break the loop.
								}
							}
						}
					}
					
					sim = 1 / (1 + distTotal);										// similarity score = 1 / (1 + total distance of all films shared by both users).
					if (match == true) {
						matrix[labels.get(i)][labels.get(j)] = sim;					// set these values to the similarity score, if there has been at least one match
						matrix[labels.get(j)][labels.get(i)] = sim;					// otherwise, retain similarity value of 0
					} 
					
					setMatrix[labels.get(i)][labels.get(j)] = true;					// set 'compared' flag to true for this combination.
					setMatrix[labels.get(j)][labels.get(i)] = true;
				}
			}
			
			for (Rating r1 : testRatings.get(i)) {									// for each review by the user in the test data
				simTotal = 0;
				weightTotal = 0;
				int movie = r1.getMovie();						
				if (ratingsByMovie.containsKey(movie)) {							// if this film exists in the training data
					for (Rating r2 : ratingsByMovie.get(movie)) {					// for each review of this film in the training data
						if (matrix[labels.get(i)][labels.get(r2.getUser())]!=0) {	// if the similarity score is greater than zero 
							simTotal += matrix[labels.get(i)][labels.get(r2.getUser())] * r2.getRating();	// add (similarity score * user2's rating to similarity score total.
							weightTotal += matrix[labels.get(i)][labels.get(r2.getUser())];					//add the similarity score to the weight total.
						}
					} if (weightTotal>0) r1.setPredicted(simTotal/weightTotal);		// if the weightTotal isn't zero, set the new predicted value. 
				}
			}
		}
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
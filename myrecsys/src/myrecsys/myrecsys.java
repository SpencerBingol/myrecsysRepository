package myrecsys;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;

public class myrecsys {
	static HashMap<Integer, HashMap<Integer, Rating>> testRatings;
	static HashMap<Integer, HashMap<Integer, Rating>> ratingsByUser;
	static HashMap<Integer, HashMap<Integer, Rating>> ratingsByMovie;
	
 	static double err = 0;
 	
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Format: java myrecsys training.data test.data algorithm-name");
			System.exit(1);
		}
		
		testRatings = new HashMap<Integer, HashMap<Integer, Rating>>();
		ratingsByUser = new HashMap<Integer, HashMap<Integer, Rating>>();
		ratingsByMovie = new HashMap<Integer, HashMap<Integer, Rating>>();
		
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
		double avg, total;										// total - Cumulative sum of rating values (1-5). avg - total/count 
		for (int movie : testRatings.keySet()) {				// for each movie in the test data
			total = 0;
			avg = 0;

			if (ratingsByMovie.containsKey(movie)) {			// if reviews for this movie also exist in the training data
				for (int user : ratingsByMovie.get(movie).keySet()) total += ratingsByMovie.get(movie).get(user).getRating();						// add movie rating to total
				avg = total / ratingsByMovie.get(movie).size();							// calculate average
							
				for (int i : testRatings.get(movie).keySet()) {		// for every test data review
					Rating r = testRatings.get(movie).get(i);
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
					HashMap<Integer, Rating> tmp1 = ratingsByUser.get(i);					// set each to a temporary arraylist of their ratings to find films reviewed in common
					HashMap<Integer, Rating> tmp2 = ratingsByUser.get(j);
					
					if (tmp1.keySet().size() > tmp2.keySet().size()) {								// make sure to iterate through the shorter review history (films must appear in both lists regardless).
						tmp1 = ratingsByUser.get(j);								
						tmp2 = ratingsByUser.get(i);								
						
						for (int mov1 : tmp1.keySet()) {									// for each rating by user 1
							for (int mov2 : tmp2.keySet()) {								// compare to each rating by user 2
								if (mov1==mov2) {							// if the same film exists in both review histories
									distTotal+=Math.abs(tmp1.get(mov1).getRating() - tmp2.get(mov2).getRating());	// add the absolute value of u1 rating - u2 rating
									match = true;
									break;											// break the loop.
								}
							}
						}
					}
					
					sim = 1 / (1 + distTotal);										// similarity score = 1 / (1 + total distance of all films shared by both users).
					if (match == true) {
						matrix[labels.get(i)][labels.get(j)] = sim;					// set these values to the similarity score, if there has been at least one match
					} 
					
					setMatrix[labels.get(i)][labels.get(j)] = true;					// set 'compared' flag to true for this combination.
				}
			}
			
			for (int movie : testRatings.get(i).keySet()) {									// for each review by the user in the test data
				simTotal = 0;
				weightTotal = 0;
				
				if (ratingsByMovie.containsKey(movie)) {							// if this film exists in the training data
					for (int user : ratingsByMovie.get(movie).keySet()) {					// for each review of this film in the training data
						Rating r2 = ratingsByMovie.get(movie).get(user);
						if (matrix[labels.get(i)][labels.get(r2.getUser())]!=0) {	// if the similarity score is greater than zero 
							simTotal += matrix[labels.get(i)][labels.get(r2.getUser())] * r2.getRating();	// add (similarity score * user2's rating to similarity score total.
							weightTotal += matrix[labels.get(i)][labels.get(r2.getUser())];					//add the similarity score to the weight total.
						}
					} if (weightTotal>0) testRatings.get(i).get(movie).setPredicted(simTotal/weightTotal);		// if the weightTotal isn't zero, set the new predicted value. 
				}
			}
		}
	}
	
	static void userPearson() {
		double xSum=0, ySum=0, xySum=0,							// xSum - Sum of all ratings from user 1. ySum - Sum of ratings from user 2. xySum - Sum of product of ratings from user 1 & 2.
				xSqSum=0, ySqSum=0, n=0,						// xSqSum - Sum of all user 1 ratings squared. ySqSum  - Sum of all user 2 ratings squared. n - count of films reviewed by both user 1 & 2.
				num, denom, denomLeft, denomRight, coefficient=0;
		double[][] matrix = new double[ratingsByUser.size()][ratingsByUser.size()];
		boolean[][] setMatrix = new boolean[ratingsByUser.size()][ratingsByUser.size()];
		HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>();
		
		for (int i=0; i < ratingsByUser.size(); i++) {								
			labels.put((int) ratingsByUser.keySet().toArray()[i], i);				// add entry to labels mapping each user to an index by order of appearance
		}
		
		for (int user1 : testRatings.keySet()) {
			for (int user2 : ratingsByUser.keySet()) {
				if ((user1!=user2) && (setMatrix[labels.get(user1)][labels.get(user2)] == false)) {
					xSum=0; ySum=0; xySum=0;
					xSqSum=0; ySqSum=0; n=0;
					
					HashMap<Integer, Rating> tmp1 = ratingsByUser.get(user1);
					HashMap<Integer, Rating> tmp2 = ratingsByUser.get(user2);
					if (tmp1.size() > tmp2.size()) {
						tmp1 = ratingsByUser.get(user2);
						tmp2 = ratingsByUser.get(user1);
					}
					
					for (int mov : tmp1.keySet()) {
						if (tmp2.containsKey(mov)) {
							n++;
							xSum += tmp1.get(mov).getRating();
							ySum += tmp2.get(mov).getRating();
							xySum += tmp1.get(mov).getRating() * tmp2.get(mov).getRating();
							xSqSum += tmp1.get(mov).getRating() * tmp1.get(mov).getRating();
							ySqSum += tmp2.get(mov).getRating()*tmp2.get(mov).getRating();
						}
					}
						
					if (n > 0) { /* Calculating the similarity weight between two users */
						num = (n * xySum) - (xSum * ySum);
						denomLeft = ((n*xSqSum) - Math.pow(xSum, 2));
						denomRight = ((n*ySqSum) - Math.pow(ySum, 2));
						denom = Math.sqrt(denomLeft * denomRight);
						coefficient = num / denom;
						
						if (Double.isNaN(coefficient)) coefficient = 0;
						matrix[labels.get(user1)][labels.get(user2)] = coefficient;
					} 
					setMatrix[labels.get(user1)][labels.get(user2)] = true;					// set 'already compared' flag to true for this combination.
				}
			}
			for (int movie : testRatings.get(user1).keySet()) {									// for each review by the user in the test data
				double simTotal=0, weightTotal=0;
				
				if (ratingsByMovie.containsKey(movie)) {							// if this film exists in the training data
					for (int user3 : ratingsByMovie.get(movie).keySet()) {					// for each review of this film in the training data
						if (setMatrix[labels.get(user1)][labels.get(user3)]==true) {	// if the similarity score is greater than zero 
							simTotal += matrix[labels.get(user1)][labels.get(user3)] * ratingsByMovie.get(movie).get(user3).getRating();
							weightTotal += Math.abs(matrix[labels.get(user1)][labels.get(user3)]);					//add the similarity score to the weight total.
						}
					} if (weightTotal>0) testRatings.get(user1).get(movie).setPredicted(simTotal/weightTotal);		// if the weightTotal isn't zero, set the new predicted value. 
				}
			}
		}
	}
		
	static void itemCosine() {
		double aSqSum=0, bSqSum=0, ab=0, denom=0, sim=0;	
		double[][] matrix = new double[ratingsByMovie.size()][ratingsByMovie.size()];
		boolean[][] setMatrix = new boolean[ratingsByMovie.size()][ratingsByMovie.size()];
		HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>();
		
		for (int i=0; i < ratingsByMovie.size(); i++) {								
			labels.put((int) ratingsByMovie.keySet().toArray()[i], i);				// add entry to labels mapping each user to an index by order of appearance
		}
		
		for (int mov1 : testRatings.keySet()) {
			if (ratingsByMovie.containsKey(mov1)) {
				for (int mov2 : ratingsByMovie.keySet()) {
					if ((mov1!=mov2) && (setMatrix[labels.get(mov1)][labels.get(mov2)] == false)) {
						aSqSum=0; bSqSum=0; ab=0;
						
						HashMap<Integer, Rating> tmp1 = ratingsByMovie.get(mov1);
						HashMap<Integer, Rating> tmp2 = ratingsByMovie.get(mov2);
						
						if (tmp1.size() > tmp2.size()) {
							tmp1 = ratingsByMovie.get(mov2);
							tmp2 = ratingsByMovie.get(mov1);
						}
						
						for (int user : tmp1.keySet()) {
							if(tmp2.containsKey(user)) {
								Rating r1 = tmp1.get(user);
								Rating r2 = tmp2.get(user);
								
								aSqSum += (r1.getRating()) * (r1.getRating());
								bSqSum += (r2.getRating()) * (r2.getRating());
								ab += (r1.getRating()) * (r2.getRating());
							}
						}
						
						if (ab > 0) {
							denom = Math.sqrt(aSqSum) * Math.sqrt(bSqSum);
							sim = ab/denom;
							
							matrix[labels.get(mov1)][labels.get(mov2)] = sim;
						}
						
						setMatrix[labels.get(mov1)][labels.get(mov2)] = true;
					}
				}
				
				for (int user : testRatings.get(mov1).keySet()) {
					double simTotal=0, weightTotal=0;
											
					if (ratingsByUser.containsKey(user)) {							// if this user exists in the training data
						for (int mov3 : ratingsByUser.get(user).keySet()) {					// for each review of this user in the training data
							if (setMatrix[labels.get(mov1)][labels.get(mov3)]==true) {	// if the similarity score is greater than zero 
								simTotal += matrix[labels.get(mov1)][labels.get(mov3)] * ratingsByUser.get(user).get(mov3).getRating();
								weightTotal += Math.abs(matrix[labels.get(mov1)][labels.get(mov3)]);		//add the similarity score to the weight total.
							}
						} if (weightTotal>0) testRatings.get(mov1).get(user).setPredicted(simTotal/weightTotal);		// if the weightTotal isn't zero, set the new predicted value. 
					}
				}
			}
		}
	}
	
	static void itemAdCosine() {
		double aSqSum=0, bSqSum=0, ab=0, denom=0, sim=0, avg=0;	
		double[][] matrix = new double[ratingsByMovie.size()][ratingsByMovie.size()];
		boolean[][] setMatrix = new boolean[ratingsByMovie.size()][ratingsByMovie.size()];
		HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>();
		
		for (int i=0; i < ratingsByMovie.size(); i++) {								
			labels.put((int) ratingsByMovie.keySet().toArray()[i], i);				// add entry to labels mapping each user to an index by order of appearance
		}
		
		for (int mov1 : testRatings.keySet()) {
			if (ratingsByMovie.containsKey(mov1)) {
				avg=0;
				
				for (int user : ratingsByMovie.get(mov1).keySet()) {
					avg+=ratingsByMovie.get(mov1).get(user).getRating();
				} avg /= ratingsByMovie.get(mov1).size();
				
				for (int mov2 : ratingsByMovie.keySet()) {
					if ((mov1!=mov2) && (setMatrix[labels.get(mov1)][labels.get(mov2)] == false)) {
						aSqSum=0; bSqSum=0; ab=0;
						
						HashMap<Integer, Rating> tmp1 = ratingsByMovie.get(mov1);
						HashMap<Integer, Rating> tmp2 = ratingsByMovie.get(mov2);
						
						if (tmp1.size() > tmp2.size()) {
							tmp1 = ratingsByMovie.get(mov2);
							tmp2 = ratingsByMovie.get(mov1);
						}
						
						for (int user : tmp1.keySet()) {
							if(tmp2.containsKey(user)) {
								Rating r1 = tmp1.get(user);
								Rating r2 = tmp2.get(user);
								
								aSqSum += (r1.getRating()-avg) * (r1.getRating()-avg);
								bSqSum += (r2.getRating()-avg) * (r2.getRating()-avg);
								ab += (r1.getRating()-avg) * (r2.getRating()-avg);
							}
						}
						
						if (ab > 0) {
							denom = Math.sqrt(aSqSum) * Math.sqrt(bSqSum);
							sim = ab/denom;
							
							matrix[labels.get(mov1)][labels.get(mov2)] = sim;
						}
						
						setMatrix[labels.get(mov1)][labels.get(mov2)] = true;
					}
				}
				
				for (int user : testRatings.get(mov1).keySet()) {
					double simTotal=0, weightTotal=0;
											
					if (ratingsByUser.containsKey(user)) {							// if this user exists in the training data
						for (int mov3 : ratingsByUser.get(user).keySet()) {					// for each review of this user in the training data
							if (setMatrix[labels.get(mov1)][labels.get(mov3)]==true) {	// if the similarity score is greater than zero 
								simTotal += matrix[labels.get(mov1)][labels.get(mov3)] * ratingsByUser.get(user).get(mov3).getRating();
								weightTotal += Math.abs(matrix[labels.get(mov1)][labels.get(mov3)]);		//add the similarity score to the weight total.
							}
						} if (weightTotal>0) testRatings.get(mov1).get(user).setPredicted(simTotal/weightTotal);		// if the weightTotal isn't zero, set the new predicted value. 
					}
				}
			}
		}
	}
	
	static void slopeOne() {
		double total=0, intSum=0;
		int totalCount=0, intCount=0;
		double[][] diff = new double[ratingsByMovie.size()][ratingsByMovie.size()];
		int[][] count = new int[ratingsByMovie.size()][ratingsByMovie.size()];
		boolean[][] compared = new boolean[ratingsByMovie.size()][ratingsByMovie.size()];
		HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>();
		
		for (int i=0; i < ratingsByMovie.size(); i++) {								
			labels.put((int) ratingsByMovie.keySet().toArray()[i], i);				// add entry to labels mapping each user to an index by order of appearance
		}
		
		for (int user1 : testRatings.keySet()) {
			for (int mov1 : testRatings.get(user1).keySet()) {
				total=0;
				totalCount=0;
				Rating r1 = testRatings.get(user1).get(mov1);
				
				for (int mov2 : ratingsByUser.get(user1).keySet()) {
					Rating r2 = ratingsByUser.get(user1).get(mov2);
					if (ratingsByMovie.containsKey(mov1) && !compared[labels.get(mov1)][labels.get(mov2)] && (mov1!=mov2)) {
						intCount = 0;
						intSum = 0;
						
						for (int user2 : ratingsByUser.keySet()) {
							if (ratingsByUser.get(user2).containsKey(mov1) && ratingsByUser.get(user2).containsKey(mov2) && (user1 != user2)) {
								intCount++;
								intSum+=(ratingsByUser.get(user2).get(mov1).getRating()-ratingsByUser.get(user2).get(mov2).getRating());
							}
						}
						
						if(!(intCount==0)) {
							diff[labels.get(mov1)][labels.get(mov2)] = intSum/intCount;
							count[labels.get(mov1)][labels.get(mov2)] = intCount;
						} compared[labels.get(mov1)][labels.get(mov2)] = true;
					}
					
					if(ratingsByMovie.containsKey(r1.getMovie())) {
						totalCount += count[labels.get(mov1)][labels.get(mov2)];
						total += (r2.getRating() + diff[labels.get(mov1)][labels.get(mov2)]) * count[labels.get(mov1)][labels.get(mov2)];					
					}
				} if (totalCount > 0) r1.setPredicted(total/totalCount);
			}
		}
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
		HashMap<Integer, Rating> entry;
		
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
		
				if (!ratingsByUser.containsKey(r.getUser())) ratingsByUser.put(r.getUser(), new HashMap<Integer, Rating>());
				entry = ratingsByUser.get(r.getUser());
				entry.put(r.getMovie(), r);
				ratingsByUser.put(r.getUser(), entry);
			
				if (!ratingsByMovie.containsKey(r.getMovie())) ratingsByMovie.put(r.getMovie(), new HashMap<Integer, Rating>());
				entry = ratingsByMovie.get(r.getMovie());
				entry.put(r.getUser(), r);
				ratingsByMovie.put(r.getMovie(), entry);
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
					if (!testRatings.containsKey(r.getUser())) testRatings.put(r.getUser(), new HashMap<Integer, Rating>());
					entry = testRatings.get(r.getUser());
					entry.put(r.getMovie(), r);
					testRatings.put(r.getUser(), entry);
				}
				else if (groupBy.equals("movie")) {
					if (!testRatings.containsKey(r.getMovie())) testRatings.put(r.getMovie(), new HashMap<Integer, Rating>());
					entry = testRatings.get(r.getMovie());
					entry.put(r.getUser(), r);
					testRatings.put(r.getMovie(), entry);
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
			HashMap<Integer, Rating> tmp = testRatings.get(i);
			
			for (int entry : tmp.keySet()) {
				Rating r = tmp.get(entry);
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
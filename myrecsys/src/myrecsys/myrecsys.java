package myrecsys;

public class myrecsys {
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Format: java myrecsys training.data test.data algorithm-name");
			System.exit(1);
		}
			
		System.out.printf("MYRESULTS Training \t= %s\n", args[0]);
		System.out.printf("MYRESULTS Testing \t= %s\n", args[1]);
		System.out.printf("MYRESULTS Algorithm \t= %s\n", args[2]);
		System.out.printf("MYRESULTS RMSE \t\t= %.6f\n", 0.0);
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
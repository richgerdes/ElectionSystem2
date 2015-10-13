import java.io.IOException;
import java.util.Scanner;

/**
 * Main runner for the project.
 * Instantiates a Monitor object that will be responsible for beginning and ending the election and 
 * Creates c different peers (actors) that will be a part of the election.
 *
 */
public class Runner {
	
	public static void main(String[] args){
		try {
			Scanner scan = new Scanner(System.in);
			System.out.print("Enter the number of actors to test with (1-46): ");
			int c = scan.nextInt(); //how many peers to generate
			
			new Monitor(); //Create new monitor to notify new peers of each other
			
			for(int i = 0; i < c; i++){
				new Node(20000 + i); //create peers with unique ports
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

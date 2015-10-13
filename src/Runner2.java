import java.io.IOException;

/**
 * 
 * Second main method made for extra testing purposes
 *
 */

public class Runner2 {
	
	public static void main(String[] args){
		try {
			int c = 3;
			for(int i = 0; i < c; i++){
				new Node(20000 + i);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

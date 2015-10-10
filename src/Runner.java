import java.io.IOException;


public class Runner {
	
	public static void main(String[] args){
		try {
			new Monitor();
			int c = 21;
			for(int i = 0; i < c; i++){
				new Node(20000 + i);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

package maas;

import java.util.List;
import java.util.Vector;
import maas.tutorials.BookBuyerAgent;
import java.util.Random;

public class Start {
    public static void main(String[] args) {
      String[] booktitles = {"BookA", "BookB", "BookC", "BookD", "BookE", "BookF", "BookG", "BookH", "BookI", "BookJ", "BookK", "BookL", "BookM", "BookN", "BookO", "BookP", "BookQ", "BookR", "BookS", "BookT" };
      
      int noOfbuyers = 20;
      int noOfsellers = 3;
      int[] eBooks = {10000, 200000, 300000}; 
      int[] noOfPaperbags = {10, 10};
      
      int[] prices = {14,10,69,65,95,77,70,28,43,66,32,37,34,98,41,59,36,57,96,196};
      List<String> agents = new Vector<>();

      Random rand = new Random();

      //adding buyer agents 
      for(int i=1; i<=noOfbuyers; i++){
        agents.add("buyer_"+ i + ":maas.tutorials.BookBuyerAgent("+booktitles[rand.nextInt(4)]+","+booktitles[rand.nextInt(2)]+","+booktitles[rand.nextInt(6)]+")");
      }
      
      //adding seller agents 
      for(int i=0; i<=noOfsellers; i++) {
    	  agents.add("Seller_"+ i + ":maas.tutorials.BookSellerAgent"+"("+booktitles[rand.nextInt(6)]+"_"+prices[rand.nextInt(4)]+"_"+noOfPaperbags[rand.nextInt(2)]+"_true"+","
    			  														 +booktitles[rand.nextInt(6)]+"_"+prices[rand.nextInt(4)]+"_"+noOfPaperbags[rand.nextInt(2)]+"_true"+","
    			  														 +booktitles[rand.nextInt(6)]+"_"+prices[rand.nextInt(4)]+"_"+eBooks[rand.nextInt(2)]+"_false"+","
    			  														 +booktitles[rand.nextInt(6)]+"_"+prices[rand.nextInt(4)]+"_"+eBooks[rand.nextInt(2)]+"_false"+")");
      }
      
    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : agents) {
    		sb.append(a);
    		sb.append(";");
    	}
    	cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }


}

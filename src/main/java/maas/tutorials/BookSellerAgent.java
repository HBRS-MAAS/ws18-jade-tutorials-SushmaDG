package maas.tutorials;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

//import com.sun.xml.internal.ws.api.pipe.Codec;

@SuppressWarnings("serial")
public class BookSellerAgent extends Agent{
	private AID[] buyers;
	private Hashtable<String, Integer> catalogue_paperbacks;
	private Hashtable<String, Integer> catalogue_ebooks;
	private Hashtable<String, Integer> inventory_catalogue;
	private ArrayList sellerbookList;
	private int noOfpaperbacks = 20;
	private int noOfpaperbackTitle = 2;
	private int noOfebooks = 2;
	int[] prices = {1000,200,4000,8000};
	int[] paperbackcopies = {10,10};

	String[] list_of_books = {"BookA","BookB","BookC","BookD"};

	Random rand = new Random();

	//Agent initializations
	protected void setup() {
		sellerbookList = new ArrayList();
		System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");

		initializePaperbackCatalogue();
		initializeEbookCatalogue();

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		//Add the behaviour serving requests for offer from buyer agents
		addBehaviour(new OfferRequestsServer());

		//Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());

		addBehaviour(new TickerBehaviour(this, 4000){
			protected void onTick(){
				// Update the list of buyer agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("book-buying");
				template.addServices(sd);
				try{
					DFAgentDescription[] result = DFService.search(myAgent, template);
					buyers = new AID[result.length];
					for (int i = 0; i < result.length; ++i){
						buyers[i] = result[i].getName();
					}
				} catch(FIPAException fe){
					fe.printStackTrace();
				}

				if (buyers.length == 0){
					System.out.println(getAID().getName() +" No buyers");
					addBehaviour(new shutdown());
				}
			}
		});
	}

	private void initializePaperbackCatalogue() {
		catalogue_paperbacks = new Hashtable();
		inventory_catalogue = new Hashtable();
		
        List<String> paperback_list = new Vector<>();
		List<String> paperBacks = new Vector<>();
		List<Integer> paperbackPrice = new Vector<>();
		int quantity = noOfpaperbacks/noOfpaperbackTitle;
		
		for(int i=2; i<4;i++) {
        	paperback_list.add(list_of_books[i]);
		} 


		for(int i=0; i<paperback_list.size(); i++) {
			catalogue_paperbacks.put(paperback_list.get(i),prices[rand.nextInt(4)]);
			inventory_catalogue.put(paperback_list.get(i), paperbackcopies[rand.nextInt(4)]);
		}

	}

	private void initializeEbookCatalogue() {
		catalogue_ebooks = new Hashtable();
		List<String> ebooks_list = new Vector<>();
		List<String> ebooks = new Vector<>();
		List<Integer> ebooksPrice = new Vector<>();
		
		for(int i=0;i<ebooks_list.size();i++) {
        	catalogue_ebooks.put(ebooks_list.get(i), prices[rand.nextInt(4)]);
        }

		ebooks.add("BookC");
		ebooksPrice.add(rand.nextInt(100));
		ebooks.add("BookD");
		ebooksPrice.add(rand.nextInt(100));


		for(int i=0; i<=ebooks_list.size(); i++) {
			catalogue_ebooks.put(ebooks.get(i),ebooksPrice.get(i));
		}

	}
	//Put agent clean up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		//Printout a dismissal message
		System.out.println("Seller agent"+getAID().getName()+"terminating.");
	}

	private class OfferRequestsServer extends CyclicBehaviour{
		public void action() {
						System.out.println("In the offer request*****************************");
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive();
			if(msg!= null) {
				System.out.println("Recieved message-------------------------------");
				//Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				if(catalogue_paperbacks.containsKey(title)) {
					Integer price = (Integer) catalogue_paperbacks.get(title);
					Integer quantity = (Integer) inventory_catalogue.get(title);

					//check for the quantity
					if(quantity>0) {
						//The requested book is available for sale. Reply with the price
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(String.valueOf(price.intValue()));
					}

				}

				else if(catalogue_ebooks.containsKey(title)) {
					Integer price = (Integer) catalogue_ebooks.get(title);
					//The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}			
				else {
					//The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
	//End of inner class OfferRequestsServer

	/**
		   Inner class PurchaseOrdersServer.
		   This is the behaviour used by Book-seller agents to serve incoming 
		   offer acceptances (i.e. purchase orders) from buyer agents.
		   The seller agent removes the purchased book from its catalogue 
		   and replies with an INFORM message to notify the buyer that the
		   purchase has been successfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Inside the Purchase orders server*********-----------");
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue_paperbacks.remove(title);
				
				if(catalogue_paperbacks.containsKey(title)) {
					Integer price_ = (Integer) catalogue_paperbacks.get(title);
					Integer quantity = (Integer) inventory_catalogue.get(title);
					//check for the quantity
					if(quantity>0) {
						//The requested book is available for sale. Reply with the price
						reply.setPerformative(ACLMessage.INFORM);
						System.out.println(title+" sold to agent "+msg.getSender().getName());
						//decrement the quantity
						inventory_catalogue.put(title, quantity--);
					}

				}
				else if(catalogue_ebooks.containsKey(title)){
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent "+msg.getSender().getName());
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
		// End of inner class PurchaseOrderServer

	}		

	// Reference: http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			SLCodec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
				myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
				//LOGGER.error(e);
			}
		}
	}

}
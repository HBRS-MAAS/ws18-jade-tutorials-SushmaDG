package maas.tutorials;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
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
	//maps the title name to price
	private Hashtable<String, Integer> catalogue;
	private ArrayList sellerbookList;

	//Agent initializations
	protected void setup() {
		//Create the catalogue
		catalogue = new Hashtable();
		sellerbookList = new ArrayList();
		System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");

		Object[] args = getArguments();

		if(args!=null && args.length>0){

			for (int i=0; i < args.length; i++) {
				String book_info = (String) args[i];
				String[] book_ = book_info.split("_");
				Hashtable bookCatalogue = new Hashtable();
				bookCatalogue.put("Title", book_[0]);
				bookCatalogue.put("Price", Integer.parseInt(book_[1]));
				bookCatalogue.put("Quantity", Integer.parseInt(book_[2]));
				bookCatalogue.put("is_paperback", Boolean.parseBoolean(book_[3]));
				sellerbookList.add(bookCatalogue);
				catalogue.put(book_[0], Integer.valueOf(book_[1]));
				//	                catalogue.put("Price", Integer.valueOf(book_[1]));

			}
			System.out.println("The catalogue of books being sold\t"  + catalogue);
//			System.out.println("In book list " + sellerbookList);
		}
		else {
			System.out.println("No arguments are available");
		}

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
	/*
	 * This is invoked by the GUI when the user adds a new book for sale
	 */
//	public void updateCatalogue(final String title,final int price) {
//		addBehaviour(new OneShotBehaviour() {
//			public void action() {
//				catalogue.put(title, new Integer(price));
//			}
//		});
//	}

	private class OfferRequestsServer extends CyclicBehaviour{
		public void action() {
//			System.out.println("In the offer request*****************************");
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive();
			if(msg!= null) {
				//System.out.println("Message is not null-------------------------------");
				//Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.get(title);
				System.out.println("Got the price for*****"+title+":"+price);
				if(price != null) {
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

				Integer price = (Integer) catalogue.remove(title);

				if (price != null) {
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
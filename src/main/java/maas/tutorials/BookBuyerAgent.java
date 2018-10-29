package maas.tutorials;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.List;
import java.util.Vector;
import java.util.Random;

// import java.util.*;
// import com.sun.xml.internal.ws.wsdl.writer.document.Message;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent {
	private String targetbookTitle;
	private AID[] sellerAgents = {new AID("seller1", AID.ISLOCALNAME),
            new AID("seller2", AID.ISLOCALNAME),
            new AID("seller3", AID.ISLOCALNAME)};
//	private AID[] sellerAgents;

	private int noOftargetBooks = 3;

	protected void setup() {

		// Printout a welcome message
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");

		//		System.out.println("I am here");

		Object[] args = getArguments();


		//		addBehaviour(new TickerBehaviour(this, 60000){
		//			protected void onTick(){
		//				myAgent.addBehaviour(new RequestPerformer());
		//			}
		//		});

		if(args!=null && args.length>0){
			for (int i = 0; i < args.length; i++) {
				String targetbookTitle = (String) args[i];
				addBehaviour(new RequestPerformer(targetbookTitle));
				System.out.println("Trying to buy\t" +targetbookTitle);
				
				addBehaviour(new TickerBehaviour(this, 60000) {
					protected void onTick() {
						// Update the list of seller agents
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription sd = new ServiceDescription();
						sd.setType("book-selling");
						template.addServices(sd);
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template);
							sellerAgents = new AID[result.length];
							for (int i = 0; i < result.length; ++i) {
								sellerAgents[i] = result[i].getName();
							}
						}
						catch (FIPAException fe) {
							fe.printStackTrace();
						}
						// Perform the request
						myAgent.addBehaviour(new RequestPerformer(targetbookTitle));
					}
				} );
			}
		}
		else{
			System.out.println("No book title specified");
			doDelete();
		}


		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
		//		addBehaviour(new shutdown());

	}
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
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

	//The request performer behaviour is used by the buyer agent to request seller agents the targer book
	private class RequestPerformer extends Behaviour {
		private AID bestSeller;
		private int bestPrice;
		private int repliesCnt = 0;
		private MessageTemplate mt;
		private int step = 0;
		private String booktitle;

		public RequestPerformer(String booktitle) {
			this.booktitle = booktitle;
		}

		public void action() {
			switch (step) {
			case 0:
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					//System.out.println("In for loop of case 0 of request performer -------------Buyer agent");
					cfp.addReceiver(sellerAgents[i]);
				}
				cfp.setContent(this.booktitle );
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					//System.out.println(reply);
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						System.out.println("If loop of case 1 of request performer ---------Buyer agent");
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			case 2:
				//System.out.println("In case 2 of request performer ---------------------Buyer agent");
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(this.booktitle);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				//System.out.println("In case 3 of request performer ---------------------Buyer agent");
				reply = myAgent.receive(mt);
				if(reply!=null){
					if(reply.getPerformative() == ACLMessage.INFORM){
						System.out.println(this.booktitle + "successfully purchased");
						System.out.println("Price =" +bestPrice);
						myAgent.doDelete();
					}
					step=4;
				}
				else{
					block();
				}
				break;
			}
		}

		public boolean done() {
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}
}

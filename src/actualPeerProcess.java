import java.util.ArrayList;
import java.util.Random;

public class actualPeerProcess {
	ArrayList<node> node_array = new ArrayList<node>();
	Protocol prot;
	Object sharedObj = new Object();	//for sync between send/recv thread AND preferred/optimistic thread 
	
	actualPeerProcess(int myPeerID) {
		prot = new Protocol(myPeerID);
		prot.initIO();
		init_threads(prot);
	}
	/*
	 * TODO: wait for the optimistic thread to finish
	 * Also, the optimistic thread should wait/stop for all the other threads(send, recv, preferred threads) 
	 */
	

	public void init_threads(Protocol prot){
		
		//start the connection thread, so that it can start the respective send & recieve threads for each peer
		ConnectionThread connThread = new ConnectionThread(prot.myPeerID, node_array, prot, this); 
		
		//start the preferred thread
		PreferredThread peer_pref = new PreferredThread(prot, this);
		
		//start the optimistic thread
		optimisticThread(prot);
	}
	
	public void optimisticThread(Protocol prot){
		prot.logging.debug("Starting OptimisticThread");
		try{			
			while (prot.NumPeers != node_array.size())
				Thread.currentThread().sleep(prot.OptInterval*1000);
				
			while(true){
							
				ArrayList<Integer> index_array = new ArrayList<Integer>();				
				for(int i = 0; i < prot.NumPeers; i++){
					if(node_array.get(i).PeerID != prot.myPeerID)
						index_array.add(i);
				}				

				Random r = new Random();
				int random_peer_index = r.nextInt(index_array.size()-1);
				node opt_node = node_array.get(random_peer_index);
				
				/*
				 * WARNING:::::: check if number of preferred neighbors and the total number of peers are the same
				 * If not, it cannot select any optimistic neighbor, since all the neighbors will be part of preferred neighbor list
				 * 
				 * Check if preferred neighbors > no of peers
				 */
				if(prot.NumPeers-1 != prot.NumPN){
					
					//the optimistic thread should only randomly generate the peer which has not been choked and are interested
					//hence the following condition, 
					// BE IN THE LOOP TILL THE NODE HAS THE FOLLOWING STATUS
					//		MESSAGE TYPE IS CHOKE 		&& 		NOT INTERESTED
					while(opt_node.ChokeMessageType == true && opt_node.InterestStatus == false){
						random_peer_index = r.nextInt(index_array.size()-1);
						opt_node = node_array.get(random_peer_index);
					}
					
					//send choke/unchoke message
					opt_node.send_choke_msg = true;
					//choke message type = UNCHOKE
					opt_node.ChokeMessageType = false;
				}
				prot.logging.log("Peer [" + Integer.toString(prot.myPeerID) + "] has optimistically unchoked neighbor " 
						+ Integer.toString(opt_node.PeerID));
				
				Thread.currentThread().sleep(prot.OptInterval*1000);
			}
		}
		catch(Exception e){
			prot.logging.debug("Exception in OptimisticThread" + e.getMessage());
			e.printStackTrace();
		}
		prot.logging.debug("Exiting OptimisticThread");
	}
}
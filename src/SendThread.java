import java.net.*;

/*
 * TODO: use sharedObj to synchronize write access ChokedStatus only
 * Use a variable in node, which should be tested before entering wait state by send, This variable will be set by preffered thread and optimistic thread 
 */
public class SendThread implements Runnable{
	
	Protocol prot;
	actualPeerProcess peerProcObj;
	int peer_index;		//index of the peer in the node_array
	
	SendThread(Protocol prot, actualPeerProcess peerProcObj, int peer_index){
		this.prot = prot;
		this.peerProcObj = peerProcObj;
		this.peer_index = peer_index;
		
		String thread_name = "send" + peer_index;

		Thread send_thread = new Thread(this, thread_name);
		send_thread.start();		
	}
	
	public void run(){
		prot.logging.debug(prot.myPeerID + " Starting Send thread for node" + peerProcObj.node_array.get(peer_index).PeerID);
		try{
			while(true){
				/*
				 * USE LOCK/SYNCHRONIZE ONLY ON peerProcObj.node_array[peer_index] for sync with reciever thread
				 * and LOCK/SYNCHRONIZE ONLY ON peerProcObj.sharedObj for sync with preferred thread and optimistic thread
				 */
				
				synchronized(peerProcObj.sharedObj){
					peerProcObj.sharedObj.wait();	//wait till recvThread OR preferredThread OR optimisticThread to notifies you
					
					node temp_node = peerProcObj.node_array.get(peer_index);
					byte[] msg = null;
					
					/*
					 * ALWAYS SET send_***_msg to false after getting the message
					 * since when the preferred/optimistic thread does its work and then releases the lock on the sharedObj, 
					 * either sender/reciever thread can take the ownership of the sharedObj
					 * if the reciever thread takes the ownership, it might change the value of some of the variables.
					 * 
					 * ALSO, keep it as a set of 'if' conditions rather than 'if-else' conditions
					 */
					
					//check whether it is to send a choke/unchoke message
					if(temp_node.send_choke_msg == true){
						//choke message
						if(temp_node.ChokeMessageType == true)
							msg = prot.getChoke(temp_node);
						//unchoke message
						else
							msg = prot.getUnchoke(temp_node);
						temp_node.send_choke_msg = false;
						if(msg != null)
							temp_node.out.write(msg);
					}
					//have message, **SET request_piece_id to -1 only in Sender Thread
					if(temp_node.send_have_msg == true){
						msg = prot.getHave(temp_node); 
						temp_node.send_have_msg = false;
						if(msg != null)
							temp_node.out.write(msg);
					}
					//interested message
					if(temp_node.send_interested_msg == true){
						msg = prot.getInterested(temp_node);
						temp_node.send_interested_msg = false;
						if(msg != null)
							temp_node.out.write(msg);
					}
					if(temp_node.send_piece_msg == true){
						msg = prot.getPiece(temp_node.RequestpieceID); //tODO: CHECK THIS with above todo marked
						temp_node.send_piece_msg = false;
						if(msg != null)
							temp_node.out.write(msg);
					}
					//if our state is unchoked send a request (#PP)
					if (temp_node.send_request_msg == true && 
							prot.FileStatus == false && 
							temp_node.PeerChokeStatus == false) {

						msg = prot.getRequest();
						if(msg != null)
							temp_node.out.write(msg);

						if (prot.FileStatus == false) {
							 temp_node.send_request_msg = false; // Wait till it is set to true by protocol
						 } 
						 else {
							 msg=null;
						 }
					}
					//just to be safe, check for null message and then send the message
				}
				Thread.currentThread().yield();
			}
		}
		catch(Exception e){
			prot.logging.debug(prot.myPeerID + "Exception in SendThread" + e.getMessage());
			e.printStackTrace();
		}
		prot.logging.debug(prot.myPeerID + "Exiting SendThread belonging to node" + peerProcObj.node_array.get(peer_index).PeerID);
	}
	
	
}
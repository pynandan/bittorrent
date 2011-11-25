import java.net.*;
import java.io.*;

public class RecvThread implements Runnable{
	
	Protocol prot;
	actualPeerProcess peerProcObj;
	int peer_index;
	
	RecvThread(Protocol prot, actualPeerProcess peerProcObj, int peer_index){
		this.prot = prot;
		this.peerProcObj = peerProcObj;
		this.peer_index = peer_index;
		
		String thread_name = "recv" + peer_index;

		Thread recv_thread = new Thread(this, thread_name);
		recv_thread.start();
	}
	
	public void run(){
		prot.logging.debug(prot.myPeerID + " Starting Recv thread for node" + peerProcObj.node_array.get(peer_index).PeerID);
		try{
			while(true){
				/*
				 * USE LOCK/SYNCHRONIZE ONLY ON peerProcObj.node_array[peer_index] for sync with reciever thread
				 * and LOCK/SYNCHRONIZE ONLY ON peerProcObj.sharedObj for sync with preferred thread and optimistic thread
				 */
				
				byte[] packet =  peerProcObj.node_array.get(peer_index).getPacket();
				/*String recv_str = peerProcObj.node_array.get(peer_index).br.readLine();	//since recieve is blocking, 
																	//this line should be outside the synchronized loop*/
				synchronized(peerProcObj.sharedObj){
					int ret_val = prot.processMessage(packet, peerProcObj.node_array.get(peer_index));
					if(ret_val > 0){
						peerProcObj.sharedObj.notifyAll();
					}
				}
			}
		}
		catch(Exception e){
			prot.logging.debug(prot.myPeerID + "Exception in RecvThread" + e.getMessage());
			e.printStackTrace();
		}
		prot.logging.debug(prot.myPeerID + "Exiting RecvThread, belonging to node" + peerProcObj.node_array.get(peer_index).PeerID);
	}
}
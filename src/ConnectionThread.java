import java.net.*;
import java.io.*;
import java.util.*;

public class ConnectionThread implements Runnable{
	
	ArrayList<node> node_array;
	actualPeerProcess peerProcObj;
	Thread conn_thread;
	Protocol prot;
	int peer_id = 0;
	
	ConnectionThread(int peer_id, ArrayList<node> node_array, Protocol prot, actualPeerProcess peerProcessObj){
		this.peer_id = peer_id;
		this.node_array = node_array;
		this.peerProcObj = peerProcessObj;
		this.prot = prot;
				
		conn_thread = new Thread(this, "conn");
		conn_thread.start();
	}
	
	public void run(){
		prot.logging.debug(prot.myPeerID + " Starting Connection thread");
		try{
			int index = 0;
			
			//connect with the previously started peers
			for(int i = 0; i < prot.NumPeers; i++){
				if(prot.PeerID[i] == peer_id){
					index = i;
					break;
				}
				
				Socket client_socket = new Socket(prot.Hostname[i], prot.Ports[i]);
				
				node peer_node = new node(prot, node_array);
				peer_node.socket = client_socket;
								
				peer_node.out = new DataOutputStream(client_socket.getOutputStream());
	            peer_node.in = new DataInputStream(client_socket.getInputStream());
	            
	            //send handshake
	            byte[] msg = prot.getHandshake(peer_id);
	            peer_node.out.write(msg);
	            //get handshake from the other peer
	            msg = new byte[32];
	            peer_node.readData(msg);            
	            peer_node.PeerID = prot.verifyHandshake(msg, true);
	            
	            //send bitfield msg
	            msg = prot.getBitfield();
	            peer_node.out.write(msg);
	            /*//get bitfield msg
	            msg = peer_node.getPacket();
	            int ret_val = prot.processMessage(msg, peer_node);
	            if(ret_val > 0){
					peerProcObj.sharedObj.notifyAll();
				}*/
	            				
				node_array.add(peer_node);
				//this is location of the peer in the node_array in this machine
				int peer_index = node_array.size() - 1;
				
				SendThread peer_send = new SendThread(prot, peerProcObj, peer_index);
				RecvThread peer_recv = new RecvThread(prot, peerProcObj, peer_index);
			}

			//accept connections from other peers
			ServerSocket server_socket = new ServerSocket(prot.Ports[index]);
			while(true){
				Socket client_socket = server_socket.accept();
				
				node peer_node = new node(prot, node_array);
				peer_node.socket = client_socket;
				
				peer_node.out = new DataOutputStream(client_socket.getOutputStream());
	            peer_node.in = new DataInputStream(client_socket.getInputStream());
	            
	            //get the handshake msg
	            byte[] msg = new byte[32];
	            peer_node.readData(msg);
	            
	            peer_node.PeerID = prot.verifyHandshake(msg, false);
	            //send handshake msg to the other peer
	            msg = prot.getHandshake(peer_id);
	            peer_node.out.write(msg);
	            
	            /*//get bitfield msg
	            msg = peer_node.getPacket();
	            prot.processMessage(msg, peer_node);*/
	            
	            //send bitfield msg to the other peer
	            msg = prot.getBitfield();
	            peer_node.out.write(msg);
	            
	            node_array.add(peer_node);
	            //this is location of the peer in the node_array in this machine
				int peer_index = node_array.size() - 1;
				
				SendThread peer_send = new SendThread(prot, peerProcObj, peer_index);
				RecvThread peer_recv = new RecvThread(prot, peerProcObj, peer_index);
			}
		}
		catch(Exception e){
			e.printStackTrace();
			prot.logging.debug(peer_id + "Exception in ConnectionThread" + e.getMessage());
		}
		prot.logging.debug(peer_id +"Exiting ConnectionThread");
	}
}
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
								
				peer_node.pw = new PrintWriter(client_socket.getOutputStream(), true);
	            peer_node.br = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
	            
	            //send handshake
	            byte[] msg = prot.getHandshake(peer_id);
	            peer_node.pw.println(msg);
	            //get handshake from the other peer
	            String str_msg = peer_node.br.readLine();
	            msg = str_msg.getBytes();	            
	            peer_node.PeerID = prot.verifyHandshake(msg, true);
	            
	            //send bitfield msg
	            msg = prot.getBitfield();
	            peer_node.pw.println(msg);
	            //get bitfield msg
	            str_msg = peer_node.br.readLine();
	            prot.processMessage(str_msg.getBytes(), peer_node);
	            				
				node_array.add(peer_node);
				//this is location of the peer in the node_array in this machine
				int peer_index = node_array.size() - 1;
				
				SendThread peer_send = new SendThread(prot, peerProcObj, peer_index);
				RecvThread peer_recv = new RecvThread(prot, peerProcObj, peer_index);
			}

			//accept connections from other peers
			while(true){
				ServerSocket server_socket = new ServerSocket(prot.Ports[index]);
				Socket client_socket = server_socket.accept();
				
				node peer_node = new node(prot, node_array);
				peer_node.socket = client_socket;
				
				peer_node.pw = new PrintWriter(client_socket.getOutputStream(), true);
	            peer_node.br = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
	            
	            //get the handshake msg
	            String str_msg = peer_node.br.readLine();
	            byte[] msg = str_msg.getBytes();
	            peer_node.PeerID = prot.verifyHandshake(msg, false);
	            //send handshake msg to the other peer
	            msg = prot.getHandshake(peer_id);
	            peer_node.pw.println(msg);
	            
	            //get bitfield msg
	            str_msg = peer_node.br.readLine();
	            prot.processMessage(str_msg.getBytes(), peer_node);
	            //send bitfield msg to the other peer
	            msg = prot.getBitfield();
	            peer_node.pw.println(msg);
	            
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
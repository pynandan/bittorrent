import java.net.Socket;
import java.io.*;
import java.util.*;

public class node {
	Socket socket;
	ArrayList<node> NodeArray;
	Protocol Prot;
	
	int PeerID;
	boolean bitField[];
	boolean Chokestatus;			//true-> chocked, false->unchocked, have we choked the peer? (Sent by peer to us)
	boolean InterestStatus;			//true-> interested, does the told us that it is interested in us? (Sent by peer to us)

	int DownloadRate; 				/*Number of pieces received in unchoking interval, 
									 *will be set to zero before the beginning of each unchoking interval*/
	boolean PeerInterestStatus;		// (Sent by us to Peer) Our interest status in the Peer (as stored by InterestStatus in other Peer) / Was out last message to this INTERESTED or NOT INTERESTED
	boolean PeerChokeStatus;		// (Sent by us to peer) Does this particular peer has chocked us?
	
	int RequestpieceID; 			//Stores the request piece ID. (pieceID as requested by peer) So when REQUEST packet is got from peer
	boolean ChokeMessageType;		//This value is used by sender thread when send_choke_msg is set (will be set by optimistic or preferred thread)
	int lastReceivedPieceID;
	
	boolean send_choke_msg;			//Should we send a choke message (will be set by optimistic or preferred thread)			
	boolean send_have_msg; 			//After we received a piece, send have message to all the other nodes, that don't have that piece
	boolean send_interested_msg;	//After we received a have message or bitfield, send a Interested message, if we are interested.
	boolean send_piece_msg;			//We need to send a Piece message
	boolean send_request_msg;
	
	PrintWriter pw;
	BufferedReader br;
	
	node(Protocol prot, ArrayList<node> Array) {
		
		bitField = new boolean[prot.NumPieces];
		
		Chokestatus = true;
		InterestStatus = false;
		
		PeerChokeStatus = true;
		PeerInterestStatus = false;
		
		send_choke_msg = false;
		send_have_msg = false;
		send_interested_msg = false;
		send_piece_msg = false;
		
		DownloadRate = 0;

		PeerID = -1;
		Prot = prot;
		NodeArray = Array;
	}
	
	public void UpdateBitField(int pieceIndex, Protocol prot){
		bitField[pieceIndex] = true;
		if (prot.bitField[pieceIndex] == false ){ //in prot.bitField the bit corresponding to pieceIndex is not set
			if (PeerInterestStatus == false) {//We haven't sent peer interested message
				send_interested_msg = true;
				PeerInterestStatus = true;
			}
		}
	}

	/*
	 * Change byte[] to boolean[] and update the bitfield
	 */
	public void UpdateBitField(byte [] SrcBitField, int offset){
		int mask=(byte)0x80;
		for (int i=0 ; i< Prot.NumPieces ; i++) {
			if ((SrcBitField[i/8] & (mask >>> (i%8))) != 0){
				bitField[i] = true;
			} else {
				bitField[i] = false;
			}
		}
		
		if (PeerInterestStatus == false) {
			send_interested_msg =true;
			PeerInterestStatus=true;
		}
	}
	
	public void notifyHaveMsg(int pieceIndex){
		Iterator<node> nodeIter = NodeArray.iterator();
		
		while (nodeIter.hasNext()) {
			node tmp=nodeIter.next();
			if (tmp.PeerID == Prot.myPeerID || tmp.PeerID == -1){
				continue;
			}
			else {
				//if (bitField[pieceIndex] == false) { //Why commented? To maintain consistant bitField across peers
				tmp.send_have_msg = true;
				tmp.lastReceivedPieceID = pieceIndex;	//this is required while sending have message
				//}
			}
		}
	}
}
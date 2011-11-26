import java.net.Socket;
import java.io.*;
import java.util.*;

public class node {
	Socket socket;
	ArrayList<node> NodeArray;
	Protocol Prot;
	
	int PeerID;
	volatile boolean bitField[];
	volatile boolean Chokestatus;			//true-> chocked, false->unchocked,  (Sent by peer to us)
	volatile boolean InterestStatus;			//true-> interested, does the told us that it is interested in us? (Sent by peer to us)

	volatile int DownloadRate; 				/*Number of pieces received in unchoking interval, 
									 *will be set to zero before the beginning of each unchoking interval*/
	volatile boolean PeerInterestStatus;		// (Sent by us to Peer) Our interest status in the Peer (as stored by InterestStatus in other Peer) / Was out last message to this INTERESTED or NOT INTERESTED
	volatile boolean PeerChokeStatus;		// (Sent by us to peer) 
	
	volatile int RequestpieceID; 			//Stores the request piece ID. (pieceID as requested by peer) So when REQUEST packet is got from peer 
	volatile boolean PendingRequests;//(will be set to true if there are pending requests)
	volatile boolean ChokeMessageType;		//This value is used by sender thread when send_choke_msg is set (will be set by optimistic or preferred thread)
	volatile int lastReceivedPieceID;
	
	volatile boolean send_choke_msg;			//Should we send a choke message (will be set by optimistic or preferred thread)			
	volatile boolean send_have_msg; 			//After we received a piece, send have message to all the other nodes, that don't have that piece
	volatile boolean send_interested_msg;	//After we received a have message or bitfield, send a Interested message, if we are interested.
	volatile boolean send_piece_msg;			//We need to send a Piece message
	volatile boolean send_request_msg;
	volatile boolean send_not_interested_msg; //after we receive the complete file, send the not interested msg	

	DataOutputStream out;
	DataInputStream in;
	
	node(Protocol prot, ArrayList<node> Array) {
		
		bitField = new boolean[prot.NumPieces];
		for (int i=0; i< prot.NumPieces ; i++)
			bitField[i]=false;
		
		Chokestatus = true;
		InterestStatus = false;
		
		PeerChokeStatus = true;
		PeerInterestStatus = false;
		
		send_choke_msg = false;
		send_have_msg = false;
		send_interested_msg = false;
		send_piece_msg = false;
		send_not_interested_msg = false;
		
		PendingRequests = false;

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
			if ((SrcBitField[offset + i/8] & (mask >>> (i%8))) != 0){
				bitField[i] = true;
			} else {
				bitField[i] = false;
			}	
		}
		//Send intersted message only if we have incomplete file
		if (PeerInterestStatus == false && Prot.FileStatus == false) {
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
	
	public void readData(byte[] data) {
		int dataRead =0, cnt;
		try{
			while (dataRead < data.length) {
				if ((cnt = in.read(data, dataRead, data.length-dataRead)) <= 0 ) {
					Prot.logging.debug("readData: error while reading");
					Thread.currentThread().yield();
				} 
				else {
					dataRead = dataRead + cnt;
				}
			}
		}catch (Exception ex) {
			Prot.logging.debug("Exception: readdata" + ex.getMessage());
		}
	}
	
	public void readData(byte[] data, int off, int len) {
		int dataRead =0, cnt;
		try{
			while (dataRead < len) {
				if ((cnt = in.read(data, dataRead+off, len-dataRead)) <= 0 ) {
					Prot.logging.debug("readData: error while reading");
					Thread.currentThread().yield();
				}
				else {
					dataRead = dataRead + cnt; 
				}
			}
		}catch (Exception ex) {
			Prot.logging.debug("Exception: readdata with off" + ex.getMessage());
		}
	}	
	
	public byte[] getPacket() {
		Prot.logging.debug("Entering getPacket");
		byte[] msg = new byte[4];
		readData(msg);
		int msglen = Prot.ByteToInt(msg, 0);
		
		byte[] retBuf = new byte[msglen+4];
		System.arraycopy(msg, 0, retBuf, 0 , msg.length);
		readData(retBuf, 4, msglen);
		Prot.logging.debug("Happy!! Got a packet: " + retBuf[4]);
		return retBuf;
	}
}
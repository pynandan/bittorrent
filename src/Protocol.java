import java.io.*;
import java.util.*;

class Protocol {
//peerinfo.cfg
	int NumPeers;
	int PeerID[];
	String[] Hostname;
	int Ports[];
	int HaveFile[];
//Common.cfg	
	int NumPN;
	int Interval;
	int OptInterval;
	String FileName;
	int FileSize;
	int PieceSize;
	int NumPieces;
	boolean FileStatus;		//true if file is completely present else false
	String dirname;
//Others
	int myPeerID;
	int curPieces;
	boolean bitField[];
	boolean requestBitField[];
//File read write API's
	static private RandomAccessFile fc;
	static private int isFileOpen=0;
	writeLog logging;
	
	class writeLog{
		int myPeerID;
		BufferedWriter writer;
		
		writeLog (int myID){
			//Create a directory with name peer_<peerID>
			dirname="peer_"+Integer.toString(myID);
			String filename="log_peer_"+Integer.toString(myID)+".log";
			
			try {
				boolean success = (new File(dirname)).mkdir();
				if (success) {
					System.err.println("Error Creating Directory" + myID);
				}
			}
			catch (Exception e){
				System.err.println("Error:"+e.getMessage());
			}
			
			//Open Log file with name "log_peer_<PeerID>.log"
			try {
				writer = new BufferedWriter(new FileWriter(dirname+"/"+filename));
			} 
			catch (Exception e){
				System.err.println("Error:"+e.getMessage());
			}
		}
		
		synchronized void log(String str){
			Date date;
			try {
				date = new Date();
				writer.write("["+Long.toString(date.getTime()) + "]:"+str);
				writer.newLine();
				writer.flush();
			} 
			catch (Exception e) {
				System.err.println("Error:"+e.getMessage());
			}
		}
		
		void debug(String str) {
			//	log("Debug: " + str);
		}
		
	}
	
//Functions
	public Protocol(int myID) {
		Scanner s=null;
		List<Integer> lPeerID = new ArrayList<Integer>();
		List<String> lHostname = new ArrayList<String>();
		List<Integer> lPorts = new ArrayList<Integer>();
		List<Integer> lHaveFile = new ArrayList<Integer>();
		int i; 
		
		myPeerID = myID;
		
		//Initialize logging
		logging =new writeLog(myPeerID);
		
		/*Read data from PeerInfo.cfg*/
		try {
			NumPeers=0;
			s = new Scanner (new BufferedReader(new FileReader("PeerInfo.cfg")));
			while (s.hasNext()) {
				lPeerID.add(s.nextInt());
				lHostname.add(s.next());
				lPorts.add(s.nextInt());
				lHaveFile.add(s.nextInt());
				NumPeers++;
			}
		} catch (FileNotFoundException exp) {
			logging.debug("PeerInfo.cfg Not Found");
		}finally {
			if ( s != null) {
				s.close();
			}
		}
		PeerID = new int[NumPeers];
		Hostname = new String[NumPeers];
		Ports = new int[NumPeers];
		HaveFile = new int[NumPeers];

		for (i=0 ; i < NumPeers ; i++) {
			PeerID[i] = lPeerID.get(i);
			Hostname[i] = lHostname.get(i);
			Ports[i] = lPorts.get(i);
			HaveFile[i] = lHaveFile.get(i);
		}
		
		/*Read data from Common.cfg*/
		try {
			s = new Scanner (new BufferedReader(new FileReader("Common.cfg")));
			while (s.hasNext()) {
				String str = s.next();
				if ("NumberOfPreferredNeighbors".equals(str)) {
					NumPN = s.nextInt();
				} else if ("UnchokingInterval".equals(str)) {
					Interval = s.nextInt();
				} else if ("OptimisticUnchokingInterval".equals(str)) {
					OptInterval = s.nextInt();
				} else if ("FileName".equals(str)) {
					FileName = s.next();
				} else if ("FileSize".equals(str)) {
					FileSize = s.nextInt();
				} else if ("PieceSize".equals(str)) {
					PieceSize = s.nextInt();
				} else {
					System.out.println("Unknown option in Common.cfg" + str);
					break;
				}
			}
			NumPieces = FileSize/PieceSize;
			if (FileSize % PieceSize != 0)
				NumPieces++;
		} catch (FileNotFoundException exp) {
			logging.debug("Common.cfg Not Found");
		}finally {
			if ( s != null) {
				s.close();
			}
		}
		logging.log ("NumPieces = " + NumPieces);
		bitField = new boolean[NumPieces];
		requestBitField = new boolean[NumPieces];
	}
	
	public void initIO(){
		int idx=-1,i;
		for (i=0 ; i<NumPeers ; i++) {
			if (PeerID[i]==myPeerID) {
				idx=i;
				break;
			}
		}
		if (idx == -1)
			logging.debug("Peer not found: PeerID" + myPeerID);
		if (isFileOpen == 1)
			return;

		//Add the necessary prefix(directory) to the filename
		String newFileName = dirname + "/" + FileName;
		if (HaveFile[idx] == 1) {
			try {
				/*Open File*/
				fc = new RandomAccessFile(FileName, "r");
				isFileOpen = 1;
			} catch (FileNotFoundException exp) {
				logging.debug("Data file not found" + exp.getMessage());
			}
			for(i=0 ; i < bitField.length ;i++){
				bitField[i] = true;
				requestBitField[i] = true;
			}
			FileStatus = true;	//Set the status to true if the file is completely available
			curPieces = NumPieces;
		} else {
			FileStatus = false;
			try {
				fc = new RandomAccessFile(newFileName, "rwd");
				isFileOpen = 1;
			} catch (Exception  exp) {
				logging.debug("Excepion:"+exp.getMessage());
			}
			for(i=0 ; i < bitField.length ;i++){
				bitField[i] = false;
				requestBitField[i] = false;
			}
			/*Allocate file length*/
			try {
				fc.setLength(FileSize);
			} catch (IOException exp) {
				logging.debug("IOExcepion while setting length:"+exp.getMessage());
			}
			curPieces = 0;
		}
	}
	
	/*
	 * Reads a piece from a given piece index
	 */
	public byte[] readPiece(int pieceIndex) {
		byte returnBuffer[] = new byte[PieceSize];
		int len = PieceSize;
		
		if (pieceIndex >= NumPieces) {
			logging.debug("Error readpiece: Invalid Piece Index");
			return returnBuffer;
		} else if (pieceIndex == NumPieces-1) { //Asking last chunk, so len can be < PieceSize
			if (FileSize % PieceSize != 0)
				len = FileSize % PieceSize;
		}
		try {
			synchronized(fc) {
				fc.seek(pieceIndex * PieceSize);
				fc.readFully(returnBuffer, 0, len);
			}
		} catch (IOException exp) {
			logging.debug("IOException in readPiece" + exp.getMessage());
		}
		return returnBuffer;
	}
	
	/*
	 * Writes a Piece to the file at given pieceIndex
	 */
	public int writePiece (int pieceIndex, byte[] buffer) {
		int len=PieceSize;
		
		if (pieceIndex >= NumPieces) {
			logging.debug("Error readpiece: Invalid Piece Index");
			return -1;
		} else if (pieceIndex == NumPieces-1) { //Asking last chunk, so len can be < PieceSize
			if (FileSize % PieceSize != 0)
				len = FileSize % PieceSize;
		}
		try {
			synchronized(fc){
				fc.seek(pieceIndex * PieceSize);
				fc.write(buffer, 0, len);
			}
			return 0;
		} catch (IOException exp) {
			logging.debug("IOException in writePiece" + exp.getMessage());
			return -1;
		}
	}

	/*
	 * Actual Protocol begins Now 
	 */
	
	private final byte header[]= {'C','E','N','5','5','0','1','C','2','0','0','8','S','P','R','I','N','G'};
	
	private final int handshakeLen = 32;
	private final byte MSGLEN=4;

	private final byte CHOKE=0;
	private final byte UNCHOKE=1;
	private final byte INTERESTED=2;
	private final byte NOTINTERESTED=3;
	private final byte HAVE=4;
	private final byte BITFIELD=5;
	private final byte REQUEST=6;
	private final byte PIECE=7;
	
	
	public void UpdateBitField(int pieceIndex){
		bitField[pieceIndex]=true;
	}
	/*
	 * IntToByte(int num, byte[] buf, int offset);
	 */
	public void IntToByte(int num, byte[] buf, int offset) {
		buf[offset] = (byte)(num & 0xFF);
		buf[offset+1] = (byte)((num>>>8) & 0xFF);
		buf[offset+2] = (byte)((num>>>16) & 0xFF);
		buf[offset+3] = (byte)((num>>>24) & 0xFF);
	}
	
	public int ByteToInt(byte[] buf, int offset) {
		 int num = ((int)buf[offset] & 0xFF )+ ((int)(buf[offset+1] << 8)& 0xFF00 ) + ((int)(buf[offset+2] << 16) & 0xFF0000 )+ 
		 		((int)(buf[offset+3] << 24) & 0xFF000000 );
		 return num;
	}
	
	/*
	 * Returns a byte[] to send as handshake Message
	 */
	public byte[] getHandshake(int myPeerID) {
		byte Handshake[] = new byte[32];
		
		System.arraycopy(header, 0, Handshake, 0, header.length);
		IntToByte(myPeerID, Handshake, handshakeLen-4);
		return Handshake;
	}
	
    private boolean is_equals(byte[] header,byte[] hdr) {
    	if (header.length !=  hdr.length) {
                    return false;
            }
            for (int i=0; i< header.length;	i++)
		if (header[i] != hdr[i])
                            return false;
            return true;
    }
	
	/*
	 *Verifies header and returns PeerID in the Handshake 
	 *
	 *if to == true, connected to
	 *   to == false, connected from
	 */
	public int verifyHandshake(byte[] Handshake, boolean to) {	
		int PeerID=-1;
		byte hdr[] = new byte[header.length];
		
		System.arraycopy(Handshake, 0, hdr, 0, header.length);
		if (is_equals(hdr, header) == false) {
			logging.debug("Wrong Header");
			return PeerID;
		}
		PeerID= ByteToInt(Handshake, handshakeLen-4);
		
		if (to) {
			logging.log("Peer [" + Integer.toString(myPeerID) + 
					"] makes a connection to Peer [" + PeerID + "]");
		} else {
			logging.log("Peer [" + Integer.toString(myPeerID) + 
					"] is connected from Peer [" + PeerID + "]");
		}
		return PeerID;
	}
	
	/*
	 * ------------------------------------------------------------------
	 * | 	MSGLENGTH	|	MSGTYPE		|		Message Payload			|
	 * | 	(4 bytes)	|	(1 byte)	|		(0 t0 many bytes)		|
	 * | 	(0 to 3)	|	(4)			|		(5 to X)				|
	 * ------------------------------------------------------------------
	 */
	public byte[] getChoke(node nd){
		byte[] Msg = new byte[MSGLEN+1];
		IntToByte(1, Msg, 0);
		Msg[4] = CHOKE;
		nd.PeerChokeStatus = true;
		return Msg;
	}
	
	public byte[] getUnchoke(node nd){
		byte[] Msg = new byte[MSGLEN+1];
		IntToByte(1,Msg,0);
		Msg[4] = UNCHOKE;
		nd.PeerChokeStatus = false;
		return Msg;
	}
	
	public byte[] getInterested(node nd){
		byte[] Msg = new byte[MSGLEN+1];
		IntToByte(1,Msg,0);
		Msg[4] = INTERESTED;
		nd.PeerInterestStatus = true;			//Our interest status in the Peer (as stored by InterestStatus in other Peer) 
		return Msg;
	}

	public byte[] getNotInterested(node nd){
		byte[] Msg = new byte[MSGLEN+1];
		IntToByte(1,Msg,0);
		Msg[4] = NOTINTERESTED;
		nd.PeerInterestStatus = false;			//We are no more interested in peer 
		return Msg;
	}
	
	public byte[] getHave(node nd) {
		int MsgLen = 1+4;
		byte[] Msg = new byte[MSGLEN + MsgLen]; 
		IntToByte(MsgLen,Msg,0);				//Message Length
		Msg[4] = HAVE;							//Message Type
		IntToByte(nd.lastReceivedPieceID, Msg, MSGLEN+1);	//Message Payload
		logging.debug("Sending Have");
		return Msg;
	}
	
	/*
	 * Generate byte[] from boolean[]
	 */
	public byte[] generateBitfield (boolean bitField[]) {
		byte retBuf[] = new byte[bitField.length/8 + 1];
		int mask=0x80;
		for (int i=0 ; i<bitField.length ; i++) {
			if (bitField[i] == true) {
				retBuf[i/8]= (byte)(retBuf[i/8] ^ ((byte)(mask >>> (i%8))));
			}
		}
		return retBuf;
	}
	
	public byte[] getBitfield() {
		int MsgLen = (bitField.length/8 + 1) + 1; //payload + msg_type
		byte[] Msg = new byte[MSGLEN + MsgLen]; 
		byte[] psuedoBitField = generateBitfield(bitField);
		IntToByte(MsgLen,Msg,0);				//Message Length
		Msg[4] = BITFIELD;						// Message Type
		System.arraycopy(psuedoBitField, 0, Msg, MSGLEN+1, psuedoBitField.length); //Payload
		return Msg;
	}
	
	//Thsi function will be used only to search for a chunck in getRequest
	private int CheckForCompletion(int start) {
		for (int i=start; i<NumPieces ; i++) {
			if (bitField[i] == true) {
				continue;
			} 
			else{
				if (requestBitField[i] == false) {
					return i;
				}
			}
		}
		return -1;
	}

	//If it returns -1 it means it is complete
	private int CheckForCompletion() {
		for (int i=0; i<NumPieces ; i++) {
			if (bitField[i] == true) {
				continue;
			} 
			else{
				if (requestBitField[i] == false) {
					return i;
				}
			}
		}
		return -1;
	}
	 
	//Selects a random piece and makes a request packet
	/*
	 * It is expected that the user of this routine checks the file status before and after the function call
	 * THE RETURNED PACKET IS ""VALID"" IF AND ONLY IF, FileStatus IS ""FALSE"" 
	 */
	public byte[] getRequest(node nd) {
		int MsgLen = 1 + 4;
		byte[]Msg = new byte[MSGLEN + MsgLen];
		Random gen = new Random();
		int rand = gen.nextInt(NumPieces);
		int count=0;
		//logging.debug("Entering getRequest");
		while (true) {
			if (FileStatus == true) {
				logging.debug("Oh my god ..file is complete why on earth you need a request packet" +
						"Send him a dummy packet and hope he checks the FileStatus");
				return null;
			}
			if (curPieces == NumPieces) {
				int res = CheckForCompletion();
				if (res == -1) {
					logging.log("Peer [" + Integer.toString(myPeerID) + "] has downloaded the complete file");
					return null;
				}
			}
			if (nd.PendingRequests == true) {
				return null;
			}
			/* try again if:
			 *  the chunk is already present
			 *  the chunk is slaready reqquested
			 *  is not avialable at the node to which we are sending the request
			 */
			while (bitField[rand] == true || ( bitField[rand]== false && requestBitField[rand]==true ) || nd.bitField[rand] == false) {
				rand = gen.nextInt(NumPieces);
				count++;
				if (count == NumPieces/4) {
					rand = CheckForCompletion();
					if (rand == -1) {
						logging.debug("File complete");
						return null;//We have complete file
					}
					else {
						while (rand != -1 && nd.bitField[rand] == false)
							rand = CheckForCompletion(rand+1); //try again
						if (rand == -1) {
							logging.debug(myPeerID + " No valid Request possible on node " + nd.PeerID);
							return null; // no valid request is possible
						}
						else
							break;
					}
				}
			}
			if (requestBitField[rand] == false && bitField[rand] == false && nd.bitField[rand]==true) {
				requestBitField[rand] = true;
				break;
			} 
			else {
				logging.debug ("Check getRequest Control should never come here");
			}
			/*Take lock and set the bit*/
			/*synchronized(requestBitField) {	
				if (requestBitField[rand] == false && bitField[rand] == true) {
					requestBitField[rand] = true;
					break;
				}
				else {
					continue;	//Oops someone modified it..it is alright continue
				}
				}*/
		}
		IntToByte(MsgLen,Msg,0);			//Write Messaage Length
		Msg[4] = REQUEST;					//Message Type
		IntToByte(rand, Msg, MSGLEN+1);		//Payload
		logging.log("Sending Request" + rand);
		return Msg;
	}
	
	public byte[] getPiece(int pieceIndex) {
		int MsgLen = 1 + 4 + PieceSize;
		byte[] Msg = new byte[MSGLEN + MsgLen];
		IntToByte(MsgLen,Msg,0);				//Message Length
		Msg[4] = PIECE;							//Message Type
		IntToByte(pieceIndex, Msg, MSGLEN+1);	//Payload a. Piece Index

		byte[] Piece = readPiece(pieceIndex);	//Read Piece	
		// (Msg Length)0-3  (Msg Type)4  (Piece Index)5-8	(Piece data) 9-X
		System.arraycopy(Piece, 0, Msg, 9, PieceSize); // Payload b. Piece data 
		logging.log("Sending Piece");
		return Msg;
	}

	/*
	 * processMessage
	 * 0 => NO need to notify any thread
	 * +ve => Needs notification
	 */
	public int processMessage(byte[] packet, node nd) {
		int retVal=0;
		switch (packet[4]) {
		case CHOKE:
			nd.Chokestatus = true; 
			logging.log("Peer [" + Integer.toString(myPeerID) + "] is choked by " + Integer.toString(nd.PeerID));
			break;
		case UNCHOKE:
			nd.Chokestatus = false; 
			if ( nd.send_request_msg == true ) {
				retVal = 0;
			} 
			else {
				nd.send_request_msg = true;
				retVal = 1;
			}
			/* We cannot set a boolean every time a request should be sent.
									  * So the user of protocol is expected to check the Chokestatus and FileStatus and decide to send
									  * if he decides to send, he will use getRequest()
									  */
			logging.log("Peer [" + Integer.toString(myPeerID) + "] is unchoked by " + Integer.toString(nd.PeerID));
			break;
		case INTERESTED:
			nd.InterestStatus = true;
			logging.log("Peer [" + Integer.toString(myPeerID) + "] received the interested message from " 
					+ Integer.toString(nd.PeerID));
			break;
		case NOTINTERESTED:
			nd.InterestStatus = false;
			logging.log("Peer [" + Integer.toString(myPeerID) + "] received the not interested message from " 
					+ Integer.toString(nd.PeerID));
			break;
		case HAVE:
			nd.UpdateBitField(ByteToInt(packet,5), this);
			logging.log("Peer [" + Integer.toString(myPeerID) + "] received the have message from " 
					+ Integer.toString(nd.PeerID) + "for piece " + ByteToInt(packet,5));
			retVal=1;	//We might have to send a interested message
			break;
		case BITFIELD:
			nd.UpdateBitField(packet,5);
			logging.log("Peer [" + Integer.toString(myPeerID) + "] received the bitField message from " 
					+ Integer.toString(nd.PeerID));
			retVal=2;	//We might have to send a interested message 
			break;
		case REQUEST: {
			if (nd.send_piece_msg == true) {
				logging.log("Request received even before servicing the old request");
			}
			if (nd.PeerChokeStatus == true) {
				logging.debug("GOTCHA!!! - You are not supposed to send a piece request, you have been choked, still we will send you a piece in a peaceful manner");
			}
			nd.RequestpieceID = ByteToInt(packet, 5);
			nd.send_piece_msg = true;
			logging.log("Peer [" + myPeerID + "] has received a request for piece " + nd.RequestpieceID  + " from " 
				    + nd.PeerID);
			retVal=3;
			break;
		}
		case PIECE:
			byte[] pieceData = new byte[PieceSize];
			int pieceIndex = ByteToInt(packet, 5);
			if (writePiece(pieceIndex, pieceData) < 0) {
				System.out.println("Error Writing piece " + pieceIndex);
			}
			curPieces ++;
			logging.log("Peer [" + Integer.toString(myPeerID) + "] has downloaded the piece " + pieceIndex  + " from " 
					+ Integer.toString(nd.PeerID) + "Now the number of pieces it has is " + Integer.toString(curPieces));
			UpdateBitField(pieceIndex);		
			nd.notifyHaveMsg(pieceIndex);
			retVal=4;
			if (nd.RequestpieceID == pieceIndex) {
				nd.PendingRequests = false;
			} 
			else {
				logging.log ("Received a different piece than requested");
			}
			if (curPieces == NumPieces) {
				nd.send_not_interested_msg = true;
				FileStatus = true;	//Indicate that the file is complete
			}
			else {
				nd.send_request_msg=true;
			}
			break;
		default:
			break;
		}
		return retVal;
	}
}
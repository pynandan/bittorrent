import java.io.*;
import java.util.*;

public class peerProcess{
	
	public static void main(String[] args){
		int myPeerID;
		actualPeerProcess pp;
		
		try {
			myPeerID = Integer.parseInt(args[0]);
		} catch (NumberFormatException exp){
			System.out.println("Exception converting string in main" + exp.getMessage());
			return;
		}
		
		pp = new actualPeerProcess(myPeerID);
		return;
	}
	
///////////////////////////////////////////////////////////////////////////	
	public static void Test_data(int myPeerID) {
		Protocol d = new Protocol(myPeerID);
		int i,j;
		System.out.println("PeerInfo.cfg");
		System.out.println("Number of peers"+d.NumPeers);
		for (i=0 ; i < d.NumPeers ; i++) {
			System.out.println(d.PeerID[i] + " " + d.Hostname[i] + " " + d.Ports[i] + " " + d.HaveFile[i]);
		}
		System.out.println("Common.cfg");
		System.out.println(d.NumPN);
		System.out.println(d.Interval);
		System.out.println(d.OptInterval);
		System.out.println(d.FileName);
		System.out.println(d.FileSize);
		System.out.println(d.PieceSize);
		System.out.println(d.NumPieces);
		
		d.initIO();
		byte b[]=null;
		byte buffer[] = new byte[d.PieceSize];
		
		for(i=0 ; i < 20 ; i+=1) {
			b = d.readPiece(i);
			System.out.print(i+":");
			for(j=0; j< d.PieceSize ;j++){
				System.out.print(b[j]);
			}
			System.out.println("");
		}
		for(i=0 ; i < 20 ; i+=2) {
			b = d.readPiece(i);
			System.out.print(i+":");
			for(j=0; j< d.PieceSize ;j++){
				System.out.print(b[j]);
			}
			System.out.println("");
		}
		
		for(j=0 ; j<d.PieceSize ; j++)	buffer[j]='X';
		d.writePiece(4, buffer);
		
		for(j=0 ; j<d.PieceSize ; j++)	buffer[j]='Y';
		d.writePiece(7, buffer);
		
		for(j=0 ; j<d.PieceSize ; j++)	buffer[j]='Z';
		d.writePiece(15, buffer);
	
		
		for(i=0 ; i < 20 ; i+=1) {
			b = d.readPiece(i);
			System.out.print(i+":");
			for(j=0; j< d.PieceSize ;j++){
				System.out.print(b[j]);
			}
			System.out.println("");
		}	
	}
///////////////////////////////////////////////////////////
}

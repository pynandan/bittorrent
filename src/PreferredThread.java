
public class PreferredThread implements Runnable{
	
	Protocol prot;
	actualPeerProcess peerProcObj;
	Thread pref_thread;
	
	PreferredThread(Protocol prot, actualPeerProcess peerProcObj){
		this.prot = prot;
		this.peerProcObj = peerProcObj;
		
		pref_thread = new Thread(this, "preferred");
		pref_thread.start();
	}
	
	public void run(){
		prot.logging.debug("Starting PrefferedThread");
		
		try{
			/*TODO: WORKAROUND: till we figure out a clean way of choosing among partial nodes*/
			while(peerProcObj.node_array.size() != (prot.NumPeers-1))
				pref_thread.sleep(prot.Interval*1000);
			
			prot.logging.debug("Exec of PrefferedThread started");
			
			while(true){
				node preferred_list[] = new node[prot.NumPN]; //consists of the peer ids of the preferred neighbors chosen in this cycle
				
				synchronized(peerProcObj.sharedObj){
					//get the preferred list using the download rate of each peer
					int cnt=0; //Number of entries in preferred_list
					for(int i = 0; i < peerProcObj.node_array.size(); i++){
						//See if ith particular node is interested
						if (peerProcObj.node_array.get(i).InterestStatus != true) {
							continue;
						}
						
						//Fillup the preferred_list first
						if (cnt < prot.NumPN) {
							preferred_list[cnt] = peerProcObj.node_array.get(i);
							cnt++;
							continue;
						}
						//Find smallest among the preferred_list
						int smallest=0;
						for(int j = 1; j < prot.NumPN ; j++){
							if (preferred_list[smallest].DownloadRate > preferred_list[j].DownloadRate)
								smallest=j;
						}
						//If smallest is smaller than current one, then replace
						if(preferred_list[smallest].DownloadRate < peerProcObj.node_array.get(i).DownloadRate)
							preferred_list[smallest] = peerProcObj.node_array.get(i);
					}
					
					//Aggregate the peerID's of preferred neighbors
					String logStr="Peer [" + Integer.toString(prot.myPeerID) + "] has the preferred neighbours ";
					for (int j=0; (j < prot.NumPN)  && (preferred_list[j] != null); j++)
						logStr = logStr + "," + Integer.toString(preferred_list[j].PeerID);
					//Write to Log
					prot.logging.log(logStr);					
					
					//now set the send_choke_msg and ChokeMessageType for each node
					for(int i = 0; i < prot.NumPN; i++){
						/*
						 * only when the node's status in the previous cycle is choked, then set send_choke_msg = true
						 * If in the previous cycle, this node's status is unchoked, and again if it was selected in 
						 * the preferred list, dont set the send_choke_msg since it is not needed as specified in the specifications
						 */
						if(preferred_list[i] !=null && preferred_list[i].ChokeMessageType == true){
							preferred_list[i].send_choke_msg = true;
							preferred_list[i].ChokeMessageType = false;
						}
					}
					
					//now set the send_choke_msg for all the other nodes to false
					//****DO NOT CHANGE ChokeMessageType for any node 
					//reason: it will be used by preferred/optimistic thread in the next cycle
					for(int i = 0; i < peerProcObj.node_array.size(); i++){
						
						//check the preferred list for the presence of this node
						boolean found = false;
						for(int j = 0; j < prot.NumPN; j++){
							if((preferred_list[j] !=null) && peerProcObj.node_array.get(i).PeerID == preferred_list[j].PeerID){
								found = true;
								break;
							}
						}
						
						//if the current node was not found in the preferred list, then set the send_choke_msg to false
						if(found == false){
							peerProcObj.node_array.get(i).send_choke_msg = false;
						}
					}
					peerProcObj.sharedObj.notifyAll();
				}
				pref_thread.sleep(prot.Interval*1000);
			}			
		}
		catch(Exception e){
			prot.logging.debug("Exception in PrefferedThread" + e.getMessage());
			e.printStackTrace();
		}
		prot.logging.debug("Exiting PrefferedThread");
	}
	
}

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
		try{
			
			while(true){
				node preferred_list[] = new node[prot.NumPN]; //consists of the peer ids of the preferred neighbours chosen in this cycle
				
				synchronized(peerProcObj.sharedObj){
					
					//get the preferred list using the download rate of each peer
					for(int i = 0; i < peerProcObj.node_array.size(); i++){
						for(int j = 0; j < prot.NumPN; j++){
							if(preferred_list[j] == null || 
									(preferred_list[j].DownloadRate < peerProcObj.node_array.get(i).DownloadRate 
											&& peerProcObj.node_array.get(i).InterestStatus == true))
								preferred_list[j] = peerProcObj.node_array.get(i);
						}					
					}
					
					//now set the send_choke_msg and ChokeMessageType for each node
					for(int i = 0; i < prot.NumPN; i++){
						/*
						 * only when the node's status in the previous cycle is choked, then set send_choke_msg = true
						 * If in the previous cycle, this node's status is unchoked, and again if it was selected in 
						 * the preferred list, dont set the send_choke_msg since it is not needed as specified in the specifications
						 */
						if(preferred_list[i].ChokeMessageType == true){
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
							if(peerProcObj.node_array.get(i).PeerID == preferred_list[j].PeerID){
								found = true;
								break;
							}
						}
						
						//if the current node was not found in the preferred list, then set the send_choke_msg to false
						if(found == false){
							peerProcObj.node_array.get(i).send_choke_msg = false;
						}
					}
				}
				
				pref_thread.sleep(prot.Interval*1000);
			}			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
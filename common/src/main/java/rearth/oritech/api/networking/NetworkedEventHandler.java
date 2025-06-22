package rearth.oritech.api.networking;

public interface NetworkedEventHandler {
    
    // this will be called when a packet for this block has been received, and after all fields have been updated.
    void onNetworkUpdated();
    
}

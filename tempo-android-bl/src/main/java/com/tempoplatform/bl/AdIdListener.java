package com.tempoplatform.bl;

// Interface required as callback reference when completing async Ad ID request
public interface AdIdListener {
    void sendAdId(String adId);
}
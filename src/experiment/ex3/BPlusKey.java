package experiment.ex3;

import encoding.PPBitset;

public class BPlusKey {
    private PPBitset encoding;
    private long edgeTime;

    public BPlusKey(PPBitset encoding, long edgeTime) {
        this.encoding = encoding;
        this.edgeTime = edgeTime;
    }



    public PPBitset getEncoding() {
        return encoding;
    }

    public long getEdgeTime() {
        return edgeTime;
    }
}

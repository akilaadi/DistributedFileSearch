package lk.uom.dc;

import javafx.util.Pair;

import java.util.ArrayList;

public class Rank {
    private ArrayList<Pair<String, Integer>> rankValues = new ArrayList<Pair<String, Integer>>();

    public double getAvgRank() {
        int rankSum = 0;
        for (Pair<String, Integer> r : this.rankValues) {
            rankSum += r.getValue();
        }
        if(this.rankValues.size() > 0){
            return (rankSum / (double)this.rankValues.size());
        }
        else {
            return 0;
        }
    }

    public void addRank(String ranker, int rankValue) {
        boolean rankerFound = false;
        for (Pair<String, Integer> r : this.rankValues) {
            rankerFound = r.getKey().equals(ranker);
        }
        if (!rankerFound) {
            this.rankValues.add(new Pair<String, Integer>(ranker, rankValue));
        }
    }

    @Override
    public String toString() {
        return "Rank: " + this.getAvgRank();
    }
}

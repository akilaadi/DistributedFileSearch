package lk.uom.dc;

import java.util.ArrayList;

public class FileReview {
    private int fileId = 0;
    private String fileName;
    private Rank rank;
    private ArrayList<Comment> comments;

    public int getFileId() {
        return fileId;
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public FileReview(int fileId,String fileName) {
        this.rank = new Rank();
        this.comments = new ArrayList<Comment>();
        this.fileId = fileId;
        this.fileName = fileName;
    }

    public void rankReview(int rankValue, String rankerIP, int rankerPort) {
        this.rank.addRank(rankerIP + ":" + rankerPort, rankValue);
    }

    public void addComment(String commentText,String commentorAddress,int commentorPort,int lTimestamp) {
        if(this.comments.stream().anyMatch(t -> t.getCommentor().equals(commentorAddress+":"+commentorPort) && t.getlTimestamp() == lTimestamp)){
            return;
        }
        this.comments.add(new Comment(commentText,commentorAddress,commentorPort,lTimestamp));
        this.comments.sort((a,b)->Double.compare(a.getlTimestamp(),b.getlTimestamp()));
    }

    public String toString() {
        String txt = this.fileId + ":" + this.fileName + " - " + this.rank.toString();
        for (Comment comment : this.comments) {
            txt += comment.toString("");
        }
        return txt;
    }
}

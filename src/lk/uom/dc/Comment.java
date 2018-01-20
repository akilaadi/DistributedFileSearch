package lk.uom.dc;

import java.util.ArrayList;
import java.util.Random;

public class Comment {
    private int commentId;
    private String commentText;
    private String commentor;
    private int lTimestamp;
    private ArrayList<Comment> replies;
    private Rank rank;

    public String getCommentor() {
        return commentor;
    }

    public int getlTimestamp() {
        return lTimestamp;
    }

    public ArrayList<Comment> getReplies() {
        return replies;
    }

    public int getCommentId() {
        return commentId;
    }

    public String getCommentText() {
        return commentText;
    }

    public Rank getRank() {
        return rank;
    }

    public Comment(String commentText, String commentorAddress, int commentorPort, int lTimestamp) {
        this.commentId = new Random().nextInt(10000 - 1) + 1;
        this.replies = new ArrayList<Comment>();
        this.commentor = commentorAddress + ":" + commentorPort;
        this.lTimestamp = lTimestamp;
        this.rank = new Rank();
        this.commentText = commentText;
    }

    public void rankComment(int rankValue, String rankerIP, int rankerPort) {
        this.rank.addRank(rankerIP + ":" + rankerPort, rankValue);
    }

    public void addReply(String replyText, String commentorAddress, int commentorPort, int lTimestamp) {
        if (this.replies.stream().anyMatch(t -> t.getCommentor().equals(commentorAddress + ":" + commentorPort) && t.getlTimestamp() == lTimestamp)) {
            return;
        }
        this.replies.add(new Comment(replyText, commentorAddress, commentorPort, lTimestamp));
        this.replies.sort((a,b)->Double.compare(a.getlTimestamp(),b.getlTimestamp()));
    }

    public String toString(String indentSpace) {
        indentSpace += "  ";
        return "\n"+indentSpace+ this.getCommentId() + ":" + this.getCommentText() + " By:" + this.getCommentor() + " At Timestamp:" + this.getlTimestamp() + " - " + this.getRank().toString() + this.toString(indentSpace, this.replies);
    }

    private String toString(String indentSpace, ArrayList<Comment> replies) {
        String txt = "";
        for (Comment comment : replies) {
            txt += comment.toString(indentSpace);
        }
        return txt;
    }
}

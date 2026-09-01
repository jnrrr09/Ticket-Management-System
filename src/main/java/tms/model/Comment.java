package tms.model;

/** Matches the {userName, text, createdAt} shape ticket-detail.js reads
 *  when merging comments into the timeline in renderTimeline(). */
public class Comment {
    public String userName;
    public String text;
    public String createdAt;

    public Comment(String userName, String text, String createdAt) {
        this.userName = userName;
        this.text = text;
        this.createdAt = createdAt;
    }
}

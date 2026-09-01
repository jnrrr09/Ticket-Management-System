package tms.model;

/** Matches the {type, at, text} shape ticket-detail.js reads in renderTimeline(). */
public class HistoryEvent {
    public String type;
    public String at;
    public String text;

    public HistoryEvent(String type, String at, String text) {
        this.type = type;
        this.at = at;
        this.text = text;
    }
}

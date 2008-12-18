package fi.hut.soberit.agilefant.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.hut.soberit.agilefant.model.AFTime;
import fi.hut.soberit.agilefant.model.Backlog;

/**
 * Carrier class for load data for a backlog to Daily Work page.
 * Values are stored to maps with week numbers as keys.
 * 
 * @author rjokelai
 */
public class BacklogLoadData {
    
    // The backlog, whose effort is shown
    private Backlog backlog;
    
    // Week list
    private List<Integer> weekNumbers = new ArrayList<Integer>();
    
    // Weekly efforts
    private Map<Integer, AFTime> efforts = new HashMap<Integer, AFTime>();
    
    // Weekly overheads
    private Map<Integer, AFTime> overheads = new HashMap<Integer, AFTime>();
    
    // Weekly totals (effortLeft + overhead)
    private Map<Integer, AFTime> weeklyTotals = new HashMap<Integer, AFTime>();
    
    // Total effort left in backlog
    private AFTime totalEffort = new AFTime(0);
    
    // Total overhead (overhead * number of weeks)
    private AFTime totalOverhead = new AFTime(0);
    
    // Absolute total = totalEffort + totalOverhead
    private AFTime absoluteTotal = new AFTime(0);
    
    private boolean unestimatedItems = false;
    
    
    /*
     * Autogenerated list of setters and getters.
     */
    
    public Backlog getBacklog() {
        return backlog;
    }

    public void setBacklog(Backlog backlog) {
        this.backlog = backlog;
    }

    public Map<Integer, AFTime> getEfforts() {
        return efforts;
    }

    public void setEfforts(Map<Integer, AFTime> efforts) {
        this.efforts = efforts;
    }

    public Map<Integer, AFTime> getWeeklyTotals() {
        return weeklyTotals;
    }

    public void setWeeklyTotals(Map<Integer, AFTime> weeklyTotals) {
        this.weeklyTotals = weeklyTotals;
    }

    public AFTime getTotalEffort() {
        return totalEffort;
    }

    public void setTotalEffort(AFTime totalEffort) {
        this.totalEffort = totalEffort;
    }

    public AFTime getTotalOverhead() {
        return totalOverhead;
    }

    public void setTotalOverhead(AFTime totalOverhead) {
        this.totalOverhead = totalOverhead;
    }

    public boolean isUnestimatedItems() {
        return unestimatedItems;
    }

    public void setUnestimatedItems(boolean unestimatedItems) {
        this.unestimatedItems = unestimatedItems;
    }

    public List<Integer> getWeekNumbers() {
        return weekNumbers;
    }

    public void setWeekNumbers(List<Integer> weekNumbers) {
        this.weekNumbers = weekNumbers;
    }

    public Map<Integer, AFTime> getOverheads() {
        return overheads;
    }

    public void setOverheads(Map<Integer, AFTime> overheads) {
        this.overheads = overheads;
    }

    public AFTime getAbsoluteTotal() {
        return absoluteTotal;
    }

    public void setAbsoluteTotal(AFTime absoluteTotal) {
        this.absoluteTotal = absoluteTotal;
    }
}

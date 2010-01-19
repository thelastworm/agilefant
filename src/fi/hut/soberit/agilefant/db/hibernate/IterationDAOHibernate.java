package fi.hut.soberit.agilefant.db.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import fi.hut.soberit.agilefant.db.IterationDAO;
import fi.hut.soberit.agilefant.model.Iteration;
import fi.hut.soberit.agilefant.model.Story;
import fi.hut.soberit.agilefant.model.StoryState;
import fi.hut.soberit.agilefant.model.Task;
import fi.hut.soberit.agilefant.model.TaskState;
import fi.hut.soberit.agilefant.model.User;
import fi.hut.soberit.agilefant.util.Pair;

/**
 * Hibernate implementation of IterationDAO interface using GenericDAOHibernate.
 */
@Repository("iterationDAO")
public class IterationDAOHibernate extends GenericDAOHibernate<Iteration>
        implements IterationDAO {

    public IterationDAOHibernate() {
        super(Iteration.class);
    }

    @SuppressWarnings("unchecked")
    public Collection<Task> getTasksWithoutStoryForIteration(Iteration iteration) {
        DetachedCriteria crit = DetachedCriteria.forClass(Task.class);
        crit.add(Restrictions.eq("iteration", iteration));
        crit.add(Restrictions.isNull("story"));
        return hibernateTemplate.findByCriteria(crit);
    }

    private Criteria addIterationRestriction(Criteria criteria,
            Collection<String> joins, Iteration iteration) {
        for (String join : joins)
            criteria = criteria.createCriteria(join);
        criteria.add(Restrictions.idEq(iteration.getId()));
        return criteria;
    }

    public List<Task> getAllTasksForIteration(Iteration iteration) {
        Criteria storyTaskCrit = getCurrentSession().createCriteria(Task.class);
        storyTaskCrit.setFetchMode("iteration",FetchMode.SELECT);
        storyTaskCrit = storyTaskCrit.createCriteria("story").createCriteria("backlog");
        storyTaskCrit.add(Restrictions.idEq(iteration.getId()));
        
        Criteria tasksWoStoryCrit = getCurrentSession().createCriteria(Task.class);
        tasksWoStoryCrit.setFetchMode("story", FetchMode.SELECT);
        tasksWoStoryCrit = tasksWoStoryCrit.createCriteria("iteration");
        tasksWoStoryCrit.add(Restrictions.idEq(iteration.getId()));
        
        List<Task> tasks = new ArrayList<Task>();
        List<Task> storyTasks = asList(storyTaskCrit);
        tasks.addAll(storyTasks);
        List<Task> iterationTasks = asList(tasksWoStoryCrit);
        tasks.addAll(iterationTasks);

        return tasks;
    }

    public Map<StoryState, Integer> countIterationStoriesByState(int iterationId) {
        Criteria criteria = getCurrentSession().createCriteria(Story.class);
        criteria.add(Restrictions.eq("backlog.id", iterationId));
        criteria.setProjection(Projections.projectionList().add(
                Projections.property("state")).add(Projections.rowCount(),
                "storyCount").add(Projections.groupProperty("state"), "state"));

        Map<StoryState, Integer> results = new EnumMap<StoryState, Integer>(
                StoryState.class);

        for (StoryState state : StoryState.values()) {
            results.put(state, 0);
        }

        List<Object[]> queryResults = asList(criteria);

        for (Object[] row : queryResults) {
            StoryState state = (StoryState) row[0];
            Integer count = (Integer) row[1];
            results.put(state, count);
        }

        return results;
    }

    private Pair<Integer, Integer> getCounOfDoneAndAll(Class<?> type,
            Object doneValue, Collection<String> joins, Iteration iteration) {
        Criteria criteria = getCurrentSession().createCriteria(type);
        criteria.setProjection(Projections.projectionList().add(
                Projections.property("state")).add(Projections.rowCount(),
                "taskCount").add(Projections.groupProperty("state"), "state"));
        criteria = addIterationRestriction(criteria, joins, iteration);

        List<Object[]> results = asList(criteria);

        int total = 0;
        int done = 0;

        for (Object[] row : results) {
            Integer count = (Integer) row[1];
            total += count;
            if (row[0].equals(doneValue))
                done += count;
        }

        return Pair.create(done, total);
    }

    public Pair<Integer, Integer> getCountOfDoneAndAllTasks(Iteration iteration) {
        Pair<Integer, Integer> noStory = getCounOfDoneAndAll(Task.class,
                TaskState.DONE, Arrays.asList("iteration"), iteration);
        Pair<Integer, Integer> inStory = getCounOfDoneAndAll(Task.class,
                TaskState.DONE, Arrays.asList("story", "backlog"), iteration);
        return Pair.create(noStory.first + inStory.first, noStory.second
                + inStory.second);
    }

    public Pair<Integer, Integer> getCountOfDoneAndAllStories(
            Iteration iteration) {
        return getCounOfDoneAndAll(Story.class, StoryState.DONE, Arrays
                .asList("backlog"), iteration);
    }

    public List<Iteration> retrieveIterationsByIds(Set<Integer> iterationIds) {
        if (iterationIds == null || iterationIds.size() == 0) {
            return Collections.emptyList();
        }
        Criteria crit = getCurrentSession().createCriteria(Iteration.class);
        crit.add(Restrictions.in("id", iterationIds));
        return asList(crit);
    }

    public Map<Integer, Integer> getTotalAvailability(Set<Integer> iterationIds) {
        if (iterationIds == null || iterationIds.size() == 0) {
            return Collections.emptyMap();
        }
        Criteria crit = getCurrentSession().createCriteria(Iteration.class);
        crit.add(Restrictions.in("id", iterationIds));
        crit.createAlias("assignments", "assignments");
        crit.setProjection(Projections.projectionList().add(
                Projections.groupProperty("id")).add(
                Projections.sum("assignments.availability")));
        List<Object[]> data = asList(crit);
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (Object[] row : data) {
            result.put((Integer) row[0], (Integer) row[1]);
        }
        return result;
    }

    public List<Iteration> retrieveEmptyIterationsWithPlannedSize(
            DateTime startDate, DateTime endDate, User assignee) {
        Criteria crit = getCurrentSession().createCriteria(Iteration.class);

        // interval limitations
        Criterion startDateLimit = Restrictions.between("startDate", startDate,
                endDate);
        // iteration end during the interval
        Criterion endDateLimit = Restrictions.between("endDate", startDate,
                endDate);
        // interval may be within the iteration
        Criterion overlaps = Restrictions.or(startDateLimit, endDateLimit);
        Criterion withinIteration = Restrictions.and(Restrictions.le(
                "startDate", startDate), Restrictions.ge("endDate", endDate));
        crit.add(Restrictions.or(overlaps, withinIteration));

        // limit by assignee
        crit.createCriteria("assignments").createCriteria("user").add(
                Restrictions.eq("id", assignee.getId()));

        // must have planned size set
        crit.add(Restrictions.isNotNull("backlogSize"));

        // must be empty
        crit.add(Restrictions.isEmpty("stories"));
        crit.add(Restrictions.isEmpty("tasks"));

        return asList(crit);
    }

    public List<Iteration> retrieveCurrentAndFutureIterationsAt(DateTime point) {

        Criteria crit = getCurrentSession().createCriteria(Iteration.class);
        crit.add(Restrictions.ge("endDate", point));
        return asList(crit);
    }

    public Iteration retrieveDeep(int iterationId) {
        Criteria crit = getCurrentSession().createCriteria(Iteration.class);

        Criteria iterationTasksCrit = crit.createCriteria("tasks", CriteriaSpecification.LEFT_JOIN);

        iterationTasksCrit.setFetchMode("responsibles", FetchMode.JOIN);
        iterationTasksCrit.setFetchMode("whatsNextEntries", FetchMode.JOIN);

        Criteria storiesCrit = crit.createCriteria("stories", CriteriaSpecification.LEFT_JOIN);

        storiesCrit.setFetchMode("labels", FetchMode.JOIN);

        Criteria storyTasksCrit = storiesCrit.createCriteria("tasks", CriteriaSpecification.LEFT_JOIN);

        storyTasksCrit.setFetchMode("responsibles", FetchMode.JOIN);
        storyTasksCrit.setFetchMode("whatsNextEntries", FetchMode.JOIN);

        crit.add(Restrictions.idEq(iterationId));

        crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return (Iteration) crit.uniqueResult();
    }

    public List<Iteration> retrieveActiveWithUserAssigned(int userId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria crit = session.createCriteria(Iteration.class);
        crit.setFetchMode("parent", FetchMode.JOIN);
        crit.add(Restrictions.gt("endDate", new DateTime()));
        crit = crit.createCriteria("assignments");
        crit = crit.createCriteria("user");
        crit.add(Restrictions.idEq(userId));
        return asList(crit);
    }

}

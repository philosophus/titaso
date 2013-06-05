package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.Timeslot;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class HyperHallSeperator extends GRBCallback {

   private GRBEnv env;
   private GRBModel model;
   private Map<TimeslotGroup, GRBVar> variables;
   private Instance instance;

   public HyperHallSeperator(GRBEnv env, GRBModel model, Instance instance, Map<TimeslotGroup, GRBVar> variables) {
      this.env = env;
      this.model = model;
      this.instance = instance;
      this.variables = variables;
   }

   @Override
   protected void callback() {
      if (this.where == GRB.CB_MIPNODE) {
      }
      if (this.where == GRB.CB_MIPSOL) {
         try {
            optimallySeperateIntegralSolution();
         } catch (GRBException ex) {
            ex.printStackTrace();
            Logger.getLogger(HyperHallSeperator.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
         }
      }
   }

   private void optimallySeperateIntegralSolution() throws GRBException {
      // retrieve current solution
      Map<Event, TimeslotGroup> assignedTimeslots = new HashMap();
      for (Map.Entry<TimeslotGroup, GRBVar> variable : variables.entrySet()) {
         if (getSolution(variable.getValue()) > 0.5) {
            assignedTimeslots.put(variable.getKey().getEvent(), variable.getKey());
         }
      }

      // solve sub problem
      SimpleMatcher matcher = new SimpleMatcher(instance, assignedTimeslots);
      matcher.setUpAndCreateModel(new GRBEnv());
      matcher.solve();
      Set<Event> unmatchedEvents = matcher.getUnmatchedEvents();

      while (!unmatchedEvents.isEmpty()) {
         Event anyEvent = unmatchedEvents.iterator().next();
         Set<TimeslotGroup> connectedTimeslots = getConnectedTimeslots(assignedTimeslots.get(anyEvent), assignedTimeslots.values());

         // remove unmatched events in the connected component
         int removedEvents = 0;
         for (TimeslotGroup assignedTimeslot : connectedTimeslots) {
            if (unmatchedEvents.remove(assignedTimeslot.getEvent())) {
               ++removedEvents;
            }
         }

         // create new constraint
         GRBLinExpr expr = new GRBLinExpr();
         for (TimeslotGroup assignedTimeslot : connectedTimeslots) {
            assert (variables.containsKey(assignedTimeslot));
            expr.addTerm(1.0, variables.get(assignedTimeslot));
         }
         addLazy(expr, GRB.LESS_EQUAL, connectedTimeslots.size() - removedEvents);
      }

   }

   private Map<Timeslot, Set<TimeslotGroup>> createTimeslotToTimeslotGroupMap(Collection<TimeslotGroup> world) {
      Map<Timeslot, Set<TimeslotGroup>> timeslotToTimeslotGroupMap = new HashMap();

      for (TimeslotGroup current : world) {
         for (Timeslot timeslot : current.getTimeslots()) {
            if (!timeslotToTimeslotGroupMap.containsKey(timeslot)) {
               timeslotToTimeslotGroupMap.put(timeslot, new HashSet());
            }
            timeslotToTimeslotGroupMap.get(timeslot).add(current);
         }
      }

      return timeslotToTimeslotGroupMap;
   }

   private Set<TimeslotGroup> getConnectedTimeslots(TimeslotGroup startPoint, Collection<TimeslotGroup> world) {
      Set<TimeslotGroup> connectedTimeslots = new HashSet();

      Map<Timeslot, Set<TimeslotGroup>> timeslotToTimeslotGroupMap = createTimeslotToTimeslotGroupMap(world);

      Queue<Timeslot> toConsider = new LinkedList(startPoint.getTimeslots());
      Set<Timeslot> considered = new HashSet();

      while (!toConsider.isEmpty()) {
         Timeslot nextTimeslot = toConsider.poll();
         considered.add(nextTimeslot);

         // TODO: Also check if the two timeslotgroups share possible rooms
         Set<TimeslotGroup> connecting = timeslotToTimeslotGroupMap.get(nextTimeslot);
         connectedTimeslots.addAll(connecting);

         // collect other connected timeslots
         Set<Timeslot> otherTimeslots = new HashSet();
         for (TimeslotGroup connectingGroup : connecting) {
            otherTimeslots.addAll(connectingGroup.getTimeslots());
         }
         otherTimeslots.removeAll(considered);
         toConsider.addAll(otherTimeslots);
      }

      return connectedTimeslots;
   }
}

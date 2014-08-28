package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.EventGroup;
import de.rwthaachen.hyperhallsolver.model.EventsRoomsEdge;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.Room;
import de.rwthaachen.hyperhallsolver.model.RoomGroup;
import de.rwthaachen.hyperhallsolver.model.StableRoomGroup;
import de.rwthaachen.hyperhallsolver.model.Timeslot;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class SimpleMatcher {

   private Instance instance;
   private Map<Event, TimeslotGroup> assignedTimeslots;
   private Map<Timeslot, List<EventGroup>> assignedAtTimeslot;
   private GRBEnv env;
   private GRBModel model;
   private Map<EventsRoomsEdge, GRBVar> variables;
   private Map<EventGroup, GRBConstr> eventMatchingConstaints;
   private Map<Room, Map<Timeslot, GRBConstr>> roomConflicts;
   public  List<EventGroup> eventGroups;

   public SimpleMatcher(Instance instance, Map<Event, TimeslotGroup> assignedTimeslots) {
      this.instance = instance;
      this.assignedTimeslots = assignedTimeslots;
   }

   private void calculateAssignedTimeslotsAt() {
      assert (assignedTimeslots != null);

      this.assignedAtTimeslot = new HashMap();
      for (EventGroup eg: this.eventGroups) {
         for (Event e: eg.events) {
            if (this.assignedTimeslots.containsKey(e)) {   // Making sure the event was assigned in the coloring problem
               for (Timeslot t: this.assignedTimeslots.get(e).getTimeslots()) {
                  if (!assignedAtTimeslot.containsKey(t)) {
                     assignedAtTimeslot.put(t, new ArrayList());
                  }
                  assignedAtTimeslot.get(t).add(eg);
               }
            }
         }
      }
   }

   public void setUp(GRBEnv env) throws GRBException {
      this.env = env;
      env.set(GRB.IntParam.OutputFlag, 0);
      model = new GRBModel(env);
   }

   public void createVariables() throws GRBException {
      assert (env != null);
      assert (model != null);

      variables = new HashMap();
      for (EventGroup eventGroup : this.eventGroups) {
         for (List<Room> possibleRooms : eventGroup.getPossibleRoomGroups()) {
            EventsRoomsEdge ere = new EventsRoomsEdge(eventGroup, possibleRooms);
            GRBVar var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ere.getId());
            eventGroup.eventsRoomsEdges.add(ere);
            variables.put(ere, var);
         }
      }

      model.update();
   }

   public void createEventMatchingConstraints() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      this.eventMatchingConstaints = new HashMap();
      for (EventGroup eventGroup : this.eventGroups) {
         String consName = "Assign Events " + eventGroup.getId() + " at most onece";
         GRBLinExpr expr = new GRBLinExpr();
         for (EventsRoomsEdge ere : eventGroup.eventsRoomsEdges) {
            expr.addTerm(1.0, variables.get(ere));
         }
         GRBConstr cons = model.addConstr(expr, GRB.LESS_EQUAL, 1.0, consName);
         this.eventMatchingConstaints.put(eventGroup, cons);
      }
   }

   public void createRoomConflictConstraints() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      roomConflicts = new HashMap();
      for (Room room : instance.getRooms()) {
         roomConflicts.put(room, new HashMap());
         for (Timeslot timeslot : instance.getTimeslots()) {
            String consName = "No two events in room " + room.getId() + " at time " + timeslot.getId();

            GRBLinExpr expr = new GRBLinExpr();
            // ToDo: Check if this is not too inefficient
            if (this.assignedAtTimeslot.containsKey(timeslot)) {
               for (EventGroup eg : this.assignedAtTimeslot.get(timeslot)) {
                  for (EventsRoomsEdge ere: eg.eventsRoomsEdges) {
                     if (ere.getRooms().contains(room)) {
                        expr.addTerm(1.0, variables.get(ere));
                     }
                  }
               }
            }
            GRBConstr cons = model.addConstr(expr, GRB.LESS_EQUAL, 1.0, consName);
            roomConflicts.get(room).put(timeslot, cons);
         }
      }
   }

   public void setObjective() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      GRBLinExpr obj = new GRBLinExpr();
      for (GRBVar var : this.variables.values()) {
         obj.addTerm(1.0, var);
      }
      model.setObjective(obj, GRB.MAXIMIZE);
      model.update();
   }

   public void solve() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      model.optimize();
   }

   public void groupEvents() {
      this.eventGroups = new ArrayList();
      Set<Event> events = new HashSet(instance.getEvents());
      for (StableRoomGroup srg : instance.getStableRoomGroups()) {
         EventGroup eg = new EventGroup(srg.getEvents());
         this.eventGroups.add(eg);
         events.removeAll(eg.events);
      }
      for (Event e : events) {
         this.eventGroups.add(new EventGroup(e));
      }
   }

   public void setUpAndCreateModel(GRBEnv env) throws GRBException {
      setUp(env);
      groupEvents();
      calculateAssignedTimeslotsAt();
      createVariables();
      createEventMatchingConstraints();
      createRoomConflictConstraints();
      setObjective();
   }

   public Set<Event> getUnmatchedEvents() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      Set<Event> unmatchedEvents = new HashSet(this.instance.getEvents());
      for (Map.Entry<EventsRoomsEdge, GRBVar> variable : this.variables.entrySet()) {
         if (variable.getValue().get(GRB.DoubleAttr.X) > 0.5) {
            for (Event e : variable.getKey().getEventGroup().events) {
               unmatchedEvents.remove(e);
            }
         }
      }

      return unmatchedEvents;
   }

   public Map<Event, RoomGroup> getSolution() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      Map<Event, RoomGroup> solution = new HashMap();
      for (Map.Entry<EventsRoomsEdge, GRBVar> variable : this.variables.entrySet()) {
         if (variable.getValue().get(GRB.DoubleAttr.X) > 0.5) {
            for (Event e : variable.getKey().getEventGroup().events) {
               for (RoomGroup rg : e.getPossibleRooms()) {
                  if (CollectionUtils.isEqualCollection(rg.getRooms(), variable.getKey().getRooms())) {
                     solution.put(e, rg);
                  }
               }
            }
         }
      }

      return solution;
   }
}

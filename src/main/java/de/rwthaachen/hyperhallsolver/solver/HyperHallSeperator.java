package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.Room;
import de.rwthaachen.hyperhallsolver.model.RoomGroup;
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
//         Set<TimeslotGroup> connectedTimeslots = getConnectedTimeslots(assignedTimeslots.get(anyEvent), assignedTimeslots.values());
         Set<TimeslotGroup> connectedTimeslots = getConnectedTimeslots(anyEvent, assignedTimeslots, matcher.getSolution());
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
         System.out.println("Created lazy constraint");
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

   private Set<TimeslotGroup> getConnectedTimeslots(Event startEvent, Map<Event, TimeslotGroup> assignedTimeslots, Map<Event, RoomGroup> assignedRooms) {
      Queue<Event> toConsider = new LinkedList();
      toConsider.add(startEvent);
      Set<Event> considered = new HashSet();
      Set<TimeslotGroup> result = new HashSet();
      result.add(assignedTimeslots.get(startEvent));

      Map<Timeslot, Set<TimeslotGroup>> timeslotToTimeslotGroupMap = createTimeslotToTimeslotGroupMap(assignedTimeslots.values());

      while (!toConsider.isEmpty()) {
         Event current = toConsider.poll();
         Set<Room> currentsPossibleRooms = new HashSet();
         for (RoomGroup rooms : current.getPossibleRooms()) {
            currentsPossibleRooms.addAll(rooms.getRooms());
         }

         considered.add(current);

         Set<Event> connecting = new HashSet();
         for (Timeslot timeslot : assignedTimeslots.get(current).getTimeslots()) {
            for (TimeslotGroup assignedTimeslot : timeslotToTimeslotGroupMap.get(timeslot)) {
               RoomGroup assignedRoom = assignedRooms.get(assignedTimeslot.getEvent());
               if (assignedRoom != null) {
                  for (Room room : assignedRoom.getRooms()) {
                     if (currentsPossibleRooms.contains(room)) {
                        connecting.add(assignedTimeslot.getEvent());
                        result.add(assignedTimeslot);
                     }
                  }
               } else { // TODO: Check if this is an improvement or makes things worse
                  for (RoomGroup possibleRoomGroup : assignedTimeslot.getEvent().getPossibleRooms()) {
                     for (Room room : possibleRoomGroup.getRooms()) {
                        if (currentsPossibleRooms.contains(room)) {
                           connecting.add(assignedTimeslot.getEvent());
                           result.add(assignedTimeslot);
                        }
                     }
                  }
               }
            }
         }

         connecting.removeAll(considered);
         toConsider.addAll(connecting);
      }

      return result;
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

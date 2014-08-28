package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.Room;
import de.rwthaachen.hyperhallsolver.model.RoomGroup;
import de.rwthaachen.hyperhallsolver.model.StableRoomGroup;
import de.rwthaachen.hyperhallsolver.model.Timeslot;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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
      Map<Event, RoomGroup> solution = matcher.getSolution();

      // Set up graph for connected components search
      ConnectedComponents cc = new ConnectedComponents();
      // Add vertices
      cc.addVertices(this.instance.getEvents());
      for (Room r : this.instance.getRooms()) {
         for (Timeslot t : this.instance.getTimeslots()) {
            Pair<Room, Timeslot> roomTime = new ImmutablePair(r, t);
            cc.addVertex(roomTime);
         }
      }
      // Add edges
      for (Map.Entry<Event, RoomGroup> solutionElt : solution.entrySet()) {
         for (Room room : solutionElt.getValue().getRooms()) {
            for (Timeslot timeslot : assignedTimeslots.get(solutionElt.getKey()).getTimeslots()) {
               Pair<Room, Timeslot> roomTime = new ImmutablePair(room, timeslot);
               cc.addEdge(solutionElt.getKey(), roomTime);
            }
         }
      }
      for (Event e : solution.keySet()) {
         for (RoomGroup rg : e.getPossibleRooms()) {
            for (Room r : rg.getRooms()) {
               for (Timeslot t : assignedTimeslots.get(e).getTimeslots()) {
                  Pair<Room, Timeslot> roomTime = new ImmutablePair(r, t);
                  cc.addEdge(roomTime, e);
               }
            }
         }
      }
      for (Event e : unmatchedEvents) {
         for (RoomGroup rg : e.getPossibleRooms()) {
            for (Room r : rg.getRooms()) {
               for (Timeslot t : assignedTimeslots.get(e).getTimeslots()) {
                  Pair<Room, Timeslot> roomTime = new ImmutablePair(r, t);
                  cc.addEdge(e, roomTime);
               }
            }
         }
      }
      for (StableRoomGroup srg : this.instance.getStableRoomGroups()) {
         Iterator<Event> eventIter = srg.getEvents().iterator();
         Event current = eventIter.next();
         while (eventIter.hasNext()) {
            Event last = current;
            current = eventIter.next();
            cc.addEdge(last, current);
            cc.addEdge(current, last);
         }
      }

      // Find connected components and create constraints
      while (!unmatchedEvents.isEmpty()) {
         Event anyEvent = unmatchedEvents.iterator().next();

         List<Object> component = cc.getConnectedComponent(anyEvent);
         List<TimeslotGroup> connectedTimeslots = new ArrayList();
         for (Object o : component) {
            if (o instanceof Event) {
               connectedTimeslots.add(assignedTimeslots.get((Event) o));
            }
         }

//         Set<TimeslotGroup> connectedTimeslots = getConnectedTimeslots(assignedTimeslots.get(anyEvent), assignedTimeslots.values());
//         Set<TimeslotGroup> connectedTimeslots = getConnectedTimeslots(anyEvent, assignedTimeslots, matcher.getSolution());
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
}

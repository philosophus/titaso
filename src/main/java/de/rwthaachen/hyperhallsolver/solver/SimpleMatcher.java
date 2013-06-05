package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.Room;
import de.rwthaachen.hyperhallsolver.model.RoomGroup;
import de.rwthaachen.hyperhallsolver.model.Timeslot;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class SimpleMatcher {

   private Instance instance;
   private Map<TimeslotGroup, Event> assignedTimeslots;
   private Map<Timeslot, Set<TimeslotGroup>> assignedTimeslotsAt;
   private GRBEnv env;
   private GRBModel model;
   private Map<RoomGroup, GRBVar> variables;
   private Map<Event, GRBConstr> shallBeMatchedConstrains;
   private Map<Event, GRBVar> shallBeMatchedVariables;
   private Map<Room, Map<Timeslot, GRBConstr>> roomConflicts;

   public SimpleMatcher(Instance instance, Map<TimeslotGroup, Event> assignedTimeslots) {
      this.instance = instance;
      this.assignedTimeslots = assignedTimeslots;

      calculateAssignedTimeslotsAt();
   }

   private void calculateAssignedTimeslotsAt() {
      assert (assignedTimeslots != null);

      assignedTimeslotsAt = new HashMap();
      for (TimeslotGroup possibleTimeslot : assignedTimeslots.keySet()) {
         for (Timeslot timeslot : possibleTimeslot.getTimeslots()) {
            if (!assignedTimeslotsAt.containsKey(timeslot)) {
               assignedTimeslotsAt.put(timeslot, new HashSet());
            }
            assignedTimeslotsAt.get(timeslot).add(possibleTimeslot);
         }
      }
   }

   public void setUp(GRBEnv env) throws GRBException {
      this.env = env;
      model = new GRBModel(env);
   }

   public void createVariables() throws GRBException {
      assert (env != null);
      assert (model != null);

      variables = new HashMap();
      for (Event event : instance.getEvents()) {
         for (RoomGroup possibleRooms : event.getPossibleRooms()) {
            String varName = "Event " + event.getId() + " in rooms";
            for (Room room : possibleRooms.getRooms()) {
               varName += " " + room.getId();
            }
            GRBVar var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);

            variables.put(possibleRooms, var);
         }
      }

      model.update();
   }

   public void createShallBeMatchedConstraintsAndVariables() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      this.shallBeMatchedVariables = new HashMap();
      this.shallBeMatchedConstrains = new HashMap();
      for (Event event : instance.getEvents()) {
         // create variable taking value 1 if the event is not assigned
         String varName = "Event " + event.getId() + " was not assigned";
         GRBVar var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
         this.shallBeMatchedVariables.put(event, var);
      }
      model.update();
      for (Event event : instance.getEvents()) {
         String consName = "Assign Event " + event.getId() + " if possible";
         GRBLinExpr expr = new GRBLinExpr();
         expr.addTerm(1.0, this.shallBeMatchedVariables.get(event));
         for (RoomGroup possibleRooms : event.getPossibleRooms()) {
            expr.addTerm(1.0, variables.get(possibleRooms));
         }
         GRBConstr cons = model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, consName);
         this.shallBeMatchedConstrains.put(event, cons);
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
            if (assignedTimeslotsAt.containsKey(timeslot)) {
               for (TimeslotGroup assignedTimeslot : assignedTimeslotsAt.get(timeslot)) {
                  for (RoomGroup possibleRooms : assignedTimeslot.getEvent().getPossibleRooms()) {
                     if (possibleRooms.getRooms().contains(room)) {
                        expr.addTerm(1.0, variables.get(possibleRooms));
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
      for (GRBVar var : this.shallBeMatchedVariables.values()) {
         obj.addTerm(1.0, var);
      }
      model.setObjective(obj, GRB.MINIMIZE);
      model.update();
   }

   public void solve() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      model.optimize();

      for (GRBVar var : model.getVars()) {
         if (var.get(GRB.DoubleAttr.X) != 0.0) {
            System.out.println(var.get(GRB.StringAttr.VarName) + ": " + var.get(GRB.DoubleAttr.Obj));
         }
      }
   }
}

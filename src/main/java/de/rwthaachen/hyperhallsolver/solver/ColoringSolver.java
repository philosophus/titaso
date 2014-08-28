package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.StableRoomGroup;
import de.rwthaachen.hyperhallsolver.model.TimeConflict;
import de.rwthaachen.hyperhallsolver.model.Timeslot;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class ColoringSolver {

   private Instance instance;
   private Map<Timeslot, Set<TimeslotGroup>> possibleTimeslotsAt;
   private GRBEnv env;
   private GRBModel model;
   private Map<TimeslotGroup, GRBVar> variables;
   private Map<Event, GRBConstr> boundEventOccurenceConstraints;
   private Map<TimeConflict, Set<GRBConstr>> strictTimeConflictConstraints;
   private Map<TimeConflict, Set<GRBConstr>> softTimeConflictConstraints;
   private Map<TimeConflict, Set<GRBVar>> softTimeConflictVariables;
   private HyperHallSeparator hyperHallSeperator;

   public ColoringSolver(Instance instance) {
      this.instance = instance;

      calculatePossibleTimeslotsAt();
   }

   private void calculatePossibleTimeslotsAt() {
      possibleTimeslotsAt = new HashMap();
      for (Timeslot timeslot : instance.getTimeslots()) {
         possibleTimeslotsAt.put(timeslot, new HashSet());
      }

      for (Event event : instance.getEvents()) {
         for (TimeslotGroup possibleTimeslot : event.getPossibleTimeslots()) {
            for (Timeslot timeslot : possibleTimeslot.getTimeslots()) {
//               if (!possibleTimeslotsAt.containsKey(timeslot)) {
//                  possibleTimeslotsAt.put(timeslot, new HashSet());
//               }
               possibleTimeslotsAt.get(timeslot).add(possibleTimeslot);
            }
         }
      }
   }

   public void setUp(String logfilename) throws GRBException {
      assert (logfilename != null);
      assert (logfilename.length() >= 1);

      env = new GRBEnv(logfilename);
      env.set(GRB.DoubleParam.TimeLimit, 1200);
      env.set(GRB.IntParam.LazyConstraints, 1);
      model = new GRBModel(env);
   }

   public void createVaribles() throws GRBException {
      assert (env != null);
      assert (model != null);

      variables = new HashMap();
      for (Event event : instance.getEvents()) {
         for (TimeslotGroup possibleTimeslot : event.getPossibleTimeslots()) {
            String varName = "Event " + event.getId() + " at time";
            for (Timeslot timeslot : possibleTimeslot.getTimeslots()) {
               varName += " " + timeslot.getId();
            }
            GRBVar var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);

            variables.put(possibleTimeslot, var);
         }
      }

      // Integrate variables
      model.update();
   }

   public void createBoundEventOccurenceConstraints() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      this.boundEventOccurenceConstraints = new HashMap();
      for (Event event : instance.getEvents()) {
         GRBLinExpr expr = new GRBLinExpr();
         for (TimeslotGroup possibleTimeslot : event.getPossibleTimeslots()) {
            assert (variables.containsKey(possibleTimeslot));
            expr.addTerm(1.0, variables.get(possibleTimeslot));
         }
         String consName = "No two appointments for Event " + event.getId();
         GRBConstr constraint = model.addConstr(expr, GRB.LESS_EQUAL, 1.0, consName);
         this.boundEventOccurenceConstraints.put(event, constraint);
      }
   }

   private Set<Set<TimeslotGroup>> getCollidingTimeslots(Collection<Event> events) {
      Set<Set<TimeslotGroup>> collidingTimeslots = new HashSet();
      for (Timeslot timeslot : instance.getTimeslots()) {
         Set<TimeslotGroup> conflictGroup = new HashSet();
         for (TimeslotGroup possibleTimeslot : this.possibleTimeslotsAt.get(timeslot)) {
            if (events.contains(possibleTimeslot.getEvent())) {
               conflictGroup.add(possibleTimeslot);
            }
         }
         if (conflictGroup.size() >= 2) {   // No need to put a timeslot in conflict with itself
            collidingTimeslots.add(conflictGroup);
         }
      }

      return collidingTimeslots;
   }

   public void createStrictTimeConflictConstraints() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      this.strictTimeConflictConstraints = new HashMap();
      for (TimeConflict timeConflict : instance.getStrictTimeConflicts()) {
         // Collect set of colliding time slots
         Set<Set<TimeslotGroup>> collidingTimeslots = getCollidingTimeslots(timeConflict.getEvents());

         // Create the conflicts
         int i = 0;
         Set<GRBConstr> conss = new HashSet();
         for (Set<TimeslotGroup> conflictGroup : collidingTimeslots) {
            ++i;
            GRBLinExpr expr = new GRBLinExpr();
            for (TimeslotGroup timeslotGroup : conflictGroup) {
               expr.addTerm(1.0, variables.get(timeslotGroup));
            }
            GRBConstr cons = model.addConstr(expr, GRB.LESS_EQUAL, 1.0, "Time conflict " + timeConflict.getId() + ", constraint number " + i);
            conss.add(cons);
         }
         this.strictTimeConflictConstraints.put(timeConflict, conss);
      }

      // there is an implicit strict constraint for all stableRoomGroups
      for (StableRoomGroup srg : instance.getStableRoomGroups()) {
         Set<Set<TimeslotGroup>> collidingTimeslots = getCollidingTimeslots(srg.getEvents());

         // Create the conflicts
         int i = 0;
         Set<GRBConstr> conss = new HashSet();
         for (Set<TimeslotGroup> conflictGroup : collidingTimeslots) {
            ++i;
            GRBLinExpr expr = new GRBLinExpr();
            for (TimeslotGroup timeslotGroup : conflictGroup) {
               expr.addTerm(1.0, variables.get(timeslotGroup));
            }
            GRBConstr cons = model.addConstr(expr, GRB.LESS_EQUAL, 1.0, "Implicit conflict for stable room group" + srg.getId() + ", constraint number " + i);
            conss.add(cons);
         }
      }
   }

   public void createSoftTimeConflictConstraintsAndVariables() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      // TODO: make sure it is paid only once if a 2 timeslotevent overlaps

      this.softTimeConflictConstraints = new HashMap();
      this.softTimeConflictVariables = new HashMap();
      for (TimeConflict timeConflict : instance.getSoftTimeConflicts()) {
         Set<Set<TimeslotGroup>> collidingTimeslots = getCollidingTimeslots(timeConflict.getEvents());

         int i = 0;
         Set<GRBConstr> conss = new HashSet();
         Set<GRBVar> vars = new HashSet();
         for (Set<TimeslotGroup> conflictGroup : collidingTimeslots) {
            ++i;
            GRBVar var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "Time conflict " + timeConflict.getId() + ", variable number " + i);
            vars.add(var);

            model.update();

            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(-1.0, var);
            for (TimeslotGroup possibleTimeslot : conflictGroup) {
               expr.addTerm(1.0, variables.get(possibleTimeslot));
            }
            GRBConstr cons = model.addConstr(expr, GRB.LESS_EQUAL, 1.0, "Time conflict " + timeConflict.getId() + ", constraint number " + i);
            conss.add(cons);
         }
         this.softTimeConflictConstraints.put(timeConflict, conss);
         this.softTimeConflictVariables.put(timeConflict, vars);
      }

   }

   public void setObjective() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      GRBLinExpr obj = new GRBLinExpr();
      for (Map.Entry<TimeslotGroup, GRBVar> var : variables.entrySet()) {
         //obj.addTerm(var.getKey().getWeight(), var.getValue());
        obj.addTerm(1.0, var.getValue());
      }

//      for (Map.Entry<TimeConflict, Set<GRBVar>> vars : this.softTimeConflictVariables.entrySet()) {
//         for (GRBVar var : vars.getValue()) {
//            obj.addTerm(vars.getKey().getWeight(), var);
//         }
//      }

      model.setObjective(obj, GRB.MAXIMIZE);
      model.update();
   }

   public void addHyperHallSeperator() {
      hyperHallSeperator = new HyperHallSeparator(env, model, instance, variables);
      model.setCallback(hyperHallSeperator);
   }

   public void solve() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      long startTime = System.nanoTime();

      model.optimize();

      System.out.println("Total time spend for optimization: " + ((double)(System.nanoTime() - startTime) / 1E9));
      System.out.println("Total time spend in Separator: " + ((double)(HyperHallSeparator.getTimeSpend()) / 1E9));
   }

   public Map<Event, TimeslotGroup> getSolution() throws GRBException {
      Map<Event, TimeslotGroup> assignedTimeslots = new HashMap();
      for (Map.Entry<TimeslotGroup, GRBVar> variable : variables.entrySet()) {
         if (variable.getValue().get(GRB.DoubleAttr.X) > 0.5) {
            assignedTimeslots.put(variable.getKey().getEvent(), variable.getKey());
         }
      }
      return assignedTimeslots;
   }

   public GRBModel getModel() {
      return model;
   }

   public Map<TimeslotGroup, GRBVar> getVariables() {
      return variables;
   }

   public Map<Event, GRBConstr> getBoundEventOccurenceConstraints() {
      return boundEventOccurenceConstraints;
   }

   public Map<TimeConflict, Set<GRBConstr>> getStrictTimeConflictConstraints() {
      return strictTimeConflictConstraints;
   }

   public Map<TimeConflict, Set<GRBConstr>> getSoftTimeConflictConstraints() {
      return softTimeConflictConstraints;
   }
}

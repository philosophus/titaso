package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
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

   public ColoringSolver(Instance instance) {
      this.instance = instance;

      calculatePossibleTimeslotsAt();
   }

   private void calculatePossibleTimeslotsAt() {
      possibleTimeslotsAt = new HashMap();
      for (Event event : instance.getEvents()) {
         for (TimeslotGroup possibleTimeslot : event.getPossibleTimeslots()) {
            for (Timeslot timeslot : possibleTimeslot.getTimeslots()) {
               if (!possibleTimeslotsAt.containsKey(timeslot)) {
                  possibleTimeslotsAt.put(timeslot, new HashSet());
               }
               possibleTimeslotsAt.get(timeslot).add(possibleTimeslot);
            }
         }
      }
   }

   public void setUp(String logfilename) throws GRBException {
      assert (logfilename != null);
      assert (logfilename.length() >= 1);

      env = new GRBEnv(logfilename);
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
         GRBConstr constraint = model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, consName);
         this.boundEventOccurenceConstraints.put(event, constraint);
      }
   }

   private Set<Set<TimeslotGroup>> getCollidingTimeslots(TimeConflict timeConflict) {
      Set<Set<TimeslotGroup>> collidingTimeslots = new HashSet();
      for (Timeslot timeslot : instance.getTimeslots()) {
         Set<TimeslotGroup> conflictGroup = new HashSet();
         for (TimeslotGroup possibleTimeslot : this.possibleTimeslotsAt.get(timeslot)) {
            if (timeConflict.getEvents().contains(possibleTimeslot.getEvent())) {
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
         Set<Set<TimeslotGroup>> collidingTimeslots = getCollidingTimeslots(timeConflict);

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
   }

   public void createSoftTimeConflictConstraintsAndVariables() throws GRBException {
      assert (env != null);
      assert (model != null);
      assert (variables != null);

      this.softTimeConflictConstraints = new HashMap();
      this.softTimeConflictVariables = new HashMap();
      for (TimeConflict timeConflict : instance.getSoftTimeConflicts()) {
         Set<Set<TimeslotGroup>> collidingTimeslots = getCollidingTimeslots(timeConflict);

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
         obj.addTerm(var.getKey().getWeight(), var.getValue());
      }

      for (Map.Entry<TimeConflict, Set<GRBVar>> vars : this.softTimeConflictVariables.entrySet()) {
         for (GRBVar var : vars.getValue()) {
            obj.addTerm(vars.getKey().getWeight(), var);
         }
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

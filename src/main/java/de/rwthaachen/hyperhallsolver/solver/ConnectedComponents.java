package de.rwthaachen.hyperhallsolver.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class ConnectedComponents {

   private class Vertex {
      public Vertex(Object element) {
         this.element = element;
      }
      public Object element;
      public List<Vertex> neighbors = new ArrayList();
      public boolean mark = false;
   }

   private List<Vertex> vertices = new ArrayList();
   private Map<Object, Vertex> elementToVertex = new HashMap();
   private boolean dirty = false; // determines if cleanup necessary

   public void addVertices(Collection elements) {
      for (Object element : elements) {
         this.addVertex(element);
      }
   }

   public void addVertex(Object element) {
      Vertex vertex = new Vertex(element);
      this.vertices.add(vertex);
      this.elementToVertex.put(element, vertex);
   }

   public void addEdges(Object element, Collection<Object> neighbors) {
      Vertex vertex = this.elementToVertex.get(element);
      for (Object neighbor : neighbors) {
         vertex.neighbors.add(this.elementToVertex.get(neighbor));
      }
   }

   public void addEdge(Object element, Object neighbor) {
      this.elementToVertex.get(element).neighbors.add(this.elementToVertex.get(neighbor));
   }

   public List<Object> getConnectedComponent(Object element) {
      if (this.dirty) { // Clean up if necessary
         for (Vertex vertex : this.vertices) {
            vertex.mark = false;
         }
      }
      this.dirty = true; // It will be dirty after this function

      List<Object> connectedComponent = new LinkedList();
      Queue<Vertex> queue = new LinkedList();

      Vertex startVertex = this.elementToVertex.get(element);
      startVertex.mark = true;
      queue.add(startVertex);

      while (!queue.isEmpty()) {
         Vertex current = queue.remove();
         connectedComponent.add(current.element);

         for (Vertex neighbor : current.neighbors) {
            if (!neighbor.mark) {
               neighbor.mark = true;
               queue.add(neighbor);
            }
         }
      }

      return connectedComponent;
   }

}

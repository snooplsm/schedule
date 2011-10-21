package com.happytap.transit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.happytap.transit.App.Route;
import com.happytap.transit.App.Station;

public class TrainGraph {
	
	private class Edge {
		
		private Station source,target;
		private Route route;
		private Double weight;
		
		public Edge(Station source, Station target, Route route, Double weight) {
			this.source = source;
			this.target = target;
			this.route = route;
			this.weight = weight;
		}
		
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((route == null) ? 0 : route.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			result = prime * result
					+ ((target == null) ? 0 : target.hashCode());
			result = prime * result
					+ ((weight == null) ? 0 : weight.hashCode());
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Edge other = (Edge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (route == null) {
				if (other.route != null)
					return false;
			} else if (!route.equals(other.route))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			if (weight == null) {
				if (other.weight != null)
					return false;
			} else if (!weight.equals(other.weight))
				return false;
			return true;
		}



		private TrainGraph getOuterType() {
			return TrainGraph.this;
		}
		
		
	}
	
	private Map<Station, Set<Edge>> stationNeighbors = new HashMap<Station,Set<Edge>>();
	private Set<Edge> edges = new HashSet<Edge>();

	public void addVertex(Station station) {
		if(stationNeighbors.containsKey(station)) {
			return;
		} else {
			stationNeighbors.put(station, new HashSet<Edge>());
		}
	}
	
	public void addEdge(Station vertex, Station vertexNeighbor, Route edge, Double weight) {		
		_addEdge(vertex, vertexNeighbor, edge, weight);
	}
	
	private void _addEdge(Station vertex, Station vertexNeighbor, Route edge, Double weight) {		
		edges.add(new Edge(vertex,vertexNeighbor,edge, weight));
		//stationNeighbors.get(vertex).add(edgex);
	}
	
	public List<Edge> findShortestPath(Station from, Station to) {
		List<Station> queue = new LinkedList<Station>();
		queue.add(from);
		Set<Station> marked = new HashSet<Station>();
		while(!queue.isEmpty()) {
			Station s = queue.remove(0);
			for(Route r : s.routes) {
				for(Station station : r.stations) {
					if(station.equals(to)) {
						System.out.println("found");
					}
					if(!marked.contains(station)) {
						marked.add(station);
						queue.add(station);
					}
				}
			}
		}
//		for(Route route : from.routes) {
//			List<Route> queue = new LinkedList<Route>();
//			Set<Route> marked = new HashSet<Route>();
//			queue.add(route);
//			marked.add(route);
//			while(queue.isEmpty()) {
//				
//			}
//			
//		}
		
		return null;
		
	}
}

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
		private Station station;
		private Route route;
		
		
		public Edge(Station station, Route route) {
			super();
			this.station = station;
			this.route = route;
		}
		
		public String toString() {
			return station + " - " + route;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((route == null) ? 0 : route.hashCode());
			result = prime * result
					+ ((station == null) ? 0 : station.hashCode());
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
			if (station == null) {
				if (other.station != null)
					return false;
			} else if (!station.equals(other.station))
				return false;
			return true;
		}
		private TrainGraph getOuterType() {
			return TrainGraph.this;
		}
		
		
	}
	
	private Map<Station, Set<Edge>> stationNeighbors = new HashMap<Station,Set<Edge>>();

	public void addVertex(Station station) {
		if(stationNeighbors.containsKey(station)) {
			return;
		} else {
			stationNeighbors.put(station, new HashSet<Edge>());
		}
	}
	
	public void addEdge(Station vertex, Station vertexNeighbor, Route edge) {		
		_addEdge(vertex, vertexNeighbor, edge);
		_addEdge(vertexNeighbor,vertex, edge);
	}
	
	private void _addEdge(Station vertex, Station vertexNeighbor, Route edge) {
		Set<Edge> neighbors = stationNeighbors.get(vertex);
		if(neighbors==null) {
			neighbors = new HashSet<Edge>();
			stationNeighbors.put(vertex,neighbors);
		}
		neighbors.add(new Edge(vertexNeighbor,edge));
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

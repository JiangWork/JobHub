package org.smartframework.jobhub.example.xmlparser.object;

import java.util.List;

public class FlightsInfo {
	
	private String source;
	private String updated;
	private List<Flights> flights;
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}
	public List<Flights> getFlights() {
		return flights;
	}
	public void setFlights(List<Flights> flights) {
		this.flights = flights;
	}
	
	
}

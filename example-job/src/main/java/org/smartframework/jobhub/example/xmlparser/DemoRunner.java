package org.smartframework.jobhub.example.xmlparser;

import org.smartframework.jobhub.common.ProgressReporter;
import org.smartframework.jobhub.common.Progressable;
import org.smartframework.jobhub.example.xmlparser.object.FlightsInfo;

public class DemoRunner implements Progressable {
	
	private ProgressReporter reporter;
	
	/**
	 * args[0] = xml file
	 * args[1] = mapping file
	 * @param args
	 * @throws ParsingException 
	 */
	public void parse(String[] args) throws ParsingException {
		if (args.length < 2) {
			throw new ParsingException("Lack parameters: xml and mapping file should be provided.");
		}
		String xmlFile = args[0];
		String mappingFile = args[1];
		reporter.report(10);
		CastorXmlParser<FlightsInfo> parser = new CastorXmlParser<FlightsInfo>(mappingFile);
		// simulate a time-consuming task
		for (int i = 10; i <= 100; i += 10) {
			FlightsInfo info = parser.parse(xmlFile);
			System.out.println("Time " + i + ", content:" + info.getSource());
			reporter.report(i);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//ignored
			}
		}
	}

	@Override
	public void setReporter(ProgressReporter reporter) {
		this.reporter = reporter;
	}
	
	public static void main(String[] args) throws ParsingException {
		DemoRunner runner = new DemoRunner();
		runner.setReporter(new ProgressReporter() {

			@Override
			public void report(int percent) {
				System.out.println("Report progress: " + percent);
				
			}
			
		});
		runner.parse(new String[]{"flights.xml", "flights-mapping.xml"});
	}
}

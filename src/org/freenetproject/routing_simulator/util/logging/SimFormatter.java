package org.freenetproject.routing_simulator.util.logging;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SimFormatter extends Formatter {
	// This method is called for every log records
		@Override
		public String format(LogRecord rec) {
			StringBuffer buf = new StringBuffer(1000);
			
			buf.append(rec.getLevel());
			buf.append(' ');
			buf.append(rec.getThreadID());
			buf.append(' ');
			buf.append(rec.getSourceClassName());
			buf.append(' ');
			buf.append(rec.getSourceMethodName());
			buf.append(' ');
			buf.append(new Date(rec.getMillis()));
			buf.append('\n');
			
			buf.append('\t');
			buf.append(formatMessage(rec).replaceAll("\\r\\n|\\r|\\n", "\n\t"));
			buf.append('\n');
			
			return buf.toString();
		}
}

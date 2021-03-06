package EAnalysis.BinPacking;

import java.util.Comparator;
import java.util.TreeSet;

public class IncreasingPeriodComparator implements Comparator<ProcessingLoad> {

	public int compare(ProcessingLoad t0, ProcessingLoad t1) {

		if (t0.getUniqueID() == t1.getUniqueID())
			return 0;
		if (t0.getPeriod() > t1.getPeriod())
			return 1;
		if (t0.getPeriod() < t1.getPeriod())
			return -1;
		else
			return (int) (t0.getUniqueID() - t1.getUniqueID());
	}

	// unit test
	public static void main(String args[]) {
		// SoftwareNode(long cycles, long period, long deadline, String name)
		SoftwareNode t0 = new SoftwareNode(1, 1, 1, "t0");
		SoftwareNode t1 = new SoftwareNode(2, 2, 2, "t1");

		TreeSet<ProcessingLoad> set = new TreeSet<ProcessingLoad>(new IncreasingPeriodComparator());
		set.add(t0);
		set.add(t1);

		for (ProcessingLoad l : set) {
			System.out.println("period: " + l.getPeriod() + " name:" + l.getName());
		}
	}
}

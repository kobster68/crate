package metrics;

import java.util.Scanner;

public class Main {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		System.out.println("1 - Code Structure Metrics | 2 - Testability Metrics");
		System.out.println("3 - Both | 4 - Exit");

		System.out.print("Enter your choice: ");

		String input = scanner.nextLine();
		int choice = Integer.parseInt(input);

		scanner.close();

		switch (choice) {
			case 1:
				CodeStructureMetrics.linesOfCode();
				CodeStructureMetrics.commentDensity();
				break;
			case 2:
				TestabilityMetrics.testCount();
				TestabilityMetrics.testCoverage();
				break;
			case 3:
				CodeStructureMetrics.linesOfCode();
				CodeStructureMetrics.commentDensity();
				TestabilityMetrics.testCount();
				TestabilityMetrics.testCoverage();
				break;
			case 4:
				System.out.println("Exiting the program.");
				break;

		}
	}
	
}
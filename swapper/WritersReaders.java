package swapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WritersReaders {
	static int W = 10;
	static int R = 10;

	static Swapper<Integer> library = new Swapper<>();

	static HashSet<Integer> emptySet = new HashSet<>();
	static HashSet<Integer> guard = new HashSet<>(Arrays.asList(0));
	static HashSet<Integer> writerAccess = new HashSet<>(Arrays.asList(1));
	static HashSet<Integer> readerAccess = new HashSet<>(Arrays.asList(2));

	static AtomicInteger readersWaiting = new AtomicInteger(0);
	static AtomicInteger writersWaiting = new AtomicInteger(0);
	static AtomicInteger reading = new AtomicInteger(0);
	static AtomicInteger writing = new AtomicInteger(0);

	static AtomicInteger sthToWrite = new AtomicInteger(0);


	private static class Writer implements Runnable {
		Writer() {}

		void write() {
			System.out.println("Writing");
			sthToWrite.incrementAndGet();
		}

		@Override
		public void run() {
			while (true) {
				try {
					library.swap(guard, emptySet);
					if (writing.get() + reading.get() > 0) {
						writersWaiting.incrementAndGet();
						library.swap(emptySet, guard);
						library.swap(writerAccess, emptySet);
						writersWaiting.decrementAndGet();
					}
					writing.incrementAndGet();
					library.swap(emptySet, guard);

					write();

					library.swap(guard, emptySet);
					writing.decrementAndGet();

					if (readersWaiting.get() > 0)
						library.swap(emptySet, readerAccess);
					else if (writersWaiting.get() > 0)
						library.swap(emptySet, writerAccess);
					else
						library.swap(emptySet, guard);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class Reader implements Runnable {
		Reader() {
		}

		void read() {
			System.out.println("Reading");
		}

		@Override
		public void run() {
			while (true) {
				try {
					library.swap(guard, emptySet);

					if (writing.get() + writersWaiting.get() > 0) {
						readersWaiting.incrementAndGet();
						library.swap(emptySet, guard);
						library.swap(readerAccess, emptySet);
						readersWaiting.decrementAndGet();
					}
					reading.incrementAndGet();

					if (readersWaiting.get() > 0)
						library.swap(emptySet, readerAccess);
					else
						library.swap(emptySet, guard);

					read();
					library.swap(guard, emptySet);

					reading.decrementAndGet();

					if ((reading.get() == 0) && (writersWaiting.get() > 0))
						library.swap(emptySet, writerAccess);
					else
						library.swap(emptySet, guard);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		List<Thread> writers = new ArrayList<>();
		List<Thread> readers = new ArrayList<>();

		for (int i = 0; i < W; i++) writers.add(new Thread(new Writer()));
		for (int i = 0; i < R; i++) readers.add(new Thread(new Reader()));

		try {
			library.swap(emptySet, guard);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (Thread p : writers) p.start();
		for (Thread c : readers) c.start();
	}
}

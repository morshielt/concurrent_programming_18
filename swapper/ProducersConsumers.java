package swapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class ProducersConsumers {
	static int bufferSize = 5;
	static int P = 5;
	static int C = 10;
	static final int PRODUCT = 1;
	static final int EMPTY = 0;

	static Swapper<Integer> factory = new Swapper<>();

	static HashSet<Integer> emptySet = new HashSet<>();
	static HashSet<Integer> guard = new HashSet<>(Arrays.asList(0));
	static HashSet<Integer> noProducts = new HashSet<>(Arrays.asList(1));
	static HashSet<Integer> noSpace = new HashSet<>(Arrays.asList(2));

	static AtomicInteger productsAvailable = new AtomicInteger(0);
	static AtomicInteger producerIndex = new AtomicInteger(0);
	static AtomicInteger consumerIndex = new AtomicInteger(0);
	static AtomicIntegerArray buffer = new AtomicIntegerArray(bufferSize);

	private static class Producer implements Runnable {
		Producer() {}

		int index() {
			return producerIndex.incrementAndGet();
		}

		int produce() {
			System.out.println("Producing");
			return PRODUCT;
		}

		@Override
		public void run() {
			int k = index() % bufferSize;
			int p;
			while (true) {
			    try {
                    p = produce();
                    if (productsAvailable.get() == bufferSize)
                        factory.swap(noSpace, emptySet);

                    buffer.getAndSet(k, p);
                    productsAvailable.incrementAndGet();

                    if (productsAvailable.get() == 1) {
                        factory.swap(emptySet, noProducts);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
			}
		}
	}

	private static class Consumer implements Runnable {
		Consumer() {}

		int index() {
			return consumerIndex.incrementAndGet();
		}

		void consume(int p) {
			System.out.println("Consuming");
		}

		@Override
		public void run() {
			int p = EMPTY;
			int k = index() % bufferSize;

			while (true) {
			    try {
                    if (productsAvailable.get() == 0)
                        factory.swap(noProducts, emptySet);

                    p = buffer.getAndSet(k, p);
                    productsAvailable.decrementAndGet();

                    if (productsAvailable.get() == bufferSize - 1) {
                        factory.swap(emptySet, noSpace);
                    }
                    consume(p);
                }
                catch (InterruptedException e) {
			        e.printStackTrace();
                }
            }
		}
	}

	public static void main(String[] args) {
		List<Thread> producers = new ArrayList<>();
		List<Thread> consumers = new ArrayList<>();

		for (int i = 0; i < P; i++) producers.add(new Thread(new Producer()));
		for (int i = 0; i < C; i++) consumers.add(new Thread(new Consumer()));

		for (Thread p : producers) p.start();
		for (Thread c : consumers) c.start();
	}
}


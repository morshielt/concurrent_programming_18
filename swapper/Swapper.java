package swapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

public class Swapper<E> {
	private HashSet<E> set = new HashSet<>();
	private HashMap<Collection<E>, WaitingRoom> waitingRoom = new HashMap<>();
	private ReentrantLock guard = new ReentrantLock(true);
	private HashSet<E> copy;

	public Swapper() {}

	public void swap(Collection<E> removed, Collection<E> added) throws InterruptedException {

		guard.lockInterruptibly();
		while (!set.containsAll(removed)) {
			try {
				if (!waitingRoom.containsKey(removed)) {
					waitingRoom.put(removed, new WaitingRoom(guard.newCondition()));
				}
				waitingRoom.get(removed).newThreadIsWaiting();
			}
			catch (InterruptedException e) {
				wakeNextThread();
				guard.unlock();
				throw new InterruptedException();

			}
			finally {
				if (waitingRoom.containsKey(removed) && !waitingRoom.get(removed).isAnyoneWaiting()) {
					waitingRoom.remove(removed);
				}
			}
		}

		try {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			copy = new HashSet<>(set);
			set.removeAll(removed);
			set.addAll(added);
		}
		catch (InterruptedException e) {
			wakeNextThread();
			guard.unlock();
			throw new InterruptedException();
		}

		try {
			if (Thread.currentThread().isInterrupted()) {
				set = copy;
				throw new InterruptedException();
			}
		}
		catch (InterruptedException e) {
			throw new InterruptedException();
		}
		finally {
			wakeNextThread();
			guard.unlock();
		}
	}

	private void wakeNextThread() {
		for (Collection<E> key : waitingRoom.keySet()) {
			if (set.containsAll(key)) {
				waitingRoom.get(key).wakeUp();
				break;
			}
		}
	}
}

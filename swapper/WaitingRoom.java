package swapper;

import java.util.concurrent.locks.Condition;

public class WaitingRoom {
	private int howManyAreWaiting = 0;
	private Condition wait;

	public WaitingRoom (Condition c) {
		wait = c;
	}

	public void newThreadIsWaiting() throws InterruptedException {
		howManyAreWaiting++;
		wait.await();
	}

	public void wakeUp() {
		howManyAreWaiting--;
		wait.signal();
	}

	public boolean isAnyoneWaiting() {
		return howManyAreWaiting > 0;
	}
}

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class Driver {
	public static void main(String[] args) {

		Consumer<String> output = s -> System.out.println(Thread.currentThread().getName() + ": " + s);

		problemOne(output);

		problemTwo(output);
	}

	public abstract static class Guest {
		private Runnable whenCalled;
		private Thread thread;
		private boolean running = true;
		private String name;

		public Guest(String name) {
			this.name = name;
		}

		protected String name() {
			return this.name;
		}

		protected abstract void whenCalled();

		protected void start() {
			Thread mainThread = Thread.currentThread();
			this.thread = new Thread(() -> {
				while (this.running) {
					try {
						Thread.sleep(999999);
					} catch (InterruptedException e) {
						// Thread.interrupted();
					}
					this.whenCalled();
					mainThread.interrupt();
				}
			}, this.name);
			this.thread.start();
		}

		public void call() {
			this.thread.interrupt();
		}
	}

	public static class P1Guest extends Guest {
		private boolean eatenCupcake = false;
		private Consumer<P1Guest> action;

		public P1Guest(Consumer<P1Guest> action, String name) {
			super(name);
			this.action = action;
			this.start();
		}

		@Override
		protected void whenCalled() {
			this.action.accept(this);
		}

		public boolean eatenCupcake() {
			return this.eatenCupcake;
		}

		public void eatCupcake() {
			this.eatenCupcake = true;
		}

		@Override
		public String toString() {
			return this.name() + " [eatenCupcake=" + eatenCupcake + "]";
		}
	}

	private static void problemTwo(Consumer<String> output) {

	}

	private static void problemOne(Consumer<String> output) {
		Random rnd = new Random();
		AtomicBoolean running = new AtomicBoolean(true);
		AtomicBoolean partyOver = new AtomicBoolean(false);

		AtomicBoolean cupcakePresent = new AtomicBoolean(true);
		List<Guest> guests = createGuests(name -> new P1Guest(g -> {
			/*
			 * If this particular guest hasn't eaten a cupcake, and the cupcake is present,
			 * eat the cupcake!
			 */
			if (!g.eatenCupcake() && cupcakePresent.get()) {
				output.accept("Yum yum, cupcakes are tasty!");
				g.eatCupcake();
				cupcakePresent.set(false);
			}
		}, name), 10);

		// Special guest 0
		AtomicInteger count = new AtomicInteger(1);
		guests.add(0, new P1Guest(g -> {
			if (!g.eatenCupcake() && cupcakePresent.get()) {
				// If guest 0 hasn't eaten the cupcake, and the cupcake is there, we eat it, and
				// call up another, so leaving the cupcake present.
				g.eatCupcake();
			}
			output.accept("Count is currently " + count.get() + " out of " + guests.size());
			if (!cupcakePresent.get()) {
				cupcakePresent.set(true);
				int tmp = count.incrementAndGet();
				if (tmp >= guests.size()) {
					partyOver.set(true);
				}
			}
		}, "Guest 0"));

		/*
		 * At this point, all guests are waiting to be called into the maze
		 */

		while (!partyOver.get()) {
			// Let a random guest through the maze
			int guest = rnd.nextInt(guests.size());
			// output.accept("Letting guest " + guest + " through maze...");
			guests.get(guest).call();
			try {
				Thread.sleep(999999);
			} catch (InterruptedException e) {
				// Guest is done! Ready to grab the next one...
			}
		}

		output.accept("Party over! Status of each guest:");
		for (Guest g : guests)
			output.accept(g.toString());
	}

	private static List<Guest> createGuests(Function<String, Guest> src, int count) {
		List<Guest> guests = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			guests.add(src.apply("Guest " + (i + 1)));
		return guests;
	}
}

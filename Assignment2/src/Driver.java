import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main class for the assignment. Compile and run this to run the application.
 * 
 * @author Tatyana Binns
 */
public class Driver {
	/**
	 * Entry point to the program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		/**
		 * Simple output handler, uses the thread name for pretty printing later
		 */
		Consumer<String> output = s -> System.out.println(Thread.currentThread().getName() + ": " + s);

		/**
		 * Runs the first problem example. Uses the simple method of Guest-0 counting
		 * using the cupcake as a flag.
		 */
		System.out.println("===Problem 1===");
		problemOne(output);

		/**
		 * Uses the 'lock-free' method of handling it. Each guest tries to get in, and
		 * if they don't succeed, tries again. Each guest will go back in and try to
		 * look again if
		 */
		System.out.println("\n\n===Problem 2===");
		problemTwo(output);
	}

	/**
	 * Wrapper class that holds various fields that all guests need.
	 * 
	 * @author Tatyana Binns
	 */
	public abstract static class Guest {
		protected Thread thread;
		protected boolean running = true;
		private String name;

		public Guest(String name) {
			this.name = name;
		}

		protected String name() {
			return this.name;
		}

		protected abstract void whenCalled();

		protected abstract void start();

		public void call() {
			this.thread.interrupt();
		}

		public void leaveParty() {
			this.running = false;
			this.thread.interrupt();
		}
	}

	/**
	 * Implementation of Guest for Problem 2
	 */
	public static class P2Guest extends Guest {
		private Consumer<P2Guest> action;

		public P2Guest(Consumer<P2Guest> action, String name) {
			super(name);
			this.action = action;
			this.start();
		}

		private boolean seenVase = false;

		public boolean seenVase() {
			return this.seenVase;
		}

		/**
		 * Loops as long as this guest hasn't seen the vase. Sleeps for 10ms at a time
		 * until this happens.
		 */
		public void await() {
			while (true) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (this.seenVase())
					break;
			}
		}

		public void seeVase() {
			this.seenVase = true;
		}

		@Override
		protected void whenCalled() {
			this.action.accept(this);
		}

		@Override
		public String toString() {
			return this.name() + " [seenVase=" + seenVase + "]";
		}

		@Override
		protected void start() {
			this.thread = new Thread(() -> {
				while (this.running)
					this.action.accept(this);
			}, this.name());
			this.thread.start();
		}
	}

	/**
	 * Implementation of Guest for Problem 1
	 */
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

		@Override
		/**
		 * Starts a thread that will call {@link #whenCalled()} whenever this thread is
		 * interrupted, then interrupts the thread which started it in return. Used so
		 * one can call back and forth.
		 */
		protected void start() {
			Thread mainThread = Thread.currentThread();
			this.thread = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(999999);
					} catch (InterruptedException e) {
						// Thread.interrupted();
					}
					if (!this.running)
						break;
					this.whenCalled();
					mainThread.interrupt();
				}
			}, this.name());
			this.thread.start();
		}
	}

	/**
	 * Simulation of Problem 2
	 * 
	 * @param output the function to call with text output
	 */
	private static void problemTwo(Consumer<String> output) {

		AtomicReference<P2Guest> viewer = new AtomicReference<>(null);

		List<P2Guest> guests = createGuests(name -> new P2Guest(g -> {
			try {
				/*
				 * Try to see the vase, if we managed, look awhile
				 */
				if (viewer.compareAndSet(null, g)) {
					output.accept("Looking at the vase");
					g.seeVase();
					/*
					 * Get a good look
					 */
					Thread.sleep(ThreadLocalRandom.current().nextLong(100));
					/*
					 * Leave the room, and unlock the door
					 */
					output.accept("Content with the viewing experience");
					viewer.compareAndSet(g, null);
				}
			} catch (InterruptedException e) {
				// Ignore extra sleeps
			}
		}, name), 10);

		guests.forEach(P2Guest::await);
		output.accept("Party over! Status of each guest:");
		for (Guest g : guests)
			output.accept(g.toString());

		guests.forEach(Guest::leaveParty);
	}

	/**
	 * Simulation of Problem 1
	 * 
	 * @param output the function to call with text output
	 */
	private static void problemOne(Consumer<String> output) {
		Random rnd = new Random();
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

		guests.forEach(Guest::leaveParty);
	}

	/**
	 * Simple helper function to make a list of guests
	 * 
	 * @param <T>   the particular type of guest
	 * @param src   a function to create guests based on a name
	 * @param count the number of guests to create
	 * @return a list of the created guests
	 */
	private static <T extends Guest> List<T> createGuests(Function<String, T> src, int count) {
		List<T> guests = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			guests.add(src.apply("Guest " + (i + 1)));
		return guests;
	}
}

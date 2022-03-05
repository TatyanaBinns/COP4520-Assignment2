# COP4520 Assignment2

## Usage
To compile and run, first ensure you have a properly setup and installed JDK with access to the `javac` and `java` commands. Then:
* Clone this repository
* Open a terminal
* Navigate to wherever you cloned the repository, then to Assignment1/src
* Run `javac Driver.java`
* Run `java Driver`

## Efficiency

### Problem One
In terms of efficiency, this isn't great. Given the lack of means of communication between the running threads however, it is one of the most effective options. Relying on Guest 0 to count up all the other visits through the maze results in potentially a very large runtime, as on average, for every N guests, N guests must visit to count 1/N, resulting in an average runtime on the order of N^2, potentially much longer, or shorter.

### Problem Two

In this problem, the efficiency really depends mostly on how long people spend looking at the vase, how often they want to go back in, and other factors. In principal, the idea of using a lock-free method should be one of the most efficient methods, though this particular one may need additional tweaking to be as efficient as possible. 

## Problem One Correctness

This method is not super efficient, but is correct. It ensures that every guest will only ever eat the cupcake once, and guest 0 eats the cupcake once, (as long as it was already there), then counts the number of times they find the cupcake missing. That guest then calls the party complete after counting n-1 missing cupcakes, ensuring that everyone has had it.

## Problem Two Options

As far as option one goes, the good thing about this method, is that it doesn't have any guest wait around for a long time, assuming that it doesn't take too much time to try the door and find it locked. It doesn't do a good job of ensuring that all guests will see the vase, as guests going back multiple times to see the vase will make it harder for other people to see it, but that is a problem with all options.

The difference between option two and one is that option one uses the door being locked as the flag, and options two uses a sign. This can be faster if the sign is large, obvious, and easy to look at. This can be slower if the sign takes more time to read and update than just locking and unlocking the door.

Option three has the issue that it involves guests waiting around, for a potentially undefined amount of time. In this case it could be more efficient, but does need a fairly efficient person-to-person communication method, something that may not always be present. If that is around, creating a queue to iterate over can be faster.



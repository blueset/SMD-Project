package strategies;

import automail.IMailDelivery;
import automail.Robot;
import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;

public class Automail {

    public Robot robot1, robot2;
    public IMailPool mailPool;

    public Automail(IMailDelivery delivery) {
        // Swap between simple provided strategies and your strategies here

        /* Initialize the MailPool */

        //// Swap the next line for the one below
        mailPool = new WeakStrongMailPool();

        /* Initialize the RobotAction */
        boolean weak = false;  // Can't handle more than 2000 grams
        boolean strong = true; // Can handle any weight that arrives at the building

        //// Swap the next two lines for the two below those
        IRobotBehaviour robotBehaviourW = new MyRobotBehaviour(weak);
        IRobotBehaviour robotBehaviourS = new MyRobotBehaviour(strong);

        /* Initialize robot */
        robot1 = new Robot(robotBehaviourW, delivery, this, weak); /* shared behaviour because identical and stateless */
        robot2 = new Robot(robotBehaviourS, delivery, this, strong);
    }

    public void step() throws ExcessiveDeliveryException, ItemTooHeavyException {
        robot1.step();
        robot2.step();
    }

    public void priorityArrival(int priority, int weight) {
        robot1.behaviour.priorityArrival(priority, weight);
        robot2.behaviour.priorityArrival(priority, weight);
    }

}

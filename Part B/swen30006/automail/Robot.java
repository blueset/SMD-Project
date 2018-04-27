package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IRobotBehaviour;

import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot {

    /**
     * Possible states the robot can be in
     */
    public enum RobotState {
        DELIVERING, WAITING, RETURNING
    }

    /**
     * Types of robot.
     */
    public enum RobotType {
        WEAK, STRONG, BIG
    }

    /**
     * Roles of robot
     */
    public enum RobotRole {
        LOWER, UPPER
    }

    public final StorageTube tube;
    public final IRobotBehaviour behaviour;
    public final IMailDelivery delivery;
    protected final String id;
    private RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private RobotType type;
    private RobotRole role;
    private int deliveryCounter;

    static int count = 0;
    static Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     *
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery  governs the final delivery
     * @param automail  is the automail system the robot belongs to
     * @param type      is the type of robot
     */
    public Robot(IRobotBehaviour behaviour, IMailDelivery delivery, RobotType type) {
        id = "R" + hashCode();
        // current_state = RobotState.WAITING;
        current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        tube = new StorageTube(type);
        this.behaviour = behaviour;
        this.delivery = delivery;
        this.type = type;
        this.deliveryCounter = 0;
        behaviour.setRobot(this);
    }

    /**
     * This is called on every time step
     *
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException, ItemTooHeavyException {
        switch (current_state) {
            /* This state is triggered when the robot is returning to the mailroom after a delivery */
            case RETURNING:
                /* If its current position is at the mailroom, then the robot should change state */
                if (current_floor == Building.MAILROOM_LOCATION) {
                    changeState(RobotState.WAITING);
                } else {
                    /* If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                    break;
                }
            case WAITING:

                /* If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if (!tube.isEmpty()) {
                    deliveryCounter = 0; // reset delivery counter
                    behaviour.startDelivery();
                    setRoute();
                    changeState(RobotState.DELIVERING);
                }
                break;
            case DELIVERING:
                /* Check whether or not the call to return is triggered manually */
                if (current_floor == destination_floor) { // If already here drop off either way
                    /* Delivery complete, report this to the simulator! */
                    delivery.deliver(tube.pop());
                    boolean wantToReturn = behaviour.returnToMailRoom(tube);
                    deliveryCounter++;
                    if (deliveryCounter > tube.getMaximumCapacity()) {
                        throw new ExcessiveDeliveryException();
                    }
                    /* Check if want to return or if there are more items in the tube*/
                    if (wantToReturn || tube.isEmpty()) {
                        // if(tube.isEmpty()){
                        changeState(RobotState.RETURNING);
                    } else {
                        /* If there are more items, set the robot's route to the location to deliver the item */
                        setRoute();
                        changeState(RobotState.DELIVERING);
                    }
                } else {/*
	    			if(wantToReturn){
	    				changeState(RobotState.RETURNING);
	    			}
	    			else{*/
                    /* The robot is not at the destination yet, move towards it! */
                    moveTowards(destination_floor);
	                /*
	    			}
	    			*/
                }
                break;
        }
    }

    /**
     * Sets the route for the robot
     */
    private void setRoute() throws ItemTooHeavyException {
        /* Pop the item from the StorageUnit */
        if (type == RobotType.WEAK && tube.peek().weight > 2000)
            throw new ItemTooHeavyException();
        /* Set the destination floor */
        destination_floor = tube.getNextDestFloor();
    }

    /**
     * Generic function that moves the robot towards the destination
     *
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
        if (current_floor < destination) {
            current_floor++;
        } else {
            current_floor--;
        }
    }

    @Override
    public int hashCode() {
        Integer hash0 = super.hashCode();
        Integer hash = hashMap.get(hash0);
        if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
        return hash;
    }

    /**
     * Prints out the change in state
     *
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState) {
        if (current_state != nextState) {
            System.out.printf("T: %3d > %11s changed from %s to %s%n", Clock.Time(), id, current_state, nextState);
        }
        current_state = nextState;
        if (nextState == RobotState.DELIVERING) {
            System.out.printf("T: %3d > %11s-> [%s]%n", Clock.Time(), id, tube.peek().toString());
        }
    }

    public boolean atMailRoom() {
        return current_floor == Building.MAILROOM_LOCATION;
    }

    public RobotState getState() {
        return this.current_state;
    }

    public RobotType getType() {
        return this.type;
    }

    public void setRole(RobotRole role) {
        this.role = role;
    }

    public RobotRole getRole() {
        return this.role;
    }
}

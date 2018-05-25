package mycontroller;

import controller.CarController;
import javafx.geometry.Pos;
import swen30006.driving.Simulation;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;



public class MyAIController extends CarController {

    private static final float BRAKING_FORCE = 2f;
    private static final float ACCELERATION = 2f;
    private static final float FRICTION_FORCE = 0.5f;

    /** Angle threshold to detect U-turns */
    private static final float U_TURN_THRESHOLD = 120.0f;

    /** Threshold for maximum addition of length for alternative path that includes the next key needed to collect **/
    private static final int MAX_EXTRA_LEN_ALT_PATH = 10;

    private Pipeline<RoutingData, MyAIController> pathPlanner;
    private StrategyManager strategyManager;


    private int lastX = -1;
    private int lastY = -1;




    MapRecorder mapRecorder;

    private RoutingData routingData;


	public MyAIController(Car car) {

        super(car);

        mapRecorder = new MapRecorder(getMap(), getKey());
        mapRecorder.addCarView(Math.round(getX()), Math.round(getY()), getView(), getKey());

        pathPlanner = new Pipeline<>();
        pathPlanner.appendStep(new AStar());
        pathPlanner.appendStep(new AddDestinationPair());
        pathPlanner.appendStep(new AvoidWall());
        pathPlanner.appendStep(new RemoveRedundantPath());
        pathPlanner.appendStep(new SimplifyPath());

        strategyManager = new StrategyManager();

	}

    private void calculateTargets() {
        routingData = strategyManager.getTargets(this);

        // targets changed, recalculate the route
        calculateRoute();
    }

	private void calculateRoute() {
	    if (routingData.targets == null) return;
	    // the last one should be the current position


        routingData = pathPlanner.execute(routingData, this);


    }


	@Override
	public void update(float delta) {

        int foundFlags = 0;

        if (strategyManager.update(this)) {
            foundFlags = MapRecorder.NEXT_KEY_FOUND; // Strategy changed
        }

        int currentX = Math.round(getX());
        int currentY = Math.round(getY());
        if (currentX!=lastX || currentY!=lastY) {
            foundFlags |= mapRecorder.addCarView(Math.round(getX()), Math.round(getY()), getView(), getKey());
            lastX = currentX;
            lastY = currentY;

        }
        if ((foundFlags & MapRecorder.NEXT_KEY_FOUND) != 0)
            calculateTargets(); // Recalculate targets
        else if ((foundFlags & MapRecorder.LAVA_FOUND) != 0 && routingData.path.size() != 0)
            calculateRoute(); // Reroute




        System.out.print("X: ");
        System.out.println(getX());
        System.out.print("Y: ");
        System.out.println(getY());

        if (routingData==null || routingData.path.isEmpty()) {
            calculateTargets();
        }

        if (!routingData.path.isEmpty()) {

            int targetX = Math.round(routingData.path.get(0).x);
            int targetY = Math.round(routingData.path.get(0).y);

            Simulation.flagList = routingData.path;


            if (currentX==targetX && currentY==targetY) {
                strategyManager.carMoved();
//                if (getSpeed()<=1.5) {
                routingData.path.remove(0);

                Coordinate currentCoord = new Coordinate(currentX, currentY);
                routingData.targets.remove(currentCoord);
                if (routingData.targetPairs.containsKey(currentCoord)) {
                    routingData.targetPairs.clear();
                }

//                } else {
//                    applyBrake();
//                }

                return;
            }

            Position currentPos = new Position(getX(), getY());
            float targetAngle = getAngleBetweenPos(currentPos, routingData.path.get(0));
            float dist = getTargetDistance(routingData.path.get(0));
            System.out.println(dist);
            System.out.println(targetAngle);
            System.out.println(getAngle());

            // ------
            float cmp = compareAngles(getAngle(), targetAngle);
            if (cmp!=0) {
                if (Math.abs(cmp) > U_TURN_THRESHOLD) {
                    // Trying to u-turn, turn to the side further to the wall.
                    int orientationAngle = (int) getAngle();
                    switch (getOrientation()) {
                        case EAST: orientationAngle = WorldSpatial.EAST_DEGREE_MIN; break;
                        case WEST: orientationAngle = WorldSpatial.WEST_DEGREE; break;
                        case SOUTH: orientationAngle = WorldSpatial.SOUTH_DEGREE; break;
                        case NORTH: orientationAngle = WorldSpatial.NORTH_DEGREE; break;
                    }
                    double xOffsetL = Math.cos(orientationAngle + 90),
                           yOffsetL = Math.sin(orientationAngle + 90),
                           xOffsetR = Math.cos(orientationAngle - 90),
                           yOffsetR = Math.sin(orientationAngle - 90);

                    MapRecorder.TileStatus leftTile = mapRecorder.mapStatus
                            [(int) Math.round(getX() + xOffsetL)]
                            [(int) Math.round(getY() + yOffsetL)];
                    MapRecorder.TileStatus rightTile = mapRecorder.mapStatus
                            [(int) Math.round(getX() + xOffsetR)]
                            [(int) Math.round(getY() + yOffsetR)];

                    if (leftTile == MapRecorder.TileStatus.UNREACHABLE) {
                        cmp = -1.0f;
                    } else if (rightTile == MapRecorder.TileStatus.UNREACHABLE) {
                        cmp = 1.0f;
                    }
                }

                if (cmp<0) {
                    turnRight(delta);
                } else {
                    turnLeft(delta);
                }
            }

            float endingSpeed = getAllowedEndingSpeed(currentPos);

            System.out.print("dist: ");
            System.out.println(dist);


            float allowedSpeed = 0f;
            if (Math.abs(cmp) < 40) {
                allowedSpeed = computeAllowedVelocity(dist, endingSpeed);
            } else {
                // big turn
                allowedSpeed = 0.3f;
            }


            System.out.print("speed: ");
            System.out.println(allowedSpeed);

            if (getSpeed()<allowedSpeed) {
                applyForwardAcceleration();
            } else if (getSpeed()>allowedSpeed) {
                applyBrake();
            }



        } else {

            if (getSpeed()>0) {
                applyBrake();
            }

        }




	}
    // v^2 - u^2 = 2as
	float computeAllowedVelocity(float s, float u) {
	    // this is weird but it is what happened in the Car class
	    float a = BRAKING_FORCE-FRICTION_FORCE;
	    return (float) Math.sqrt(2*a*s + u*u);
//        return 0.6f;
    }

    // returns 1 if otherAngle is to the right of sourceAngle,
    //         0 if the angles are identical
    //         -1 if otherAngle is to the left of sourceAngle

    // now return difference
    float compareAngles(float sourceAngle, float otherAngle) {
        // sourceAngle and otherAngle should be in the range -180 to 180
        float difference = otherAngle - sourceAngle;

        if(difference < -180.0f)
            difference += 360.0f;
        if(difference > 180.0f)
            difference -= 360.0f;

        return difference;
    }

    float getAllowedEndingSpeed(Position currentPos) {
	    if (routingData.path.size()<2) {
	        return 0.3f;
        }

        float angle1 = getAngleBetweenPos(currentPos, routingData.path.get(0));
        float angle2 = getAngleBetweenPos(routingData.path.get(0), routingData.path.get(1));
        if (Math.abs(angle1-angle2) < 45) {
            return 2;
        }
        return 0.4f;
    }

    public RoutingData getRoutingData() {
        return routingData;
    }

    private float getTargetDistance(Position target) {
        float target_x = target.x;// + 0.5f;
        float target_y = target.y;// + 0.5f;

        return (float) Math.sqrt(Math.pow(target_x - getX(),2)+Math.pow(target_y - getY(),2));
    }


    private float getAngleBetweenPos(Position pos1, Position pos2) {
        float angle = (float) Math.toDegrees(Math.atan2(pos2.y - pos1.y, pos2.x - pos1.x));

        if(angle < 0){
            angle += 360;
        }
        return angle;
    }


}

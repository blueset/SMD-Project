package mycontroller;

import controller.CarController;
import swen30006.driving.Simulation;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;


import java.util.ArrayList;
import java.util.Objects;


public class MyAIController extends CarController{

    private static final float BRAKING_FORCE = 2f;
    private static final float ACCELERATION = 2f;
    private static final float FRICTION_FORCE = 0.5f;
    private static final int[][] DIRECTS = new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}};
    private static final float PRECISION_LEVEL = 0.001f;

    /** Angle threshold to detect U-turns */
    private static final float U_TURN_THRESHOLD = 120.0f;

    /** Threshold for maximum addition of length for alternative path that includes the next key needed to collect **/
    private static final int MAX_EXTRA_LEN_ALT_PATH = 10;


    private int lastX = -1;
    private int lastY = -1;

//    String[] test_pos = new String[]{"3,3", "7,3", "7,11"};  // easy-map
//    String[] test_pos = new String[]{"26,2", "21,12", "2,12", "2,2", "26,2", "21,12", "2,12", "2,2"}; // test-key-map
//    String[] test_pos = new String[]{"23,3", "23,17"}; // narrow-road


    MapRecorder mapRecorder;
    ArrayList<Position> targetPositions = new ArrayList<>();


	public MyAIController(Car car) {

        super(car);

//	    for(String s: test_pos) {
//	        targetPositions.add(new Coordinate(s));
//        }

        mapRecorder = new MapRecorder(getMap(), getKey());
        mapRecorder.addCarView(Math.round(getX()), Math.round(getY()), getView());

//        AStar aStar = new AStar(mapRecorder, 2, 2, 21, 12);
//	    ArrayList<Node> path = aStar.start();
//	    for (Node n:path) {
//            targetPositions.add(avoidWall(n.coord.x, n.coord.y));
//        }

        // Use the simple random path to navigate the car to explore
        // the map
//        loadNextDestinationPoint();
	}

    /**
     * Load next destination point.
     * Return True if a point is found. False otherwise.
     */
	private boolean loadNextDestinationPoint() {
//        Coordinate firstPt = mapRecorder.getNearestExplorationPoint(getX(), getY());
//        if (firstPt == null) return false;
//        Simulation.flagX = firstPt.x;
//        Simulation.flagY = firstPt.y;
//
//        if (mapRecorder.mapStatus[firstPt.x][firstPt.y] == MapRecorder.TileStatus.UNREACHABLE) {
//            Simulation.flagText = "V";
//        } else {
//            Simulation.flagText = "X";
//        }

//        ArrayList<Node> path = new AStar(mapRecorder,
//                Math.round(getX()), Math.round(getY()),
//                firstPt.x, firstPt.y
//        ).start();
        ArrayList<Coordinate> destinations = mapRecorder.coordinatesToExplore();
        if (getKey() > 1 && mapRecorder.keysCoord[getKey() - 2] != null) { // If next key pos is known
            destinations.add(mapRecorder.keysCoord[getKey() - 2]);
            boolean allKeyFound = true;
            for (int i = getKey() - 2; i >= 0; i--)
                allKeyFound &= mapRecorder.keysCoord[i] != null;
            if (allKeyFound) {
                destinations.clear();
                for (int i = getKey() - 2; i >= 0; i--)
                    destinations.add(mapRecorder.keysCoord[i]);
            }
        } else if (getKey() == 1) {
            destinations = mapRecorder.finishCoords;
        }
        if (destinations.isEmpty()) return false;
        ArrayList<Node> path = getShortestPath(new Coordinate(Math.round(getX()), Math.round(getY())),
                destinations);

        targetPositions.clear();

        for (Node n: path) {
            targetPositions.add(avoidWall(n.coord.x, n.coord.y));
        }

        System.out.println(targetPositions.size());
        removeUselessPos(targetPositions);
        System.out.println(targetPositions.size());
        return true;
    }

    private ArrayList<Node> getShortestPath(Coordinate source, ArrayList<Coordinate> destinations) {
        return new AStar(mapRecorder, source, destinations).start();
//        ArrayList<Node> path = new AStar(mapRecorder, source, destinations).start();
//        if (getKey() > 1 && mapRecorder.keysCoord[getKey() - 2] != null) {
//            ArrayList<Coordinate> middlePoints = new ArrayList<>();
//            middlePoints.add(mapRecorder.keysCoord[getKey() - 2]);
//            ArrayList<Node> alternativePath = new AStar(mapRecorder, source, middlePoints).start();
//            alternativePath.addAll(new AStar(mapRecorder, middlePoints.get(0), destinations).start());
//            if (alternativePath.size() - path.size() < MAX_EXTRA_LEN_ALT_PATH)
//                path = alternativePath;
//        }
//        return path;
    }

    private void reRoute() {

	    int lastid = targetPositions.size() - 1;

//        ArrayList<Node> path = new AStar(mapRecorder,
//                Math.round(getX()), Math.round(getY()),
//                Math.round(targetPositions.get(lastid).x), Math.round(targetPositions.get(lastid).y)
//        ).start();
        ArrayList<Coordinate> destinations = new ArrayList<>();
        destinations.add(new Coordinate(Math.round(targetPositions.get(lastid).x), Math.round(targetPositions.get(lastid).y)));
        ArrayList<Node> path = getShortestPath(new Coordinate(Math.round(getX()), Math.round(getY())),
                destinations);

        targetPositions.clear();

        for (Node n: path) {
            targetPositions.add(avoidWall(n.coord.x, n.coord.y));
        }

        System.out.println(targetPositions.size());
        removeUselessPos(targetPositions);
        System.out.println(targetPositions.size());

    }

	private boolean floatEquals(float a, float b) {
	    return Math.abs(a-b) < PRECISION_LEVEL;
    }


	private Position avoidWall(int x, int y) {

	    int offsetX = 0;
	    int offsetY = 0;

	    MapRecorder.TileStatus[][] mapStatus = mapRecorder.getTileStatus();

	    for(int[] dir: DIRECTS) {
	        int newX = x + dir[0];
            int newY = y + dir[1];

            if(mapRecorder.inRange(newX, newY)) {
                if (mapStatus[newX][newY] == MapRecorder.TileStatus.UNREACHABLE) {
                    if (offsetX==0) offsetX = dir[0];
                    if (offsetY==0) offsetY = dir[1];
                }
            }
        }

        return new Position(x - 0.3f*offsetX, y - 0.3f*offsetY);
    }

    private void removeUselessPos(ArrayList<Position> positions) {
	    for (int i=0; i<positions.size()-2; i++) {
	        if (positions.get(i)==null) continue;;

	        for (int j=i+2; j<positions.size(); j++) {
                Position pos1 = positions.get(i);
                Position pos2 = positions.get(j);

                if (floatEquals(pos1.x, pos2.x) || floatEquals(pos1.y, pos2.y)) {
                    positions.set(j-1, null);
                } else {
                    break;
                }
            }
        }

        // clean up
        positions.removeIf(Objects::isNull);

    }

	@Override
	public void update(float delta) {

        int currentX = Math.round(getX());
        int currentY = Math.round(getY());

        if (currentX!=lastX || currentY!=lastY) {
            boolean lavaFound = mapRecorder.addCarView(Math.round(getX()), Math.round(getY()), getView());
            if (lavaFound && targetPositions.size()!=0) {
                reRoute();
            }
        }


        System.out.print("X: ");
        System.out.println(getX());
        System.out.print("Y: ");
        System.out.println(getY());

        if (!targetPositions.isEmpty() || loadNextDestinationPoint()) {

            int targetX = Math.round(targetPositions.get(0).x);
            int targetY = Math.round(targetPositions.get(0).y);

            Simulation.flagList = targetPositions;

//            if (Math.abs(targetX - getX())<=0.1 && Math.abs(targetY - getY())<=0.1) {
//                targetPositions.remove(0);
//                return;
//            }

            if (currentX==targetX && currentY==targetY) {
                targetPositions.remove(0);
                return;
            }


            float targetAngle = getTargetAngle(targetPositions.get(0));
            float dist = getTargetDistance(targetPositions.get(0));
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

            float endingSpeed = 0f;

            System.out.print("dist: ");
            System.out.println(dist);


            float allowedSpeed = 0f;
            if (Math.abs(cmp) < 60) {
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
    float compareAngles(float sourceAngle, float otherAngle)
    {
        // sourceAngle and otherAngle should be in the range -180 to 180
        float difference = otherAngle - sourceAngle;

        if(difference < -180.0f)
            difference += 360.0f;
        if(difference > 180.0f)
            difference -= 360.0f;

        return difference;
    }



    private float getTargetDistance(Position target) {
        float target_x = target.x;// + 0.5f;
        float target_y = target.y;// + 0.5f;

        return (float) Math.sqrt(Math.pow(target_x - getX(),2)+Math.pow(target_y - getY(),2));
    }


    private float getTargetAngle(Position target) {
	    float target_x = target.x;// + 0.5f;
        float target_y = target.y;// + 0.5f;
        float angle = (float) Math.toDegrees(Math.atan2(target_y - getY(), target_x - getX()));

        if(angle < 0){
            angle += 360;
        }
        return angle;
    }



    public class Position {
        public float x;
        public float y;


        public Position(float x, float y){
            this.x = x;
            this.y = y;
        }

        public String toString(){
            return x+","+y;
        }

        /**
         * Defined in order to use it as keys in a hashmap
         */
        public boolean equals(Object c){
            if(c == this){
                return true;
            }
            if(!(c instanceof Position)){
                return false;
            }
            Position pos = (Position) c;
            return (pos.x == this.x) && (pos.y == this.y);
        }

        public int hashCode(){
            return Objects.hash(x,y);
        }

    }


}
